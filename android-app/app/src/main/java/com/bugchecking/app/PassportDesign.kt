package com.bugchecking.app

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.tooling.preview.Preview

object PassportSpacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val minTouchTarget: Dp = 48.dp
    val formMaxWidth: Dp = 720.dp
    val listMaxWidth: Dp = 840.dp
}

/** Track-home hierarchy tokens: signature features stay visible without competing with Add. */
object TrackHomeActionTokens {
    val signatureTargetSize: Dp = 48.dp
    val actionGap: Dp = 8.dp
    val safeBottomReserve: Dp = 104.dp
    const val cardTitleMaxLines: Int = 1
}

/** Scrimless primary-page action overlay shared by Tracks, Checklist, QR, and Me. */
object PrimaryTopActionTokens {
    val targetSize: Dp = 48.dp
    val avatarInset: Dp = 4.dp
    val safeAreaGap: Dp = 8.dp
    val primaryContentBreathingRoom: Dp = 64.dp
    val detailContentGap: Dp = 16.dp
    val minimumGroupGap: Dp = 8.dp
    val compactStatusMaxWidth: Dp = 132.dp
    val expandedStatusMaxWidth: Dp = 220.dp
    const val statusEnterMillis: Int = 320
    const val statusExitMillis: Int = 260
}

/** Shared translucent surfaces for the four primary destinations. */
@Composable
fun primaryPageChromeColor(): Color = MaterialTheme.colorScheme.surface.copy(
    alpha = if (isSystemInDarkTheme()) 0.66f else 0.92f,
)

@Composable
fun primaryPageNavigationColor(): Color = MaterialTheme.colorScheme.surfaceContainer.copy(
    alpha = if (isSystemInDarkTheme()) 0.70f else 0.92f,
)

@Composable
fun primaryPageCardColor(): Color = MaterialTheme.colorScheme.surfaceContainerLow.copy(
    alpha = if (isSystemInDarkTheme()) 0.68f else 0.84f,
)

internal val PassportNeonGreen = Color(0xFF53FF9A)
internal val PassportOnNeonGreen = Color(0xFF00351D)
internal val PassportDisconnectedGreen = Color(0xFF007A49)

internal fun deviceActionTint(state: DeviceActionState): Color =
    if (state == DeviceActionState.DISCONNECTED) PassportDisconnectedGreen else PassportNeonGreen

private val NeonGreen = PassportNeonGreen
private val OnNeonGreen = PassportOnNeonGreen

internal data class FavoriteTierPalette(
    val container: Color,
    val pulse: Color,
    val accent: Color,
)

private val FavoriteLightPalettes = listOf(
    FavoriteTierPalette(Color(0xFFFFD8D0), Color(0xFFFFB8AA), Color(0xFF9C4055)),
    FavoriteTierPalette(Color(0xFFFFB0C2), Color(0xFFFF8EAA), Color(0xFFB90048)),
    FavoriteTierPalette(Color(0xFFFF7A9F), Color(0xFFFF5A86), Color(0xFFC3004F)),
)
private val FavoriteDarkPalettes = listOf(
    FavoriteTierPalette(Color(0xFF3A2928), Color(0xFF5B3430), Color(0xFFFFB8A9)),
    FavoriteTierPalette(Color(0xFF542B37), Color(0xFF83354F), Color(0xFFFF8DA9)),
    FavoriteTierPalette(Color(0xFF743046), Color(0xFFA93C62), Color(0xFFFF5A86)),
)

internal fun favoriteTierPalette(darkTheme: Boolean, tier: Int): FavoriteTierPalette =
    (if (darkTheme) FavoriteDarkPalettes else FavoriteLightPalettes)[tier.coerceIn(1, 3) - 1]

@Composable
fun trackFavoriteContainerColor(tier: Int): Color {
    if (tier !in 1..3) return primaryPageCardColor()
    return favoriteTierPalette(isSystemInDarkTheme(), tier).container.copy(
        alpha = if (isSystemInDarkTheme()) 0.82f else 0.86f,
    )
}

