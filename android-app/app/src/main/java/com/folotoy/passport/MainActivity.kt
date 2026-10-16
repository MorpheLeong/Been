package com.folotoy.passport

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.bluetooth.BluetoothManager
import android.provider.Settings
import android.provider.OpenableColumns
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import java.text.DateFormat
import java.util.Date
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    private var permissionContinuation: (() -> Unit)? = null
    private val bleState = mutableStateOf("未连接")
    private val bleConnected = mutableStateOf(false)
    private val dataRevision = mutableIntStateOf(0)
    private val pendingShare = mutableStateOf<ImportSource?>(null)
    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.getStringExtra(PassportBleService.EXTRA_STATE)?.let { bleState.value = it }
            bleConnected.value = intent.getBooleanExtra(PassportBleService.EXTRA_CONNECTED, false)
            if (intent.getBooleanExtra(PassportBleService.EXTRA_DATA_CHANGED, false)) dataRevision.intValue++
        }
    }
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val darkStatusBar = resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkStatusBar
            isAppearanceLightNavigationBars = !darkStatusBar
        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val continuation = permissionContinuation
            permissionContinuation = null
            if (hasRequiredPermissions()) continuation?.invoke()
            else Toast.makeText(this, "需要附近设备和定位权限才能连接 Passport", Toast.LENGTH_LONG).show()
        }
        val filter = IntentFilter(PassportBleService.STATE)
        ContextCompat.registerReceiver(this, stateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        pendingShare.value = decodeShare(intent)
        setContent {
            val context = LocalContext.current
            val db = remember { PassportDb(context) }
            val windowSizeClass = calculateWindowSizeClass(this)
            PassportTheme {
                PassportApp(db, bleState.value, bleConnected.value, dataRevision.intValue,
                    windowSizeClass.widthSizeClass,
                    { requestBlePermissions { connectOrOpenSettings() } },
                    { stopBle() },
                    { requestBlePermissions { requestSync() } },
                    pendingShare.value,
                    { pendingShare.value = null })
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingShare.value = decodeShare(intent)
    }
    @Suppress("DEPRECATION")
    private fun decodeShare(intent: Intent?): ImportSource? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        if (uri != null) {
            val displayName = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            return ImportSource.Document(uri, intent.type, displayName)
        }
        return intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf(String::isNotBlank)?.let(ImportSource::Pasted)
    }
    private fun hasRequiredPermissions(): Boolean {
        val locationReady = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val bluetoothReady = Build.VERSION.SDK_INT < 31 ||
            (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
        return locationReady && bluetoothReady
    }
    private fun requestBlePermissions(done: () -> Unit) {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= 31) { add(Manifest.permission.BLUETOOTH_SCAN); add(Manifest.permission.BLUETOOTH_CONNECT) }
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
        val missing = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) done() else {
            permissionContinuation = done
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
    private fun startBle() {
        ContextCompat.startForegroundService(this, Intent(this, PassportBleService::class.java).setAction(PassportBleService.ACTION_START))
    }
    @Suppress("MissingPermission")
    private fun connectOrOpenSettings() {
        val adapter = getSystemService(BluetoothManager::class.java)?.adapter
        val hasBondedPassport = runCatching {
            adapter?.bondedDevices?.any { it.name == "AI Passport" } == true
        }.getOrDefault(false)
        if (!hasBondedPassport) {
            Toast.makeText(this, "请先在系统蓝牙设置中配对 AI Passport", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            return
        }
        startBle()
    }
    private fun stopBle() {
        startService(Intent(this, PassportBleService::class.java).setAction(PassportBleService.ACTION_STOP))
    }
    private fun requestSync() {
        ContextCompat.startForegroundService(this, Intent(this, PassportBleService::class.java).setAction(PassportBleService.ACTION_SYNC))
    }
    override fun onDestroy() { runCatching { unregisterReceiver(stateReceiver) }; super.onDestroy() }
}

@Composable
private fun PassportTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isDark = configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
    val scheme = if (Build.VERSION.SDK_INT >= 31) {
        if (isDark) androidx.compose.material3.dynamicDarkColorScheme(context)
        else androidx.compose.material3.dynamicLightColorScheme(context)
    } else if (isDark) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()
    MaterialTheme(
        colorScheme = scheme,
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
        ),
        content = content,
    )
}
private enum class Tab(val title: String, val label: String, val icon: ImageVector) {
    Tracks("轨迹记录", "轨迹", Icons.Default.Route),
    Checklist("打卡清单", "清单", Icons.Default.Checklist),
    Qr("刷我的码", "刷码", Icons.Default.QrCode2),
    Me("我", "我", Icons.Default.Person),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassportApp(
    db: PassportDb,
    bleState: String,
    bleConnected: Boolean,
    externalRevision: Int,
    widthSizeClass: WindowWidthSizeClass,
    connect: () -> Unit,
    disconnect: () -> Unit,
    sync: () -> Unit,
    sharedSource: ImportSource?,
    onSharedSourceConsumed: () -> Unit,
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(Tab.Tracks) }
    var revision by remember { mutableIntStateOf(0) }
    var importing by remember { mutableStateOf(false) }
    var initialImportSource by remember { mutableStateOf<ImportSource?>(null) }
    var editingTrack by remember { mutableStateOf<TrackPoint?>(null) }
    var initialMapLink by remember { mutableStateOf<String?>(null) }
    var pendingMapShare by remember { mutableStateOf<String?>(null) }
    var mapMode by remember { mutableStateOf(false) }
    val destinations = remember(revision, externalRevision) { db.destinations() }
    val qrs = remember(revision, externalRevision) { db.qrs() }
    val tracks = remember(revision, externalRevision) { db.tracks() }
    val profile = remember(revision, externalRevision) { db.profile() }
    val volume = remember(revision, externalRevision) { db.soundVolume() }
    val screenTimeout = remember(revision, externalRevision) { db.screenTimeoutSeconds() }
    val refresh: () -> Unit = { revision++ }
    val compact = widthSizeClass == WindowWidthSizeClass.Compact
    LaunchedEffect(sharedSource) {
        if (sharedSource == null) return@LaunchedEffect
        val sharedText = (sharedSource as? ImportSource.Pasted)?.value
        val mapUrl = sharedText?.let(PlaceLinkParser::firstUrl)?.takeIf(PlaceLinkParser::isSupportedUrl)
        if (mapUrl != null) {
            if (tracks.isEmpty()) Toast.makeText(context, "还没有可关联的轨迹记录", Toast.LENGTH_LONG).show()
            else pendingMapShare = sharedText
        } else {
            initialImportSource = sharedSource
            importing = true
        }
        onSharedSourceConsumed()
    }
    if (mapMode) {
        TrackMapScreen(rows = tracks, onBack = { mapMode = false }, onEdit = { editingTrack = it })
    } else Row(Modifier.fillMaxSize()) {
        if (!compact) {
            NavigationRail {
                Spacer(Modifier.size(12.dp))
                Tab.entries.forEach { item ->
                    NavigationRailItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        }
        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(tab.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (bleConnected) "设备已连接" else bleState,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = sync) {
                            Icon(Icons.Default.Sync, contentDescription = "同步到 Passport")
                        }
                    },
                )
            },
            bottomBar = {
                if (compact) NavigationBar {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                Column(
                    Modifier.widthIn(max = 840.dp).fillMaxSize().align(Alignment.TopCenter)
                        .padding(horizontal = if (compact) 16.dp else 24.dp),
                ) {
                    when (tab) {
                        Tab.Tracks -> TracksPage(tracks, { editingTrack = it }, { mapMode = true })
                        Tab.Checklist -> ChecklistPage(db, destinations, refresh, { importing = true })
                        Tab.Qr -> QrPage(db, qrs, destinations, refresh)
                        Tab.Me -> ProfileSettingsPage(
                            profile, volume, screenTimeout, bleState, bleConnected, connect, disconnect,
                            onSave = { value ->
                                runCatching { db.saveProfile(value) }
                                    .onSuccess { refresh(); Toast.makeText(context, "已保存；点右上角同步到 Passport", Toast.LENGTH_SHORT).show() }
                                    .onFailure { Toast.makeText(context, "个人资料保存失败", Toast.LENGTH_LONG).show() }
                            },
                            onAvatarChange = { path ->
                                db.setAvatarPath(path); refresh()
                                Toast.makeText(context, "头像已保存；点右上角同步到 Passport", Toast.LENGTH_SHORT).show()
                            },
                            onVolumeSave = { value -> db.setSoundVolume(value); refresh() },
                            onScreenTimeoutSave = { value ->
                                db.setScreenTimeoutSeconds(value); refresh()
                                Toast.makeText(context, "已保存；点右上角同步到 Passport", Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }
            }
        }
    }
    pendingMapShare?.let { sharedText ->
        AlertDialog(
            onDismissRequest = { pendingMapShare = null },
            title = { Text("选择要更新的轨迹") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tracks, key = { it.id }) { track ->
                        TextButton(onClick = {
                            initialMapLink = sharedText
                            editingTrack = track
                            pendingMapShare = null
                        }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(track.name, fontWeight = FontWeight.Medium)
                                Text(formatTrackDate(track), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pendingMapShare = null }) { Text("取消") } },
        )
    }
    if (importing) SmartImportDialog(initialImportSource, destinations.map { it.name }.toSet(), onDismiss = {
        importing = false; initialImportSource = null
    }) { names ->
        val added = db.addDestinations(names)
        if (added < names.size) Toast.makeText(context, "仅导入 $added 项：Passport 一份清单最多同步 64 个目的地", Toast.LENGTH_LONG).show()
        refresh(); importing = false; initialImportSource = null
    }
    editingTrack?.let { track ->
        TrackEditorDialog(
            track,
            initialLink = initialMapLink,
            onDismiss = { editingTrack = null; initialMapLink = null },
            onDelete = { db.deleteTrack(track.id); refresh(); editingTrack = null },
            onSave = { name, lat, lon -> db.updateTrack(track.id, name, lat, lon); refresh(); editingTrack = null; initialMapLink = null },
            onApplyPlace = { place -> db.updateTrackPlace(track.id, place); refresh(); editingTrack = null; initialMapLink = null },
        )
    }
}

private fun formatTrackDate(track: TrackPoint): String =
    "${DateFormat.getDateTimeInstance().format(Date(track.time))} · ${track.lat}, ${track.lon}"

@Composable
private fun ProfileSettingsPage(
    profile: PassportProfile,
    volume: Int,
    screenTimeout: Int,
    bleState: String,
    bleConnected: Boolean,
    connect: () -> Unit,
    disconnect: () -> Unit,
    onSave: (PassportProfile) -> Unit,
    onAvatarChange: (String?) -> Unit,
    onVolumeSave: (Int) -> Unit,
    onScreenTimeoutSave: (Int) -> Unit,
) {
    var nickname by remember(profile) { mutableStateOf(profile.nickname) }
    var bio by remember(profile) { mutableStateOf(profile.bio) }
    var socialIds by remember(profile) { mutableStateOf(profile.socialIds.joinToString("\n")) }
    var volumeDraft by remember(volume) { mutableFloatStateOf(volume.toFloat()) }
    var timeoutMenuOpen by remember { mutableStateOf(false) }
    var cropSource by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var avatarBusy by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val avatar = remember(profile.avatarPath) { AvatarImage.loadPreview(profile.avatarPath) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            avatarBusy = true; avatarError = null
            runCatching { withContext(Dispatchers.IO) { AvatarImage.decode(context, uri) } }
                .onSuccess { cropSource = it }
                .onFailure { avatarError = it.message ?: "无法读取头像" }
            avatarBusy = false
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ) {
                        Icon(
                            if (bleConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.padding(12.dp).size(24.dp),
                        )
                    }
                    Column(Modifier.weight(1f).padding(start = 16.dp)) {
                        Text("AI Passport", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(bleState, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                if (bleConnected) {
                    OutlinedButton(onClick = disconnect, modifier = Modifier.fillMaxWidth()) { Text("断开连接") }
                } else {
                    Button(onClick = connect, modifier = Modifier.fillMaxWidth()) { Text("连接设备") }
                }
            }
        }

        Text("个人资料", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        modifier = Modifier.size(88.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        if (avatar != null) Image(
                            avatar.asImageBitmap(), "当前头像", Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                        ) else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(38.dp))
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { avatarPicker.launch(arrayOf("image/*")) },
                            enabled = !avatarBusy,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (avatarBusy) "正在读取…" else if (avatar == null) "从本地选择头像" else "更换头像") }
                        if (profile.avatarPath != null) OutlinedButton(onClick = {
                            AvatarImage.remove(profile.avatarPath); onAvatarChange(null)
                        }, modifier = Modifier.fillMaxWidth()) { Text("移除头像") }
                    }
                }
                Text("裁切比例固定为 1:1，可拖动和双指缩放；手机保留裁切后的高清原图。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                avatarError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                OutlinedTextField(nickname, { nickname = it }, label = { Text("机主昵称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(bio, { bio = it }, label = { Text("简介") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(socialIds, { socialIds = it }, label = { Text("社交账号 ID") }, supportingText = { Text("每行一个，最多 4 个") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Text("设备字库可能无法显示生僻字。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = {
                    val accounts = socialIds.lineSequence().map(String::trim).filter(String::isNotEmpty).take(4).toList()
                    onSave(PassportProfile(nickname, bio, accounts, profile.avatarPath))
                }, modifier = Modifier.fillMaxWidth()) { Text("保存资料") }
            }
        }

        Text("设备设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("提示音量", style = MaterialTheme.typography.titleSmall)
                    Text("${volumeDraft.toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Slider(value = volumeDraft, onValueChange = { volumeDraft = it }, valueRange = 0f..100f,
                    onValueChangeFinished = { onVolumeSave(volumeDraft.toInt()) })
                Text("0% 为静音", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("亮屏时长", style = MaterialTheme.typography.titleSmall)
                Box {
                    OutlinedButton(onClick = { timeoutMenuOpen = true }) {
                        Text(if (screenTimeout < 60) "$screenTimeout 秒" else "${screenTimeout / 60} 分钟")
                    }
                    DropdownMenu(expanded = timeoutMenuOpen, onDismissRequest = { timeoutMenuOpen = false }) {
                        listOf(15, 30, 60, 120, 300, 600).forEach { seconds ->
                            DropdownMenuItem(text = {
                                Text(if (seconds < 60) "$seconds 秒" else "${seconds / 60} 分钟")
                            }, onClick = { timeoutMenuOpen = false; onScreenTimeoutSave(seconds) })
                        }
                    }
                }
                Text("按键操作后重新计时；息屏后蓝牙仍保持运行。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    cropSource?.let { source ->
        AvatarCropDialog(
            source = source,
            busy = avatarBusy,
            onDismiss = { if (!avatarBusy) { source.recycle(); cropSource = null } },
            onConfirm = { viewport, scale, offset ->
                scope.launch {
                    avatarBusy = true; avatarError = null
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val cropped = AvatarImage.cropSquare(source, viewport, scale, offset)
                            try { AvatarImage.save(context, cropped, profile.avatarPath) }
                            finally { cropped.recycle() }
                        }
                    }.onSuccess { path ->
                        source.recycle(); cropSource = null; onAvatarChange(path)
                    }.onFailure { avatarError = it.message ?: "头像保存失败" }
                    avatarBusy = false
                }
            },
        )
    }
}

@Composable
private fun AvatarCropDialog(
    source: android.graphics.Bitmap,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (IntSize, Float, Offset) -> Unit,
) {
    var scale by remember(source) { mutableFloatStateOf(1f) }
    var offset by remember(source) { mutableStateOf(Offset.Zero) }
    var viewport by remember(source) { mutableStateOf(IntSize.Zero) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("裁切头像") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                        .background(androidx.compose.ui.graphics.Color.Black)
                        .onSizeChanged {
                            viewport = it
                            offset = AvatarImage.clampOffset(source, viewport, scale, offset)
                        }
                        .pointerInput(source, viewport) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 8f)
                                scale = nextScale
                                offset = AvatarImage.clampOffset(source, viewport, nextScale, offset + pan)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        source.asImageBitmap(), "头像裁切预览",
                        Modifier.fillMaxSize().graphicsLayer {
                            scaleX = scale; scaleY = scale
                            translationX = offset.x; translationY = offset.y
                        },
                        contentScale = ContentScale.Crop,
                    )
                }
                Text("拖动调整位置，双指缩放大小", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(viewport, scale, offset) }, enabled = !busy && viewport.width > 0) {
                Text(if (busy) "正在保存…" else "使用此裁切")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") } },
    )
}

@Composable
private fun TracksPage(rows: List<TrackPoint>, edit: (TrackPoint) -> Unit, openMap: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("最近记录", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = openMap) { Icon(Icons.Default.Map, null); Spacer(Modifier.width(8.dp)); Text("地图模式") }
        }
        if (rows.isEmpty()) Text("还没有轨迹记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { it.id }) { row -> Card(modifier = Modifier.fillMaxWidth().clickable { edit(row) }) { Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text(row.name, fontWeight = FontWeight.Medium)
                Text("${DateFormat.getDateTimeInstance().format(Date(row.time))} · ${row.lat}, ${row.lon}", style = MaterialTheme.typography.bodySmall)
            } } }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChecklistPage(db: PassportDb, destinations: List<Destination>, refresh: () -> Unit, import: () -> Unit) {
    val context = LocalContext.current
    var adding by remember { mutableStateOf(false) }
    var editingDestination by remember { mutableStateOf<Destination?>(null) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { adding = true }, modifier = Modifier.weight(1f)) { Text("添加") }
            OutlinedButton(onClick = import, modifier = Modifier.weight(1f)) { Text("导入") }
        }
        Text("长按卡片并拖动排序", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(destinations, key = { _, item -> item.id }) { index, destination ->
                val containerColor = if (destination.done) MaterialTheme.colorScheme.surfaceContainerLow
                    else MaterialTheme.colorScheme.secondaryContainer
                val contentColor = if (destination.done) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSecondaryContainer
                Card(
                    colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
                    modifier = Modifier.fillMaxWidth().clickable { editingDestination = destination }.pointerInput(destination.id, destinations) {
                    detectDragGesturesAfterLongPress(onDragEnd = { dragDistance = 0f }) { change, amount ->
                        change.consume(); dragDistance += amount.y
                        val steps = (dragDistance / 84f).toInt()
                        if (steps != 0) {
                            val from = destinations.indexOfFirst { it.id == destination.id }
                            db.reorder(from, (from + steps).coerceIn(destinations.indices))
                            dragDistance -= steps * 84f; refresh()
                        }
                    }
                }) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(12.dp), color = contentColor.copy(alpha = 0.10f)) {
                            Text(
                                (index + 1).toString().padStart(2, '0'),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = contentColor,
                            )
                        }
                        Text(
                            destination.name,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = contentColor,
                        )
                        if (destination.qrId != null) {
                            Icon(Icons.Default.Link, contentDescription = "已绑定二维码", tint = contentColor)
                        }
                        Icon(Icons.Default.DragHandle, contentDescription = "长按拖动排序", tint = contentColor.copy(alpha = 0.65f))
                    }
                }
            }
        }
    }
    if (adding) DestinationDialog(onDismiss = { adding = false }) { name ->
        runCatching { db.addDestination(name) }
            .onSuccess { refresh(); adding = false }
            .onFailure { Toast.makeText(context, it.message ?: "添加失败", Toast.LENGTH_LONG).show() }
    }
    editingDestination?.let { destination ->
        DestinationDialog(
            destination = destination,
            onDismiss = { editingDestination = null },
            onDelete = { db.deleteDestination(destination.id); refresh(); editingDestination = null },
            onSave = { name -> db.updateDestination(destination.id, name); refresh(); editingDestination = null },
        )
    }
}

