package com.bugchecking.app

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.bluetooth.BluetoothManager
import android.location.Location
import android.provider.Settings
import android.provider.OpenableColumns
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.scrollableArea
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.location.Address
import android.location.Geocoder
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        AppDataBackup.syncSchedule(this)
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
                    { requestBlePermissions { requestSync() } },
                    pendingShare.value,
                    { pendingShare.value = null })
            }
        }
    }

    override fun onStart() {
        super.onStart()
        sendBroadcast(Intent(PassportBleService.ACTION_QUERY_STATE).setPackage(packageName))
        AppDataBackup.enqueueCatchUp(this)
    }
    override fun onStop() {
        AppDataBackup.enqueueCatchUp(this)
        super.onStop()
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
            bleState.value = "请先在系统蓝牙设置中配对 BugChecking"
            Toast.makeText(this, "请先在系统蓝牙设置中配对 BugChecking", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            return
        }
        bleState.value = "正在连接 BugChecking"
        startBle()
    }
    private fun requestSync() {
        bleState.value = "正在同步"
        ContextCompat.startForegroundService(this, Intent(this, PassportBleService::class.java).setAction(PassportBleService.ACTION_SYNC))
    }
    override fun onDestroy() { runCatching { unregisterReceiver(stateReceiver) }; super.onDestroy() }
}

internal enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Tracks("tracks", "轨迹", Icons.Default.Route),
    Checklist("checklist", "清单", Icons.Default.Checklist),
    Qr("qr", "二维码", Icons.Default.QrCode2),
    Me("me", "设备与我", Icons.Default.Person),
}

internal val primaryNavigationTabs = listOf(Tab.Tracks, Tab.Checklist, Tab.Qr)

internal fun primaryNavigationPosition(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    pageCount: Int = primaryNavigationTabs.size,
): Float = (currentPage + currentPageOffsetFraction).coerceIn(0f, (pageCount - 1).coerceAtLeast(0).toFloat())

internal fun primaryNavigationSelectionFraction(position: Float, itemIndex: Int): Float =
    (1f - kotlin.math.abs(position - itemIndex)).coerceIn(0f, 1f)

private object Routes {
    const val TrackMap = "track-map"
    const val TrackFavorites = "track-favorites"
    const val PlaceDetail = "place-detail"
    const val TrackCreate = "track-create"
    const val TrackEdit = "track-edit"
    const val ChecklistImport = "checklist-import"
    const val QrEdit = "qr-edit"
    const val QrPresent = "qr-present"
    const val AvatarCrop = "avatar-crop"
}

private val DetailEnterEasing = CubicBezierEasing(0f, 0f, 0f, 1f)
private val DetailExitEasing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