@Composable
fun trackFavoriteContentColor(tier: Int): Color = MaterialTheme.colorScheme.onSurface

@Composable
fun trackFavoritePulseColor(tier: Int): Color =
    favoriteTierPalette(isSystemInDarkTheme(), tier).pulse

@Composable
fun trackFavoriteAccentColor(tier: Int): Color =
    favoriteTierPalette(isSystemInDarkTheme(), tier).accent

/** Opaque map-marker colors; stronger than translucent cards so they remain legible over map tiles. */
@Composable
fun trackFavoriteMapColor(tier: Int): Color {
    return favoriteTierPalette(isSystemInDarkTheme(), tier).accent
}

private val PassportLightScheme = lightColorScheme(
    primary = NeonGreen,
    onPrimary = OnNeonGreen,
    primaryContainer = NeonGreen,
    onPrimaryContainer = OnNeonGreen,
    inversePrimary = NeonGreen,
    secondary = NeonGreen,
    onSecondary = OnNeonGreen,
    secondaryContainer = NeonGreen,
    onSecondaryContainer = OnNeonGreen,
    tertiary = Color(0xFFC3004F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFB1C5),
    onTertiaryContainer = Color(0xFF5B001F),
    background = Color(0xFFFBE8DB),
    onBackground = Color(0xFF201A1D),
    surface = Color(0xFFFBE8DB),
    onSurface = Color(0xFF201A1D),
    surfaceVariant = Color(0xFFEBCFC9),
    onSurfaceVariant = Color(0xFF65555B),
    surfaceTint = NeonGreen,
    inverseSurface = Color(0xFF352F31),
    inverseOnSurface = Color(0xFFFBEDEF),
    error = Color(0xFFA33C1E),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD0),
    onErrorContainer = Color(0xFF3B0900),
    outline = Color(0xFF836D74),
    outlineVariant = Color(0xFFAC7482),
    scrim = Color.Black,
    surfaceBright = Color(0xFFFFF2E9),
    surfaceDim = Color(0xFFDDBCB2),
    surfaceContainerLowest = Color(0xFFFFF4EB),
    surfaceContainerLow = Color(0xFFF5D9D5),
    surfaceContainer = Color(0xFFEFCFCB),
    surfaceContainerHigh = Color(0xFFE7C4C1),
    surfaceContainerHighest = Color(0xFFDDB6B5),
)

private val PassportDarkScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = OnNeonGreen,
    primaryContainer = NeonGreen,
    onPrimaryContainer = OnNeonGreen,
    inversePrimary = NeonGreen,
    secondary = NeonGreen,
    onSecondary = OnNeonGreen,
    secondaryContainer = NeonGreen,
    onSecondaryContainer = OnNeonGreen,
    tertiary = Color(0xFFFF5A86),
    onTertiary = Color(0xFF5A001F),
    tertiaryContainer = Color(0xFFA8003E),
    onTertiaryContainer = Color(0xFFFFD9E2),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE8BDC8),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE8BDC8),
    surfaceVariant = Color(0xFF202020),
    onSurfaceVariant = Color(0xFFC2A0A8),
    surfaceTint = NeonGreen,
    inverseSurface = Color(0xFFE8BDC8),
    inverseOnSurface = Color(0xFF352F31),
    error = Color(0xFFFFB59E),
    onError = Color(0xFF5E1705),
    errorContainer = Color(0xFF7D2A12),
    onErrorContainer = Color(0xFFFFDAD0),
    outline = Color(0xFF9B7882),
    outlineVariant = Color(0xFF5D4149),
    scrim = Color.Black,
    surfaceBright = Color(0xFF333333),
    surfaceDim = Color(0xFF0A0A0A),
    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = Color(0xFF181818),
    surfaceContainer = Color(0xFF202020),
    surfaceContainerHigh = Color(0xFF292929),
    surfaceContainerHighest = Color(0xFF333333),
)