@Composable
private fun QrPage(db: PassportDb, qrs: List<PassportQr>, destinations: List<Destination>, refresh: () -> Unit) {
    var showEditor by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf<PassportQr?>(null) }
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("最多星标 3 个；设备最多缓存 10 个", style = MaterialTheme.typography.titleSmall)
                Text("二维码原文保存在手机本地，仅同步星标码和清单关联码。", style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(onClick = { current = null; showEditor = true }) { Text("添加二维码") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(qrs, key = { it.id }) { qr ->
                val boundDestination = destinations.firstOrNull { it.qrId == qr.id }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(qr.name, fontWeight = FontWeight.Medium)
                        val details = buildList {
                            add(if (qr.starred) "星标同步" else "仅保存在手机")
                            boundDestination?.let { add("绑定 ${it.name}") }
                        }.joinToString(" · ")
                        Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { current = qr; showEditor = true }) { Text("编辑") }
                    IconButton(onClick = { db.deleteQr(qr.id); refresh() }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除 ${qr.name}")
                    }
                } }
            }
        }
    }
    if (showEditor) QrEditorDialog(
        current,
        db.qrs().count { it.starred },
        destinations,
        destinations.firstOrNull { it.qrId == current?.id }?.id,
        onDismiss = { showEditor = false },
    ) { name, payload, starred, destinationId ->
        runCatching {
            val qrId = db.addQr(name, payload, starred, current?.id)
            db.bindQrToDestination(qrId, destinationId)
        }
            .onSuccess { showEditor = false; refresh() }
            .onFailure { Toast.makeText(context, it.message ?: "保存失败", Toast.LENGTH_LONG).show() }
    }
}

