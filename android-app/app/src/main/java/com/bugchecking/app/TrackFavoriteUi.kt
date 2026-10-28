package com.bugchecking.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

@Composable
internal fun FavoriteTrackCard(
    track: TrackPoint,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onFavorite: (Int) -> Boolean,
    content: @Composable (contentColor: Color, displayedTier: Int) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    val density = LocalDensity.current
    val pulseProgress = remember { Animatable(0f) }
    val persistedFavoriteTier by rememberUpdatedState(track.favoriteTier)
    val favoriteHandler by rememberUpdatedState(onFavorite)
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    var pressOrigin by remember { mutableStateOf(Offset.Zero) }
    var pressed by remember { mutableStateOf(false) }
    var settledTier by remember(track.id) { mutableIntStateOf(track.favoriteTier) }
    var gestureTier by remember(track.id) { mutableIntStateOf(0) }
    var pulseTier by remember(track.id) { mutableIntStateOf(0) }
    var timerJob by remember(track.id) { mutableStateOf<Job?>(null) }
    var pulseJob by remember(track.id) { mutableStateOf<Job?>(null) }

    fun resetToPersisted() {
        timerJob?.cancel()
        pulseJob?.cancel()
        gestureTier = 0
        pulseTier = 0
        settledTier = persistedFavoriteTier
        scope.launch { pulseProgress.snapTo(0f) }
    }

    fun advanceTo(tier: Int) {
        gestureTier = tier
        haptic.performHapticFeedback(
            if (tier == 3) HapticFeedbackType.LongPress else HapticFeedbackType.SegmentFrequentTick,
        )
        pulseJob?.cancel()
        pulseJob = scope.launch {
            pulseTier = tier
            pulseProgress.snapTo(0f)
            pulseProgress.animateTo(1f, tween(360))
            settledTier = tier
            pulseTier = 0
            pulseProgress.snapTo(0f)
            if (!pressed) gestureTier = 0
        }
    }

    LaunchedEffect(track.favoriteTier, pressed) {
        if (!pressed && pulseTier == 0) settledTier = track.favoriteTier
    }
    LaunchedEffect(interactionSource, track.id, viewConfiguration.longPressTimeoutMillis) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressed = true
                    pressOrigin = interaction.pressPosition
                    gestureTier = 0
                    timerJob?.cancel()
                    timerJob = scope.launch {
                        val base = viewConfiguration.longPressTimeoutMillis
                        delay(base + 1_000L)
                        if (pressed) advanceTo(1)
                        delay(1_000L)
                        if (pressed) advanceTo(2)
                        delay(1_000L)
                        if (pressed) advanceTo(3)
                    }
                }
                is PressInteraction.Release -> {
                    pressed = false
                    timerJob?.cancel()
                    val committedTier = gestureTier
                    if (committedTier > 0) {
                        if (!favoriteHandler(committedTier)) resetToPersisted()
                        else if (pulseTier == 0) {
                            settledTier = committedTier
                            gestureTier = 0
                        }
                    } else if (pulseTier == 0) {
                        settledTier = persistedFavoriteTier
                    }
                }
                is PressInteraction.Cancel -> {
                    pressed = false
                    resetToPersisted()
                }
            }
        }
    }

    val displayedTier = if (gestureTier > 0) gestureTier else settledTier
    val settledColor = trackFavoriteContainerColor(settledTier)
    val activePulseColor = trackFavoriteContainerColor(pulseTier)
    val contentColor = trackFavoriteContentColor(displayedTier)
    val starDescription = trackFavoriteList(track.favoriteTier)?.let { "${it.stars} ${it.name}" } ?: "未收藏"
    val actions = buildList {
        trackFavoriteLists.forEach { list ->
            add(CustomAccessibilityAction("收藏到${list.name}") { onFavorite(list.tier) })
        }
        if (track.favoriteTier > 0) add(CustomAccessibilityAction("取消收藏") { onFavorite(0) })
    }
    val strokeWidth = with(density) { 2.dp.toPx() }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = contentColor),
        modifier = modifier
            .semantics {
                stateDescription = starDescription
                customActions = actions
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = indication,
                onClick = onClick,
                onLongClickLabel = "按住选择收藏等级",
                onLongClick = {},
            ),
    ) {
        Box(
            Modifier.fillMaxWidth()
                .drawWithContent {
                    drawRect(settledColor)
                    if (pulseTier in 1..3 && cardSize != IntSize.Zero) {
                        val maxRadius = maxOf(
                            hypot(pressOrigin.x, pressOrigin.y),
                            hypot(cardSize.width - pressOrigin.x, pressOrigin.y),
                            hypot(pressOrigin.x, cardSize.height - pressOrigin.y),
                            hypot(cardSize.width - pressOrigin.x, cardSize.height - pressOrigin.y),
                        )
                        val radius = maxRadius * pulseProgress.value
                        drawCircle(activePulseColor, radius, pressOrigin)
                        drawCircle(
                            contentColor.copy(alpha = 0.28f * (1f - pulseProgress.value)),
                            radius,
                            pressOrigin,
                            style = Stroke(width = strokeWidth),
                        )
                    }
                    drawContent()
                }
                .onSizeChanged { cardSize = it },
        ) {
            content(contentColor, displayedTier)
            FavoriteTierStars(
                tier = displayedTier,
                color = contentColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = PassportSpacing.md, bottom = PassportSpacing.md),
            )
        }
    }
}

