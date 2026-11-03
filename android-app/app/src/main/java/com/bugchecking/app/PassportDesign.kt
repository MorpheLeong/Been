package com.bugchecking.app

import android.os.Build
import android.view.RoundedCorner
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

@Immutable
data class PassportPalette(
    val light: androidx.compose.material3.ColorScheme,
    val dark: androidx.compose.material3.ColorScheme,
    val neonGreen: Color = PassportNeonGreen,
    val onNeonGreen: Color = PassportOnNeonGreen,
    val disconnectedGreen: Color = PassportDisconnectedGreen,
    val mediaBackdrop: Color = Color.Black,
    val qrBackground: Color = Color.White,
    val qrForeground: Color = Color.Black,
)

@Immutable
data class PassportShapeTokens(
    val screenCornerRadius: Dp,
    val floatingEdgeInset: Dp = PassportSpacing.md,
    val floatingNavigationHeight: Dp = 64.dp,
) {
    val cardCornerRadius: Dp = screenCornerRadius
    val fieldCornerRadius: Dp = 16.dp
    val buttonCornerRadius: Dp = 24.dp
    val floatingNavigationCornerRadius: Dp = concentricCornerRadius(
        screenCornerRadius = screenCornerRadius,
        edgeInset = floatingEdgeInset,
        containerHeight = floatingNavigationHeight,
    )
    val cardShape = RoundedCornerShape(cardCornerRadius)
    val fieldShape = RoundedCornerShape(fieldCornerRadius)
    val buttonShape = RoundedCornerShape(buttonCornerRadius)
    val floatingNavigationShape = RoundedCornerShape(floatingNavigationCornerRadius)
    val circular = CircleShape
    val full = RoundedCornerShape(percent = 50)
    val navigationSelectionShape = full
}

internal fun concentricCornerRadius(
    screenCornerRadius: Dp,
    edgeInset: Dp,
    containerHeight: Dp,
    minimumRadius: Dp = 16.dp,
): Dp = (screenCornerRadius - edgeInset).coerceIn(
    minimumValue = minimumRadius,
    maximumValue = containerHeight / 2,
)

@Immutable
data class PassportContentTokens(
    val screenCornerRadius: Dp,
) {
    val cardHorizontalPadding: Dp = maxOf(PassportSpacing.lg, screenCornerRadius / 2)
    val cardVerticalPadding: Dp = PassportSpacing.lg
    val compactCardVerticalPadding: Dp = PassportSpacing.md
}

@Immutable
data class PassportBorderTokens(
    val standard: Dp = 1.dp,
    val focused: Dp = 2.dp,
    val selected: Dp = 2.dp,
    val floating: Dp = 1.dp,
)

@Immutable
data class PassportComponentTokens(
    val primaryNavigationHeight: Dp = 64.dp,
    val cornerActionSize: Dp = 48.dp,
    val floatingHorizontalInset: Dp = PassportSpacing.md,
    val compactNavigationHeight: Dp = 64.dp,
    val compactFabSize: Dp = 64.dp,
    val compactNavigationGap: Dp = 8.dp,
    val compactNavigationBottomGap: Dp = 12.dp,
    val compactNavigationContentPadding: Dp = 4.dp,
    val compactNavigationItemHeight: Dp = 56.dp,
    val compactNavigationIconSize: Dp = 24.dp,
    val compactNavigationLabelGap: Dp = 2.dp,
    val signatureContainerHorizontalPadding: Dp = 4.dp,
    val trackToolRowMinHeight: Dp = 64.dp,
    val overlaySafeGap: Dp = 8.dp,
    val floatingControlShadowElevation: Dp = 4.dp,
    val floatingNavigationShadowElevation: Dp = 4.dp,
    val floatingFabDefaultElevation: Dp = 6.dp,
    val floatingFabPressedElevation: Dp = 8.dp,
)

@Immutable
data class PassportDesignTokens(
    val palette: PassportPalette,
    val shapes: PassportShapeTokens,
    val content: PassportContentTokens,
    val borders: PassportBorderTokens = PassportBorderTokens(),
    val components: PassportComponentTokens = PassportComponentTokens(),
)