@Composable
private fun SmartImportDialog(
    initialSource: ImportSource?,
    existingNames: Set<String>,
    onDismiss: () -> Unit,
    onImport: (List<String>) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pasted by remember { mutableStateOf("") }
    var extraction by remember { mutableStateOf<ImportExtraction?>(null) }
    var candidates by remember { mutableStateOf<List<ImportCandidate>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var columnMenuExpanded by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }

    fun applyExtraction(found: ImportExtraction) {
        val normalizedExisting = existingNames.map { it.trim().lowercase() }.toSet()
        extraction = found
        candidates = found.candidates.map { item ->
            item.copy(duplicate = item.duplicate || item.text.trim().lowercase() in normalizedExisting)
        }
        selected = candidates.filterNot { it.duplicate }.map { it.id }.toSet()
        progress = if (candidates.isEmpty()) "没有识别到可导入项目" else "识别到 ${candidates.size} 项，请确认"
    }

    fun start(source: ImportSource) {
        job?.cancel()
        error = null
        extraction = null
        job = scope.launch {
            runCatching { ImportEngine.extract(context, source) { progress = it } }
                .onSuccess(::applyExtraction)
                .onFailure { failure -> if (failure !is kotlinx.coroutines.CancellationException) error = failure.message ?: "解析失败" }
            job = null
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            start(ImportSource.Document(uri, context.contentResolver.getType(uri), name))
        }
    }
    LaunchedEffect(initialSource) { initialSource?.let(::start) }

    AlertDialog(
        onDismissRequest = { job?.cancel(); onDismiss() },
        title = { Text("智能导入清单") },
        text = {
            Column(Modifier.heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (extraction == null) {
                    OutlinedTextField(
                        pasted, { pasted = it }, label = { Text("粘贴清单或公开网页链接") },
                        minLines = 4, modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { start(ImportSource.Pasted(pasted)) }, enabled = pasted.isNotBlank() && job == null) { Text("解析粘贴内容") }
                        OutlinedButton(onClick = {
                            fileLauncher.launch(arrayOf(
                                "text/*", "image/*", "application/pdf", "application/vnd.ms-excel",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            ))
                        }, enabled = job == null) { Text("选择文件") }
                    }
                } else {
                    val result = extraction!!
                    if (result.tables.isNotEmpty()) {
                        Text("已识别为表格", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Box {
                            OutlinedButton(onClick = { columnMenuExpanded = true }) {
                                Text("正在导入：${result.selectedColumn ?: "请选择列"}")
                            }
                            DropdownMenu(expanded = columnMenuExpanded, onDismissRequest = { columnMenuExpanded = false }) {
                                result.availableColumns.forEach { column ->
                                    DropdownMenuItem(
                                        text = { Text(column) },
                                        onClick = {
                                            columnMenuExpanded = false
                                            applyExtraction(ImportEngine.selectColumn(result, column))
                                        },
                                    )
                                }
                            }
                        }
                    }
                    result.notice?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Text("可编辑、取消勾选或调整顺序；已有项默认不选。", style = MaterialTheme.typography.bodySmall)
                    if (candidates.isEmpty()) {
                        Text("当前列没有可导入内容，可切换其他列或关闭后重新选择来源。", style = MaterialTheme.typography.bodyMedium)
                    }
                    LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(candidates, key = { _, item -> item.id }) { index, item ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(item.id in selected, onCheckedChange = { checked ->
                                            selected = if (checked) selected + item.id else selected - item.id
                                        })
                                        OutlinedTextField(
                                            item.text,
                                            { value -> candidates = candidates.toMutableList().also { it[index] = item.copy(text = value) } },
                                            modifier = Modifier.weight(1f), singleLine = true,
                                        )
                                        TextButton(onClick = {
                                            candidates = candidates.toMutableList().also { it.removeAt(index) }
                                            selected = selected - item.id
                                        }) { Text("删除") }
                                    }
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${item.source} · ${confidenceLabel(item.confidence)}${if (item.duplicate) " · 已存在" else ""}", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                        TextButton(onClick = {
                                            if (index > 0) candidates = candidates.toMutableList().also { val moved = it.removeAt(index); it.add(index - 1, moved) }
                                        }, enabled = index > 0) { Text("↑") }
                                        TextButton(onClick = {
                                            if (index < candidates.lastIndex) candidates = candidates.toMutableList().also { val moved = it.removeAt(index); it.add(index + 1, moved) }
                                        }, enabled = index < candidates.lastIndex) { Text("↓") }
                                    }
                                }
                            }
                        }
                    }
                }
                if (job != null) Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp)); Text(progress, Modifier.padding(start = 8.dp))
                    TextButton(onClick = { job?.cancel(); job = null; progress = "已取消" }) { Text("取消解析") }
                } else if (progress.isNotBlank()) Text(progress, style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(candidates.filter { it.id in selected }.map { it.text.trim() }.filter(String::isNotEmpty)) },
                enabled = selected.isNotEmpty() && job == null,
            ) { Text("确认导入 ${selected.size} 项") }
        },
        dismissButton = { TextButton(onClick = { job?.cancel(); onDismiss() }) { Text("关闭") } },
    )
}

