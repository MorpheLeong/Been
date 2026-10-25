package com.bugchecking.app

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import java.text.DateFormat
import java.util.Date
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.pow

private const val OPENFREEMAP_LIGHT_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val OPENFREEMAP_DARK_STYLE = "https://tiles.openfreemap.org/styles/dark"

internal data class TrackCluster(val tracks: List<TrackPoint>, val latitude: Double, val longitude: Double)
internal data class TrackDaySegment(val date: LocalDate, val tracks: List<TrackPoint>)

internal fun clusterTracks(rows: List<TrackPoint>, zoom: Double): List<TrackCluster> {
    if (rows.isEmpty()) return emptyList()
    val cellDegrees = 360.0 / (2.0.pow(zoom.coerceIn(1.0, 20.0)) * 8.0)
    return rows.groupBy { floor(it.lat / cellDegrees) to floor(it.lon / cellDegrees) }.values.map { group ->
        TrackCluster(group, group.map { it.lat }.average(), group.map { it.lon }.average())
    }
}

internal fun dailyTrackSegments(
    rows: List<TrackPoint>,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<TrackDaySegment> = rows
    .groupBy { Instant.ofEpochMilli(it.time).atZone(zoneId).toLocalDate() }
    .entries
    .sortedBy(Map.Entry<LocalDate, List<TrackPoint>>::key)
    .map { (date, tracks) -> TrackDaySegment(date, tracks.sortedBy(TrackPoint::time)) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackMapScreen(
    rows: List<TrackPoint>,
    onBack: () -> Unit,
    onEdit: (TrackPoint) -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val routeColor = MaterialTheme.colorScheme.primary.toArgb()
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).also { it.onCreate(Bundle()) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var selected by remember { mutableStateOf<TrackPoint?>(null) }
    var mapPickerTrack by remember { mutableStateOf<TrackPoint?>(null) }
    var zoom by remember { mutableDoubleStateOf(4.0) }
    var styleAttempt by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

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
            ready.setStyle(if (darkTheme) OPENFREEMAP_DARK_STYLE else OPENFREEMAP_LIGHT_STYLE) {
                loading = false
                error = null
                zoom = ready.cameraPosition.zoom
                ready.addOnCameraIdleListener { zoom = ready.cameraPosition.zoom }
                fitMap(ready)
            }
        }
        delay(12_000)
        if (loading) error = "底图加载失败或网络不可用"
    }

    LaunchedEffect(map, rows, zoom, loading, routeColor) {
        val current = map ?: return@LaunchedEffect
        if (loading) return@LaunchedEffect
        current.clear()
        dailyTrackSegments(rows).forEach { day ->
            if (day.tracks.size < 2) return@forEach
                current.addPolyline(
                    PolylineOptions().addAll(day.tracks.map { LatLng(it.lat, it.lon) }).color(routeColor).width(4f)
                )
            }
        val markerRows = mutableMapOf<Long, TrackCluster>()
        clusterTracks(rows.take(300), zoom).forEach { cluster ->
            val marker = current.addMarker(
                MarkerOptions()
                    .position(LatLng(cluster.latitude, cluster.longitude))
                    .title(if (cluster.tracks.size == 1) cluster.tracks[0].name else "${cluster.tracks.size} 个地点")
                    .snippet(if (cluster.tracks.size == 1) formatTrackTime(cluster.tracks[0]) else "点按放大")
            )
            markerRows[marker.id] = cluster
        }
        current.setOnMarkerClickListener { marker: Marker ->
            val cluster = markerRows[marker.id] ?: return@setOnMarkerClickListener false
            if (cluster.tracks.size > 1) {
                current.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, (current.cameraPosition.zoom + 2).coerceAtMost(19.0)))
            } else selected = cluster.tracks[0]
            true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("轨迹地图") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回轨迹列表") } },
                actions = { IconButton(onClick = ::fitAll, enabled = rows.isNotEmpty()) { Icon(Icons.Default.CenterFocusStrong, "重新定位全部点位") } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
            if (rows.isEmpty()) {
                Card(Modifier.align(Alignment.Center).padding(24.dp)) { Text("还没有轨迹记录，地图已准备好。", Modifier.padding(20.dp)) }
            }
            if (loading || error != null) {
                Card(Modifier.align(Alignment.Center).padding(24.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error ?: "正在加载地图…")
                        if (error != null) Button(onClick = { styleAttempt++ }) { Text("重试") }
                    }
                }
            }
            Text(
                "© OpenStreetMap contributors · OpenFreeMap",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            )
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
                    "${formatTrackTime(track)} · 精度 ${track.accuracy.toInt()} m",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { onEdit(track) }, modifier = Modifier.fillMaxWidth()) { Text("编辑地点") }
                OutlinedButton(
                    onClick = { selected = null; mapPickerTrack = track },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("在地图中查看")
                }
            }
        }
    }
    mapPickerTrack?.let { track ->
        MapProviderPickerSheet(
            onDismiss = { mapPickerTrack = null; selected = track },
            onAmap = {
                mapPickerTrack = null
                ExternalMapLauncher.openAmap(context, track)
            },
            onGoogle = {
                mapPickerTrack = null
                ExternalMapLauncher.openGoogle(context, track)
            },
        )
    }
}

private fun formatTrackTime(track: TrackPoint): String = DateFormat.getDateTimeInstance().format(Date(track.time))