private val LocalPassportDesignTokens = compositionLocalOf<PassportDesignTokens> {
    error("PassportDesignTokens are unavailable outside PassportTheme")
}

val PassportTokens: PassportDesignTokens
    @Composable get() = LocalPassportDesignTokens.current

internal fun uniformScreenCornerRadiusPx(
    apiLevel: Int,
    cornerRadiiPx: List<Int?>,
    fallbackPx: Int,
): Int {
    if (apiLevel < Build.VERSION_CODES.S) return fallbackPx
    val available = cornerRadiiPx.filterNotNull().filter { it > 0 }
    return if (available.isEmpty()) 0 else available.min()
}

@Composable
private fun rememberScreenCornerRadius(): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    val fallbackPx = with(density) { 16.dp.roundToPx() }
    var radiusPx by remember(view) { mutableIntStateOf(fallbackPx) }
    LaunchedEffect(view, density) {
        withFrameNanos { }
        val insets = view.rootWindowInsets
        radiusPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && insets != null) {
            uniformScreenCornerRadiusPx(
                apiLevel = Build.VERSION.SDK_INT,
                cornerRadiiPx = listOf(
                    insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius,
                    insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius,
                    insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius,
                    insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius,
                ),
                fallbackPx = fallbackPx,
            )
        } else {
            uniformScreenCornerRadiusPx(Build.VERSION.SDK_INT, emptyList(), fallbackPx)
        }
    }
    return with(density) { radiusPx.toDp() }
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
fun primaryPageNavigationColor(): Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
    alpha = if (isSystemInDarkTheme()) 0.74f else 0.88f,
)

@Composable
fun primaryPageCardColor(): Color = MaterialTheme.colorScheme.surfaceContainerLow.copy(
    alpha = if (isSystemInDarkTheme()) 0.68f else 0.84f,
)

/** A quiet shared rim that keeps floating controls legible over moving map content. */
@Composable
fun floatingControlBorder(): BorderStroke = BorderStroke(
    PassportTokens.borders.floating,
    MaterialTheme.colorScheme.onSurface.copy(alpha = floatingControlBorderAlpha(isSystemInDarkTheme())),
)

internal fun floatingControlBorderAlpha(darkTheme: Boolean): Float =
    if (darkTheme) 0.28f else 0f

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

internal val PassportLightScheme = lightColorScheme(
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

internal val PassportDarkScheme = darkColorScheme(
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

@Composable
fun PassportTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: PassportPalette = PassportPalette(PassportLightScheme, PassportDarkScheme),
    content: @Composable () -> Unit,
) {
    val screenCornerRadius = rememberScreenCornerRadius()
    val componentTokens = remember { PassportComponentTokens() }
    val shapeTokens = remember(screenCornerRadius, componentTokens) {
        PassportShapeTokens(
            screenCornerRadius = screenCornerRadius,
            floatingEdgeInset = componentTokens.floatingHorizontalInset,
            floatingNavigationHeight = componentTokens.primaryNavigationHeight,
        )
    }
    val shapes = remember(shapeTokens) {
        Shapes(
            extraSmall = shapeTokens.fieldShape,
            small = shapeTokens.fieldShape,
            medium = shapeTokens.buttonShape,
            large = shapeTokens.cardShape,
            extraLarge = shapeTokens.cardShape,
        )
    }
    val contentTokens = remember(screenCornerRadius) { PassportContentTokens(screenCornerRadius) }
    val tokens = remember(palette, shapeTokens, contentTokens, componentTokens) {
        PassportDesignTokens(
            palette = palette,
            shapes = shapeTokens,
            content = contentTokens,
            components = componentTokens,
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalPassportDesignTokens provides tokens) {
        MaterialTheme(
            colorScheme = if (darkTheme) palette.dark else palette.light,
            typography = PassportTypography,
            shapes = shapes,
            content = content,
        )
    }
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

@Composable
fun PassportCornerAction(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    icon: @Composable () -> Unit,
) {
    val tokens = PassportTokens
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(tokens.components.cornerActionSize)
            .alpha(if (enabled) 1f else 0.38f)
            .semantics {
            this.contentDescription = contentDescription
        },
        shape = tokens.shapes.circular,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 1.dp,
        shadowElevation = tokens.components.floatingControlShadowElevation,
        border = floatingControlBorder(),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(24.dp)) { icon() }
        }
    }
}