private fun confidenceLabel(value: ImportConfidence) = when (value) {
    ImportConfidence.HIGH -> "高置信度"
    ImportConfidence.MEDIUM -> "中置信度"
    ImportConfidence.LOW -> "低置信度"
}
@Composable
private fun DestinationDialog(
    destination: Destination? = null,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (String) -> Unit,
) {
    var text by remember(destination?.id) { mutableStateOf(destination?.name.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (destination == null) "添加地点" else "编辑地点") },
        text = { OutlinedTextField(text, { text = it }, label = { Text("目的地") }, singleLine = true) },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = { onSave(text) }) { Text("保存") }
            }
        })
}
@Composable
private fun QrEditorDialog(
    qr: PassportQr?,
    starredCount: Int,
    destinations: List<Destination>,
    boundDestinationId: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean, String?) -> Unit,
) {
    var name by remember(qr?.id) { mutableStateOf(qr?.name.orEmpty()) }
    var payload by remember(qr?.id) { mutableStateOf(qr?.payload.orEmpty()) }
    var starred by remember(qr?.id) { mutableStateOf(qr?.starred ?: false) }
    var selectedDestinationId by remember(qr?.id, boundDestinationId) { mutableStateOf(boundDestinationId) }
    var destinationMenuOpen by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (qr == null) "添加二维码" else "编辑二维码") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("名称") }, singleLine = true)
            OutlinedTextField(payload, { payload = it }, label = { Text("二维码原文") }, minLines = 3)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("星标同步", style = MaterialTheme.typography.titleSmall)
                    Text("最多 3 个", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = starred,
                    enabled = starred || qr?.starred == true || starredCount < 3,
                    onCheckedChange = { starred = it },
                )
            }
            Text("绑定打卡项目", style = MaterialTheme.typography.titleSmall)
            Box {
                OutlinedButton(onClick = { destinationMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(destinations.firstOrNull { it.id == selectedDestinationId }?.name ?: "不绑定")
                }
                DropdownMenu(expanded = destinationMenuOpen, onDismissRequest = { destinationMenuOpen = false }) {
                    DropdownMenuItem(text = { Text("不绑定") }, onClick = {
                        selectedDestinationId = null; destinationMenuOpen = false
                    })
                    destinations.forEach { destination ->
                        DropdownMenuItem(text = { Text(destination.name) }, onClick = {
                            selectedDestinationId = destination.id; destinationMenuOpen = false
                        })
                    }
                }
            }
            Text("原文按 UTF-8 最多 192 字节；二维码扫描效果需在 Passport 屏幕上验证。", style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton(onClick = { onSave(name, payload, starred, selectedDestinationId) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun TrackEditorDialog(
    track: TrackPoint,
    initialLink: String? = null,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (String, Double, Double) -> Unit,
    onApplyPlace: (PlaceResolution) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember(track.id) { mutableStateOf(track.name) }
    var latitude by remember(track.id) { mutableStateOf(track.lat.toString()) }
    var longitude by remember(track.id) { mutableStateOf(track.lon.toString()) }
    var query by remember(track.id) { mutableStateOf("") }
    var results by remember(track.id) { mutableStateOf<List<Address>>(emptyList()) }
    var message by remember(track.id) { mutableStateOf("") }
    var linkText by remember(track.id, initialLink) { mutableStateOf(initialLink ?: track.originalUrl.orEmpty()) }
    var linkPreview by remember(track.id) { mutableStateOf<PlaceResolution?>(null) }
    var linkPreviewName by remember(track.id) { mutableStateOf("") }
    var parsingLink by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("编辑轨迹位置") },
        text = { Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("位置名称") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(latitude, { latitude = it }, label = { Text("纬度") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(longitude, { longitude = it }, label = { Text("经度") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    ExternalMapLauncher.openAmap(context, track.copy(
                        name = name.ifBlank { track.name }, lat = latitude.toDoubleOrNull() ?: track.lat,
                        lon = longitude.toDoubleOrNull() ?: track.lon,
                    ))
                }, modifier = Modifier.weight(1f)) { Text("高德查看") }
                OutlinedButton(onClick = {
                    ExternalMapLauncher.openGoogle(context, track.copy(
                        name = name.ifBlank { track.name }, lat = latitude.toDoubleOrNull() ?: track.lat,
                        lon = longitude.toDoubleOrNull() ?: track.lon,
                    ))
                }, modifier = Modifier.weight(1f)) { Text("Google 查看") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(query, { query = it }, label = { Text("搜索地点") }, modifier = Modifier.weight(1f), singleLine = true)
                TextButton(onClick = {
                    if (!Geocoder.isPresent() || query.isBlank()) {
                        message = "系统地点搜索当前不可用"; return@TextButton
                    }
                    scope.launch {
                        val found = runCatching {
                            withContext(Dispatchers.IO) { Geocoder(context).getFromLocationName(query, 6).orEmpty() }
                        }.getOrElse { emptyList() }
                        results = found
                        message = if (found.isEmpty()) "没有找到地点，可直接编辑经纬度" else "选择一个搜索结果设置名称和坐标"
                    }
                }) { Text("搜索") }
            }
            if (message.isNotEmpty()) Text(message, style = MaterialTheme.typography.bodySmall)
            results.forEach { address ->
                val resultName = address.featureName ?: address.getAddressLine(0) ?: query
                TextButton(onClick = {
                    name = resultName
                    latitude = address.latitude.toString()
                    longitude = address.longitude.toString()
                    results = emptyList()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("$resultName  ·  ${address.latitude}, ${address.longitude}")
                }
            }
            Text("搜索结果由 Android 系统 Geocoder 提供；也可以直接输入地图上选定的坐标。", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                linkText, { linkText = it; linkPreview = null; linkPreviewName = "" }, label = { Text("高德或 Google 地图分享链接") },
                minLines = 2, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = {
                parsingLink = true
                scope.launch {
                    val result = runCatching {
                        val original = PlaceLinkParser.firstUrl(linkText)
                        val direct = PlaceLinkParser.parse(linkText)
                        val expanded = if (
                            original != null && PlaceLinkParser.needsRedirect(original) &&
                            (direct?.provider == PlaceProvider.GOOGLE && direct.name.isNullOrBlank())
                        ) {
                            withContext(Dispatchers.IO) { PlaceRedirectResolver.expand(original) }
                        } else null
                        PlaceLinkParser.parse(linkText, expanded)?.takeIf { !it.name.isNullOrBlank() }
                            ?: error("没有从分享内容中识别到场所名称")
                    }
                    parsingLink = false
                    result.onSuccess {
                        linkPreview = it
                        linkPreviewName = it.name.orEmpty()
                        message = "请核对场所名称；确认后只更新名称，不修改打卡坐标"
                    }
                        .onFailure { message = it.message ?: "链接解析失败" }
                }
            }, enabled = linkText.isNotBlank() && !parsingLink) { Text(if (parsingLink) "解析中…" else "解析链接") }
            linkPreview?.let { preview ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            linkPreviewName, { linkPreviewName = it }, label = { Text("识别到的场所名称") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                        )
                        Text("${providerLabel(preview.provider)} · 保留打卡坐标 ${track.lat}, ${track.lon}", style = MaterialTheme.typography.bodySmall)
                        preview.canonicalUrl?.let { Text(it, style = MaterialTheme.typography.labelSmall, maxLines = 2) }
                        Button(
                            onClick = { onApplyPlace(preview.copy(name = linkPreviewName.trim())) },
                            enabled = linkPreviewName.isNotBlank(),
                        ) { Text("确认并更新场所名称") }
                    }
                }
            }
        } },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = {
                    val lat = latitude.toDoubleOrNull(); val lon = longitude.toDoubleOrNull()
                    if (name.isBlank() || lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
                        message = "请输入有效的位置名称和经纬度"
                    } else onSave(name, lat, lon)
                }) { Text("保存") }
            }
        })
}

private fun providerLabel(provider: PlaceProvider) = when (provider) {
    PlaceProvider.MANUAL -> "手动"
    PlaceProvider.SYSTEM -> "系统地点"
    PlaceProvider.AMAP -> "高德地图"
    PlaceProvider.GOOGLE -> "Google Maps"
}
