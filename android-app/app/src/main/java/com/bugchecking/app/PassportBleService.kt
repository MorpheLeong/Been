package com.bugchecking.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.Geocoder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import java.util.UUID
import java.util.concurrent.Executors

/** User-started BLE companion service. Android may still stop it or the user may force-stop the app. */
class PassportBleService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private lateinit var db: PassportDb
    private var gatt: BluetoothGatt? = null
    private var scanning = false
    private var reconnects = 0
    private var revision = 0
    private var activeCheckIn = false
    private var activeRequestId = -1
    private var syncWhenConnected = false
    private var snapshotWriteActive = false
    private var snapshotPreparing = false
    private var syncGeneration = 0
    private var resyncAfterWrites = false
    private var stopping = false
    private var ready = false
    private var reconnectAllowed = false
    private var serviceDiscoveryStarted = false
    private var protocolDiscoveryRetries = 0
    private var negotiatedMtu = DEFAULT_ATT_MTU
    private data class PendingWrite(
        val characteristic: UUID,
        val value: ByteArray,
        val opcode: Int = -1,
        val attempts: Int = 0,
        val optionalTrackNameBatch: Int? = null,
    )
    private val writes = java.util.ArrayDeque<PendingWrite>()
    private var currentWrite: PendingWrite? = null
    private var pendingSnapshotFrames = 0
    private val commandAssembler = PassportCommandAssembler()
    private val syncExecutor = Executors.newSingleThreadExecutor()
    private val writeTimeout = Runnable {
        val timedOut = currentWrite ?: return@Runnable
        currentWrite = null
        Log.w(TAG, "GATT write timed out: ${timedOut.characteristic}")
        failWrite(timedOut)
    }
    private val scanTimeout = Runnable {
        if (!scanning) return@Runnable
        stopScanning()
        broadcast(STATE, "未找到 Passport，准备重试")
        scheduleReconnect()
    }
    private val reconnect = Runnable { if (!stopping && reconnectAllowed) connectOrScan() }
    private val connectionTimeout = Runnable {
        if (gatt == null || ready) return@Runnable
        closeConnection()
        broadcast(STATE, "连接超时，切换到后台重连")
        scheduleReconnect()
    }

    override fun onCreate() {
        super.onCreate()
        db = PassportDb(this)
        createNotificationChannel()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopping = true
                reconnectAllowed = false
                closeConnection()
                broadcast(STATE, "已断开 Passport")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START -> {
                stopping = false
                reconnectAllowed = true
                reconnects = 0
                syncWhenConnected = false
                startForeground(NOTIFICATION_ID, notification("正在连接 BugChecking"))
                if (ready) broadcast(STATE, "已安全连接，等待 Passport 指令") else connectOrScan()
            }
            ACTION_SYNC -> {
                stopping = false
                reconnectAllowed = true
                reconnects = 0
                syncWhenConnected = true
                startForeground(NOTIFICATION_ID, notification("正在连接 BugChecking"))
                if (ready) {
                    syncSnapshot()
                    syncTrackNames()
                } else connectOrScan()
            }
            ACTION_TRACK_NAME_SYNC -> {
                stopping = false
                reconnectAllowed = true
                reconnects = 0
                startForeground(NOTIFICATION_ID, notification("正在同步地点名称"))
                val trackId = intent.getLongExtra(EXTRA_TRACK_ID, -1L).takeIf { it > 0 }
                if (ready) syncTrackNames(trackId) else connectOrScan()
            }
            else -> {
                // START_STICKY restarts only a service the user previously started. We
                // connect exclusively to an already bonded Passport, so this cannot
                // create an unsolicited pairing prompt after process recreation. Use a
                // bounded direct connection here: some Android vendors leave
                // autoConnect GATT requests suspended indefinitely while the phone is
                // locked, which also prevents our retry loop from making progress.
                stopping = false
                reconnectAllowed = true
                startForeground(NOTIFICATION_ID, notification("正在后台恢复 Passport 连接"))
                connectOrScan()
            }
        }
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        stopping = true
        closeConnection()
        syncExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun has(permission: String) = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    private fun hasBluetoothPermissions() = Build.VERSION.SDK_INT < 31 ||
        (has(Manifest.permission.BLUETOOTH_SCAN) && has(Manifest.permission.BLUETOOTH_CONNECT))
    @Suppress("MissingPermission")
    private fun connectOrScan() {
        if (!hasBluetoothPermissions()) {
            broadcast(STATE, "需要附近设备权限"); return
        }
        val adapter = getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null || !adapter.isEnabled) { broadcast(STATE, "请开启蓝牙"); scheduleReconnect(); return }
        if (scanning || gatt != null) return
        val bondedPassport = try {
            adapter.bondedDevices.firstOrNull { it.name in DEVICE_NAMES }
        } catch (_: SecurityException) {
            broadcast(STATE, "蓝牙连接权限被撤销")
            return
        }
        if (bondedPassport != null) {
            connectDevice(bondedPassport, "正在连接 Passport")
            return
        }
        val scanner = adapter.bluetoothLeScanner ?: run { scheduleReconnect(); return }
        scanning = true
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()
        try {
            // Scan broadly for controller compatibility, but only trust the exact protocol UUID.
            scanner.startScan(null, settings, scanCallback)
            main.removeCallbacks(scanTimeout)
            main.postDelayed(scanTimeout, 15_000L)
            broadcast(STATE, "正在查找 Passport")
        }
        catch (_: SecurityException) { scanning = false; broadcast(STATE, "蓝牙扫描权限被撤销") }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult) {
            if (!scanning) return
            try {
                val advertisedService = result.scanRecord?.serviceUuids
                    ?.contains(android.os.ParcelUuid(PassportProtocol.service)) == true
                if (!advertisedService) return
                if (result.device.bondState != BluetoothDevice.BOND_BONDED) {
                    reconnectAllowed = false
                    stopScanning()
                    broadcast(STATE, "请先在系统蓝牙设置中配对 BugChecking")
                    return
                }
                connectDevice(result.device, "已发现 Passport，正在连接")
            } catch (_: SecurityException) {
                stopScanning()
                broadcast(STATE, "蓝牙连接权限被撤销")
                scheduleReconnect()
            }
        }
        override fun onScanFailed(errorCode: Int) {
            main.removeCallbacks(scanTimeout)
            scanning = false
            broadcast(STATE, "扫描失败 ($errorCode)")
            scheduleReconnect()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            main.removeCallbacks(connectionTimeout)
            Log.i(TAG, "GATT state status=$status state=$newState device=${g.device.address}")
            if (!hasBluetoothPermissions()) {
                if (gatt === g) gatt = null
                broadcast(STATE, "蓝牙连接权限被撤销")
                return
            }
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED) {
                try {
                    ready = false
                    serviceDiscoveryStarted = false
                    protocolDiscoveryRetries = 0
                    when (g.device.bondState) {
                        BluetoothDevice.BOND_BONDED -> {
                            broadcast(STATE, "已连接，正在读取设备服务")
                            requestPreferredMtu(g)
                            main.postDelayed({ discoverServices(g) }, 500L)
                        }
                        else -> {
                            reconnectAllowed = false
                            closeConnection()
                            broadcast(STATE, "请先在系统蓝牙设置中配对 BugChecking")
                        }
                    }
                }
                catch (_: SecurityException) { broadcast(STATE, "蓝牙连接权限被撤销"); closeConnection() }
            } else {
                if (gatt !== g) {
                    try { g.close() }
                    catch (_: SecurityException) { }
                    return
                }
                Log.w(TAG, "GATT disconnected status=$status state=$newState")
                ready = false; commandAssembler.reset()
                serviceDiscoveryStarted = false
                gatt = null
                try { g.close() }
                catch (_: SecurityException) { broadcast(STATE, "蓝牙连接权限被撤销") }
                if (reconnectAllowed) {
                    broadcast(STATE, "Passport 已断开，准备重连")
                    scheduleReconnect()
                } else {
                    broadcast(STATE, "Passport 已断开")
                }
            }
        }
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            main.removeCallbacks(connectionTimeout)
            serviceDiscoveryStarted = false
            if (!hasBluetoothPermissions()) { broadcast(STATE, "蓝牙连接权限被撤销"); closeConnection(); return }
            if (status != BluetoothGatt.GATT_SUCCESS) { broadcast(STATE, "读取设备服务失败"); closeConnection(); scheduleReconnect(); return }
            logGattTable(g)
            val service = g.getService(PassportProtocol.service)
            val characteristic = service?.getCharacteristic(PassportProtocol.command)
            val result = service?.getCharacteristic(PassportProtocol.result)
            val snapshot = service?.getCharacteristic(PassportProtocol.snapshot)
            if (characteristic == null || result == null || snapshot == null) {
                val missing = buildList {
                    if (service == null) add("服务")
                    if (characteristic == null) add("指令")
                    if (result == null) add("结果")
                    if (snapshot == null) add("同步")
                }.joinToString("、")
                if (protocolDiscoveryRetries++ == 0) {
                    Log.w(TAG, "Incomplete Passport GATT table; retrying discovery, missing=$missing")
                    main.postDelayed({ discoverServices(g) }, 1_000L)
                    return
                }
                reconnectAllowed = false
                closeConnection()
                broadcast(STATE, "设备服务缺少${missing}通道，请重启 Passport 后点连接重试")
                return
            }
            main.postDelayed({ configureNotifications(g) }, 300L)
        }
        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            if (gatt !== g) return
            negotiatedMtu = if (status == BluetoothGatt.GATT_SUCCESS)
                mtu.coerceIn(DEFAULT_ATT_MTU, PREFERRED_ATT_MTU) else DEFAULT_ATT_MTU
            Log.i(TAG, "ATT MTU status=$status negotiated=$negotiatedMtu")
        }
        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (descriptor.uuid == CCC_UUID) {
                main.removeCallbacks(connectionTimeout)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    ready = true
                    reconnects = 0
                    broadcast(STATE, "已安全连接，等待 Passport 指令")
                    if (syncWhenConnected) syncSnapshot() else syncClock()
                    syncTrackNames()
                } else {
                    reconnectAllowed = false
                    val state = if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION ||
                        status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION)
                        "安全密钥失效，请在系统蓝牙中取消配对后点连接重试" else "订阅指令失败，请点连接重试"
                    closeConnection()
                    broadcast(STATE, state)
                }
            }
        }
        override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val completed = currentWrite
            if (completed == null || completed.characteristic != characteristic.uuid) {
                Log.w(TAG, "Unexpected GATT write callback: ${characteristic.uuid}")
                return
            }
            main.removeCallbacks(writeTimeout)
            currentWrite = null
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "GATT write failed: $status")
                if (retryWrite(g, completed)) return
                failWrite(completed)
                return
            }
            if (completed.characteristic == PassportProtocol.snapshot && snapshotWriteActive) {
                pendingSnapshotFrames--
                if (pendingSnapshotFrames == 0) {
                    snapshotWriteActive = false
                    syncWhenConnected = false
                    broadcast(STATE, "清单、二维码、头像和设备设置已同步")
                }
            } else if (completed.characteristic == PassportProtocol.snapshot &&
                completed.opcode == PassportProtocol.TIME_SYNC) {
                broadcast(STATE, "已安全连接，设备时间已同步")
            } else if (completed.opcode == PassportProtocol.TRACK_NAMES_COMMIT) {
                broadcast(STATE, "地点名称已同步到 Passport")
            } else if (completed.characteristic == PassportProtocol.result) {
                broadcast(STATE, "定位结果已回传 Passport")
            }
            pumpWrites(g)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            handleCommand(characteristic.value ?: return)
        }
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            handleCommand(value)
        }
    }

    @Suppress("MissingPermission")
    private fun configureNotifications(g: BluetoothGatt) {
        if (gatt !== g || ready) return
        val characteristic = g.getService(PassportProtocol.service)
            ?.getCharacteristic(PassportProtocol.command) ?: return
        try {
            if (!g.setCharacteristicNotification(characteristic, true)) {
                reconnectAllowed = false
                broadcast(STATE, "无法订阅设备指令，请重启 Passport 后点连接重试"); closeConnection(); return
            }
        } catch (_: SecurityException) {
            broadcast(STATE, "蓝牙连接权限被撤销"); closeConnection(); return
        }
        val ccc = characteristic.getDescriptor(CCC_UUID)
        val subscription = if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0)
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        if (ccc == null || !writeDescriptor(g, ccc, subscription)) {
            reconnectAllowed = false
            broadcast(STATE, if (ccc == null) "设备服务缺少通知通道，请更新固件" else "订阅指令失败，请点连接重试")
            closeConnection()
        } else main.postDelayed(connectionTimeout, DISCOVERY_TIMEOUT_MS)
    }

    private fun handleCommand(frame: ByteArray) {
        Log.w(TAG, "command frame received bytes=${frame.size}")
        val complete = commandAssembler.accept(frame, android.os.SystemClock.elapsedRealtime()) ?: return
        val parsed = PassportProtocol.parseCommand(complete) ?: return
        Log.w(TAG, "command assembled request=${parsed.request} opcode=${parsed.opcode} destination=${parsed.destinationId}")
        broadcast(STATE, when (parsed.opcode) {
            PassportProtocol.CHECK_IN -> "收到清单打卡请求，正在获取位置"
            PassportProtocol.UNCHECK_IN -> "收到取消打卡请求"
            else -> "收到轨迹记录请求，正在获取位置"
        })
        if (activeCheckIn) {
            if (activeRequestId != parsed.request) sendResult(parsed.request, false, 4)
            return
        }
        if (!PassportProtocol.commandRecordsTrack(parsed.opcode)) {
            val destination = db.destinations().firstOrNull { it.id == parsed.destinationId }
            if (destination == null || !destination.done) {
                sendResult(parsed.request, false, 5); return
            }
            db.setDestinationDone(parsed.destinationId, false)
            sendResult(parsed.request, true)
            broadcast(STATE, "已取消打卡，轨迹记录保留", dataChanged = true)
            return
        }
        val requestId = "device-" + parsed.request + "-" + parsed.destinationId
        val existingTrack = db.trackByRequestId(requestId)
        if (existingTrack != null) {
            if (parsed.destinationId.isNotEmpty()) {
                db.markDestinationDone(parsed.destinationId)
            }
            sendResult(parsed.request, true, timestampMillis = existingTrack.time)
            broadcast(STATE, "轨迹已存在，结果已重新回传 Passport", dataChanged = true)
            return
        }
        val destination = if (parsed.destinationId.isEmpty()) null else
            db.destinations().firstOrNull { it.id == parsed.destinationId && !it.done }
        if (parsed.destinationId.isNotEmpty() && destination == null) {
            sendResult(parsed.request, false, 5); return
        }
        val request = if (destination == null) parsed else parsed.copy(name = destination.name)
        activeCheckIn = true
        activeRequestId = request.request
        if (!has(Manifest.permission.ACCESS_FINE_LOCATION) && !has(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            finishCheckIn(request, null, 1); return
        }
        getCurrentLocation { location -> finishCheckIn(request, location, if (location == null) 2 else 0) }
    }

    private fun finishCheckIn(request: PassportProtocol.CheckIn, location: Location?, error: Int) {
        var saved = false
        var recordedAt = System.currentTimeMillis()
        if (location != null) {
            val requestId = "device-" + request.request + "-" + request.destinationId
            recordedAt = System.currentTimeMillis()
            Log.w(TAG, "location result request=${request.request} provider=${location.provider} " +
                "lat=${location.latitude} lon=${location.longitude} accuracy_m=${location.accuracy} " +
                "location_time_ms=${location.time} saved_at_ms=$recordedAt")
            saved = db.hasTrack(requestId) || db.saveTrackOnce(
                requestId, request.destinationId, request.name, location.latitude, location.longitude,
                recordedAt, location.accuracy
            )
            if (saved && Geocoder.isPresent()) {
                Thread {
                    runCatching {
                        val addresses = Geocoder(this).getFromLocation(location.latitude, location.longitude, 10).orEmpty()
                        GeocoderDiagnostics.record(
                            context = this,
                            source = "track_saved_auto_name",
                            latitude = location.latitude,
                            longitude = location.longitude,
                            requestId = requestId,
                            addresses = addresses,
                        )
                        val best = addresses.bestGeocoderLabel()
                        if (best != null) {
                            db.enrichTrackFromSystem(
                                requestId,
                                best.candidate.text,
                                best.address.getAddressLine(0),
                            )
                            val updated = db.trackByRequestId(requestId)
                            main.post { if (updated != null) syncTrackNames(updated.id) }
                            broadcast(STATE, "地点信息已更新", dataChanged = true)
                        }
                    }.onFailure { error ->
                        GeocoderDiagnostics.record(
                            context = this,
                            source = "track_saved_auto_name",
                            latitude = location.latitude,
                            longitude = location.longitude,
                            requestId = requestId,
                            error = error,
                        )
                        Log.w(TAG, "reverse geocoder failed for request=$requestId", error)
                    }
                }.start()
            }
        } else {
            Log.w(TAG, "location unavailable request=${request.request} error=$error")
        }
        if (saved) {
            if (request.destinationId.isNotEmpty()) db.markDestinationDone(request.destinationId)
            Log.w(TAG, "track saved request=${request.request} destination=${request.destinationId}")
            broadcast(STATE, "轨迹已保存，正在回传 Passport", dataChanged = true)
            sendResult(request.request, true, timestampMillis = recordedAt)
        } else {
            sendResult(request.request, false, if (location == null) error else 3)
            broadcast(STATE, if (location == null) "定位失败，Passport 清单未推进" else "记录保存失败，Passport 清单未推进")
        }
        activeCheckIn = false; activeRequestId = -1
    }

    @Suppress("MissingPermission")
    private fun getCurrentLocation(callback: (Location?) -> Unit) {
        BestEffortLocation.request(this, callback = callback)
    }

    private fun sendResult(
        request: Int,
        success: Boolean,
        code: Int = 0,
        timestampMillis: Long = System.currentTimeMillis(),
    ) {
        val g = gatt ?: run {
            broadcast(STATE, "连接已断开，无法发送打卡结果")
            return
        }
        if (g.getService(PassportProtocol.service)?.getCharacteristic(PassportProtocol.result) == null) {
            broadcast(STATE, "设备固件缺少结果通道")
            return
        }
        val pending = PendingWrite(
            PassportProtocol.result,
            PassportProtocol.checkInResult(
                request, success, code, timestampMillis = timestampMillis,
            ),
        )
        writes.addFirst(pending)
        pumpWrites(g)
    }
    private fun syncSnapshot() {
        val g = gatt ?: return
        if (snapshotWriteActive || snapshotPreparing) { resyncAfterWrites = true; return }
        if (g.getService(PassportProtocol.service)?.getCharacteristic(PassportProtocol.snapshot) == null) return
        val chunkPayload = (negotiatedMtu - SNAPSHOT_FRAME_OVERHEAD)
            .coerceIn(LEGACY_CHUNK_PAYLOAD, MAX_CHUNK_PAYLOAD)
        val avatarChunkPayload = (negotiatedMtu - AVATAR_FRAME_OVERHEAD)
            .coerceIn(LEGACY_AVATAR_CHUNK_PAYLOAD, MAX_AVATAR_CHUNK_PAYLOAD)
        val generation = syncGeneration
        snapshotPreparing = true
        broadcast(STATE, "正在准备清单和头像")
        syncExecutor.execute {
            val prepared = runCatching {
                val profile = db.profile()
                val snapshot = PassportProtocol.snapshotPayload(db)
                val avatar = profile.avatarPath?.let { path ->
                    AvatarImage.deviceRgb565(path) ?: error("头像原图无法读取")
                }
                Triple(snapshot, avatar, db.screenTimeoutSeconds())
            }
            main.post {
                if (generation != syncGeneration || gatt !== g || !ready) return@post
                snapshotPreparing = false
                val data = prepared.getOrElse {
                    broadcast(STATE, "头像或清单读取失败，请重新选择头像后重试")
                    return@post
                }
                val frames = runCatching {
                    PassportProtocol.snapshotFrames(++revision, data.first, chunkPayload) +
                        PassportProtocol.avatarFrames(data.second, avatarChunkPayload) +
                        PassportProtocol.screenTimeoutFrame(data.third) + PassportProtocol.timeSyncFrame()
                }.getOrElse {
                    broadcast(STATE, "同步数据超出设备协议限制")
                    return@post
                }
                pendingSnapshotFrames = frames.size
                snapshotWriteActive = true
                frames.forEach { frame ->
                    writes.addLast(PendingWrite(
                        PassportProtocol.snapshot,
                        frame,
                        opcode = frame[1].toInt() and 0xff,
                    ))
                }
                pumpWrites(g)
            }
        }
    }

    private fun syncTrackNames(trackId: Long? = null) {
        val g = gatt ?: return
        if (!ready || g.getService(PassportProtocol.service)
                ?.getCharacteristic(PassportProtocol.snapshot) == null) return
        val generation = syncGeneration
        val batchRevision = ++revision
        val chunkPayload = (negotiatedMtu - SNAPSHOT_FRAME_OVERHEAD)
            .coerceIn(LEGACY_CHUNK_PAYLOAD, MAX_CHUNK_PAYLOAD)
        syncExecutor.execute {
            val tracks = if (trackId == null) db.deviceTracksForNameSync()
                else listOfNotNull(db.track(trackId))
            val frames = runCatching {
                PassportProtocol.trackNameFrames(batchRevision, tracks, chunkPayload)
            }.getOrElse {
                Log.w(TAG, "Could not prepare track-name batch", it)
                emptyList()
            }
            main.post {
                if (generation != syncGeneration || gatt !== g || !ready || frames.isEmpty())
                    return@post
                frames.forEach { frame ->
                    writes.addLast(PendingWrite(
                        PassportProtocol.snapshot,
                        frame,
                        opcode = frame[1].toInt() and 0xff,
                        optionalTrackNameBatch = batchRevision,
                    ))
                }
                pumpWrites(g)
            }
        }
    }

    private fun syncClock() {
        val g = gatt ?: return
        if (g.getService(PassportProtocol.service)?.getCharacteristic(PassportProtocol.snapshot) == null) {
            broadcast(STATE, "设备固件缺少时间同步通道")
            return
        }
        writes.addLast(PendingWrite(
            PassportProtocol.snapshot,
            PassportProtocol.timeSyncFrame(),
            opcode = PassportProtocol.TIME_SYNC,
        ))
        pumpWrites(g)
    }

    private fun pumpWrites(g: BluetoothGatt) {
        if (gatt !== g || currentWrite != null) return
        val next = writes.pollFirst()
        if (next == null) {
            if (resyncAfterWrites) {
                resyncAfterWrites = false
                syncSnapshot()
            }
            return
        }
        val characteristic = g.getService(PassportProtocol.service)
            ?.getCharacteristic(next.characteristic)
        if (characteristic == null || !writeCharacteristic(
                g, characteristic, next.value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )) {
            if (retryWrite(g, next)) return
            failWrite(next)
            return
        }
        currentWrite = next
        main.removeCallbacks(writeTimeout)
        main.postDelayed(writeTimeout, WRITE_TIMEOUT_MS)
    }

    private fun retryWrite(g: BluetoothGatt, write: PendingWrite): Boolean {
        if (gatt !== g || write.attempts >= MAX_WRITE_RETRIES) return false
        writes.addFirst(write.copy(attempts = write.attempts + 1))
        main.postDelayed({ pumpWrites(g) }, WRITE_RETRY_DELAY_MS)
        return true
    }

    private fun failWrite(write: PendingWrite) {
        if (write.optionalTrackNameBatch != null) {
            val failedBatch = write.optionalTrackNameBatch
            writes.removeAll { it.optionalTrackNameBatch == failedBatch }
            Log.w(TAG, "Track-name batch $failedBatch was rejected; keeping phone data for retry")
            broadcast(STATE, "设备固件暂未接收地点名称，稍后将重试")
            gatt?.let(::pumpWrites)
            return
        }
        writes.clear()
        currentWrite = null
        main.removeCallbacks(writeTimeout)
        snapshotWriteActive = false
        snapshotPreparing = false
        pendingSnapshotFrames = 0
        val avatarOpcode = write.opcode in PassportProtocol.AVATAR_BEGIN..PassportProtocol.AVATAR_CLEAR
        broadcast(STATE, if (write.characteristic == PassportProtocol.result)
            "打卡结果发送失败，请在设备上重试" else if (write.opcode == PassportProtocol.SCREEN_TIMEOUT)
            "清单已同步，亮屏设置失败，请更新固件后重试" else if (write.opcode == PassportProtocol.TIME_SYNC)
            "设备时间同步失败，请断开后重连" else if (avatarOpcode)
            "头像同步失败，请确认设备固件已更新后重试" else "清单同步写入失败")
    }

    @Suppress("MissingPermission", "DEPRECATION")
    private fun writeDescriptor(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, value: ByteArray): Boolean {
        if (!hasBluetoothPermissions()) { broadcast(STATE, "蓝牙连接权限被撤销"); return false }
        @Suppress("DEPRECATION")
        return try {
            if (Build.VERSION.SDK_INT >= 33) g.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
            else { descriptor.value = value; g.writeDescriptor(descriptor) }
        } catch (_: SecurityException) { broadcast(STATE, "蓝牙连接权限被撤销"); false }
    }
    @Suppress("MissingPermission", "DEPRECATION")
    private fun writeCharacteristic(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray, type: Int): Boolean {
        if (!hasBluetoothPermissions()) { broadcast(STATE, "蓝牙写入权限被撤销"); return false }
        try {
            return if (Build.VERSION.SDK_INT >= 33) {
                g.writeCharacteristic(c, value, type) == BluetoothStatusCodes.SUCCESS
            } else {
                c.writeType = type
                c.value = value
                g.writeCharacteristic(c)
            }
        } catch (_: SecurityException) { broadcast(STATE, "蓝牙写入权限被撤销"); return false }
    }

    @Suppress("MissingPermission")
    private fun discoverServices(g: BluetoothGatt) {
        if (gatt !== g || serviceDiscoveryStarted || ready) return
        serviceDiscoveryStarted = true
        broadcast(STATE, "正在读取 Passport 服务")
        try {
            if (!g.discoverServices()) {
                serviceDiscoveryStarted = false
                reconnectAllowed = false
                closeConnection()
                broadcast(STATE, "无法读取设备服务，请点连接重试")
                return
            }
            main.removeCallbacks(connectionTimeout)
            main.postDelayed(connectionTimeout, DISCOVERY_TIMEOUT_MS)
        } catch (_: SecurityException) {
            serviceDiscoveryStarted = false
            reconnectAllowed = false
            closeConnection()
            broadcast(STATE, "蓝牙连接权限被撤销")
        }
    }

    @Suppress("MissingPermission")
    private fun requestPreferredMtu(g: BluetoothGatt) {
        if (gatt !== g || !hasBluetoothPermissions()) return
        negotiatedMtu = DEFAULT_ATT_MTU
        try {
            if (!g.requestMtu(PREFERRED_ATT_MTU)) Log.w(TAG, "ATT MTU request was not started")
        } catch (_: SecurityException) {
            broadcast(STATE, "蓝牙连接权限被撤销")
        }
    }

    private fun logGattTable(g: BluetoothGatt) {
        val table = g.services.joinToString(separator = "\n") { service ->
            val characteristics = service.characteristics.joinToString { characteristic ->
                val descriptors = characteristic.descriptors.joinToString { it.uuid.toString() }
                "${characteristic.uuid}[properties=0x${characteristic.properties.toString(16)}, descriptors=$descriptors]"
            }
            "${service.uuid}: $characteristics"
        }
        Log.i(TAG, "Discovered GATT table:\n$table")
    }

    private fun scheduleReconnect() {
        if (!reconnectAllowed || stopping || gatt != null || scanning) return
        // Issue the first reconnect before vendor background-freeze policies can
        // suspend this process. Later failures still back off to avoid a busy loop.
        val delay = if (reconnects == 0) 0L
            else (1_000L shl (reconnects - 1).coerceAtMost(6)).coerceAtMost(60_000L)
        reconnects = (reconnects + 1).coerceAtMost(7)
        main.removeCallbacks(reconnect)
        main.postDelayed(reconnect, delay)
    }
    @Suppress("MissingPermission")
    private fun connectDevice(device: BluetoothDevice, state: String) {
        stopScanning()
        broadcast(STATE, state)
        try {
            val connection = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            if (connection == null) {
                broadcast(STATE, "无法连接 Passport，稍后重试")
                scheduleReconnect()
                return
            }
            gatt = connection
            main.removeCallbacks(connectionTimeout)
            main.postDelayed(connectionTimeout, DISCOVERY_TIMEOUT_MS)
        } catch (_: SecurityException) {
            broadcast(STATE, "蓝牙连接权限被撤销")
            scheduleReconnect()
        }
    }
    @Suppress("MissingPermission")
    private fun stopScanning() {
        main.removeCallbacks(scanTimeout)
        val wasScanning = scanning
        scanning = false
        if (wasScanning && hasBluetoothPermissions()) {
            try { getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
            catch (_: SecurityException) { }
        }
    }
    @Suppress("MissingPermission")
    private fun closeConnection() {
        main.removeCallbacks(reconnect)
        main.removeCallbacks(connectionTimeout)
        stopScanning()
        ready = false
        serviceDiscoveryStarted = false
        protocolDiscoveryRetries = 0
        negotiatedMtu = DEFAULT_ATT_MTU
        commandAssembler.reset()
        writes.clear()
        currentWrite = null
        main.removeCallbacks(writeTimeout)
        snapshotWriteActive = false
        snapshotPreparing = false
        syncGeneration++
        resyncAfterWrites = false
        pendingSnapshotFrames = 0
        activeCheckIn = false
        activeRequestId = -1
        val connection = gatt
        gatt = null
        if (connection != null && hasBluetoothPermissions()) {
            try { connection.disconnect(); connection.close() }
            catch (_: SecurityException) { }
        }
    }
    private fun broadcast(action: String, state: String, dataChanged: Boolean = false) {
        sendBroadcast(Intent(action).setPackage(packageName).putExtra(EXTRA_STATE, state)
            .putExtra(EXTRA_CONNECTED, ready).putExtra(EXTRA_DATA_CHANGED, dataChanged))
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(state))
    }
    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "BugChecking 连接", NotificationManager.IMPORTANCE_LOW)
        )
    }
    private fun notification(text: String): Notification {
        val stop = Intent(this, PassportBleService::class.java).setAction(ACTION_STOP)
        val pending = android.app.PendingIntent.getService(this, 1, stop, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL_ID).setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("BugChecking").setContentText(text)
            .addAction(Notification.Action.Builder(null, "断开", pending).build())
            .setOngoing(true).build()
    }
    companion object {
        const val ACTION_START = "com.bugchecking.app.START_BLE"
        const val ACTION_STOP = "com.bugchecking.app.STOP_BLE"
        const val ACTION_SYNC = "com.bugchecking.app.SYNC"
        const val ACTION_TRACK_NAME_SYNC = "com.bugchecking.app.SYNC_TRACK_NAME"
        const val STATE = "com.bugchecking.app.BLE_STATE"
        const val EXTRA_STATE = "state"
        const val EXTRA_CONNECTED = "connected"
        const val EXTRA_DATA_CHANGED = "data_changed"
        const val EXTRA_TRACK_ID = "track_id"
        private const val TAG = "PassportBleService"
        private const val CHANNEL_ID = "passport_ble"
        private const val NOTIFICATION_ID = 40
        private val DEVICE_NAMES = setOf("BugChecking", "AI Passport")
        private const val DISCOVERY_TIMEOUT_MS = 15_000L
        private const val DEFAULT_ATT_MTU = 23
        private const val PREFERRED_ATT_MTU = 247
        private const val SNAPSHOT_FRAME_OVERHEAD = 9
        private const val LEGACY_CHUNK_PAYLOAD = 14
        private const val MAX_CHUNK_PAYLOAD = 238
        private const val AVATAR_FRAME_OVERHEAD = 7
        private const val LEGACY_AVATAR_CHUNK_PAYLOAD = 16
        private const val MAX_AVATAR_CHUNK_PAYLOAD = 240
        private const val MAX_WRITE_RETRIES = 2
        private const val WRITE_RETRY_DELAY_MS = 120L
        private const val WRITE_TIMEOUT_MS = 5_000L
        private val CCC_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
