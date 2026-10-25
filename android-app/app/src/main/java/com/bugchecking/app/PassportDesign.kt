package com.bugchecking.app

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
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

/** Shared translucent surfaces for the four primary destinations. */
@Composable
fun primaryPageChromeColor(): Color = MaterialTheme.colorScheme.surface.copy(
    alpha = if (isSystemInDarkTheme()) 0.66f else 0.72f,
)

@Composable
fun primaryPageNavigationColor(): Color = MaterialTheme.colorScheme.surfaceContainer.copy(
    alpha = if (isSystemInDarkTheme()) 0.70f else 0.74f,
)

@Composable
fun primaryPageCardColor(): Color = MaterialTheme.colorScheme.surfaceContainerLow.copy(
    alpha = if (isSystemInDarkTheme()) 0.68f else 0.68f,
)

@Composable
fun primaryPageSummaryColor(): Color = MaterialTheme.colorScheme.primaryContainer.copy(
    alpha = if (isSystemInDarkTheme()) 0.76f else 0.74f,
)

@Composable
fun primaryPageSelectedColor(): Color = MaterialTheme.colorScheme.secondaryContainer.copy(
    alpha = if (isSystemInDarkTheme()) 0.76f else 0.74f,
)

private val FavoriteLightContainers = listOf(
    Color(0xFFC7E1DD), Color(0xFF9EDBD3), Color(0xFF65D7C8),
)
private val FavoriteDarkContainers = listOf(
    Color(0xFF294744), Color(0xFF315E59), Color(0xFF397A71),
)

@Composable
fun trackFavoriteContainerColor(tier: Int): Color {
    if (tier !in 1..3) return primaryPageCardColor()
    return (if (isSystemInDarkTheme()) FavoriteDarkContainers else FavoriteLightContainers)[tier - 1]
        .copy(alpha = 0.68f)
}

@Composable
fun trackFavoriteContentColor(tier: Int): Color = when {
    tier !in 1..3 -> MaterialTheme.colorScheme.onSurface
    isSystemInDarkTheme() -> Color(0xFFDDF8F3)
    else -> Color(0xFF123B37)
}

private val PassportInk = Color(0xFF172A3A)
private val RouteBlue = Color(0xFF4B69A2)
private val CandyBlue = Color(0xFFD9E5FF)
private val WaypointMint = Color(0xFF3A7566)
private val CandyMint = Color(0xFFC5F1DF)
private val JourneyPeach = Color(0xFF95566B)
private val CandyPeach = Color(0xFFFFD9E3)
private val CandyLemon = Color(0xFFFFE8A3)
private val MistSurface = Color(0xFFFFF9F2)
private val NightSurface = Color(0xFF202B36)
private val NightSurfaceDim = Color(0xFF19232D)
private val NightSurfaceBright = Color(0xFF3B4957)
private val NightSurfaceLowest = Color(0xFF17212A)
private val NightSurfaceLow = Color(0xFF2A3642)
private val NightSurfaceContainer = Color(0xFF303E4A)
private val NightSurfaceHigh = Color(0xFF394854)
private val NightSurfaceHighest = Color(0xFF455563)
private val NightOnSurface = Color(0xFFF0F4F8)
private val NightOnSurfaceVariant = Color(0xFFCED7E0)
private val NightOutline = Color(0xFF9CAABA)
private val NightOutlineVariant = Color(0xFF718294)

private val PassportLightScheme = lightColorScheme(
    primary = RouteBlue,
    onPrimary = Color.White,
    primaryContainer = CandyBlue,
    onPrimaryContainer = Color(0xFF102A50),
    secondary = WaypointMint,
    onSecondary = Color.White,
    secondaryContainer = CandyMint,
    onSecondaryContainer = Color(0xFF0B302D),
    tertiary = JourneyPeach,
    onTertiary = Color.White,
    tertiaryContainer = CandyPeach,
    onTertiaryContainer = Color(0xFF3D1020),
    surface = MistSurface,
    onSurface = PassportInk,
    surfaceContainerLow = Color(0xFFFFF4E7),
    surfaceContainer = Color(0xFFF9EEDC),
    surfaceContainerHigh = Color(0xFFFFE9BC),
    surfaceContainerHighest = CandyLemon,
)