internal fun passportColorScheme(darkTheme: Boolean) =
    if (darkTheme) PassportDarkScheme else PassportLightScheme

private val PassportTypography = Typography().let { baseline ->
    baseline.copy(
        headlineSmall = baseline.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = baseline.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = baseline.titleMedium.copy(fontWeight = FontWeight.Medium),
    )
}

private val PassportShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PassportTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = passportColorScheme(darkTheme),
        typography = PassportTypography,
        shapes = PassportShapes,
        content = content,
    )
}

/** Clears focus only for an unhandled tap; scrolling and child controls keep their active editor. */
fun Modifier.clearFocusOnTapOutside(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    pointerInput(focusManager, keyboard) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
            if (up != null && !up.isConsumed) {
                focusManager.clearFocus()
                keyboard?.hide()
            }
        }
    }
}

enum class PassportSyncStatus(val label: String) {
    DISCONNECTED("未连接 Passport"),
    LOCAL_SAVED("已保存到手机"),
    PENDING("等待同步"),
    SYNCING("正在同步"),
    SYNCED("已同步到 Passport"),
    FAILED("同步失败，可重试"),
}

internal fun resolveSyncStatus(
    connected: Boolean,
    message: String,
    hasLocalChanges: Boolean,
): PassportSyncStatus {
    if (!connected) return PassportSyncStatus.DISCONNECTED
    val value = message.trim()
    return when {
        value.contains("失败") || value.contains("错误") || value.contains("超出") || value.contains("重试") ->
            PassportSyncStatus.FAILED
        value.contains("正在") || value.contains("准备") || value.contains("写入") ->
            PassportSyncStatus.SYNCING
        value.contains("已同步") || value.contains("同步完成") -> PassportSyncStatus.SYNCED
        hasLocalChanges -> PassportSyncStatus.PENDING
        else -> PassportSyncStatus.LOCAL_SAVED
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassportPageScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    primaryActionLabel: String? = null,
    primaryActionEnabled: Boolean = true,
    onPrimaryAction: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.clearFocusOnTapOutside(),
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = actions,
            )
        },
        bottomBar = {
            if (primaryActionLabel != null && onPrimaryAction != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
                ) {
                    Button(
                        onClick = onPrimaryAction,
                        enabled = primaryActionEnabled,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = PassportSpacing.md, vertical = PassportSpacing.sm),
                    ) { Text(primaryActionLabel) }
                }
            }
        },
        content = content,
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    description: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(description) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("删除", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapProviderPickerSheet(
    onDismiss: () -> Unit,
    onAmap: () -> Unit,
    onGoogle: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(
                start = PassportSpacing.md,
                end = PassportSpacing.md,
                bottom = PassportSpacing.lg,
            ),
        ) {
            Text("选择地图应用", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(PassportSpacing.md))
            MapProviderChoice(
                title = "高德地图",
                icon = { Icon(Icons.Default.Map, contentDescription = null) },
                onClick = onAmap,
            )
            Spacer(Modifier.size(PassportSpacing.xs))
            MapProviderChoice(
                title = "Google Maps",
                icon = { Icon(Icons.Default.Public, contentDescription = null) },
                onClick = onGoogle,
            )
        }
    }
}

@Composable
private fun MapProviderChoice(
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ListItem(
            headlineContent = { Text(title) },
            leadingContent = {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(PassportSpacing.minTouchTarget),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) { icon() }
                }
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Preview(name = "Passport · Light", showBackground = true, widthDp = 393, heightDp = 852)
@Preview(
    name = "Passport · Dark",
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PassportDesignPreview() {
    PassportTheme {
        PassportPageScaffold(
            title = "编辑地点",
            onBack = {},
            primaryActionLabel = "保存地点名称",
            onPrimaryAction = {},
        ) { padding ->
            Column(Modifier.padding(padding).padding(PassportSpacing.md)) {
                Text("栗Cafe（长宁馥邦苑店）", style = MaterialTheme.typography.titleMedium)
                Text("距打卡位置 42 m", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
