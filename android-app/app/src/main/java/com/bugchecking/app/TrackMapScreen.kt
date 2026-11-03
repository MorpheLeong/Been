package com.bugchecking.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.text.DateFormat
import java.util.Date
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val OPENFREEMAP_LIGHT_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val OPENFREEMAP_DARK_STYLE = "https://tiles.openfreemap.org/styles/dark"
private const val TRACK_ROUTE_SOURCE_ID = "bugchecking-track-routes"
private const val TRACK_POINT_SOURCE_ID = "bugchecking-track-points"
private const val TRACK_ROUTE_LAYER_ID = "bugchecking-track-route-line"
private const val TRACK_ID_PROPERTY = "trackId"
private const val FAVORITE_TIER_PROPERTY = "favoriteTier"
private const val TRACK_ROUTE_WIDTH = 4f
private const val TRACK_POINT_RADIUS = 9f
private const val TRACK_POINT_OUTLINE_WIDTH = 2.5f
private const val TRACK_FAVORITE_MARKER_SIZE_DP = 36f
private const val TRACK_FAVORITE_STAR_OUTER_RADIUS_DP = 14f
private const val TRACK_FAVORITE_STAR_INNER_RADIUS_DP = 6.2f

private const val TRACK_ORDINARY_POINT_LAYER_ID = "bugchecking-track-point-0"
private val trackFavoriteLayerIds = (1..3).map { "bugchecking-track-favorite-$it" }
private val trackFavoriteImageIds = (1..3).map { "bugchecking-track-favorite-star-$it" }
private val trackPointLayerIds = listOf(TRACK_ORDINARY_POINT_LAYER_ID) + trackFavoriteLayerIds

internal data class TrackDaySegment(val date: LocalDate, val tracks: List<TrackPoint>)

