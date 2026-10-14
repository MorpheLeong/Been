package com.folotoy.passport

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.bluetooth.BluetoothManager
import android.provider.Settings
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import java.text.DateFormat
import java.util.Date
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    private var permissionContinuation: (() -> Unit)? = null
    private val bleState = mutableStateOf("未连接")
    private val bleConnected = mutableStateOf(false)
    private val dataRevision = mutableIntStateOf(0)
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
        setContent {
            val context = LocalContext.current
            val db = remember { PassportDb(context) }
            val windowSizeClass = calculateWindowSizeClass(this)
            PassportTheme {
                PassportApp(db, bleState.value, bleConnected.value, dataRevision.intValue,
                    windowSizeClass.widthSizeClass,
                    { requestBlePermissions { connectOrOpenSettings() } },
                    { stopBle() },
                    { requestBlePermissions { requestSync() } })
            }
        }
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
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(Tab.Tracks) }
    var revision by remember { mutableIntStateOf(0) }
    var importing by remember { mutableStateOf(false) }
    var editingTrack by remember { mutableStateOf<TrackPoint?>(null) }
    val destinations = remember(revision, externalRevision) { db.destinations() }
    val qrs = remember(revision, externalRevision) { db.qrs() }
    val tracks = remember(revision, externalRevision) { db.tracks() }
    val profile = remember(revision, externalRevision) { db.profile() }
    val volume = remember(revision, externalRevision) { db.soundVolume() }
    val screenTimeout = remember(revision, externalRevision) { db.screenTimeoutSeconds() }
    val refresh: () -> Unit = { revision++ }
    val compact = widthSizeClass == WindowWidthSizeClass.Compact
    Row(Modifier.fillMaxSize()) {
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
                                bleState,
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
                        Tab.Tracks -> TracksPage(tracks, { editingTrack = it })
                        Tab.Checklist -> ChecklistPage(db, destinations, refresh, { importing = true })
                        Tab.Qr -> QrPage(db, qrs, destinations, refresh)
                        Tab.Me -> ProfileSettingsPage(
                            profile, volume, screenTimeout, bleState, bleConnected, connect, disconnect,
                            onSave = { value ->
                                runCatching { db.saveProfile(value) }
                                    .onSuccess { refresh(); Toast.makeText(context, "已保存；点右上角同步到 Passport", Toast.LENGTH_SHORT).show() }
                                    .onFailure { Toast.makeText(context, "个人资料保存失败", Toast.LENGTH_LONG).show() }
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
    if (importing) ImportDialog(onDismiss = { importing = false }) { text ->
        val names = text.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        val capacity = (64 - db.destinations().size).coerceAtLeast(0)
        names.take(capacity).forEach(db::addDestination)
        if (names.size > capacity) Toast.makeText(context, "Passport 一份清单最多同步 64 个目的地", Toast.LENGTH_LONG).show()
        refresh(); importing = false
    }
    editingTrack?.let { track ->
        TrackEditorDialog(track, onDismiss = { editingTrack = null }) { name, lat, lon ->
            db.updateTrack(track.id, name, lat, lon); refresh(); editingTrack = null
        }
    }
}

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
    onVolumeSave: (Int) -> Unit,
    onScreenTimeoutSave: (Int) -> Unit,
) {
    var nickname by remember(profile) { mutableStateOf(profile.nickname) }
    var bio by remember(profile) { mutableStateOf(profile.bio) }
    var socialIds by remember(profile) { mutableStateOf(profile.socialIds.joinToString("\n")) }
    var volumeDraft by remember(volume) { mutableFloatStateOf(volume.toFloat()) }
    var timeoutMenuOpen by remember { mutableStateOf(false) }
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
                OutlinedTextField(nickname, { nickname = it }, label = { Text("机主昵称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(bio, { bio = it }, label = { Text("简介") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(socialIds, { socialIds = it }, label = { Text("社交账号 ID") }, supportingText = { Text("每行一个，最多 4 个") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Text("设备字库可能无法显示生僻字。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = {
                    val accounts = socialIds.lineSequence().map(String::trim).filter(String::isNotEmpty).take(4).toList()
                    onSave(PassportProfile(nickname, bio, accounts))
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
}

@Composable
private fun TracksPage(rows: List<TrackPoint>, edit: (TrackPoint) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("用 Passport 短按确认键记录此刻", style = MaterialTheme.typography.titleMedium)
                Text("手机定位成功并保存后，才会向设备确认。定位失败时清单不会推进。", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text("最近记录", style = MaterialTheme.typography.titleMedium)
        if (rows.isEmpty()) Text("还没有轨迹记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { it.id }) { row -> Card { Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(row.name, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { edit(row) }) { Text("编辑位置") }
                }
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
    var dragDistance by remember { mutableFloatStateOf(0f) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { adding = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("添加")
            }
            OutlinedButton(onClick = import) { Text("导入") }
        }
        Text("长按卡片并拖动排序", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(destinations, key = { _, item -> item.id }) { index, destination ->
                var menuOpen by remember(destination.id) { mutableStateOf(false) }
                val containerColor = if (destination.done) MaterialTheme.colorScheme.surfaceContainerLow
                    else MaterialTheme.colorScheme.secondaryContainer
                val contentColor = if (destination.done) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSecondaryContainer
                Card(
                    colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
                    modifier = Modifier.fillMaxWidth().pointerInput(destination.id, destinations) {
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
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多操作", tint = contentColor)
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("删除") },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                    onClick = { menuOpen = false; db.deleteDestination(destination.id); refresh() },
                                )
                            }
                        }
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
private fun ImportDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("粘贴旅行地点清单") },
        text = { OutlinedTextField(text, { text = it }, label = { Text("每行一个地点") }, minLines = 5) },
        confirmButton = { TextButton(onClick = { onImport(text) }) { Text("导入") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
@Composable
private fun DestinationDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("添加地点") },
        text = { OutlinedTextField(text, { text = it }, label = { Text("目的地") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
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
private fun TrackEditorDialog(track: TrackPoint, onDismiss: () -> Unit, onSave: (String, Double, Double) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember(track.id) { mutableStateOf(track.name) }
    var latitude by remember(track.id) { mutableStateOf(track.lat.toString()) }
    var longitude by remember(track.id) { mutableStateOf(track.lon.toString()) }
    var query by remember(track.id) { mutableStateOf("") }
    var results by remember(track.id) { mutableStateOf<List<Address>>(emptyList()) }
    var message by remember(track.id) { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("编辑轨迹位置") },
        text = { Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("位置名称") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(latitude, { latitude = it }, label = { Text("纬度") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(longitude, { longitude = it }, label = { Text("经度") }, modifier = Modifier.weight(1f), singleLine = true)
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
        } },
        confirmButton = { TextButton(onClick = {
            val lat = latitude.toDoubleOrNull(); val lon = longitude.toDoubleOrNull()
            if (name.isBlank() || lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
                message = "请输入有效的位置名称和经纬度"
            } else onSave(name, lat, lon)
        }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