private fun NavBackStackEntry.isSecondaryDestination(): Boolean {
    val route = destination.route.orEmpty()
    return route == Tab.Me.route || route == Routes.TrackMap || route == Routes.TrackFavorites ||
        route == Routes.TrackCreate ||
        route == Routes.ChecklistImport || route.startsWith(Routes.TrackEdit) ||
        route.startsWith(Routes.PlaceDetail) || route.startsWith(Routes.QrEdit) ||
        route.startsWith(Routes.QrPresent) || route.startsWith(Routes.AvatarCrop)
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

private fun NavGraphBuilder.detailComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable(
    route = route,
    arguments = arguments,
    content = content,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassportApp(
    db: PassportDb,
    bleState: String,
    bleConnected: Boolean,
    externalRevision: Int,
    widthSizeClass: WindowWidthSizeClass,
    connect: () -> Unit,
    sync: () -> Unit,
    sharedSource: ImportSource?,
    onSharedSourceConsumed: () -> Unit,
) {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    var initialImportSource by remember { mutableStateOf<ImportSource?>(null) }
    var initialMapLink by remember { mutableStateOf<String?>(null) }
    var pendingMapShare by remember { mutableStateOf<String?>(null) }
    var trackFocusTarget by remember { mutableStateOf<TrackPoint?>(null) }
    var lastSyncedRevision by remember { mutableIntStateOf(0) }
    var statusOverlayMessage by remember { mutableStateOf<String?>(null) }
    var deviceStatusInitialized by remember { mutableStateOf(false) }
    val navController = rememberNavController()
    val appScope = rememberCoroutineScope()
    val primaryPagerState = rememberPagerState(pageCount = { primaryNavigationTabs.size })
    val destinations = remember(revision, externalRevision) { db.destinations() }
    val qrs = remember(revision, externalRevision) { db.qrs() }
    val tracks = remember(revision, externalRevision) { db.tracks() }
    val savedPlaces = remember(revision, externalRevision) { db.savedPlaces() }
    val profile = remember(revision, externalRevision) { db.profile() }
    val volume = remember(revision, externalRevision) { db.soundVolume() }
    val screenTimeout = remember(revision, externalRevision) { db.screenTimeoutSeconds() }
    val refresh: () -> Unit = { revision++ }
    val syncTrackName: (Long) -> Unit = { trackId ->
        ContextCompat.startForegroundService(
            context,
            Intent(context, PassportBleService::class.java)
                .setAction(PassportBleService.ACTION_TRACK_NAME_SYNC)
                .putExtra(PassportBleService.EXTRA_TRACK_ID, trackId),
        )
    }
    LaunchedEffect(bleState) {
        if (resolveSyncStatus(bleConnected, bleState, revision > lastSyncedRevision) == PassportSyncStatus.SYNCED) {
            lastSyncedRevision = revision
        }
    }
    val syncStatus = resolveSyncStatus(bleConnected, bleState, revision > lastSyncedRevision)
    val deviceActionState = resolveDeviceActionState(bleConnected, syncStatus, bleState)
    LaunchedEffect(bleState, bleConnected, syncStatus) {
        if (!deviceStatusInitialized) {
            deviceStatusInitialized = true
            return@LaunchedEffect
        }
        val message = when {
            !bleConnected && bleState.isNotBlank() -> bleState.replace("Passport", "BugChecking")
            syncStatus == PassportSyncStatus.FAILED -> bleState
            syncStatus == PassportSyncStatus.PENDING -> syncStatus.label
            syncStatus == PassportSyncStatus.SYNCING -> syncStatus.label
            syncStatus == PassportSyncStatus.SYNCED -> syncStatus.label
            else -> bleState
        }.ifBlank { syncStatus.label }
        statusOverlayMessage = message
        delay(5_000L)
        if (statusOverlayMessage == message) statusOverlayMessage = null
    }
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
        if (tab != Tab.Tracks) trackFocusTarget = null
        if (compact && tab in primaryNavigationTabs) {
            navController.navigate(Tab.Tracks.route) {
                popUpTo(Tab.Tracks.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            appScope.launch {
                primaryPagerState.animateScrollToPage(
                    page = primaryNavigationTabs.indexOf(tab),
                    animationSpec = tween(durationMillis = 300, easing = DetailEnterEasing),
                )
            }
            return
        }
        navController.navigate(tab.route) {
            popUpTo(Tab.Tracks.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    fun openProfile() {
        navController.navigate(Tab.Me.route) { launchSingleTop = true }
    }
    Box(Modifier.fillMaxSize()) {
        PrimaryPageBackdrop()
        NavHost(
            navController = navController,
            startDestination = Tab.Tracks.route,
            enterTransition = {
                if (targetState.isSecondaryDestination()) detailEnterTransition() else EnterTransition.None
            },
            exitTransition = {
                if (targetState.isSecondaryDestination()) parentExitTransition() else ExitTransition.None
            },
            popEnterTransition = {
                if (initialState.isSecondaryDestination()) parentReturnTransition() else EnterTransition.None
            },
            popExitTransition = {
                if (initialState.isSecondaryDestination()) detailExitTransition() else ExitTransition.None
            },
        ) {
        composable(route = Tab.Tracks.route) {
            if (compact) {
                val settledTab = primaryNavigationTabs[primaryPagerState.settledPage]
                val navigationPosition = primaryNavigationPosition(
                    currentPage = primaryPagerState.currentPage,
                    currentPageOffsetFraction = primaryPagerState.currentPageOffsetFraction,
                )
                LaunchedEffect(primaryPagerState.settledPage) {
                    if (settledTab != Tab.Tracks) trackFocusTarget = null
                }
                PassportRootScaffold(
                    tab = settledTab,
                    compact = true,
                    deviceActionState = deviceActionState,
                    statusOverlayMessage = statusOverlayMessage,
                    onTabSelected = ::openTab,
                    onDeviceAction = if (bleConnected) sync else connect,
                    profileAvatarPath = profile.avatarPath,
                    onOpenProfile = ::openProfile,
                    onOpenMap = if (settledTab == Tab.Tracks) {
                        { navController.navigate(Routes.TrackMap) }
                    } else null,
                    onOpenFavorites = if (settledTab == Tab.Tracks) {
                        { navController.navigate(Routes.TrackFavorites) }
                    } else null,
                    onAdd = when (settledTab) {
                        Tab.Tracks -> { { navController.navigate(Routes.TrackCreate) } }
                        Tab.Checklist -> { { navController.navigate(Routes.ChecklistImport) } }
                        Tab.Qr -> { { navController.navigate("${Routes.QrEdit}/new") } }
                        Tab.Me -> null
                    },
                    addContentDescription = when (settledTab) {
                        Tab.Tracks -> "手动添加轨迹"
                        Tab.Checklist -> "添加到清单"
                        Tab.Qr -> "添加二维码"
                        Tab.Me -> "添加"
                    },
                    navigationPosition = navigationPosition,
                ) { topContentPadding ->
                    HorizontalPager(
                        state = primaryPagerState,
                        key = { page -> primaryNavigationTabs[page].route },
                        beyondViewportPageCount = 1,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        Box(Modifier.fillMaxSize().clipToBounds()) {
                            when (primaryNavigationTabs[page]) {
                            Tab.Tracks -> TracksPage(
                                rows = remember(tracks, trackFocusTarget) {
                                    val target = trackFocusTarget
                                    if (target == null || tracks.any { it.id == target.id }) tracks
                                    else (tracks + target).sortedByDescending(TrackPoint::time)
                                },
                                focusTrackId = trackFocusTarget?.id,
                                topContentPadding = topContentPadding,
                                edit = { navController.navigate("${Routes.TrackEdit}/${it.id}") },
                                favorite = { track, tier ->
                                    runCatching { db.setTrackFavoriteTier(track.id, tier) }
                                        .onSuccess { if (it) refresh() }
                                        .onFailure { Toast.makeText(context, "收藏更新失败", Toast.LENGTH_SHORT).show() }
                                        .getOrDefault(false)
                                },
                            )
                            Tab.Checklist -> ChecklistPage(db, destinations, refresh, topContentPadding)
                            Tab.Qr -> QrPage(
                                db = db,
                                qrs = qrs,
                                destinations = destinations,
                                topContentPadding = topContentPadding,
                                refresh = refresh,
                                edit = { qr -> navController.navigate("${Routes.QrEdit}/${qr.id}") },
                                present = { qr -> navController.navigate("${Routes.QrPresent}/${qr.id}") },
                            )
                            Tab.Me -> Unit
                            }
                        }
                    }
                }
            } else {
                PassportRootScaffold(
                    Tab.Tracks,
                    compact,
                    deviceActionState,
                    statusOverlayMessage,
                    ::openTab,
                    if (bleConnected) sync else connect,
                    profileAvatarPath = profile.avatarPath,
                    onOpenProfile = ::openProfile,
                    onOpenMap = { navController.navigate(Routes.TrackMap) },
                    onOpenFavorites = { navController.navigate(Routes.TrackFavorites) },
                    onAdd = { navController.navigate(Routes.TrackCreate) },
                    addContentDescription = "手动添加轨迹",
                ) { topContentPadding ->
                    TracksPage(
                        rows = tracks,
                        focusTrackId = trackFocusTarget?.id,
                        topContentPadding = topContentPadding,
                        edit = { navController.navigate("${Routes.TrackEdit}/${it.id}") },
                        favorite = { track, tier ->
                            runCatching { db.setTrackFavoriteTier(track.id, tier) }
                                .onSuccess { if (it) refresh() }
                                .onFailure { Toast.makeText(context, "收藏更新失败", Toast.LENGTH_SHORT).show() }
                                .getOrDefault(false)
                        },
                    )
                }
            }
        }
        composable(route = Tab.Checklist.route) {
            PassportRootScaffold(
                Tab.Checklist,
                compact,
                deviceActionState,
                statusOverlayMessage,
                ::openTab,
                if (bleConnected) sync else connect,
                profileAvatarPath = profile.avatarPath,
                onOpenProfile = ::openProfile,
                onAdd = { navController.navigate(Routes.ChecklistImport) },
                addContentDescription = "添加到清单",
            ) { topContentPadding ->
                ChecklistPage(db, destinations, refresh, topContentPadding)
            }
        }
        composable(route = Tab.Qr.route) {
            PassportRootScaffold(
                Tab.Qr,
                compact,
                deviceActionState,
                statusOverlayMessage,
                ::openTab,
                if (bleConnected) sync else connect,
                profileAvatarPath = profile.avatarPath,
                onOpenProfile = ::openProfile,
                onAdd = { navController.navigate("${Routes.QrEdit}/new") },
                addContentDescription = "添加二维码",
            ) { topContentPadding ->
                QrPage(
                    db = db,
                    qrs = qrs,
                    destinations = destinations,
                    topContentPadding = topContentPadding,
                    refresh = refresh,
                    edit = { qr -> navController.navigate("${Routes.QrEdit}/${qr.id}") },
                    present = { qr -> navController.navigate("${Routes.QrPresent}/${qr.id}") },
                )
            }
        }
        composable(route = Tab.Me.route) {
            PassportRootScaffold(
                Tab.Me,
                compact,
                deviceActionState,
                statusOverlayMessage,
                ::openTab,
                if (bleConnected) sync else connect,
                profileAvatarPath = profile.avatarPath,
                onOpenProfile = ::openProfile,
                onBackFromProfile = {
                    if (!navController.popBackStack()) openTab(Tab.Tracks)
                },
            ) { topContentPadding ->
                ProfileSettingsPage(
                    profile, volume, screenTimeout,
                    topContentPadding = topContentPadding,
                    onSave = { value ->
                        runCatching { db.saveProfile(value) }
                            .onSuccess { refresh(); Toast.makeText(context, "已保存到手机；同步后会更新 Passport", Toast.LENGTH_SHORT).show() }
                            .onFailure { Toast.makeText(context, "个人资料保存失败", Toast.LENGTH_LONG).show() }
                    },
                    onAvatarCropRequested = { token ->
                        navController.navigate("${Routes.AvatarCrop}/$token")
                    },
                    onVolumeSave = { value -> db.setSoundVolume(value); refresh() },
                    onScreenTimeoutSave = { value ->
                        db.setScreenTimeoutSeconds(value); refresh()
                        Toast.makeText(context, "亮屏时长已保存到手机", Toast.LENGTH_SHORT).show()
                    },
                    onDataImported = refresh,
                )
            }
        }
        composable(route = Routes.TrackMap) {
            TrackMapScreen(
                rows = tracks,
                onBack = { navController.popBackStack() },
                onLocateInTracks = { track ->
                    trackFocusTarget = track
                    if (!navController.popBackStack(Tab.Tracks.route, inclusive = false)) {
                        openTab(Tab.Tracks)
                    }
                },
            )
        }
        detailComposable(route = Routes.TrackFavorites) {
            PlaceLibraryScreen(
                places = savedPlaces,
                onBack = { navController.popBackStack() },
                onOpen = { navController.navigate("${Routes.PlaceDetail}/${it.id}") },
            )
        }
        detailComposable(
            route = "${Routes.PlaceDetail}/{placeId}",
            arguments = listOf(navArgument("placeId") { type = NavType.StringType }),
        ) { entry ->
            val placeId = entry.arguments?.getString("placeId").orEmpty()
            val place = savedPlaces.firstOrNull { it.id == placeId } ?: db.savedPlace(placeId)
            if (place == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                PlaceDetailScreen(
                    place = place,
                    visits = remember(place.id, revision, externalRevision) { db.tracksForSavedPlace(place.id) },
                    onBack = { navController.popBackStack() },
                    onSave = { name, tier, originalUrl, canonicalUrl ->
                        runCatching { db.updateSavedPlace(place.id, name, tier, originalUrl, canonicalUrl) }
                            .onSuccess { refresh(); navController.popBackStack() }
                            .onFailure { Toast.makeText(context, "场所保存失败", Toast.LENGTH_LONG).show() }
                    },
                    onOpenTrack = { track ->
                        trackFocusTarget = track
                        if (!navController.popBackStack(Tab.Tracks.route, inclusive = false)) {
                            openTab(Tab.Tracks)
                        }
                    },
                    onUnlinkTrack = { track ->
                        if (db.unlinkTrackFromSavedPlace(track.id)) refresh()
                    },
                )
            }
        }
        detailComposable(
            route = Routes.TrackCreate,
        ) {
            ManualTrackEditorScreen(
                track = null,
                onBack = { navController.popBackStack() },
                onSave = { draft ->
                    db.addManualTrack(draft)
                    refresh()
                    navController.popBackStack()
                },
            )
        }
        detailComposable(
            route = "${Routes.TrackEdit}/{trackId}",
            arguments = listOf(navArgument("trackId") { type = NavType.LongType }),
        ) { entry ->
            val track = tracks.firstOrNull { it.id == entry.arguments?.getLong("trackId") }
            if (track == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else if (track.entrySource == TrackEntrySource.MANUAL) {
                ManualTrackEditorScreen(
                    track = track,
                    onBack = { navController.popBackStack() },
                    onDelete = { db.deleteTrack(track.id); refresh(); navController.popBackStack() },
                    onFavorite = { tier -> db.setTrackFavoriteTier(track.id, tier); refresh() },
                    onSave = { draft ->
                        db.updateManualTrack(track.id, draft)
                        refresh()
                        navController.popBackStack()
                    },
                )
            } else {
                TrackEditorScreen(
                    track = track,
                    initialLink = initialMapLink,
                    onBack = { initialMapLink = null; navController.popBackStack() },
                    onDelete = { db.deleteTrack(track.id); refresh(); initialMapLink = null; navController.popBackStack() },
                    onFavorite = { tier -> db.setTrackFavoriteTier(track.id, tier); refresh() },
                    onSave = { name ->
                        db.updateTrackName(track.id, name)
                        syncTrackName(track.id)
                        refresh(); initialMapLink = null; navController.popBackStack()
                    },
                    onApplyPlace = { place ->
                        db.updateTrackPlace(track.id, place)
                        syncTrackName(track.id)
                        refresh(); initialMapLink = null; navController.popBackStack()
                    },
                )
            }
        }
        composable(route = Routes.ChecklistImport) {
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
        detailComposable(
            route = "${Routes.QrEdit}/{qrId}",
            arguments = listOf(navArgument("qrId") { type = NavType.StringType }),
        ) { entry ->
            val qrId = entry.arguments?.getString("qrId")
            val qr = qrId?.takeUnless { it == "new" }?.let { id -> qrs.firstOrNull { it.id == id } }
            if (qrId != "new" && qr == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                QrEditorScreen(
                    qr = qr,
                    starredCount = qrs.count { it.starred },
                    destinations = destinations,
                    boundDestinationId = qr?.id?.let { qrId ->
                        destinations.firstOrNull { it.qrId == qrId }?.id
                    },
                    onBack = { navController.popBackStack() },
                    onSave = { name, payload, starred, destinationId ->
                        runCatching {
                            val savedId = db.addQr(name, payload, starred, qr?.id)
                            db.bindQrToDestination(savedId, destinationId)
                        }.onSuccess {
                            refresh()
                            navController.popBackStack()
                        }.onFailure {
                            Toast.makeText(context, it.message ?: "保存失败", Toast.LENGTH_LONG).show()
                        }
                    },
                )
            }
        }
        detailComposable(
            route = "${Routes.AvatarCrop}/{sourceToken}",
            arguments = listOf(navArgument("sourceToken") { type = NavType.StringType }),
        ) { entry ->
            val token = entry.arguments?.getString("sourceToken").orEmpty()
            AvatarCropScreen(
                sourceToken = token,
                previousAvatarPath = profile.avatarPath,
                onBack = {
                    AvatarImage.discardStagedCropSource(context, token)
                    navController.popBackStack()
                },
                onSaved = { path ->
                    AvatarImage.discardStagedCropSource(context, token)
                    db.setAvatarPath(path)
                    refresh()
                    Toast.makeText(context, "头像已保存到手机；同步后会更新 Passport", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
            )
        }
        composable(
            route = "${Routes.QrPresent}/{qrId}",
            arguments = listOf(navArgument("qrId") { type = NavType.StringType }),
        ) { entry ->
            val qr = qrs.firstOrNull { it.id == entry.arguments?.getString("qrId") }
            if (qr == null) LaunchedEffect(Unit) { navController.popBackStack() }
            else QrPresentScreen(qr = qr, onBack = { navController.popBackStack() })
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
                            pendingMapShare = null
                            navController.navigate("${Routes.TrackEdit}/${track.id}")
                        }, modifier = Modifier.fillMaxWidth(), shape = PassportTokens.shapes.buttonShape) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(track.name, fontWeight = FontWeight.Medium)
                                Text(formatTrackDate(track), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { pendingMapShare = null },
                    shape = PassportTokens.shapes.buttonShape,
                ) { Text("取消") }
            },
        )
    }
}

@Composable
private fun PrimaryPageBackdrop() {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val backgroundMatrix = remember(dark) {
        ColorMatrix().apply { setToSaturation(if (dark) 0.30f else 0.68f) }
    }
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bugchecking_city_map_halftone),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            colorFilter = ColorFilter.colorMatrix(backgroundMatrix),
            modifier = Modifier.fillMaxSize().alpha(if (dark) 0.50f else 0.78f),
        )
        Box(
            Modifier.fillMaxSize().background(
                MaterialTheme.colorScheme.surface.copy(alpha = if (dark) 0.76f else 0.92f),
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassportRootScaffold(
    tab: Tab,
    compact: Boolean,
    deviceActionState: DeviceActionState,
    statusOverlayMessage: String?,
    onTabSelected: (Tab) -> Unit,
    onDeviceAction: () -> Unit,
    profileAvatarPath: String?,
    onOpenProfile: () -> Unit,
    onBackFromProfile: (() -> Unit)? = null,
    onOpenMap: (() -> Unit)? = null,
    onOpenFavorites: (() -> Unit)? = null,
    onAdd: (() -> Unit)? = null,
    addContentDescription: String = "添加",
    navigationPosition: Float? = null,
    content: @Composable (topContentPadding: Dp) -> Unit,
) {
    Box(Modifier.fillMaxSize().clearFocusOnTapOutside()) {
        Row(Modifier.fillMaxSize()) {
            if (!compact && tab != Tab.Me) NavigationRail(
                containerColor = primaryPageNavigationColor(),
            ) {
                Spacer(Modifier.size(PassportSpacing.sm))
                primaryNavigationTabs.forEach { item ->
                    NavigationRailItem(
                        selected = tab == item,
                        onClick = { onTabSelected(item) },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
                val actionViewportTop = safeTop + PrimaryTopActionTokens.safeAreaGap +
                    PrimaryTopActionTokens.targetSize + PrimaryTopActionTokens.detailContentGap
                val firstContentPadding = if (tab == Tab.Me) 0.dp else {
                    PrimaryTopActionTokens.primaryContentBreathingRoom - PrimaryTopActionTokens.detailContentGap
                }
                val designTokens = PassportTokens
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                ) { padding ->
                    Box(
                        Modifier.fillMaxSize().padding(padding),
                    ) {
                        Column(
                            Modifier.widthIn(max = PassportSpacing.listMaxWidth).fillMaxSize()
                                .align(Alignment.TopCenter)
                                .padding(horizontal = if (compact) PassportSpacing.md else PassportSpacing.lg)
                                .clipToBounds(),
                        ) {
                            content(actionViewportTop + firstContentPadding)
                        }
                    }
                }
                PrimaryTopActions(
                    tab = tab,
                    compact = compact,
                    profileAvatarPath = profileAvatarPath,
                    deviceActionState = deviceActionState,
                    statusMessage = statusOverlayMessage,
                    onOpenProfile = onOpenProfile,
                    onBackFromProfile = onBackFromProfile,
                    onOpenMap = onOpenMap,
                    onOpenFavorites = onOpenFavorites,
                    onDeviceAction = onDeviceAction,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                if (compact && tab != Tab.Me) {
                    val edgeInset = designTokens.components.floatingHorizontalInset
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                            .padding(start = edgeInset, end = edgeInset, bottom = PassportSpacing.sm)
                            .height(designTokens.components.primaryNavigationHeight),
                        horizontalArrangement = Arrangement.spacedBy(PassportSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = designTokens.shapes.floatingNavigationShape,
                            color = primaryPageNavigationColor(),
                            tonalElevation = 1.dp,
                            shadowElevation = designTokens.components.floatingNavigationShadowElevation,
                            border = floatingControlBorder(),
                        ) {
                            BoxWithConstraints(
                                Modifier.fillMaxSize().padding(designTokens.components.compactNavigationContentPadding),
                            ) {
                                val itemWidth = maxWidth / primaryNavigationTabs.size
                                val resolvedPosition = (navigationPosition
                                    ?: primaryNavigationTabs.indexOf(tab).coerceAtLeast(0).toFloat())
                                    .coerceIn(0f, primaryNavigationTabs.lastIndex.toFloat())
                                Box(
                                    Modifier.align(Alignment.CenterStart)
                                        .offset(x = itemWidth * resolvedPosition)
                                        .width(itemWidth)
                                        .height(designTokens.components.compactNavigationItemHeight)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            designTokens.shapes.navigationSelectionShape,
                                        ),
                                )
                                Row(
                                    Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    primaryNavigationTabs.forEachIndexed { index, item ->
                                        PassportCompactNavigationItem(
                                            item = item,
                                            modifier = Modifier.weight(1f),
                                            selected = tab == item,
                                            selectionFraction = primaryNavigationSelectionFraction(
                                                resolvedPosition,
                                                index,
                                            ),
                                            onClick = { onTabSelected(item) },
                                        )
                                    }
                                }
                            }
                        }
                        if (onAdd != null) FloatingActionButton(
                            onClick = onAdd,
                            modifier = Modifier.size(designTokens.components.primaryNavigationHeight)
                                .border(floatingControlBorder(), CircleShape),
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = designTokens.components.floatingFabDefaultElevation,
                                pressedElevation = designTokens.components.floatingFabPressedElevation,
                                focusedElevation = designTokens.components.floatingFabPressedElevation,
                                hoveredElevation = designTokens.components.floatingFabPressedElevation,
                            ),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = addContentDescription)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PassportCompactNavigationItem(
    item: Tab,
    selected: Boolean,
    selectionFraction: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = PassportTokens
    Surface(
        modifier = modifier.height(tokens.components.compactNavigationItemHeight),
        shape = tokens.shapes.navigationSelectionShape,
        color = Color.Transparent,
        contentColor = lerp(
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.onPrimary,
            selectionFraction,
        ),
    ) {
        Column(
            Modifier.fillMaxSize()
                .selectable(selected = selected, onClick = onClick, role = Role.Tab)
                .semantics(mergeDescendants = true) { contentDescription = item.label },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                modifier = Modifier.size(tokens.components.compactNavigationIconSize),
            )
            Spacer(Modifier.height(tokens.components.compactNavigationLabelGap))
            Text(
                item.label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PrimaryTopActions(
    tab: Tab,
    compact: Boolean,
    profileAvatarPath: String?,
    deviceActionState: DeviceActionState,
    statusMessage: String?,
    onOpenProfile: () -> Unit,
    onBackFromProfile: (() -> Unit)?,
    onOpenMap: (() -> Unit)?,
    onOpenFavorites: (() -> Unit)?,
    onDeviceAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalInset = if (compact) {
        PassportTokens.components.floatingHorizontalInset
    } else PassportSpacing.lg
    Box(
        modifier = modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(top = PrimaryTopActionTokens.safeAreaGap)
            .padding(horizontal = horizontalInset),
        contentAlignment = Alignment.TopCenter,
    ) {
        BoxWithConstraints(
            modifier = Modifier.widthIn(max = PassportSpacing.listMaxWidth).fillMaxWidth()
                .height(PrimaryTopActionTokens.targetSize),
        ) {
            val hasCenteredActions = tab == Tab.Tracks && onOpenMap != null && onOpenFavorites != null
            val preferredMaxWidth = if (compact) {
                PrimaryTopActionTokens.compactStatusMaxWidth
            } else {
                PrimaryTopActionTokens.expandedStatusMaxWidth
            }
            val statusMaxWidth = deviceCapsuleMaxWidthDp(
                contentWidthDp = maxWidth.value,
                hasCenteredActions = hasCenteredActions,
                preferredMaxWidthDp = preferredMaxWidth.value,
            ).dp

            if (tab == Tab.Me && onBackFromProfile != null) {
                PrimaryBackAction(
                    onClick = onBackFromProfile,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            } else if (tab != Tab.Me) {
                ProfileAvatarAction(
                    avatarPath = profileAvatarPath,
                    onClick = onOpenProfile,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
            if (hasCenteredActions) {
                Box(Modifier.align(Alignment.Center)) {
                    TrackSignatureActions(onOpenMap!!, onOpenFavorites!!)
                }
            }
            DeviceStatusAction(
                state = deviceActionState,
                statusMessage = statusMessage,
                maxWidth = statusMaxWidth,
                onClick = onDeviceAction,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
private fun PrimaryBackAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(PrimaryTopActionTokens.targetSize),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        shadowElevation = PassportTokens.components.floatingControlShadowElevation,
        border = floatingControlBorder(),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
    }
}

@Composable
private fun ProfileAvatarAction(
    avatarPath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val avatar = remember(avatarPath) { AvatarImage.loadPreview(avatarPath) }
    Surface(
        onClick = onClick,
        modifier = modifier.size(PrimaryTopActionTokens.targetSize)
            .semantics { contentDescription = "打开设备与我" },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        shadowElevation = PassportTokens.components.floatingControlShadowElevation,
        border = floatingControlBorder(),
    ) {
        if (avatar != null) {
            Image(
                avatar.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().padding(PrimaryTopActionTokens.avatarInset)
                    .clip(CircleShape),
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null)
            }
        }
    }
}

@Composable
private fun DeviceStatusAction(
    state: DeviceActionState,
    statusMessage: String?,
    maxWidth: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by rememberInfiniteTransition(label = "sync rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "sync rotation value",
    )
    val enabled = state != DeviceActionState.CONNECTING && state != DeviceActionState.SYNCING
    val actionDescription = when (state) {
        DeviceActionState.DISCONNECTED -> "连接 BugChecking"
        DeviceActionState.CONNECTING -> "正在连接 BugChecking"
        DeviceActionState.FAILED -> "重试同步"
        DeviceActionState.SYNCING -> "正在同步到 BugChecking"
        DeviceActionState.CONNECTED -> "同步到 BugChecking"
    }
    val labelMaxWidth = (maxWidth - PrimaryTopActionTokens.targetSize)
        .coerceAtLeast(0.dp)
    val labelVisible = statusMessage != null && labelMaxWidth > 0.dp
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(PrimaryTopActionTokens.targetSize)
            .widthIn(min = PrimaryTopActionTokens.targetSize, max = maxWidth)
            .semantics(mergeDescendants = true) {
                contentDescription = if (statusMessage == null) actionDescription
                else "$actionDescription，$statusMessage"
                if (statusMessage != null) liveRegion = LiveRegionMode.Polite
            },
        shape = PassportTokens.shapes.full,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        shadowElevation = PassportTokens.components.floatingControlShadowElevation,
        border = floatingControlBorder(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedVisibility(
                visible = labelVisible,
                enter = expandHorizontally(
                    expandFrom = Alignment.End,
                    animationSpec = tween(
                        PrimaryTopActionTokens.statusEnterMillis,
                        easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f),
                    ),
                ) + fadeIn(tween(PrimaryTopActionTokens.statusEnterMillis)),
                exit = shrinkHorizontally(
                    shrinkTowards = Alignment.End,
                    animationSpec = tween(
                        PrimaryTopActionTokens.statusExitMillis,
                        easing = CubicBezierEasing(0.3f, 0f, 1f, 1f),
                    ),
                ) + fadeOut(tween(PrimaryTopActionTokens.statusExitMillis)),
            ) {
                Text(
                    statusMessage.orEmpty(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(max = labelMaxWidth).padding(start = PassportSpacing.sm),
                )
            }
            Box(
                Modifier.size(PrimaryTopActionTokens.targetSize),
                contentAlignment = Alignment.Center,
            ) {
                val icon = when (state) {
                    DeviceActionState.DISCONNECTED -> Icons.Default.BluetoothDisabled
                    DeviceActionState.CONNECTING -> Icons.AutoMirrored.Filled.BluetoothSearching
                    else -> Icons.Default.Sync
                }
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = if (state == DeviceActionState.SYNCING) {
                        Modifier.graphicsLayer { rotationZ = rotation }
                    } else Modifier,
                    tint = deviceActionTint(state),
                )
            }
        }
    }
}

private fun formatTrackDate(track: TrackPoint): String = DateFormat.getDateTimeInstance().format(Date(track.time))

@Composable
private fun ProfileSettingsPage(
    profile: PassportProfile,
    volume: Int,
    screenTimeout: Int,
    topContentPadding: Dp,
    onSave: (PassportProfile) -> Unit,
    onAvatarCropRequested: (String) -> Unit,
    onVolumeSave: (Int) -> Unit,
    onScreenTimeoutSave: (Int) -> Unit,
    onDataImported: () -> Unit,
) {
    var nickname by remember(profile) { mutableStateOf(profile.nickname) }
    var bio by remember(profile) { mutableStateOf(profile.bio) }
    var socialIds by remember(profile) { mutableStateOf(profile.socialIds.joinToString("\n")) }
    var volumeDraft by remember(volume) { mutableFloatStateOf(volume.toFloat()) }
    var timeoutMenuOpen by remember { mutableStateOf(false) }
    var avatarBusy by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val avatar = remember(profile.avatarPath) { AvatarImage.loadPreview(profile.avatarPath) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            avatarBusy = true; avatarError = null
            runCatching { withContext(Dispatchers.IO) { AvatarImage.stageCropSource(context, uri) } }
                .onSuccess(onAvatarCropRequested)
                .onFailure { avatarError = it.message ?: "无法读取头像" }
            avatarBusy = false
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = topContentPadding, bottom = PassportSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("个人身份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(
            shape = PassportTokens.shapes.cardShape,
            colors = CardDefaults.cardColors(
                containerColor = primaryPageCardColor(),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(PassportTokens.content.cardHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(PassportSpacing.md),
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(PassportSpacing.sm),
                ) {
                    Surface(
                        modifier = Modifier.size(96.dp),
                        shape = PassportTokens.shapes.circular,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        if (avatar != null) Image(
                            avatar.asImageBitmap(), "当前头像", Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                        ) else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(38.dp))
                        }
                    }
                    FilledTonalButton(
                        onClick = { avatarPicker.launch(arrayOf("image/*")) },
                        enabled = !avatarBusy,
                        shape = PassportTokens.shapes.buttonShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) { Text(if (avatarBusy) "正在读取…" else "更换头像") }
                }
                avatarError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                OutlinedTextField(nickname, { nickname = it }, label = { Text("机主昵称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(bio, { bio = it }, label = { Text("简介") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(socialIds, { socialIds = it }, label = { Text("社交账号 ID") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Text("设备字库可能无法显示生僻字", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = {
                    val accounts = socialIds.lineSequence().map(String::trim).filter(String::isNotEmpty).take(4).toList()
                    onSave(PassportProfile(nickname, bio, accounts, profile.avatarPath))
                }, modifier = Modifier.fillMaxWidth(), shape = PassportTokens.shapes.buttonShape, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )) { Text("保存资料") }
            }
        }

        Text("设备设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(
            shape = PassportTokens.shapes.cardShape,
            colors = CardDefaults.cardColors(
                containerColor = primaryPageCardColor(),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(PassportTokens.content.cardHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(PassportSpacing.md),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("提示音量", style = MaterialTheme.typography.titleSmall)
                    Text("${volumeDraft.toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Slider(value = volumeDraft, onValueChange = { volumeDraft = it }, valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.onSurface,
                        activeTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    onValueChangeFinished = { onVolumeSave(volumeDraft.toInt()) })
                Text("0% 为静音", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("亮屏时长", style = MaterialTheme.typography.titleSmall)
                Box {
                    OutlinedButton(
                        onClick = { timeoutMenuOpen = true },
                        shape = PassportTokens.shapes.buttonShape,
                    ) {
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

        BackupSettingsSection(onDataImported = onDataImported)
    }
}

@Composable
private fun AvatarCropScreen(
    sourceToken: String,
    previousAvatarPath: String?,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember(sourceToken) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var loading by remember(sourceToken) { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var scale by remember(sourceToken) { mutableFloatStateOf(1f) }
    var offset by remember(sourceToken) { mutableStateOf(Offset.Zero) }
    var viewport by remember(sourceToken) { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val displaySize = remember(source, viewport) {
        source?.let { AvatarImage.displaySize(it, viewport) } ?: IntSize.Zero
    }
    LaunchedEffect(sourceToken) {
        source = withContext(Dispatchers.IO) { AvatarImage.loadStagedCropSource(context, sourceToken) }
        if (source == null) error = "无法读取待裁切头像"
        loading = false
    }
    DisposableEffect(source) {
        val bitmapOwnedByThisEffect = source
        onDispose { bitmapOwnedByThisEffect?.takeUnless { it.isRecycled }?.recycle() }
    }
    BackHandler(enabled = !busy, onBack = onBack)
    fun save() {
        val bitmap = source ?: return
        if (viewport.width <= 0 || busy) return
        scope.launch {
            busy = true
            error = null
            runCatching {
                withContext(Dispatchers.IO) {
                    val cropped = AvatarImage.cropSquare(bitmap, viewport, scale, offset)
                    try { AvatarImage.save(context, cropped, previousAvatarPath) }
                    finally { cropped.recycle() }
                }
            }.onSuccess(onSaved)
                .onFailure { error = it.message ?: "头像保存失败" }
            busy = false
        }
    }
    PassportPageScaffold(
        title = "裁切头像",
        onBack = { if (!busy) onBack() },
        primaryActionLabel = if (busy) "正在保存…" else "保存头像",
        primaryActionEnabled = !loading && source != null && viewport.width > 0 && !busy,
        primaryActionLoading = busy,
        onPrimaryAction = ::save,
    ) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when {
                loading -> CircularProgressIndicator(
                    Modifier.padding(top = padding.calculateTopPadding() + PassportSpacing.lg),
                )
                source == null -> Text(
                    error ?: "无法读取待裁切头像",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(
                        top = padding.calculateTopPadding() + PassportSpacing.lg,
                        start = PassportSpacing.lg,
                        end = PassportSpacing.lg,
                    ),
                )
                else -> Column(
                    Modifier.fillMaxWidth().widthIn(max = PassportSpacing.formMaxWidth)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = PassportSpacing.md,
                            end = PassportSpacing.md,
                            top = padding.calculateTopPadding() + PassportSpacing.md,
                            bottom = padding.calculateBottomPadding() + PassportSpacing.md,
                        ),
                    verticalArrangement = Arrangement.spacedBy(PassportSpacing.sm),
                ) {
            Box(
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(PassportTokens.shapes.circular)
                        .background(PassportTokens.palette.mediaBackdrop)
                        .onSizeChanged {
                            viewport = it
                            offset = AvatarImage.clampOffset(source!!, viewport, scale, offset)
                        }
                        .pointerInput(sourceToken, viewport) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 8f)
                                val nextOffset = AvatarImage.offsetAfterGesture(
                                    viewport = viewport,
                                    currentScale = scale,
                                    nextScale = nextScale,
                                    currentOffset = offset,
                                    centroid = centroid,
                                    pan = pan,
                                )
                                scale = nextScale
                                offset = AvatarImage.clampOffset(source!!, viewport, nextScale, nextOffset)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        source!!.asImageBitmap(), "头像裁切预览",
                        Modifier.requiredSize(
                            with(density) { displaySize.width.toDp() },
                            with(density) { displaySize.height.toDp() },
                        ).graphicsLayer {
                            scaleX = scale; scaleY = scale
                            translationX = offset.x; translationY = offset.y
                        },
                        contentScale = ContentScale.FillBounds,
                    )
                }
                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlwaysStretchLazyColumn(
    modifier: Modifier = Modifier,
    state: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyListScope.() -> Unit,
) {
    val overscroll = rememberOverscrollEffect()
    val scrollableState = remember(state) {
        androidx.compose.foundation.gestures.ScrollableState { delta -> state.dispatchRawDelta(delta) }
    }
    LazyColumn(
        state = state,
        modifier = modifier.scrollableArea(
            state = scrollableState,
            orientation = Orientation.Vertical,
            overscrollEffect = overscroll,
        ),
        userScrollEnabled = false,
        overscrollEffect = null,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
private fun AlwaysStretchColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val scrollableState = rememberScrollableState { 0f }
    val overscroll = rememberOverscrollEffect()
    Column(
        modifier = modifier.scrollableArea(
            state = scrollableState,
            orientation = Orientation.Vertical,
            overscrollEffect = overscroll,
        ).padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

@Composable
private fun TracksPage(
    rows: List<TrackPoint>,
    focusTrackId: Long? = null,
    topContentPadding: Dp,
    edit: (TrackPoint) -> Unit,
    favorite: (TrackPoint, Int) -> Boolean,
) {
    val dayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dayLabelFormat = remember { SimpleDateFormat("M月d日 EEEE", Locale.CHINA) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val groups = remember(rows) { rows.groupBy { dayFormat.format(Date(it.time)) } }
    val listState = rememberLazyListState()
    var focusedTrackCueId by remember { mutableStateOf<Long?>(null) }
    val focusedItemIndex = remember(groups, focusTrackId) {
        var index = 0
        var result: Int? = null
        groups.values.forEach { dayRows ->
            index += 1 // Date header.
            dayRows.forEach { row ->
                if (row.id == focusTrackId) result = index
                index += 1
            }
        }
        result
    }
    LaunchedEffect(focusTrackId, focusedItemIndex) {
        if (focusTrackId != null && focusedItemIndex != null) {
            focusedTrackCueId = null
            listState.animateScrollToItem(focusedItemIndex)
            focusedTrackCueId = focusTrackId
            delay(1_800L)
            focusedTrackCueId = null
        }
    }
    if (rows.isEmpty()) {
            AlwaysStretchColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topContentPadding),
            ) {
                Card(
                    shape = PassportTokens.shapes.cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = primaryPageCardColor(),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(PassportTokens.content.cardHorizontalPadding),
                        verticalArrangement = Arrangement.spacedBy(PassportSpacing.xs),
                    ) {
                        Icon(Icons.Default.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("还没有轨迹记录", style = MaterialTheme.typography.titleMedium)
                        Text("完成一次打卡后，这里会按日期整理你的到访地点。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else AlwaysStretchLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    top = topContentPadding,
                    bottom = TrackHomeActionTokens.safeBottomReserve,
                ),
            ) {
            groups.forEach { (_, dayRows) ->
                item(key = "day-${dayRows.first().time}") {
                    Text(
                        dayLabelFormat.format(Date(dayRows.first().time)),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = PassportSpacing.sm, bottom = PassportSpacing.xs),
                    )
                }
                itemsIndexed(dayRows, key = { _, row -> row.id }) { index, row ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        Box(Modifier.width(36.dp).fillMaxHeight()) {
                            if (index > 0) {
                                Box(
                                    Modifier.align(Alignment.TopCenter)
                                        .width(2.dp)
                                        .fillMaxHeight(0.5f)
                                        .background(MaterialTheme.colorScheme.outlineVariant),
                                )
                            }
                            if (index < dayRows.lastIndex) {
                                Box(
                                    Modifier.align(Alignment.BottomCenter)
                                        .width(2.dp)
                                        .fillMaxHeight(0.5f)
                                        .background(MaterialTheme.colorScheme.outlineVariant),
                                )
                            }
                            Surface(
                                modifier = Modifier.align(Alignment.Center).size(12.dp),
                                shape = PassportTokens.shapes.full,
                                color = MaterialTheme.colorScheme.tertiary,
                            ) {}
                        }
                        FavoriteTrackCard(
                            track = row,
                            focusHighlighted = row.id == focusedTrackCueId,
                            onClick = { edit(row) },
                            onFavorite = { tier -> favorite(row, tier) },
                            modifier = Modifier.weight(1f).padding(vertical = PassportSpacing.xxs),
                        ) { contentColor, _ ->
                            Column(
                                Modifier.fillMaxWidth().padding(
                                    horizontal = PassportTokens.content.cardHorizontalPadding,
                                    vertical = PassportTokens.content.compactCardVerticalPadding,
                                ),
                                verticalArrangement = Arrangement.spacedBy(PassportSpacing.xxs),
                            ) {
                                Text(
                                    row.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = contentColor,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = TrackHomeActionTokens.cardTitleMaxLines,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${timeFormat.format(Date(row.time))} · ${trackAccuracyLabel(row)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor.copy(alpha = 0.78f),
                                    modifier = Modifier.padding(end = 56.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
    }
}

@Composable
private fun TrackSignatureActions(
    openMap: () -> Unit,
    openFavorites: () -> Unit,
) {
    Surface(
        shape = PassportTokens.shapes.full,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = 1.dp,
        shadowElevation = PassportTokens.components.floatingControlShadowElevation,
        border = floatingControlBorder(),
    ) {
        Row(Modifier.padding(horizontal = PassportTokens.components.signatureContainerHorizontalPadding)) {
            IconButton(
                onClick = openMap,
                modifier = Modifier.size(TrackHomeActionTokens.signatureTargetSize),
            ) {
                Icon(Icons.Default.Map, contentDescription = "打开地图模式")
            }
            IconButton(
                onClick = openFavorites,
                modifier = Modifier.size(TrackHomeActionTokens.signatureTargetSize),
            ) {
                Icon(Icons.Default.Star, contentDescription = "打开地点库")
            }
        }
    }
}

@Composable
private fun ChecklistPage(
    db: PassportDb,
    destinations: List<Destination>,
    refresh: () -> Unit,
    topContentPadding: Dp,
) {
    val haptic = LocalHapticFeedback.current
    var editingDestination by remember { mutableStateOf<Destination?>(null) }
    val completed = destinations.count { it.done }
    val progress = checklistProgress(completed, destinations.size)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 450f),
        label = "checklist progress",
    )
    val pendingItems = destinations.filterNot { it.done }
    val completedItems = destinations.filter { it.done }
    var pendingOrderIds by remember(destinations) { mutableStateOf(pendingItems.map { it.id }) }
    var completedOrderIds by remember(destinations) { mutableStateOf(completedItems.map { it.id }) }
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        val movedId = from.key as? String ?: return@rememberReorderableLazyListState
        val targetId = to.key as? String ?: return@rememberReorderableLazyListState
        when {
            movedId in pendingOrderIds && targetId in pendingOrderIds ->
                pendingOrderIds = moveChecklistId(pendingOrderIds, movedId, targetId)
            movedId in completedOrderIds && targetId in completedOrderIds ->
                completedOrderIds = moveChecklistId(completedOrderIds, movedId, targetId)
            else -> return@rememberReorderableLazyListState
        }
        haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(PassportSpacing.xs),
        contentPadding = PaddingValues(top = topContentPadding, bottom = TrackHomeActionTokens.safeBottomReserve),
    ) {
        item(key = "checklist-progress") {
        Card(
            shape = PassportTokens.shapes.cardShape,
            colors = CardDefaults.cardColors(
                containerColor = primaryPageCardColor(),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(PassportTokens.content.cardHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(PassportSpacing.xs),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("完成度", style = MaterialTheme.typography.titleMedium)
                    Text("$completed / ${destinations.size}", style = MaterialTheme.typography.labelLarge)
                }
                LinearProgressIndicator(
                    progress = { animatedProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(PassportTokens.shapes.full),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Text(
                    "已打卡 ${checklistProgressPercent(completed, destinations.size)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        }
        if (destinations.isEmpty()) {
            item(key = "checklist-empty") {
                Text("清单还是空的", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = PassportSpacing.md))
            }
        } else {
            fun androidx.compose.foundation.lazy.LazyListScope.destinationGroup(
                title: String,
                groupItems: List<Destination>,
                orderIds: List<String>,
            ) {
                val values = orderIds.mapNotNull { id -> groupItems.firstOrNull { it.id == id } }
                if (values.isEmpty()) return
                item(key = "group-$title") {
                    Text(
                        "$title · ${values.size}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = PassportSpacing.sm, bottom = PassportSpacing.xxs),
                    )
                }
                itemsIndexed(values, key = { _, destination -> destination.id }) { groupIndex, destination ->
                    val latestOrderIds by rememberUpdatedState(orderIds)
                    val latestGroupIds by rememberUpdatedState(groupItems.map { it.id })
                    val containerColor = primaryPageCardColor()
                    val contentColor = if (destination.done) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    val accessibilityActions = buildList {
                        if (groupIndex > 0) add(CustomAccessibilityAction("上移 ${destination.name}") {
                            val from = destinations.indexOfFirst { it.id == destination.id }
                            val to = destinations.indexOfFirst { it.id == values[groupIndex - 1].id }
                            if (from >= 0 && to >= 0) {
                                db.reorder(from, to)
                                refresh()
                                true
                            } else false
                        })
                        if (groupIndex < values.lastIndex) add(CustomAccessibilityAction("下移 ${destination.name}") {
                            val from = destinations.indexOfFirst { it.id == destination.id }
                            val to = destinations.indexOfFirst { it.id == values[groupIndex + 1].id }
                            if (from >= 0 && to >= 0) {
                                db.reorder(from, to)
                                refresh()
                                true
                            } else false
                        })
                    }
                    ReorderableItem(reorderableState, key = destination.id) { isDragging ->
                        val interactionSource = remember { MutableInteractionSource() }
                        val cardElevation by animateDpAsState(
                            targetValue = if (isDragging) 8.dp else 0.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "drag elevation",
                        )
                        Card(
                            onClick = { editingDestination = destination },
                            shape = PassportTokens.shapes.cardShape,
                            interactionSource = interactionSource,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDragging) {
                                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f)
                                } else containerColor,
                                contentColor = contentColor,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
                            modifier = Modifier.fillMaxWidth()
                                .animateItem(
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                )
                                .semantics { customActions = accessibilityActions }
                                .longPressDraggableHandle(
                                    interactionSource = interactionSource,
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                    },
                                    onDragStopped = {
                                        val finalIndex = latestOrderIds.indexOf(destination.id)
                                        reorderIndicesWithinGroup(
                                            allIds = destinations.map { it.id },
                                            originalGroupIds = latestGroupIds,
                                            movedId = destination.id,
                                            finalGroupIndex = finalIndex,
                                        )?.let { (fromIndex, toIndex) -> db.reorder(fromIndex, toIndex) }
                                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                        refresh()
                                    },
                                ),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(
                                    horizontal = PassportTokens.content.cardHorizontalPadding,
                                    vertical = PassportTokens.content.compactCardVerticalPadding,
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = destination.done,
                                    onCheckedChange = { db.setDestinationDone(destination.id, it); refresh() },
                                )
                                Column(Modifier.weight(1f).padding(start = PassportSpacing.xxs)) {
                                    Text(destination.name, style = MaterialTheme.typography.titleMedium, color = contentColor)
                                    if (destination.qrId != null) Text("已绑定二维码", style = MaterialTheme.typography.bodySmall, color = contentColor)
                                }
                            }
                        }
                    }
                }
            }
            destinationGroup("待打卡", pendingItems, pendingOrderIds)
            destinationGroup("已完成", completedItems, completedOrderIds)
        }
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
private fun QrPage(
    db: PassportDb,
    qrs: List<PassportQr>,
    destinations: List<Destination>,
    topContentPadding: Dp,
    refresh: () -> Unit,
    edit: (PassportQr) -> Unit,
    present: (PassportQr) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<PassportQr?>(null) }
    val context = LocalContext.current
    val lastStarredIndex = qrs.indexOfLast { it.starred }
    if (qrs.isEmpty()) AlwaysStretchColumn(
        verticalArrangement = Arrangement.spacedBy(PassportSpacing.sm),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topContentPadding),
    ) {
        QrSyncHint()
        Card(
            shape = PassportTokens.shapes.cardShape,
            colors = CardDefaults.cardColors(
                containerColor = primaryPageCardColor(),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(PassportTokens.content.cardHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(PassportSpacing.xs),
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                Text("还没有二维码", style = MaterialTheme.typography.titleMedium)
                Text("可保存登机牌、会员码或活动凭证，内容只保存在手机本地。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else AlwaysStretchLazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(PassportSpacing.xs),
        contentPadding = PaddingValues(top = topContentPadding, bottom = TrackHomeActionTokens.safeBottomReserve),
    ) {
            if (lastStarredIndex < 0) item(key = "qr-sync-hint") { QrSyncHint() }
            itemsIndexed(qrs, key = { _, qr -> qr.id }) { index, qr ->
                val boundDestination = destinations.firstOrNull { it.qrId == qr.id }
                val qrContentColor = MaterialTheme.colorScheme.onSurface
                val qrSupportingColor = MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    modifier = Modifier.animateItem(
                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    ),
                    verticalArrangement = Arrangement.spacedBy(PassportSpacing.xs),
                ) {
                    Card(
                        shape = PassportTokens.shapes.cardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = primaryPageCardColor(),
                            contentColor = qrContentColor,
                        ),
                        modifier = Modifier.fillMaxWidth().clickable { present(qr) },
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(
                                horizontal = PassportTokens.content.cardHorizontalPadding,
                                vertical = PassportTokens.content.compactCardVerticalPadding,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                Modifier.weight(1f).padding(end = PassportSpacing.xs),
                                verticalArrangement = Arrangement.spacedBy(PassportSpacing.xxs),
                            ) {
                                Text(qr.name, style = MaterialTheme.typography.titleMedium)
                                boundDestination?.let {
                                    Text("绑定 ${it.name}", style = MaterialTheme.typography.bodySmall, color = qrSupportingColor)
                                }
                            }
                            IconToggleButton(
                                checked = qr.starred,
                                onCheckedChange = { checked ->
                                    runCatching { db.addQr(qr.name, qr.payload, checked, qr.id) }
                                        .onSuccess { refresh() }
                                        .onFailure { Toast.makeText(context, it.message ?: "星标更新失败", Toast.LENGTH_LONG).show() }
                                },
                            ) {
                                Icon(
                                    if (qr.starred) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = if (qr.starred) "取消星标 ${qr.name}" else "星标 ${qr.name}",
                                    tint = if (qr.starred) PassportNeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { edit(qr) }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑 ${qr.name}")
                            }
                            IconButton(onClick = { pendingDelete = qr }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除 ${qr.name}")
                            }
                        }
                    }
                    if (index == lastStarredIndex) QrSyncHint()
                }
            }
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun QrSyncHint() {
    Text(
        "最多星标 3 个 · 星标和清单关联码会同步到 Passport",
        modifier = Modifier.padding(
            start = PassportSpacing.md,
            end = PassportSpacing.md,
            top = PassportSpacing.sm,
        ),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrPresentScreen(qr: PassportQr, onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context.findActivity()
    val bitmap = remember(qr.payload) { runCatching { QrCodeImage.create(qr.payload) }.getOrNull() }
    BackHandler(onBack = onBack)
    DisposableEffect(view, activity) {
        val oldKeepScreenOn = view.keepScreenOn
        val oldBrightness = activity?.window?.attributes?.screenBrightness
        view.keepScreenOn = true
        activity?.window?.attributes = activity.window.attributes.apply { screenBrightness = 1f }
        onDispose {
            view.keepScreenOn = oldKeepScreenOn
            if (oldBrightness != null) activity.window.attributes = activity.window.attributes.apply { screenBrightness = oldBrightness }
        }
    }
    PassportCornerScaffold(
        topStart = {
            PassportCornerAction(onClick = onBack, contentDescription = "返回二维码列表") {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
        },
    ) { _ ->
        Column(
            Modifier.fillMaxSize().padding(horizontal = PassportSpacing.lg, vertical = PassportSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("请将屏幕对准扫码器", style = MaterialTheme.typography.titleMedium)
            Text(
                "已保持亮屏并临时调至最高亮度",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = PassportSpacing.xxs, bottom = PassportSpacing.lg),
            )
            Surface(
                shape = PassportTokens.shapes.cardShape,
                color = PassportTokens.palette.qrBackground,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp).aspectRatio(1f),
            ) {
                if (bitmap != null) Image(
                    bitmap.asImageBitmap(),
                    contentDescription = "${qr.name} 二维码",
                    modifier = Modifier.fillMaxSize().padding(PassportSpacing.md),
                    contentScale = ContentScale.Fit,
                ) else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("二维码生成失败", color = MaterialTheme.colorScheme.error)
                }
            }
            Text(
                "二维码内容不会显示在页面上，避免旁人直接读取原文。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = PassportSpacing.lg),
            )
        }
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
    val sourceScrollState = rememberScrollState()
    BackHandler(onBack = leave)
    val previewOrImport: (() -> Unit) = confirmImport ?: { start(ImportSource.Pasted(pasted)) }
    val canProceed = if (extraction == null) pasted.isNotBlank() && job == null else selected.isNotEmpty() && job == null
    PassportPageScaffold(
        title = "添加到清单",
        onBack = leave,
        actions = {
            PassportCornerAction(
                onClick = {
                    fileLauncher.launch(arrayOf(
                        "text/*", "image/*", "application/pdf", "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    ))
                },
                enabled = job == null,
                contentDescription = "选择文件",
            ) { Icon(Icons.Default.FolderOpen, null) }
        },
        primaryActionLabel = if (extraction == null) "生成预览" else "添加 ${selected.size} 项",
        primaryActionIcon = if (extraction == null) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
        primaryActionEnabled = canProceed,
        primaryActionLoading = job != null,
        onPrimaryAction = previewOrImport,
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().widthIn(max = PassportSpacing.listMaxWidth).align(Alignment.TopCenter)
                    .then(if (extraction == null) Modifier.verticalScroll(sourceScrollState) else Modifier)
                    .padding(
                        start = PassportSpacing.md,
                        end = PassportSpacing.md,
                        top = padding.calculateTopPadding(),
                        bottom = if (extraction == null) padding.calculateBottomPadding() else 0.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(PassportSpacing.sm),
            ) {
                if (extraction == null) {
                    Text(
                        "输入一个地点、粘贴多行清单或公开网页链接，也可选择 Excel、CSV、图片和 PDF。内容只在手机本地解析。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = PassportSpacing.md),
                    )
                    OutlinedTextField(pasted, { pasted = it }, label = { Text("地点或清单内容") }, minLines = 6, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime))
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
                        OutlinedButton(
                            onClick = { columnMenuExpanded = true },
                            shape = PassportTokens.shapes.buttonShape,
                        ) {
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
                        contentPadding = PaddingValues(
                            bottom = padding.calculateBottomPadding() + PassportSpacing.sm,
                        ),
                        verticalArrangement = Arrangement.spacedBy(PassportSpacing.xs),
                    ) {
                        itemsIndexed(candidates, key = { _, item -> item.id }) { index, item ->
                            Card(
                                shape = PassportTokens.shapes.cardShape,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(
                                        horizontal = PassportTokens.content.cardHorizontalPadding,
                                        vertical = PassportTokens.content.compactCardVerticalPadding,
                                    ),
                                ) {
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
                    TextButton(
                        onClick = { job?.cancel(); job = null; progress = "已取消" },
                        shape = PassportTokens.shapes.buttonShape,
                    ) { Text("取消") }
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
    destination: Destination,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember(destination.id) { mutableStateOf(destination.name) }
    var confirmDelete by remember(destination.id) { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("编辑地点") },
        text = { OutlinedTextField(text, { text = it }, label = { Text("目的地") }, singleLine = true) },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { confirmDelete = true }, shape = PassportTokens.shapes.buttonShape) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss, shape = PassportTokens.shapes.buttonShape) { Text("取消") }
                TextButton(onClick = { onSave(text) }, shape = PassportTokens.shapes.buttonShape) { Text("保存") }
            }
        })
    if (confirmDelete) ConfirmDeleteDialog(
        title = "删除“${destination.name}”？",
        description = "该项目会从打卡清单中移除，此操作无法恢复。",
        onConfirm = onDelete,
        onDismiss = { confirmDelete = false },
    )
}
@Composable
private fun QrEditorScreen(
    qr: PassportQr?,
    starredCount: Int,
    destinations: List<Destination>,
    boundDestinationId: String?,
    onBack: () -> Unit,
    onSave: (String, String, Boolean, String?) -> Unit,
) {
    var name by rememberSaveable(qr?.id) { mutableStateOf(qr?.name.orEmpty()) }
    var payload by rememberSaveable(qr?.id) { mutableStateOf(qr?.payload.orEmpty()) }
    var starred by rememberSaveable(qr?.id) { mutableStateOf(qr?.starred ?: false) }
    var selectedDestinationId by rememberSaveable(qr?.id, boundDestinationId) { mutableStateOf(boundDestinationId) }
    var destinationMenuOpen by rememberSaveable { mutableStateOf(false) }
    val canSave = name.trim().isNotEmpty() && payload.trim().let {
        it.isNotEmpty() && it.toByteArray(Charsets.UTF_8).size <= 192
    }
    PassportPageScaffold(
        title = if (qr == null) "添加二维码" else "编辑二维码",
        onBack = onBack,
        primaryActionLabel = if (qr == null) "添加二维码" else "保存更改",
        primaryActionEnabled = canSave,
        onPrimaryAction = { onSave(name, payload, starred, selectedDestinationId) },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxWidth().widthIn(max = PassportSpacing.formMaxWidth)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = PassportSpacing.md,
                        end = PassportSpacing.md,
                        top = padding.calculateTopPadding() + PassportSpacing.md,
                        bottom = padding.calculateBottomPadding() + PassportSpacing.md,
                    ),
                verticalArrangement = Arrangement.spacedBy(PassportSpacing.md),
            ) {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    payload,
                    { payload = it },
                    label = { Text("二维码原文") },
                    minLines = 5,
                    supportingText = { Text("UTF-8 最多 192 字节") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Surface(
                    shape = PassportTokens.shapes.cardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(PassportTokens.content.cardHorizontalPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("星标同步", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "最多 3 个",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = starred,
                            enabled = starred || qr?.starred == true || starredCount < 3,
                            onCheckedChange = { starred = it },
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(PassportSpacing.xs)) {
                    Text("绑定打卡项目", style = MaterialTheme.typography.titleSmall)
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { destinationMenuOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = PassportTokens.shapes.buttonShape,
                        ) {
                            Text(destinations.firstOrNull { it.id == selectedDestinationId }?.name ?: "不绑定")
                        }
                        DropdownMenu(
                            expanded = destinationMenuOpen,
                            onDismissRequest = { destinationMenuOpen = false },
                        ) {
                            DropdownMenuItem(text = { Text("不绑定") }, onClick = {
                                selectedDestinationId = null
                                destinationMenuOpen = false
                            })
                            destinations.forEach { destination ->
                                DropdownMenuItem(text = { Text(destination.name) }, onClick = {
                                    selectedDestinationId = destination.id
                                    destinationMenuOpen = false
                                })
                            }
                        }
                    }
                }
                Text(
                    "保存后可直接在手机内全屏出示。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = PassportTokens.shapes.fieldShape,
        color = Color.Transparent,
        border = BorderStroke(PassportTokens.borders.standard, MaterialTheme.colorScheme.outline),
        modifier = modifier.heightIn(min = 56.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = PassportSpacing.md, vertical = PassportSpacing.xs),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TrackFieldPair(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = shouldStackTrackFields(maxWidth.value, fontScale)
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(PassportSpacing.xs)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PassportSpacing.xs),
            ) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReadOnlyTrackField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = PassportTokens.shapes.fieldShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(PassportTokens.borders.standard, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.heightIn(min = 64.dp).clearAndSetSemantics {
            contentDescription = "$label，$value，只读"
        },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = PassportSpacing.md, vertical = PassportSpacing.xs),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TrackNearbyPlacesSection(
    name: String,
    places: List<NearbyPlaceCandidate>,
    loading: Boolean,
    expanded: Boolean,
    emptyMessage: String?,
    idleActionLabel: String,
    editingEnabled: Boolean = true,
    onToggle: () -> Unit,
    onSelect: (NearbyPlaceCandidate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PassportSpacing.xs)) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("附近地点", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            val status = when {
                loading -> "正在查找"
                places.isNotEmpty() -> "已找到 ${places.size} 个候选"
                !emptyMessage.isNullOrBlank() -> emptyMessage
                else -> null
            }
            if (status != null) Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!loading) TextButton(
            onClick = onToggle,
            enabled = editingEnabled,
            shape = PassportTokens.shapes.buttonShape,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        ) {
            Text(if (places.isNotEmpty()) if (expanded) "收起" else "展开" else idleActionLabel)
        }
    }
    if (loading) Row(
        Modifier.fillMaxWidth().padding(vertical = PassportSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(PassportSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            strokeWidth = 2.dp,
        )
        Text("正在查找附近名称…", style = MaterialTheme.typography.bodyMedium)
    }
    AnimatedVisibility(visible = expanded && places.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(PassportSpacing.xxs)) {
            places.forEach { candidate ->
                val selected = name.trim() == candidate.name
                Surface(
                    shape = PassportTokens.shapes.cardShape,
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().selectable(
                        selected = selected,
                        enabled = editingEnabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(candidate) },
                    ),
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = PassportTokens.components.trackToolRowMinHeight)
                            .padding(horizontal = PassportTokens.content.cardHorizontalPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = null, enabled = editingEnabled)
                        Text(candidate.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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
}

@Composable
private fun TrackToolRow(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().heightIn(min = PassportTokens.components.trackToolRowMinHeight),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = PassportTokens.content.cardHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PassportSpacing.xs),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TrackMapToolsSection(
    mapEnabled: Boolean,
    linkExpanded: Boolean,
    linkText: String,
    parsingLink: Boolean,
    editingEnabled: Boolean = true,
    onOpenMap: () -> Unit,
    onToggleLink: () -> Unit,
    onLinkTextChange: (String) -> Unit,
    onParseLink: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PassportSpacing.xs)) {
    Text("地图与名称识别", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Surface(
        shape = PassportTokens.shapes.cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            TrackToolRow(Icons.Default.Map, "在地图中查看", enabled = mapEnabled, onClick = onOpenMap)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            TrackToolRow(
                Icons.Default.Link,
                if (linkExpanded) "收起地图分享识别" else "从地图分享内容识别",
                enabled = editingEnabled,
                onClick = onToggleLink,
            )
        }
    }
    AnimatedVisibility(visible = linkExpanded) {
        Column(verticalArrangement = Arrangement.spacedBy(PassportSpacing.xs)) {
            OutlinedTextField(
                value = linkText,
                onValueChange = onLinkTextChange,
                readOnly = !editingEnabled,
                label = { Text("粘贴高德或 Google Maps 分享内容") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            FilledTonalButton(
                onClick = onParseLink,
                enabled = editingEnabled && linkText.isNotBlank() && !parsingLink,
                modifier = Modifier.align(Alignment.End),
                shape = PassportTokens.shapes.buttonShape,
            ) { Text(if (parsingLink) "解析中…" else "识别名称") }
        }
    }
    }
}

@Composable
private fun TrackEditorPage(
    title: String,
    primaryActionLabel: String?,
    name: String,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    favoriteTier: Int = 0,
    onFavorite: ((Int) -> Unit)? = null,
    editingEnabled: Boolean = true,
    recordInfo: @Composable () -> Unit,
    nearbyPlaces: List<NearbyPlaceCandidate>,
    nearbyLoading: Boolean,
    nearbyExpanded: Boolean,
    nearbyEmptyMessage: String?,
    nearbyIdleActionLabel: String,
    onToggleNearby: () -> Unit,
    onSelectNearby: (NearbyPlaceCandidate) -> Unit,
    mapEnabled: Boolean,
    onOpenMap: () -> Unit,
    linkExpanded: Boolean,
    linkText: String,
    parsingLink: Boolean,
    onToggleLink: () -> Unit,
    onLinkTextChange: (String) -> Unit,
    onParseLink: () -> Unit,
    feedback: String,
    feedbackIsError: Boolean,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var favoriteDialogOpen by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    @Suppress("UNUSED_VARIABLE") val legacyPageTitle = title
    PassportCornerScaffold(
        topStart = {
            PassportCornerAction(onClick = onBack, contentDescription = "返回") {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
        },
        topEnd = onFavorite?.let { {
            PassportCornerAction(onClick = { favoriteDialogOpen = true }, contentDescription = "收藏这条轨迹") {
                Icon(if (favoriteTier > 0) Icons.Default.Star else Icons.Default.StarBorder, null)
            }
        } },
        bottomStart = if (onSave != null && primaryActionLabel != null) {
            {
            PassportCornerAction(onClick = onSave, contentDescription = primaryActionLabel) {
                Icon(Icons.Default.Check, null)
            }
            }
        } else null,
        bottomEnd = onDelete?.let { {
            PassportCornerAction(
                onClick = { confirmDelete = true },
                contentDescription = "删除这条轨迹",
                contentColor = MaterialTheme.colorScheme.error,
            ) { Icon(Icons.Default.Delete, null) }
        } },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().widthIn(max = PassportSpacing.formMaxWidth).align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState()).padding(
                        start = PassportSpacing.md,
                        end = PassportSpacing.md,
                        top = padding.calculateTopPadding() + PassportSpacing.md,
                        bottom = padding.calculateBottomPadding() + PassportSpacing.md,
                    ),
                verticalArrangement = Arrangement.spacedBy(PassportSpacing.md),
            ) {
                TrackNameTextField(
                    value = name,
                    onValueChange = onNameChange,
                    readOnly = !editingEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                TrackNearbyPlacesSection(
                    name = name,
                    places = nearbyPlaces,
                    loading = nearbyLoading,
                    expanded = nearbyExpanded,
                    emptyMessage = nearbyEmptyMessage,
                    idleActionLabel = nearbyIdleActionLabel,
                    editingEnabled = editingEnabled,
                    onToggle = onToggleNearby,
                    onSelect = onSelectNearby,
                )
                TrackMapToolsSection(
                    mapEnabled = mapEnabled,
                    linkExpanded = linkExpanded,
                    linkText = linkText,
                    parsingLink = parsingLink,
                    editingEnabled = editingEnabled,
                    onOpenMap = onOpenMap,
                    onToggleLink = onToggleLink,
                    onLinkTextChange = onLinkTextChange,
                    onParseLink = onParseLink,
                )
                recordInfo()
                if (feedback.isNotBlank()) Text(
                    feedback,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (feedbackIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(PassportSpacing.md))
            }
        }
    }
    if (confirmDelete && onDelete != null) ConfirmDeleteDialog(
        title = "删除这条轨迹？",
        description = "删除后，这条轨迹的名称、时间和位置记录都会从手机中移除，且无法恢复。",
        onConfirm = onDelete,
        onDismiss = { confirmDelete = false },
    )
    if (favoriteDialogOpen && onFavorite != null) FavoriteTierDialog(
        currentTier = favoriteTier,
        onDismiss = { favoriteDialogOpen = false },
        onSave = { tier ->
            favoriteDialogOpen = false
            onFavorite(tier)
        },
    )
}

internal fun collapsedEditorValue(value: String, maxCharacters: Int = 18): String {
    require(maxCharacters >= 2)
    return if (value.length <= maxCharacters) value else value.take(maxCharacters - 1).trimEnd() + "…"
}

internal fun collapsedEditorCharacterLimit(fontScale: Float): Int =
    (18f / fontScale.coerceAtLeast(1f)).toInt().coerceIn(8, 18)

private class TrailingEllipsisTransformation(
    private val maxCharacters: Int,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val collapsed = collapsedEditorValue(text.text, maxCharacters)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset.coerceAtMost(collapsed.length)
            override fun transformedToOriginal(offset: Int): Int = offset.coerceAtMost(text.length)
        }
        return TransformedText(AnnotatedString(collapsed), mapping)
    }
}

@Composable
private fun TrackNameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val characterLimit = collapsedEditorCharacterLimit(LocalDensity.current.fontScale)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        label = { Text("地点名称") },
        singleLine = true,
        visualTransformation = if (focused) VisualTransformation.None else remember(characterLimit) {
            TrailingEllipsisTransformation(maxCharacters = characterLimit)
        },
        modifier = modifier.onFocusChanged { focused = it.isFocused },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualTrackEditorScreen(
    track: TrackPoint?,
    onBack: () -> Unit,
    onSave: (ManualTrackDraft) -> Unit,
    onDelete: (() -> Unit)? = null,
    onFavorite: ((Int) -> Unit)? = null,
    favoriteOnly: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable(track?.id) { mutableStateOf(track?.name.orEmpty()) }
    var timestamp by rememberSaveable(track?.id) { mutableStateOf(track?.time ?: System.currentTimeMillis()) }
    var latitude by rememberSaveable(track?.id) { mutableStateOf(track?.lat?.let(::formatCoordinate).orEmpty()) }
    var longitude by rememberSaveable(track?.id) { mutableStateOf(track?.lon?.let(::formatCoordinate).orEmpty()) }
    var accuracy by rememberSaveable(track?.id) { mutableStateOf(track?.accuracy ?: 0f) }
    var provider by rememberSaveable(track?.id) { mutableStateOf(track?.provider ?: PlaceProvider.MANUAL) }
    var address by rememberSaveable(track?.id) { mutableStateOf(track?.address) }
    var placeId by rememberSaveable(track?.id) { mutableStateOf(track?.placeId) }
    var originalUrl by rememberSaveable(track?.id) { mutableStateOf(track?.originalUrl) }
    var canonicalUrl by rememberSaveable(track?.id) { mutableStateOf(track?.canonicalUrl) }
    var resolutionState by rememberSaveable(track?.id) {
        mutableStateOf(track?.resolutionState ?: PlaceResolutionState.MANUAL_CONFIRMATION)
    }
    var initialLocationRequested by rememberSaveable(track?.id) { mutableStateOf(track != null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var locateRequest by remember { mutableIntStateOf(0) }
    var locationMessage by rememberSaveable(track?.id) { mutableStateOf("") }
    var message by rememberSaveable(track?.id) { mutableStateOf("") }
    var messageIsError by rememberSaveable(track?.id) { mutableStateOf(false) }
    var nearbyPlaces by remember { mutableStateOf<List<NearbyPlaceCandidate>>(emptyList()) }
    var nearbyLoading by remember { mutableStateOf(false) }
    var nearbyExpanded by rememberSaveable(track?.id) { mutableStateOf(false) }
    var nearbyMessage by rememberSaveable(track?.id) { mutableStateOf("") }
    var linkToolsExpanded by rememberSaveable(track?.id) { mutableStateOf(false) }
    var linkText by rememberSaveable(track?.id) { mutableStateOf(track?.originalUrl.orEmpty()) }
    var parsingLink by remember { mutableStateOf(false) }
    var mapPickerOpen by remember { mutableStateOf(false) }

    fun coordinateTrack(): TrackPoint? {
        val lat = latitude.toDoubleOrNull()?.takeIf { it.isFinite() && it in -90.0..90.0 } ?: return null
        val lon = longitude.toDoubleOrNull()?.takeIf { it.isFinite() && it in -180.0..180.0 } ?: return null
        return TrackPoint(
            id = track?.id ?: -1L,
            requestId = track?.requestId ?: "manual-preview",
            destinationId = null,
            name = name.ifBlank { "未命名地点" },
            lat = lat,
            lon = lon,
            time = timestamp,
            accuracy = accuracy,
            provider = provider,
            address = address,
            placeId = placeId,
            originalUrl = originalUrl,
            canonicalUrl = canonicalUrl,
            resolutionState = resolutionState,
            entrySource = TrackEntrySource.MANUAL,
        )
    }

    fun findNearby(selectFirstWhenUnnamed: Boolean = false) {
        val coordinate = coordinateTrack()
        if (coordinate == null) {
            message = "请先输入有效的经纬度"
            messageIsError = true
            return
        }
        nearbyLoading = true
        nearbyMessage = ""
        scope.launch {
            runCatching { loadNearbyPlaces(context, coordinate) }
                .onSuccess { values ->
                    nearbyPlaces = values
                    nearbyExpanded = false
                    if (selectFirstWhenUnnamed && name.isBlank()) values.firstOrNull()?.let { candidate ->
                        name = candidate.name
                        provider = PlaceProvider.SYSTEM
                        address = candidate.address
                        resolutionState = PlaceResolutionState.PARTIAL
                    }
                    if (values.isEmpty()) {
                        nearbyMessage = "附近没有可用的地点名称，可继续手动输入"
                    }
                }
                .onFailure {
                    nearbyMessage = "附近地点加载失败，可继续手动输入"
                }
            nearbyLoading = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) locateRequest++ else {
            locationMessage = "未获得定位权限，请手动输入坐标或稍后重新定位"
        }
    }

    fun requestLocationPermissionOrLocate() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted) locateRequest++ else permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        )
    }

    LaunchedEffect(track?.id) {
        if (!favoriteOnly && track == null && !initialLocationRequested) {
            initialLocationRequested = true
            requestLocationPermissionOrLocate()
        }
    }
    LaunchedEffect(locateRequest) {
        if (favoriteOnly || locateRequest == 0) return@LaunchedEffect
        locating = true
        locationMessage = ""
        val location = findCurrentLocation(context)
        locating = false
        if (location == null) {
            locationMessage = "无法获取当前位置，请检查系统定位或手动输入坐标"
        } else {
            latitude = formatCoordinate(location.latitude)
            longitude = formatCoordinate(location.longitude)
            accuracy = location.accuracy.coerceAtLeast(0f)
            locationMessage = ""
            findNearby(selectFirstWhenUnnamed = true)
        }
    }

    fun parseLink(content: String = linkText) {
        if (content.isBlank() || parsingLink) return
        parsingLink = true
        message = ""
        scope.launch {
            runCatching { resolveSharedPlace(content) }
                .onSuccess { place ->
                    name = place.name.orEmpty()
                    provider = place.provider
                    address = place.address
                    placeId = place.placeId
                    originalUrl = place.originalUrl ?: content
                    canonicalUrl = place.canonicalUrl
                    resolutionState = place.state
                    linkToolsExpanded = false
                    message = "已填入地图识别到的名称，坐标保持不变"
                    messageIsError = false
                }
                .onFailure {
                    message = it.message ?: "链接解析失败"
                    messageIsError = true
                }
            parsingLink = false
        }
    }

    fun save() {
        val draft = runCatching {
            ManualTrackDraft(
                name = name.trim(),
                latitude = latitude.toDouble(),
                longitude = longitude.toDouble(),
                time = timestamp,
                accuracy = accuracy,
                provider = provider,
                address = address,
                placeId = placeId,
                originalUrl = originalUrl,
                canonicalUrl = canonicalUrl,
                resolutionState = resolutionState,
            ).also(ManualTrackDraft::requireValid)
        }.getOrElse {
            message = it.message ?: "请检查地点名称、时间和坐标"
            messageIsError = true
            return
        }
        onSave(draft)
    }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val mode = if (track == null) TrackEditorMode.CREATE_MANUAL else TrackEditorMode.EDIT_MANUAL
    val capabilities = mode.capabilities()
    TrackEditorPage(
        title = mode.title(),
        primaryActionLabel = mode.primaryActionLabel().takeUnless { favoriteOnly },
        name = name,
        onNameChange = {
            name = it
            provider = PlaceProvider.MANUAL
            resolutionState = PlaceResolutionState.MANUAL_CONFIRMATION
        },
        onBack = onBack,
        onSave = if (favoriteOnly) null else ::save,
        onDelete = onDelete.takeIf { capabilities.canDelete && !favoriteOnly },
        favoriteTier = track?.favoriteTier ?: 0,
        onFavorite = onFavorite.takeIf { track != null },
        editingEnabled = !favoriteOnly,
        recordInfo = {
            Column(verticalArrangement = Arrangement.spacedBy(PassportSpacing.xs)) {
            Text("记录信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            TrackFieldPair(
                first = { modifier ->
                    if (favoriteOnly) ReadOnlyTrackField("日期", dateFormat.format(Date(timestamp)), modifier)
                    else PickerField("日期", dateFormat.format(Date(timestamp)), { showDatePicker = true }, modifier)
                },
                second = { modifier ->
                    if (favoriteOnly) ReadOnlyTrackField("时间", timeFormat.format(Date(timestamp)), modifier)
                    else PickerField("时间", timeFormat.format(Date(timestamp)), { showTimePicker = true }, modifier)
                },
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("坐标", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                if (!favoriteOnly) FilledTonalButton(
                    onClick = ::requestLocationPermissionOrLocate,
                    enabled = !locating,
                    shape = PassportTokens.shapes.buttonShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(PassportSpacing.xxs))
                    Text(if (locating) "定位中…" else "重新定位")
                }
            }
            TrackFieldPair(
                first = { modifier ->
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it; accuracy = 0f; locationMessage = "" },
                        readOnly = favoriteOnly,
                        label = { Text("纬度") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = modifier,
                    )
                },
                second = { modifier ->
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it; accuracy = 0f; locationMessage = "" },
                        readOnly = favoriteOnly,
                        label = { Text("经度") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = modifier,
                    )
                },
            )
            trackPrecisionLabel(accuracy)?.let { precision ->
                Text(
                    precision,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (locationMessage.isNotBlank()) Text(
                locationMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }
        },
        nearbyPlaces = nearbyPlaces,
        nearbyLoading = nearbyLoading,
        nearbyExpanded = nearbyExpanded,
        nearbyEmptyMessage = nearbyMessage,
        nearbyIdleActionLabel = if (favoriteOnly) "不可修改" else "查找",
        onToggleNearby = { if (nearbyPlaces.isEmpty()) findNearby() else nearbyExpanded = !nearbyExpanded },
        onSelectNearby = { candidate ->
            name = candidate.name
            provider = PlaceProvider.SYSTEM
            address = candidate.address
            resolutionState = PlaceResolutionState.PARTIAL
        },
        mapEnabled = coordinateTrack() != null,
        onOpenMap = { mapPickerOpen = true },
        linkExpanded = linkToolsExpanded,
        linkText = linkText,
        parsingLink = parsingLink,
        onToggleLink = { linkToolsExpanded = !linkToolsExpanded },
        onLinkTextChange = { linkText = it },
        onParseLink = { parseLink() },
        feedback = message,
        feedbackIsError = messageIsError,
    )

    if (track != null && !favoriteOnly) ClipboardMapLinkPrompt(
        promptKey = "manual-track-${track.id}",
        onRecognize = { content ->
            linkText = content
            parseLink(content)
        },
    )

    if (showDatePicker && !favoriteOnly) {
        DateWheelPickerDialog(
            timestamp = timestamp,
            onDismiss = { showDatePicker = false },
            onConfirm = { year, month, day ->
                timestamp = applyPickerDate(timestamp, year, month, day)
                showDatePicker = false
            },
        )
    }
    if (showTimePicker && !favoriteOnly) {
        TimeWheelPickerDialog(
            timestamp = timestamp,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute, second ->
                timestamp = applyPickerTime(timestamp, hour, minute, second)
                showTimePicker = false
            },
        )
    }
    if (mapPickerOpen) coordinateTrack()?.let { value ->
        MapProviderPickerSheet(
            onDismiss = { mapPickerOpen = false },
            onAmap = { mapPickerOpen = false; ExternalMapLauncher.openAmap(context, value) },
            onGoogle = { mapPickerOpen = false; ExternalMapLauncher.openGoogle(context, value) },
        )
    }
}

@Composable
private fun DateWheelPickerDialog(
    timestamp: Long,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int, day: Int) -> Unit,
) {
    val initial = remember(timestamp) { Calendar.getInstance().apply { timeInMillis = timestamp } }
    val initialYear = initial.get(Calendar.YEAR)
    var year by remember(timestamp) { mutableIntStateOf(initialYear) }
    var month by remember(timestamp) { mutableIntStateOf(initial.get(Calendar.MONTH) + 1) }
    var day by remember(timestamp) { mutableIntStateOf(initial.get(Calendar.DAY_OF_MONTH)) }
    val years = remember(initialYear) { (initialYear - 100..initialYear + 100).toList() }
    val months = remember { (1..12).toList() }
    val days = remember(year, month) { (1..daysInMonth(year, month)).toList() }
    LaunchedEffect(days.size) { day = day.coerceIn(1, days.size) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期") },
        text = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PassportSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WheelPicker(
                    values = years,
                    selectedValue = year,
                    label = "年份",
                    display = { "${it}年" },
                    onSelected = { year = it },
                    modifier = Modifier.weight(1.35f),
                )
                WheelPicker(
                    values = months,
                    selectedValue = month,
                    label = "月份",
                    display = { "${it}月" },
                    onSelected = { month = it },
                    modifier = Modifier.weight(1f),
                )
                WheelPicker(
                    values = days,
                    selectedValue = day.coerceIn(1, days.size),
                    label = "日期",
                    display = { "${it}日" },
                    onSelected = { day = it },
                    modifier = Modifier.weight(1f),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(year, month, day.coerceIn(1, days.size)) },
                shape = PassportTokens.shapes.buttonShape,
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = PassportTokens.shapes.buttonShape) { Text("取消") }
        },
    )
}

@Composable
private fun TimeWheelPickerDialog(
    timestamp: Long,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int, second: Int) -> Unit,
) {
    val initial = remember(timestamp) { Calendar.getInstance().apply { timeInMillis = timestamp } }
    var hour by remember(timestamp) { mutableIntStateOf(initial.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember(timestamp) { mutableIntStateOf(initial.get(Calendar.MINUTE)) }
    var second by remember(timestamp) { mutableIntStateOf(initial.get(Calendar.SECOND)) }
    val hours = remember { (0..23).toList() }
    val minuteOrSecond = remember { (0..59).toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间") },
        text = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PassportSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WheelPicker(
                    values = hours,
                    selectedValue = hour,
                    label = "小时",
                    display = { "%02d时".format(Locale.getDefault(), it) },
                    onSelected = { hour = it },
                    modifier = Modifier.weight(1f),
                )
                WheelPicker(
                    values = minuteOrSecond,
                    selectedValue = minute,
                    label = "分钟",
                    display = { "%02d分".format(Locale.getDefault(), it) },
                    onSelected = { minute = it },
                    modifier = Modifier.weight(1f),
                )
                WheelPicker(
                    values = minuteOrSecond,
                    selectedValue = second,
                    label = "秒",
                    display = { "%02d秒".format(Locale.getDefault(), it) },
                    onSelected = { second = it },
                    modifier = Modifier.weight(1f),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(hour, minute, second) },
                shape = PassportTokens.shapes.buttonShape,
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = PassportTokens.shapes.buttonShape) { Text("取消") }
        },
    )
}

@Composable
private fun WheelPicker(
    values: List<Int>,
    selectedValue: Int,
    label: String,
    display: (Int) -> String,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    val listState = remember(values.firstOrNull(), values.size) {
        androidx.compose.foundation.lazy.LazyListState(selectedIndex)
    }
    val scope = rememberCoroutineScope()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val itemHeight = 48.dp

    LaunchedEffect(listState, values) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { item -> abs(item.offset + item.size / 2 - center) }?.index
        }.distinctUntilChanged().collect { index ->
            index?.let { values.getOrNull(it) }?.let(onSelected)
        }
    }

    Box(
        modifier.height(itemHeight * 3).semantics {
            customActions = listOf(
                CustomAccessibilityAction("增加$label") {
                    val nextIndex = values.indexOf(selectedValue) + 1
                    val next = values.getOrNull(nextIndex) ?: return@CustomAccessibilityAction false
                    onSelected(next)
                    scope.launch { listState.animateScrollToItem(nextIndex) }
                    true
                },
                CustomAccessibilityAction("减少$label") {
                    val previousIndex = values.indexOf(selectedValue) - 1
                    val previous = values.getOrNull(previousIndex) ?: return@CustomAccessibilityAction false
                    onSelected(previous)
                    scope.launch { listState.animateScrollToItem(previousIndex) }
                    true
                },
            )
        },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(itemHeight).align(Alignment.Center),
            shape = PassportTokens.shapes.fieldShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {}
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(values, key = { it }) { value ->
                Box(Modifier.fillMaxWidth().height(itemHeight), contentAlignment = Alignment.Center) {
                    Text(
                        display(value),
                        style = if (value == selectedValue) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                        color = if (value == selectedValue) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

internal fun daysInMonth(year: Int, month: Int): Int = Calendar.getInstance().apply {
    clear()
    set(Calendar.YEAR, year)
    set(Calendar.MONTH, month - 1)
}.getActualMaximum(Calendar.DAY_OF_MONTH)

private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

internal fun applyPickerDate(timestamp: Long, year: Int, month: Int, day: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
    }.timeInMillis

internal fun applyPickerTime(timestamp: Long, hour: Int, minute: Int, second: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, second)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

@Suppress("MissingPermission")
private suspend fun findCurrentLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    return suspendCancellableCoroutine { continuation ->
        var handle: LocationRequestHandle? = null
        handle = BestEffortLocation.request(context) { location ->
            if (continuation.isActive) continuation.resume(location)
        }
        continuation.invokeOnCancellation { handle?.cancel() }
    }
}

@Composable
private fun TrackEditorScreen(
    track: TrackPoint,
    initialLink: String? = null,
    favoriteOnly: Boolean = false,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: (Int) -> Unit,
    onSave: (String) -> Unit,
    onApplyPlace: (PlaceResolution) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable(track.id) { mutableStateOf(track.name) }
    var stagedProvider by rememberSaveable(track.id) { mutableStateOf<String?>(null) }
    var stagedAddress by rememberSaveable(track.id) { mutableStateOf<String?>(null) }
    var stagedLatitude by rememberSaveable(track.id) { mutableStateOf<Double?>(null) }
    var stagedLongitude by rememberSaveable(track.id) { mutableStateOf<Double?>(null) }
    var stagedPlaceId by rememberSaveable(track.id) { mutableStateOf<String?>(null) }
    var stagedOriginalUrl by rememberSaveable(track.id) { mutableStateOf<String?>(null) }
    var stagedCanonicalUrl by rememberSaveable(track.id) { mutableStateOf<String?>(null) }
    var stagedResolutionState by rememberSaveable(track.id) { mutableStateOf<String?>(null) }
    var message by rememberSaveable(track.id) { mutableStateOf("") }
    var messageIsError by rememberSaveable(track.id) { mutableStateOf(false) }
    var nearbyPlaces by remember(track.id) { mutableStateOf<List<NearbyPlaceCandidate>>(emptyList()) }
    var nearbyLoading by remember(track.id) { mutableStateOf(true) }
    var nearbyError by remember(track.id) { mutableStateOf<String?>(null) }
    var nearbyExpanded by rememberSaveable(track.id) { mutableStateOf(false) }
    var nearbyReload by rememberSaveable(track.id) { mutableIntStateOf(0) }
    var mapPickerOpen by remember(track.id) { mutableStateOf(false) }
    var linkToolsExpanded by rememberSaveable(track.id, initialLink) { mutableStateOf(!initialLink.isNullOrBlank()) }
    var linkText by rememberSaveable(track.id, initialLink) { mutableStateOf(initialLink ?: track.originalUrl.orEmpty()) }
    var parsingLink by remember { mutableStateOf(false) }

    fun stagedPlace(): PlaceResolution? {
        val provider = stagedProvider?.let { runCatching { PlaceProvider.valueOf(it) }.getOrNull() } ?: return null
        val state = stagedResolutionState?.let { runCatching { PlaceResolutionState.valueOf(it) }.getOrNull() }
            ?: PlaceResolutionState.PARTIAL
        return PlaceResolution(
            provider = provider,
            name = name.trim().ifBlank { null },
            address = stagedAddress,
            latitude = stagedLatitude,
            longitude = stagedLongitude,
            placeId = stagedPlaceId,
            originalUrl = stagedOriginalUrl,
            canonicalUrl = stagedCanonicalUrl,
            state = state,
        )
    }

    fun stagePlace(place: PlaceResolution) {
        stagedProvider = place.provider.name
        stagedAddress = place.address
        stagedLatitude = place.latitude
        stagedLongitude = place.longitude
        stagedPlaceId = place.placeId
        stagedOriginalUrl = place.originalUrl
        stagedCanonicalUrl = place.canonicalUrl
        stagedResolutionState = place.state.name
    }

    fun clearStagedPlace() {
        stagedProvider = null
        stagedAddress = null
        stagedLatitude = null
        stagedLongitude = null
        stagedPlaceId = null
        stagedOriginalUrl = null
        stagedCanonicalUrl = null
        stagedResolutionState = null
    }

    fun parseLink(content: String = linkText) {
        if (content.isBlank() || parsingLink) return
        parsingLink = true
        message = ""
        scope.launch {
            val result = runCatching { resolveSharedPlace(content) }
            parsingLink = false
            result.onSuccess { place ->
                stagePlace(place)
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
            val place = stagedPlace()
            if (place != null) onApplyPlace(place.copy(name = cleanName)) else onSave(cleanName)
        }
    }

    LaunchedEffect(track.id, nearbyReload) {
        if (favoriteOnly) {
            nearbyLoading = false
            return@LaunchedEffect
        }
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
        if (!favoriteOnly && !initialLink.isNullOrBlank()) parseLink(initialLink)
    }

    val mode = TrackEditorMode.EDIT_CHECK_IN
    val capabilities = mode.capabilities()
    val shortDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val shortTime = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    TrackEditorPage(
        title = mode.title(),
        primaryActionLabel = mode.primaryActionLabel().takeUnless { favoriteOnly },
        name = name,
        onNameChange = {
            name = it
            clearStagedPlace()
            message = ""
        },
        onBack = onBack,
        onSave = if (favoriteOnly) null else ::save,
        onDelete = onDelete.takeIf { capabilities.canDelete && !favoriteOnly },
        favoriteTier = track.favoriteTier,
        onFavorite = onFavorite,
        editingEnabled = !favoriteOnly,
        recordInfo = {
            Column(verticalArrangement = Arrangement.spacedBy(PassportSpacing.xs)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("记录信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    context.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("轨迹坐标", "${track.lat}, ${track.lon}"))
                    Toast.makeText(context, "坐标已复制", Toast.LENGTH_SHORT).show()
                }) { Icon(Icons.Default.ContentCopy, contentDescription = "复制坐标") }
            }
            TrackFieldPair(
                first = { modifier -> ReadOnlyTrackField("日期", shortDate.format(Date(track.time)), modifier) },
                second = { modifier -> ReadOnlyTrackField("时间", shortTime.format(Date(track.time)), modifier) },
            )
            TrackFieldPair(
                first = { modifier -> ReadOnlyTrackField("纬度", formatCoordinate(track.lat), modifier) },
                second = { modifier -> ReadOnlyTrackField("经度", formatCoordinate(track.lon), modifier) },
            )
            trackPrecisionLabel(track.accuracy)?.let { precision ->
                Text(
                    precision,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            }
        },
        nearbyPlaces = nearbyPlaces,
        nearbyLoading = nearbyLoading,
        nearbyExpanded = nearbyExpanded,
        nearbyEmptyMessage = nearbyError,
        nearbyIdleActionLabel = if (favoriteOnly) "不可修改" else "重试",
        onToggleNearby = { if (nearbyPlaces.isEmpty()) nearbyReload++ else nearbyExpanded = !nearbyExpanded },
        onSelectNearby = { candidate ->
            name = candidate.name
            stagePlace(
                PlaceResolution(
                    provider = PlaceProvider.SYSTEM,
                    name = candidate.name,
                    address = candidate.address,
                    latitude = candidate.latitude,
                    longitude = candidate.longitude,
                    state = PlaceResolutionState.PARTIAL,
                )
            )
            message = ""
        },
        mapEnabled = true,
        onOpenMap = { mapPickerOpen = true },
        linkExpanded = linkToolsExpanded,
        linkText = linkText,
        parsingLink = parsingLink,
        onToggleLink = { linkToolsExpanded = !linkToolsExpanded },
        onLinkTextChange = { linkText = it; message = "" },
        onParseLink = { parseLink() },
        feedback = message,
        feedbackIsError = messageIsError,
    )
    if (!favoriteOnly) ClipboardMapLinkPrompt(
        promptKey = "check-in-track-${track.id}",
        ignoredText = initialLink,
        onRecognize = { content ->
            linkText = content
            parseLink(content)
        },
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

@Suppress("DEPRECATION")
private suspend fun loadNearbyPlaces(context: Context, track: TrackPoint): List<NearbyPlaceCandidate> {
    val geocoder = Geocoder(context)
    return try {
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
        GeocoderDiagnostics.record(
            context = context,
            source = "nearby_place_candidates",
            latitude = track.lat,
            longitude = track.lon,
            requestId = track.requestId,
            addresses = addresses,
        )
        NearbyPlaceCandidates.prepare(addresses.flatMap(Address::toRawNearbyPlaces), track.lat, track.lon)
    } catch (error: Throwable) {
        GeocoderDiagnostics.record(
            context = context,
            source = "nearby_place_candidates",
            latitude = track.lat,
            longitude = track.lon,
            requestId = track.requestId,
            error = error,
        )
        throw error
    }
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
