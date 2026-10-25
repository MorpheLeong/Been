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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
        title = { Text("收藏这条轨迹") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PassportSpacing.xxs)) {
                trackFavoriteLists.forEach { list ->
                    FavoriteTierChoice(list.stars, list.name, selectedTier == list.tier) { selectedTier = list.tier }
                }
                if (currentTier > 0) {
                    FavoriteTierChoice("", "取消收藏", selectedTier == 0) { selectedTier = 0 }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selectedTier) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun FavoriteTierChoice(stars: String, name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = PassportSpacing.minTouchTarget)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected, onClick)
        Text(listOf(stars, name).filter(String::isNotBlank).joinToString(" "), style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TrackFavoritesScreen(
    tracks: List<TrackPoint>,
    onBack: () -> Unit,
    onEdit: (TrackPoint) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filterMask by rememberSaveable { mutableIntStateOf(0b111) }
    val selectedTiers = remember(filterMask) { (1..3).filterTo(mutableSetOf()) { filterMask and (1 shl (it - 1)) != 0 } }
    val filtered = remember(tracks, selectedTiers, query) { filterFavoriteTracks(tracks, selectedTiers, query) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    PassportPageScaffold(title = "收藏轨迹", onBack = onBack) { padding ->
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
                label = { Text("搜索地点或地图链接") },
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
                tracks.isEmpty() -> FavoriteEmptyState("还没有收藏轨迹")
                filtered.isEmpty() -> FavoriteEmptyState("没有符合条件的收藏轨迹")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = PassportSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(PassportSpacing.xs),
                ) {
                    items(filtered, key = TrackPoint::id) { track ->
                        val list = trackFavoriteList(track.favoriteTier)
                        Card(
                            onClick = { onEdit(track) },
                            colors = CardDefaults.cardColors(
                                containerColor = trackFavoriteContainerColor(track.favoriteTier),
                                contentColor = trackFavoriteContentColor(track.favoriteTier),
                            ),
                            modifier = Modifier.fillMaxWidth().semantics {
                                contentDescription = "${track.name}，${list?.stars.orEmpty()}${list?.name.orEmpty()}，${dateFormat.format(Date(track.time))}"
                            },
                        ) {
                            Text(track.name, Modifier.padding(PassportSpacing.md), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteEmptyState(message: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = PassportSpacing.xl), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


