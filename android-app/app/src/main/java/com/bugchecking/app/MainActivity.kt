package com.bugchecking.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.text.DateFormat
import java.util.Date
import android.location.Address
import android.location.Geocoder
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
            adapter?.bondedDevices?.any { it.name == "BugChecking" || it.name == "AI Passport" } == true
        }.getOrDefault(false)
        if (!hasBondedPassport) {
            Toast.makeText(this, "请先在系统蓝牙设置中配对 BugChecking", Toast.LENGTH_LONG).show()
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

private enum class Tab(val route: String, val title: String, val label: String, val icon: ImageVector) {
    Tracks("tracks", "轨迹记录", "轨迹", Icons.Default.Route),
    Checklist("checklist", "打卡清单", "清单", Icons.Default.Checklist),
    Qr("qr", "二维码", "二维码", Icons.Default.QrCode2),
    Me("me", "设备与我", "我", Icons.Default.Person),
}

private object Routes {
    const val TrackMap = "track-map"
    const val TrackEdit = "track-edit"
    const val ChecklistImport = "checklist-import"
}

private val DetailEnterEasing = CubicBezierEasing(0f, 0f, 0f, 1f)
private val DetailExitEasing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

private fun NavBackStackEntry.isSecondaryDestination(): Boolean {
    val route = destination.route.orEmpty()
    return route == Routes.TrackMap || route == Routes.ChecklistImport || route.startsWith(Routes.TrackEdit)
}

private fun detailEnterTransition(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { width -> width / 10 },
        animationSpec = tween(durationMillis = 250, easing = DetailEnterEasing),
    ) + fadeIn(animationSpec = tween(durationMillis = 200, easing = DetailEnterEasing))

private fun detailExitTransition(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { width -> width / 10 },
        animationSpec = tween(durationMillis = 200, easing = DetailExitEasing),
    ) + fadeOut(animationSpec = tween(durationMillis = 150, easing = DetailExitEasing))

private fun parentExitTransition(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { width -> -width / 16 },
        animationSpec = tween(durationMillis = 200, easing = DetailExitEasing),
    ) + fadeOut(animationSpec = tween(durationMillis = 150, easing = DetailExitEasing))

private fun parentReturnTransition(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { width -> -width / 16 },
        animationSpec = tween(durationMillis = 250, easing = DetailEnterEasing),
    ) + fadeIn(animationSpec = tween(durationMillis = 200, easing = DetailEnterEasing))

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
    var revision by remember { mutableIntStateOf(0) }
    var initialImportSource by remember { mutableStateOf<ImportSource?>(null) }
    var initialMapLink by remember { mutableStateOf<String?>(null) }
    var pendingMapShare by remember { mutableStateOf<String?>(null) }
    val navController = rememberNavController()
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
            navController.navigate(Routes.ChecklistImport) { launchSingleTop = true }
        }
        onSharedSourceConsumed()
    }
    fun openTab(tab: Tab) {
        navController.navigate(tab.route) {
            popUpTo(Tab.Tracks.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    NavHost(
        navController = navController,
        startDestination = Tab.Tracks.route,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable(
            route = Tab.Tracks.route,
            exitTransition = { if (targetState.isSecondaryDestination()) parentExitTransition() else ExitTransition.None },
            popEnterTransition = { if (initialState.isSecondaryDestination()) parentReturnTransition() else EnterTransition.None },
        ) {
            PassportRootScaffold(Tab.Tracks, compact, bleState, bleConnected, ::openTab, sync) {
                TracksPage(
                    tracks,
                    edit = { navController.navigate("${Routes.TrackEdit}/${it.id}") },
                    openMap = { navController.navigate(Routes.TrackMap) },
                )
            }
        }
        composable(
            route = Tab.Checklist.route,
            exitTransition = { if (targetState.isSecondaryDestination()) parentExitTransition() else ExitTransition.None },
            popEnterTransition = { if (initialState.isSecondaryDestination()) parentReturnTransition() else EnterTransition.None },
        ) {
            PassportRootScaffold(Tab.Checklist, compact, bleState, bleConnected, ::openTab, sync) {
                ChecklistPage(db, destinations, refresh) { navController.navigate(Routes.ChecklistImport) }
            }
        }
        composable(
            route = Tab.Qr.route,
            exitTransition = { if (targetState.isSecondaryDestination()) parentExitTransition() else ExitTransition.None },
            popEnterTransition = { if (initialState.isSecondaryDestination()) parentReturnTransition() else EnterTransition.None },
        ) {
            PassportRootScaffold(Tab.Qr, compact, bleState, bleConnected, ::openTab, sync) {
                QrPage(db, qrs, destinations, refresh)
            }
        }
        composable(
            route = Tab.Me.route,
            exitTransition = { if (targetState.isSecondaryDestination()) parentExitTransition() else ExitTransition.None },
            popEnterTransition = { if (initialState.isSecondaryDestination()) parentReturnTransition() else EnterTransition.None },
        ) {
            PassportRootScaffold(Tab.Me, compact, bleState, bleConnected, ::openTab, sync) {
                ProfileSettingsPage(
                    profile, volume, screenTimeout, bleState, bleConnected, connect, disconnect,
                    onSave = { value ->
                        runCatching { db.saveProfile(value) }
                            .onSuccess { refresh(); Toast.makeText(context, "已保存到手机；同步后会更新 Passport", Toast.LENGTH_SHORT).show() }
                            .onFailure { Toast.makeText(context, "个人资料保存失败", Toast.LENGTH_LONG).show() }
                    },
                    onAvatarChange = { path ->
                        db.setAvatarPath(path); refresh()
                        Toast.makeText(context, "头像已保存到手机；同步后会更新 Passport", Toast.LENGTH_SHORT).show()
                    },
                    onVolumeSave = { value -> db.setSoundVolume(value); refresh() },
                    onScreenTimeoutSave = { value ->
                        db.setScreenTimeoutSeconds(value); refresh()
                        Toast.makeText(context, "亮屏时长已保存到手机", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
        composable(
            route = Routes.TrackMap,
            enterTransition = { detailEnterTransition() },
            exitTransition = { if (targetState.isSecondaryDestination()) parentExitTransition() else ExitTransition.None },
            popEnterTransition = { if (initialState.isSecondaryDestination()) parentReturnTransition() else EnterTransition.None },
            popExitTransition = { detailExitTransition() },
        ) {
            TrackMapScreen(
                rows = tracks,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("${Routes.TrackEdit}/${it.id}") },
            )
        }
        composable(
            route = "${Routes.TrackEdit}/{trackId}",
            arguments = listOf(navArgument("trackId") { type = NavType.LongType }),
            enterTransition = { detailEnterTransition() },
            popExitTransition = { detailExitTransition() },
        ) { entry ->
            val track = tracks.firstOrNull { it.id == entry.arguments?.getLong("trackId") }
            if (track == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                TrackEditorScreen(
                    track = track,
                    initialLink = initialMapLink,
                    onBack = { initialMapLink = null; navController.popBackStack() },
                    onDelete = { db.deleteTrack(track.id); refresh(); initialMapLink = null; navController.popBackStack() },
                    onSave = { name -> db.updateTrackName(track.id, name); refresh(); initialMapLink = null; navController.popBackStack() },
                    onApplyPlace = { place -> db.updateTrackPlace(track.id, place); refresh(); initialMapLink = null; navController.popBackStack() },
                )
            }
        }
        composable(
            route = Routes.ChecklistImport,
            enterTransition = { detailEnterTransition() },
            popExitTransition = { detailExitTransition() },
        ) {
            SmartImportScreen(
                initialSource = initialImportSource,
                existingNames = destinations.map { it.name }.toSet(),
                onBack = { initialImportSource = null; navController.popBackStack() },
                onImport = { names ->
                    val added = db.addDestinations(names)
                    if (added < names.size) Toast.makeText(context, "仅导入 $added 项：Passport 一份清单最多同步 64 个目的地", Toast.LENGTH_LONG).show()
                    refresh(); initialImportSource = null; navController.popBackStack()
                },
            )
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
                            pendingMapShare = null
                            navController.navigate("${Routes.TrackEdit}/${track.id}")
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassportRootScaffold(
    tab: Tab,
    compact: Boolean,
    bleState: String,
    bleConnected: Boolean,
    onTabSelected: (Tab) -> Unit,
    sync: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxSize()) {
        if (!compact) NavigationRail {
            Spacer(Modifier.size(PassportSpacing.sm))
            Tab.entries.forEach { item ->
                NavigationRailItem(
                    selected = tab == item,
                    onClick = { onTabSelected(item) },
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(item.label) },
                )
            }
        }
        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(tab.title)
                            Text(
                                if (bleConnected) "设备已连接" else bleState,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = sync, enabled = bleConnected) {
                            Icon(Icons.Default.Sync, contentDescription = if (bleConnected) "同步到 Passport" else "未连接，无法同步")
                        }
                    },
                )
            },
            bottomBar = {
                if (compact) NavigationBar {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { onTabSelected(item) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                Column(
                    Modifier.widthIn(max = PassportSpacing.listMaxWidth).fillMaxSize().align(Alignment.TopCenter)
                        .padding(horizontal = if (compact) PassportSpacing.md else PassportSpacing.lg),
                ) { content() }
            }
        }
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
                        Text("BugChecking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
    var pendingDelete by remember { mutableStateOf<PassportQr?>(null) }
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
                    IconButton(onClick = { pendingDelete = qr }) {
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
    pendingDelete?.let { qr ->
        ConfirmDeleteDialog(
            title = "删除“${qr.name}”？",
            description = "二维码及其与打卡项目的绑定都会被移除，此操作无法恢复。",
            onConfirm = { db.deleteQr(qr.id); refresh(); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun SmartImportScreen(
    initialSource: ImportSource?,
    existingNames: Set<String>,
    onBack: () -> Unit,
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
    var editingId by remember { mutableStateOf<String?>(null) }

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
    val leave = { job?.cancel(); onBack() }
    val confirmImport: (() -> Unit)? = extraction?.let {
        {
            onImport(candidates.filter { it.id in selected }.map { it.text.trim() }.filter(String::isNotEmpty))
        }
    }
    BackHandler(onBack = leave)
    PassportPageScaffold(
        title = "智能导入清单",
        subtitle = if (extraction == null) "1 选择来源 · 2 解析 · 3 复核" else "复核识别结果",
        onBack = leave,
        primaryActionLabel = if (extraction == null) null else "导入 ${selected.size} 项",
        primaryActionEnabled = selected.isNotEmpty() && job == null,
        onPrimaryAction = confirmImport,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier.fillMaxSize().widthIn(max = PassportSpacing.listMaxWidth).align(Alignment.TopCenter)
                    .padding(horizontal = PassportSpacing.md),
                verticalArrangement = Arrangement.spacedBy(PassportSpacing.sm),
            ) {
                if (extraction == null) {
                    Text(
                        "粘贴清单、公开网页链接，或选择 Excel、CSV、图片和 PDF。内容只在手机本地解析。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = PassportSpacing.md),
                    )
                    OutlinedTextField(pasted, { pasted = it }, label = { Text("粘贴内容") }, minLines = 6, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { start(ImportSource.Pasted(pasted)) }, enabled = pasted.isNotBlank() && job == null,
                        modifier = Modifier.fillMaxWidth()) { Text("解析粘贴内容") }
                    OutlinedButton(onClick = {
                        fileLauncher.launch(arrayOf(
                            "text/*", "image/*", "application/pdf", "application/vnd.ms-excel",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        ))
                    }, enabled = job == null, modifier = Modifier.fillMaxWidth()) { Text("选择文件") }
                } else {
                    val result = extraction!!
                    Row(
                        Modifier.fillMaxWidth().padding(top = PassportSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(PassportSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("识别到 ${candidates.size} 项", style = MaterialTheme.typography.titleMedium)
                            Text("已有项默认不选，可逐项编辑和排序", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val available = candidates.filterNot { it.duplicate }.map { it.id }.toSet()
                        FilterChip(
                            selected = available.isNotEmpty() && selected.containsAll(available),
                            onClick = { selected = if (selected.containsAll(available)) emptySet() else available },
                            label = { Text("全选可用") },
                        )
                    }
                    if (result.tables.isNotEmpty()) Box {
                        OutlinedButton(onClick = { columnMenuExpanded = true }) {
                            Text("导入列：${result.selectedColumn ?: "请选择"}")
                        }
                        DropdownMenu(expanded = columnMenuExpanded, onDismissRequest = { columnMenuExpanded = false }) {
                            result.availableColumns.forEach { column ->
                                DropdownMenuItem(text = { Text(column) }, onClick = {
                                    columnMenuExpanded = false
                                    editingId = null
                                    applyExtraction(ImportEngine.selectColumn(result, column))
                                })
                            }
                        }
                    }
                    result.notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary) }
                    if (candidates.isEmpty()) Text("当前列没有可导入内容，请切换其他列或返回重新选择来源。")
                    LazyColumn(
                        Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = PassportSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(PassportSpacing.xs),
                    ) {
                        itemsIndexed(candidates, key = { _, item -> item.id }) { index, item ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                                Column(Modifier.fillMaxWidth().padding(horizontal = PassportSpacing.xs, vertical = PassportSpacing.xxs)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(item.id in selected, onCheckedChange = { checked ->
                                            selected = if (checked) selected + item.id else selected - item.id
                                        })
                                        if (editingId == item.id) {
                                            OutlinedTextField(item.text, { value ->
                                                candidates = candidates.toMutableList().also { it[index] = item.copy(text = value) }
                                            }, modifier = Modifier.weight(1f), singleLine = true)
                                        } else Text(item.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                        IconButton(onClick = { editingId = if (editingId == item.id) null else item.id }) {
                                            Icon(Icons.Default.Edit, if (editingId == item.id) "完成编辑" else "编辑 ${item.text}")
                                        }
                                    }
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${item.source} · ${confidenceLabel(item.confidence)}${if (item.duplicate) " · 已存在" else ""}",
                                            Modifier.weight(1f).padding(start = PassportSpacing.minTouchTarget),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        IconButton(onClick = {
                                            if (index > 0) candidates = candidates.toMutableList().also { val moved = it.removeAt(index); it.add(index - 1, moved) }
                                        }, enabled = index > 0) { Icon(Icons.Default.ArrowUpward, "上移 ${item.text}") }
                                        IconButton(onClick = {
                                            if (index < candidates.lastIndex) candidates = candidates.toMutableList().also { val moved = it.removeAt(index); it.add(index + 1, moved) }
                                        }, enabled = index < candidates.lastIndex) { Icon(Icons.Default.ArrowDownward, "下移 ${item.text}") }
                                        IconButton(onClick = {
                                            candidates = candidates.toMutableList().also { it.removeAt(index) }
                                            selected = selected - item.id
                                            if (editingId == item.id) editingId = null
                                        }) { Icon(Icons.Default.Delete, "删除 ${item.text}") }
                                    }
                                }
                            }
                        }
                    }
                }
                if (job != null) Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text(progress.ifBlank { "正在解析…" }, Modifier.weight(1f).padding(start = PassportSpacing.xs))
                    TextButton(onClick = { job?.cancel(); job = null; progress = "已取消" }) { Text("取消") }
                } else if (progress.isNotBlank() && extraction == null) Text(progress, style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
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
    var confirmDelete by remember(destination?.id) { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (destination == null) "添加地点" else "编辑地点") },
        text = { OutlinedTextField(text, { text = it }, label = { Text("目的地") }, singleLine = true) },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) TextButton(onClick = { confirmDelete = true }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = { onSave(text) }) { Text("保存") }
            }
        })
    if (confirmDelete && onDelete != null) ConfirmDeleteDialog(
        title = "删除“${destination?.name.orEmpty()}”？",
        description = "该项目会从打卡清单中移除，此操作无法恢复。",
        onConfirm = onDelete,
        onDismiss = { confirmDelete = false },
    )
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
private fun TrackEditorScreen(
    track: TrackPoint,
    initialLink: String? = null,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onSave: (String) -> Unit,
    onApplyPlace: (PlaceResolution) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember(track.id) { mutableStateOf(track.name) }
    var stagedPlace by remember(track.id) { mutableStateOf<PlaceResolution?>(null) }
    var message by remember(track.id) { mutableStateOf("") }
    var messageIsError by remember(track.id) { mutableStateOf(false) }
    var nearbyPlaces by remember(track.id) { mutableStateOf<List<NearbyPlaceCandidate>>(emptyList()) }
    var nearbyLoading by remember(track.id) { mutableStateOf(true) }
    var nearbyError by remember(track.id) { mutableStateOf<String?>(null) }
    var nearbyExpanded by remember(track.id) { mutableStateOf(false) }
    var nearbyReload by remember(track.id) { mutableIntStateOf(0) }
    var mapPickerOpen by remember(track.id) { mutableStateOf(false) }
    var linkToolsExpanded by remember(track.id, initialLink) { mutableStateOf(!initialLink.isNullOrBlank()) }
    var linkText by remember(track.id, initialLink) { mutableStateOf(initialLink ?: track.originalUrl.orEmpty()) }
    var parsingLink by remember { mutableStateOf(false) }
    var moreMenuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun parseLink() {
        if (linkText.isBlank() || parsingLink) return
        parsingLink = true
        message = ""
        scope.launch {
            val result = runCatching { resolveSharedPlace(linkText) }
            parsingLink = false
            result.onSuccess { place ->
                stagedPlace = place
                name = place.name.orEmpty()
                linkToolsExpanded = false
                message = "已填入地图识别到的名称，打卡坐标保持不变"
                messageIsError = false
            }.onFailure {
                message = it.message ?: "链接解析失败"
                messageIsError = true
            }
        }
    }

    fun save() {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            message = "请输入地点名称"
            messageIsError = true
        } else {
            val place = stagedPlace
            if (place != null) onApplyPlace(place.copy(name = cleanName)) else onSave(cleanName)
        }
    }

    LaunchedEffect(track.id, nearbyReload) {
        nearbyLoading = true
        nearbyError = null
        if (!Geocoder.isPresent()) {
            nearbyPlaces = emptyList()
            nearbyError = "当前无法获取附近地点"
        } else {
            runCatching { loadNearbyPlaces(context, track) }
                .onSuccess {
                    nearbyPlaces = it
                    if (it.isEmpty()) nearbyError = "附近没有可用的地点名称"
                }
                .onFailure { nearbyError = "附近地点加载失败" }
        }
        nearbyLoading = false
    }

    LaunchedEffect(track.id, initialLink) {
        if (!initialLink.isNullOrBlank()) parseLink()
    }

    BackHandler(onBack = onBack)
    PassportPageScaffold(
        title = "编辑地点",
        subtitle = "${DateFormat.getDateTimeInstance().format(Date(track.time))} · 精度 ${track.accuracy.toInt()} m",
        onBack = onBack,
        actions = {
            Box {
                IconButton(onClick = { moreMenuOpen = true }) { Icon(Icons.Default.MoreVert, "更多操作") }
                DropdownMenu(expanded = moreMenuOpen, onDismissRequest = { moreMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("删除这条轨迹", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { moreMenuOpen = false; confirmDelete = true },
                    )
                }
            }
        },
        primaryActionLabel = "保存地点名称",
        onPrimaryAction = ::save,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier.fillMaxSize().widthIn(max = PassportSpacing.formMaxWidth).align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState()).padding(horizontal = PassportSpacing.md, vertical = PassportSpacing.md),
                verticalArrangement = Arrangement.spacedBy(PassportSpacing.md),
            ) {
                OutlinedTextField(
                    name,
                    {
                        name = it
                        stagedPlace = null
                        message = ""
                    },
                    label = { Text("地点名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("附近地点", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (!nearbyLoading && nearbyPlaces.isNotEmpty()) {
                            Text(
                                "已找到 ${nearbyPlaces.size} 个候选",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (!nearbyLoading && nearbyPlaces.isNotEmpty()) {
                        TextButton(onClick = { nearbyExpanded = !nearbyExpanded }) {
                            Text(if (nearbyExpanded) "收起" else "展开")
                        }
                    }
                }
                when {
                    nearbyLoading -> Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("正在查找附近名称…", style = MaterialTheme.typography.bodyMedium)
                    }
                    nearbyPlaces.isNotEmpty() -> {
                        if (nearbyExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(PassportSpacing.xxs)) {
                                nearbyPlaces.forEach { candidate ->
                                    val selected = name.trim() == candidate.name
                                    Surface(
                                        onClick = {
                                            name = candidate.name
                                            stagedPlace = PlaceResolution(
                                                provider = PlaceProvider.SYSTEM,
                                                name = candidate.name,
                                                address = candidate.address,
                                                latitude = candidate.latitude,
                                                longitude = candidate.longitude,
                                                state = PlaceResolutionState.PARTIAL,
                                            )
                                            message = ""
                                        },
                                        shape = MaterialTheme.shapes.medium,
                                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerLow,
                                    ) {
                                        Row(
                                            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                                .padding(horizontal = PassportSpacing.xs),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            RadioButton(selected = selected, onClick = null)
                                            Text(candidate.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                formatNearbyDistance(candidate.distanceMeters),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(start = PassportSpacing.xs),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(nearbyError.orEmpty(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { nearbyReload++ }) { Text("重试") }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("实际打卡位置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatTrackCoordinates(track), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = {
                        context.getSystemService(ClipboardManager::class.java)
                            .setPrimaryClip(ClipData.newPlainText("轨迹坐标", "${track.lat}, ${track.lon}"))
                        Toast.makeText(context, "坐标已复制", Toast.LENGTH_SHORT).show()
                    }) { Text("复制") }
                }
                OutlinedButton(onClick = { mapPickerOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("在地图中查看")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TextButton(onClick = { linkToolsExpanded = !linkToolsExpanded }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (linkToolsExpanded) "收起地图链接" else "从地图分享内容识别")
                }
                if (linkToolsExpanded) {
                    OutlinedTextField(
                        linkText,
                        {
                            linkText = it
                            message = ""
                        },
                        label = { Text("粘贴高德或 Google Maps 分享内容") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FilledTonalButton(
                        onClick = ::parseLink,
                        enabled = linkText.isNotBlank() && !parsingLink,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (parsingLink) "解析中…" else "识别名称") }
                }
                if (message.isNotEmpty()) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (messageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.size(PassportSpacing.md))
            }
        }
    }
    if (confirmDelete) ConfirmDeleteDialog(
        title = "删除这条轨迹？",
        description = "删除后无法恢复，实际打卡坐标和地点名称都会被移除。",
        onConfirm = onDelete,
        onDismiss = { confirmDelete = false },
    )
    if (mapPickerOpen) MapProviderPickerSheet(
        onDismiss = { mapPickerOpen = false },
        onAmap = {
            mapPickerOpen = false
            ExternalMapLauncher.openAmap(context, track.copy(name = name.ifBlank { track.name }))
        },
        onGoogle = {
            mapPickerOpen = false
            ExternalMapLauncher.openGoogle(context, track.copy(name = name.ifBlank { track.name }))
        },
    )
}

private fun Address.toRawNearbyPlace(): RawNearbyPlace? {
    val label = listOf(featureName, premises, subLocality, locality, thoroughfare)
        .mapNotNull { it?.replace(Regex("\\s+"), " ")?.trim() }
        .firstOrNull { value -> value.isNotBlank() && value.length <= 100 && value.any(Char::isLetter) }
        ?: return null
    return RawNearbyPlace(
        name = label,
        address = getAddressLine(0),
        latitude = if (hasLatitude()) latitude else null,
        longitude = if (hasLongitude()) longitude else null,
    )
}

@Suppress("DEPRECATION")
private suspend fun loadNearbyPlaces(context: Context, track: TrackPoint): List<NearbyPlaceCandidate> {
    val geocoder = Geocoder(context)
    val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { continuation ->
            try {
                geocoder.getFromLocation(track.lat, track.lon, 10, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) continuation.resume(addresses)
                    }

                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) continuation.resumeWithException(
                            IOException(errorMessage ?: "附近地点查询失败")
                        )
                    }
                })
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
    } else {
        withContext(Dispatchers.IO) { geocoder.getFromLocation(track.lat, track.lon, 10).orEmpty() }
    }
    return NearbyPlaceCandidates.prepare(addresses.mapNotNull(Address::toRawNearbyPlace), track.lat, track.lon)
}

private suspend fun resolveSharedPlace(text: String): PlaceResolution {
    val original = PlaceLinkParser.firstUrl(text)
    val direct = PlaceLinkParser.parse(text)
    val expanded = if (
        original != null && PlaceLinkParser.needsRedirect(original) &&
        (direct?.provider == PlaceProvider.GOOGLE && direct.name.isNullOrBlank())
    ) withContext(Dispatchers.IO) { PlaceRedirectResolver.expand(original) } else null
    return PlaceLinkParser.parse(text, expanded)?.takeIf { !it.name.isNullOrBlank() }
        ?: error("没有从分享内容中识别到场所名称")
}

private fun formatNearbyDistance(distanceMeters: Int?): String = when {
    distanceMeters == null -> "附近"
    distanceMeters < 10 -> "< 10 m"
    distanceMeters < 1_000 -> "$distanceMeters m"
    else -> String.format(java.util.Locale.getDefault(), "%.1f km", distanceMeters / 1_000.0)
}

private fun formatTrackCoordinates(track: TrackPoint): String =
    String.format(java.util.Locale.US, "%.6f, %.6f", track.lat, track.lon)