internal fun dailyTrackSegments(
    rows: List<TrackPoint>,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<TrackDaySegment> = rows
    .groupBy { Instant.ofEpochMilli(it.time).atZone(zoneId).toLocalDate() }
    .entries
    .sortedBy(Map.Entry<LocalDate, List<TrackPoint>>::key)
    .map { (date, tracks) -> TrackDaySegment(date, tracks.sortedBy(TrackPoint::time)) }

internal fun trackPointFeatureCollection(rows: List<TrackPoint>): FeatureCollection =
    FeatureCollection.fromFeatures(rows.map { track ->
        Feature.fromGeometry(Point.fromLngLat(track.lon, track.lat)).apply {
            addStringProperty(TRACK_ID_PROPERTY, track.id.toString())
            addNumberProperty(FAVORITE_TIER_PROPERTY, track.favoriteTier.coerceIn(0, 3))
        }
    })

internal fun trackRouteFeatureCollection(
    rows: List<TrackPoint>,
    zoneId: ZoneId = ZoneId.systemDefault(),
): FeatureCollection = FeatureCollection.fromFeatures(
    dailyTrackSegments(rows, zoneId)
        .filter { it.tracks.size > 1 }
        .map { day ->
            Feature.fromGeometry(
                LineString.fromLngLats(day.tracks.map { Point.fromLngLat(it.lon, it.lat) }),
            ).apply { addStringProperty("date", day.date.toString()) }
        },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackMapScreen(
    rows: List<TrackPoint>,
    onBack: () -> Unit,
    onLocateInTracks: (TrackPoint) -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val routeColor = MaterialTheme.colorScheme.tertiary.toArgb()
    val markerOutlineColor = MaterialTheme.colorScheme.surface.toArgb()
    val favoriteMarkerColors = (1..3).map { trackFavoriteMapColor(it).toArgb() }
    val displayMetrics = context.resources.displayMetrics
    val favoriteMarkerImages = remember(
        favoriteMarkerColors,
        markerOutlineColor,
        displayMetrics.density,
        displayMetrics.densityDpi,
    ) {
        favoriteMarkerColors.map { color ->
            createFavoriteMarkerBitmap(
                density = displayMetrics.density,
                densityDpi = displayMetrics.densityDpi,
                fillColor = color,
                outlineColor = markerOutlineColor,
            )
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).also { it.onCreate(Bundle()) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var selected by remember { mutableStateOf<TrackPoint?>(null) }
    var styleRevision by remember { mutableIntStateOf(0) }
    var styleAttempt by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val latestRows = rememberUpdatedState(rows)

    fun fitMap(current: MapLibreMap) {
        if (rows.isEmpty()) return
        if (rows.size == 1) {
            current.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(rows[0].lat, rows[0].lon), 15.0))
        } else {
            val bounds = LatLngBounds.Builder().also { builder -> rows.forEach { builder.include(LatLng(it.lat, it.lon)) } }.build()
            current.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 96))
        }
    }
    fun fitAll() { map?.let(::fitMap) }

    DisposableEffect(mapView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(styleAttempt, darkTheme) {
        loading = true
        error = null
        mapView.getMapAsync { ready ->
            map = ready
            ready.uiSettings.isAttributionEnabled = false
            ready.uiSettings.isLogoEnabled = false
            ready.setStyle(if (darkTheme) OPENFREEMAP_DARK_STYLE else OPENFREEMAP_LIGHT_STYLE) {
                loading = false
                error = null
                installTrackGeometryLayers(
                    style = it,
                    rows = latestRows.value,
                    routeColor = routeColor,
                    markerOutlineColor = markerOutlineColor,
                    favoriteMarkerImages = favoriteMarkerImages,
                )
                styleRevision++
                fitMap(ready)
            }
        }
        delay(12_000)
        if (loading) error = "底图加载失败或网络不可用"
    }

    LaunchedEffect(map, rows) {
        val current = map ?: return@LaunchedEffect
        if (loading) return@LaunchedEffect
        current.style?.let { style ->
            updateTrackGeometrySources(style, rows)
        }
    }

    LaunchedEffect(map, styleRevision, loading, routeColor, markerOutlineColor, favoriteMarkerImages) {
        val current = map ?: return@LaunchedEffect
        if (loading) return@LaunchedEffect
        current.style?.let { style ->
            updateTrackGeometryStyle(style, routeColor, markerOutlineColor, favoriteMarkerImages)
        }
    }

    val latestRowsById = rememberUpdatedState(rows.associateBy(TrackPoint::id))
    DisposableEffect(map) {
        val current = map
        if (current == null) return@DisposableEffect onDispose { }
        val hitSlopPx = 12f * context.resources.displayMetrics.density
        val clickListener = MapLibreMap.OnMapClickListener { coordinate ->
            val screenPoint = current.projection.toScreenLocation(coordinate)
            val hitArea = RectF(
                screenPoint.x - hitSlopPx,
                screenPoint.y - hitSlopPx,
                screenPoint.x + hitSlopPx,
                screenPoint.y + hitSlopPx,
            )
            val trackFeatures = current.queryRenderedFeatures(
                hitArea,
                *trackPointLayerIds.toTypedArray(),
            )
            val trackFeature = trackFeatures.firstOrNull()
            val trackId = trackFeature?.takeIf { it.hasProperty(TRACK_ID_PROPERTY) }
                ?.getStringProperty(TRACK_ID_PROPERTY)?.toLongOrNull()
            val track = trackId?.let(latestRowsById.value::get)
            if (track != null) {
                selected = track
                true
            } else {
                false
            }
        }
        current.addOnMapClickListener(clickListener)
        onDispose {
            current.removeOnMapClickListener(clickListener)
        }
    }

    PassportCornerScaffold(
        topStart = {
            PassportCornerAction(onClick = onBack, contentDescription = "返回轨迹列表") {
                Icon(Icons.Default.ArrowBack, null)
            }
        },
        topEnd = {
            PassportCornerAction(
                onClick = ::fitAll,
                enabled = rows.isNotEmpty(),
                contentDescription = "重新定位全部点位",
            ) { Icon(Icons.Default.CenterFocusStrong, null) }
        },
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
            if (rows.isEmpty()) {
                Card(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    shape = PassportTokens.shapes.cardShape,
                ) {
                    Text(
                        "还没有轨迹记录，地图已准备好。",
                        Modifier.padding(PassportTokens.content.cardHorizontalPadding),
                    )
                }
            }
            if (loading || error != null) {
                Card(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    shape = PassportTokens.shapes.cardShape,
                ) {
                    Column(
                        Modifier.padding(PassportTokens.content.cardHorizontalPadding),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(error ?: "正在加载地图…")
                        if (error != null) Button(
                            onClick = { styleAttempt++ },
                            shape = PassportTokens.shapes.buttonShape,
                        ) { Text("重试") }
                    }
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = PassportTokens.shapes.cardShape,
                tonalElevation = 1.dp,
                modifier = Modifier.align(Alignment.BottomStart)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                    )
                    .padding(8.dp),
            ) {
                Text(
                    "© OpenStreetMap contributors · OpenFreeMap",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
    selected?.let { track ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            Column(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(track.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    formatTrackTime(track),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        selected = null
                        onLocateInTracks(track)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = PassportTokens.shapes.buttonShape,
                ) {
                    Text("在轨迹记录中定位")
                }
            }
        }
    }
}

private fun formatTrackTime(track: TrackPoint): String = DateFormat.getDateTimeInstance().format(Date(track.time))

private fun installTrackGeometryLayers(
    style: Style,
    rows: List<TrackPoint>,
    routeColor: Int,
    markerOutlineColor: Int,
    favoriteMarkerImages: List<Bitmap>,
) {
    style.addSource(GeoJsonSource(TRACK_ROUTE_SOURCE_ID, trackRouteFeatureCollection(rows)))
    style.addSource(GeoJsonSource(TRACK_POINT_SOURCE_ID, trackPointFeatureCollection(rows)))
    trackFavoriteImageIds.zip(favoriteMarkerImages).forEach { (imageId, bitmap) ->
        style.addImage(imageId, bitmap)
    }
    style.addLayer(
        LineLayer(TRACK_ROUTE_LAYER_ID, TRACK_ROUTE_SOURCE_ID).withProperties(
            lineColor(routeColor),
            lineWidth(TRACK_ROUTE_WIDTH),
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    style.addLayer(
        CircleLayer(TRACK_ORDINARY_POINT_LAYER_ID, TRACK_POINT_SOURCE_ID)
            .withFilter(trackTierFilter(0))
            .withProperties(
                circleColor(routeColor),
                circleRadius(TRACK_POINT_RADIUS),
                circleStrokeColor(markerOutlineColor),
                circleStrokeWidth(TRACK_POINT_OUTLINE_WIDTH),
            ),
    )
    trackFavoriteLayerIds.forEachIndexed { index, layerId ->
        style.addLayer(
            SymbolLayer(layerId, TRACK_POINT_SOURCE_ID)
                .withFilter(trackTierFilter(index + 1))
                .withProperties(
                    iconImage(trackFavoriteImageIds[index]),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                ),
        )
    }
}

private fun trackTierFilter(tier: Int): Expression = Expression.eq(
    Expression.get(FAVORITE_TIER_PROPERTY),
    tier,
)

private fun updateTrackGeometrySources(style: Style, rows: List<TrackPoint>) {
    style.getSourceAs<GeoJsonSource>(TRACK_ROUTE_SOURCE_ID)?.setGeoJson(trackRouteFeatureCollection(rows))
    style.getSourceAs<GeoJsonSource>(TRACK_POINT_SOURCE_ID)?.setGeoJson(trackPointFeatureCollection(rows))
}

private fun updateTrackGeometryStyle(
    style: Style,
    routeColor: Int,
    markerOutlineColor: Int,
    favoriteMarkerImages: List<Bitmap>,
) {
    style.getLayerAs<LineLayer>(TRACK_ROUTE_LAYER_ID)?.setProperties(lineColor(routeColor))
    style.getLayerAs<CircleLayer>(TRACK_ORDINARY_POINT_LAYER_ID)?.setProperties(
        circleColor(routeColor),
        circleStrokeColor(markerOutlineColor),
    )
    trackFavoriteImageIds.zip(favoriteMarkerImages).forEach { (imageId, bitmap) ->
        style.addImage(imageId, bitmap)
    }
}

private fun createFavoriteMarkerBitmap(
    density: Float,
    densityDpi: Int,
    fillColor: Int,
    outlineColor: Int,
): Bitmap {
    val size = (TRACK_FAVORITE_MARKER_SIZE_DP * density).toInt().coerceAtLeast(36)
    val center = size / 2f
    val path = Path().apply {
        repeat(10) { point ->
            val angle = Math.toRadians((-90.0 + point * 36.0))
            val radius = if (point % 2 == 0) {
                TRACK_FAVORITE_STAR_OUTER_RADIUS_DP * density
            } else {
                TRACK_FAVORITE_STAR_INNER_RADIUS_DP * density
            }
            val x = center + (kotlin.math.cos(angle) * radius).toFloat()
            val y = center + (kotlin.math.sin(angle) * radius).toFloat()
            if (point == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        this.density = densityDpi
        Canvas(this).apply {
            drawPath(
                path,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = fillColor
                    style = Paint.Style.FILL
                },
            )
            drawPath(
                path,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = outlineColor
                    style = Paint.Style.STROKE
                    strokeWidth = TRACK_POINT_OUTLINE_WIDTH * density
                    strokeJoin = Paint.Join.ROUND
                },
            )
        }
    }
}