@Composable
internal fun FavoriteTierStars(tier: Int, color: Color, modifier: Modifier = Modifier) {
    if (tier !in 1..3) return
    Row(
        modifier = modifier.semantics {
            contentDescription = trackFavoriteList(tier)?.let { "${it.stars} ${it.name}" }.orEmpty()
        },
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        repeat(tier) { Icon(Icons.Default.Star, null, tint = color, modifier = Modifier.size(16.dp)) }
    }
}

@Composable
internal fun FavoriteTierDialog(
    currentTier: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var selectedTier by remember(currentTier) { mutableIntStateOf(currentTier) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("收藏这个场所") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PassportSpacing.xxs)) {
                trackFavoriteLists.forEach { list ->
                    FavoriteTierChoice(list.name, selectedTier == list.tier) { selectedTier = list.tier }
                }
                if (currentTier > 0) {
                    FavoriteTierChoice("取消收藏", selectedTier == 0) { selectedTier = 0 }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selectedTier) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun FavoriteTierChoice(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = PassportSpacing.minTouchTarget)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected, onClick)
        Text(name, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlaceLibraryScreen(
    places: List<SavedPlace>,
    onBack: () -> Unit,
    onOpen: (SavedPlace) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filterMask by rememberSaveable { mutableIntStateOf(0b111) }
    val selectedTiers = remember(filterMask) { (1..3).filterTo(mutableSetOf()) { filterMask and (1 shl (it - 1)) != 0 } }
    val filtered = remember(places, selectedTiers, query) { filterSavedPlaces(places, selectedTiers, query) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    PassportPageScaffold(title = "地点库", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).widthIn(max = PassportSpacing.listMaxWidth)
                .padding(horizontal = PassportSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PassportSpacing.sm),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(top = PassportSpacing.md),
                singleLine = true,
                label = { Text("搜索场所或地图链接") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(PassportSpacing.xs)) {
                trackFavoriteLists.forEach { list ->
                    val bit = 1 shl (list.tier - 1)
                    FilterChip(
                        selected = filterMask and bit != 0,
                        onClick = { filterMask = filterMask xor bit },
                        label = { Text("${list.stars} ${list.name}") },
                    )
                }
            }
            when {
                filterMask == 0 -> FavoriteEmptyState("请选择至少一个收藏列表")
                places.isEmpty() -> FavoriteEmptyState("还没有收藏场所")
                filtered.isEmpty() -> FavoriteEmptyState("没有符合条件的场所")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = PassportSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(PassportSpacing.xs),
                ) {
                    items(filtered, key = SavedPlace::id) { place ->
                        val list = trackFavoriteList(place.favoriteTier)
                        Card(
                            onClick = { onOpen(place) },
                            colors = CardDefaults.cardColors(
                                containerColor = trackFavoriteContainerColor(place.favoriteTier),
                                contentColor = trackFavoriteContentColor(place.favoriteTier),
                            ),
                            modifier = Modifier.fillMaxWidth().semantics {
                                contentDescription = "${place.name}，${list?.stars.orEmpty()}${list?.name.orEmpty()}，到访 ${place.visitCount} 次"
                            },
                        ) {
                            Column(Modifier.padding(PassportSpacing.md)) {
                                Text(place.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    buildString {
                                        append("到访 ${place.visitCount} 次")
                                        place.lastVisitedAt?.let { append(" · 最近 ${dateFormat.format(Date(it))}") }
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = trackFavoriteContentColor(place.favoriteTier).copy(alpha = 0.76f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlaceDetailScreen(
    place: SavedPlace,
    visits: List<TrackPoint>,
    onBack: () -> Unit,
    onSave: (name: String, tier: Int, originalUrl: String?, canonicalUrl: String?) -> Unit,
    onOpenTrack: (TrackPoint) -> Unit,
    onUnlinkTrack: (TrackPoint) -> Unit,
) {
    var name by rememberSaveable(place.id) { mutableStateOf(place.name) }
    val initialLink = place.originalUrl ?: place.canonicalUrl.orEmpty()
    var mapLink by rememberSaveable(place.id) { mutableStateOf(initialLink) }
    var tier by rememberSaveable(place.id) { mutableIntStateOf(place.favoriteTier) }
    var tierDialogOpen by rememberSaveable(place.id) { mutableStateOf(false) }
    var pendingUnlink by remember { mutableStateOf<TrackPoint?>(null) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val identity = when {
        !place.providerPlaceId.isNullOrBlank() -> "识别依据：${place.provider.name} 场所 ID"
        !place.canonicalUrl.isNullOrBlank() -> "识别依据：规范化地图链接"
        else -> "识别依据：名称与到访坐标"
    }

    PassportPageScaffold(
        title = "场所详情",
        onBack = onBack,
        primaryActionLabel = "保存场所",
        primaryActionEnabled = name.isNotBlank(),
        onPrimaryAction = {
            val cleanLink = mapLink.trim().takeIf(String::isNotEmpty)
            val canonical = place.canonicalUrl.takeIf { mapLink == initialLink }
            onSave(name, tier, cleanLink, canonical)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).widthIn(max = PassportSpacing.formMaxWidth)
                .padding(horizontal = PassportSpacing.md),
            contentPadding = PaddingValues(top = PassportSpacing.md, bottom = PassportSpacing.xl),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(PassportSpacing.md)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("场所名称") },
                        singleLine = true,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(PassportSpacing.xxs)) {
                        OutlinedTextField(
                            value = mapLink,
                            onValueChange = { mapLink = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("地图分享链接") },
                            minLines = 2,
                        )
                        Text(
                            identity,
                            modifier = Modifier.padding(horizontal = PassportSpacing.sm),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedCard(
                        onClick = { tierDialogOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                                .padding(horizontal = PassportSpacing.md, vertical = PassportSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PassportSpacing.xxs)) {
                                Text(
                                    "收藏列表",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    trackFavoriteList(tier)?.name ?: "未收藏",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "选择收藏列表")
                        }
                    }
                    Column(
                        modifier = Modifier.padding(top = PassportSpacing.sm, bottom = PassportSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(PassportSpacing.xxs),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("关联轨迹", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(
                                "${visits.size} 条",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "用于核验是否误将不同地点识别为同一场所；解除关联不会删除轨迹。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (visits.isEmpty()) {
                item { FavoriteEmptyState("这个场所还没有关联轨迹") }
            } else {
                itemsIndexed(visits, key = { _, track -> track.id }) { index, track ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onOpenTrack(track) }
                            .heightIn(min = 56.dp)
                            .padding(start = PassportSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            dateFormat.format(Date(track.time)),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { pendingUnlink = track }) {
                            Icon(Icons.Default.LinkOff, contentDescription = "解除与这条轨迹的关联")
                        }
                    }
                    if (index < visits.lastIndex) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(0.84f),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f),
                            )
                        }
                    }
                }
            }
        }
    }

    if (tierDialogOpen) FavoriteTierDialog(
        currentTier = tier,
        onDismiss = { tierDialogOpen = false },
        onSave = {
            tier = it
            tierDialogOpen = false
        },
    )

    pendingUnlink?.let { track ->
        AlertDialog(
            onDismissRequest = { pendingUnlink = null },
            title = { Text("解除场所关联？") },
            text = { Text("“${track.name}”会保留为独立轨迹，但不再显示这个场所的收藏星标。") },
            confirmButton = {
                TextButton(onClick = { onUnlinkTrack(track); pendingUnlink = null }) { Text("解除关联") }
            },
            dismissButton = { TextButton(onClick = { pendingUnlink = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun FavoriteEmptyState(message: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = PassportSpacing.xl), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