@Composable
fun PassportCornerScaffold(
    modifier: Modifier = Modifier,
    topStart: (@Composable () -> Unit)? = null,
    topEnd: (@Composable () -> Unit)? = null,
    bottomStart: (@Composable () -> Unit)? = null,
    bottomEnd: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val tokens = PassportTokens
    val edgeInset = tokens.components.floatingHorizontalInset
    val safe = WindowInsets.safeDrawing.asPaddingValues()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val bottomInset = if (imeBottom > safe.calculateBottomPadding()) imeBottom else safe.calculateBottomPadding()
    val topReserve = safe.calculateTopPadding() + tokens.components.overlaySafeGap +
        tokens.components.cornerActionSize + PassportSpacing.md
    val bottomReserve = bottomInset + tokens.components.overlaySafeGap +
        tokens.components.cornerActionSize + PassportSpacing.md

    Box(modifier.fillMaxSize().clearFocusOnTapOutside()) {
        content(PaddingValues(top = topReserve, bottom = bottomReserve))
        if (topStart != null || topEnd != null) Row(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(top = tokens.components.overlaySafeGap, start = edgeInset, end = edgeInset),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (topStart != null) topStart() else Spacer(Modifier.size(tokens.components.cornerActionSize))
            if (topEnd != null) topEnd() else Spacer(Modifier.size(tokens.components.cornerActionSize))
        }
        if (bottomStart != null || bottomEnd != null) Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(bottom = tokens.components.overlaySafeGap, start = edgeInset, end = edgeInset),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (bottomStart != null) bottomStart() else Spacer(Modifier.size(tokens.components.cornerActionSize))
            if (bottomEnd != null) bottomEnd() else Spacer(Modifier.size(tokens.components.cornerActionSize))
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
    actions: (@Composable RowScope.() -> Unit)? = null,
    primaryActionLabel: String? = null,
    primaryActionIcon: ImageVector = Icons.Default.Check,
    primaryActionEnabled: Boolean = true,
    primaryActionLoading: Boolean = false,
    onPrimaryAction: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    @Suppress("UNUSED_VARIABLE") val pageTitle = title
    PassportCornerScaffold(
        modifier = modifier,
        topStart = {
            PassportCornerAction(onClick = onBack, contentDescription = "返回") {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        topEnd = actions?.let { actionContent -> { Row { actionContent() } } },
        bottomStart = if (primaryActionLabel != null && onPrimaryAction != null) {
            {
            PassportCornerAction(
                onClick = onPrimaryAction,
                enabled = primaryActionEnabled,
                contentDescription = primaryActionLabel,
            ) {
                if (primaryActionLoading) CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                ) else Icon(primaryActionIcon, contentDescription = null)
            }
            }
        } else null,
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
            TextButton(onClick = onConfirm, shape = PassportTokens.shapes.buttonShape) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = PassportTokens.shapes.buttonShape) { Text("取消") }
        },
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
        shape = PassportTokens.shapes.cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ListItem(
            headlineContent = { Text(title) },
            leadingContent = {
                Surface(
                    shape = PassportTokens.shapes.buttonShape,
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
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
                    start = PassportSpacing.md,
                    end = PassportSpacing.md,
                    top = padding.calculateTopPadding() + PassportSpacing.md,
                    bottom = padding.calculateBottomPadding() + PassportSpacing.md,
                ),
            ) {
                Text("栗Cafe（长宁馥邦苑店）", style = MaterialTheme.typography.titleMedium)
                Text("距打卡位置 42 m", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
