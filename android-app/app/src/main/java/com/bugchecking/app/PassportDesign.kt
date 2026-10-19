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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

private val PassportInk = Color(0xFF172A3A)
private val RouteBlue = Color(0xFF356A8A)
private val WaypointTeal = Color(0xFF347A74)
private val JourneyAmber = Color(0xFFA76A16)
private val MistSurface = Color(0xFFF6F8FB)
private val NightSurface = Color(0xFF101418)

private val PassportLightScheme = lightColorScheme(
    primary = RouteBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE5F6),
    onPrimaryContainer = PassportInk,
    secondary = WaypointTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCEE9E5),
    onSecondaryContainer = Color(0xFF0B302D),
    tertiary = JourneyAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB0),
    onTertiaryContainer = Color(0xFF351F00),
    surface = MistSurface,
    onSurface = PassportInk,
)

private val PassportDarkScheme = darkColorScheme(
    primary = Color(0xFF9CCBEB),
    onPrimary = Color(0xFF00344D),
    primaryContainer = Color(0xFF164E68),
    onPrimaryContainer = Color(0xFFCDE5F6),
    secondary = Color(0xFF92D2CB),
    onSecondary = Color(0xFF003733),
    secondaryContainer = Color(0xFF18504B),
    onSecondaryContainer = Color(0xFFCEE9E5),
    tertiary = Color(0xFFF4BD72),
    onTertiary = Color(0xFF462A00),
    tertiaryContainer = Color(0xFF654000),
    onTertiaryContainer = Color(0xFFFFDDB0),
    surface = NightSurface,
    onSurface = Color(0xFFE2E8EE),
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
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
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

enum class PassportSyncStatus(val label: String) {
    LOCAL_SAVED("已保存到手机"),
    PENDING("等待同步"),
    SYNCING("正在同步"),
    SYNCED("已同步到 Passport"),
    FAILED("同步失败，可重试"),
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
        modifier = modifier,
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
            Text(
                "按实际打卡坐标打开，不会修改这条轨迹",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = PassportSpacing.xxs, bottom = PassportSpacing.md),
            )
            MapProviderChoice(
                title = "高德地图",
                description = "适合查看中国大陆地点",
                icon = { Icon(Icons.Default.Map, contentDescription = null) },
                onClick = onAmap,
            )
            Spacer(Modifier.size(PassportSpacing.xs))
            MapProviderChoice(
                title = "Google Maps",
                description = "适合查看海外地点",
                icon = { Icon(Icons.Default.Public, contentDescription = null) },
                onClick = onGoogle,
            )
        }
    }
}

@Composable
private fun MapProviderChoice(
    title: String,
    description: String,
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
            supportingContent = { Text(description) },
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