private val PassportDarkScheme = darkColorScheme(
    primary = Color(0xFFB8D8F4),
    onPrimary = Color(0xFF17344B),
    primaryContainer = Color(0xFF416487),
    onPrimaryContainer = Color(0xFFF2F6FF),
    secondary = Color(0xFFA9DED2),
    onSecondary = Color(0xFF173A34),
    secondaryContainer = Color(0xFF3E695F),
    onSecondaryContainer = Color(0xFFF0FFF9),
    tertiary = Color(0xFFFFC4D1),
    onTertiary = Color(0xFF4F2934),
    tertiaryContainer = Color(0xFF7A5260),
    onTertiaryContainer = Color(0xFFFFF1F4),
    surface = NightSurface,
    onSurface = NightOnSurface,
    onSurfaceVariant = NightOnSurfaceVariant,
    surfaceDim = NightSurfaceDim,
    surfaceBright = NightSurfaceBright,
    surfaceContainerLowest = NightSurfaceLowest,
    surfaceContainerLow = NightSurfaceLow,
    surfaceContainer = NightSurfaceContainer,
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = NightSurfaceHighest,
    outline = NightOutline,
    outlineVariant = NightOutlineVariant,
)

private fun softenDynamicLight(scheme: androidx.compose.material3.ColorScheme) = scheme.copy(
    primaryContainer = lerp(scheme.primaryContainer, CandyBlue, 0.22f),
    secondaryContainer = lerp(scheme.secondaryContainer, CandyMint, 0.22f),
    tertiaryContainer = lerp(scheme.tertiaryContainer, CandyPeach, 0.18f),
    surface = lerp(scheme.surface, MistSurface, 0.16f),
)

private fun softenDynamicDark(scheme: androidx.compose.material3.ColorScheme) = scheme.copy(
    primaryContainer = lerp(scheme.primaryContainer, Color(0xFF416487), 0.38f),
    onPrimaryContainer = Color(0xFFF2F6FF),
    secondaryContainer = lerp(scheme.secondaryContainer, Color(0xFF3E695F), 0.36f),
    onSecondaryContainer = Color(0xFFF0FFF9),
    tertiaryContainer = lerp(scheme.tertiaryContainer, Color(0xFF7A5260), 0.34f),
    onTertiaryContainer = Color(0xFFFFF1F4),
    surface = NightSurface,
    onSurface = NightOnSurface,
    onSurfaceVariant = NightOnSurfaceVariant,
    surfaceDim = NightSurfaceDim,
    surfaceBright = NightSurfaceBright,
    surfaceContainerLowest = NightSurfaceLowest,
    surfaceContainerLow = NightSurfaceLow,
    surfaceContainer = NightSurfaceContainer,
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = NightSurfaceHighest,
    outline = NightOutline,
    outlineVariant = NightOutlineVariant,
)

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
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            softenDynamicDark(dynamicDarkColorScheme(context))
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            softenDynamicLight(dynamicLightColorScheme(context))
        darkTheme -> PassportDarkScheme
        else -> PassportLightScheme
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = PassportTypography,
        shapes = PassportShapes,
        content = content,
    )
}

/** Clears the previous editor before children handle the new down event; the event is never consumed. */
fun Modifier.clearFocusOnTapOutside(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    pointerInput(focusManager, keyboard) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            focusManager.clearFocus()
            keyboard?.hide()
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
    subtitle: String? = null,
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
                title = {
                    androidx.compose.foundation.layout.Column {
                        Text(title)
                        subtitle?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
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
                Surface(tonalElevation = 3.dp) {
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
    PassportTheme(dynamicColor = false) {
        PassportPageScaffold(
            title = "编辑地点",
            subtitle = "已保存到手机",
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
