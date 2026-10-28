package com.bugchecking.app

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.core.content.edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.security.MessageDigest
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val BACKUP_SCHEMA_VERSION = 3
private const val BACKUP_MIME = "application/zip"
private const val BACKUP_PREFIX = "BugChecking-"
private const val CHANGE_BACKUP_PREFIX = "${BACKUP_PREFIX}change-"
private const val LATEST_BACKUP_NAME = "${BACKUP_PREFIX}latest.bugchecking.zip"
private const val PREVIOUS_BACKUP_NAME = "${BACKUP_PREFIX}previous.bugchecking.zip"
private const val DAILY_BACKUP_PREFIX = "${BACKUP_PREFIX}daily-"
private const val MILESTONE_BACKUP_PREFIX = "${BACKUP_PREFIX}milestone-"
private const val PARTIAL_BACKUP_PREFIX = "${BACKUP_PREFIX}writing-"
private const val MANIFEST_ENTRY = "backup.json"
private const val AVATAR_ENTRY = "avatar.png"
private const val CHECKSUM_ENTRY = "checksum.sha256"
private const val MAX_MANIFEST_BYTES = 8 * 1024 * 1024
private const val MAX_AVATAR_BYTES = 32 * 1024 * 1024
internal const val BACKUP_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L
internal const val BACKUP_CHANGE_DEBOUNCE_MILLIS = 3_000L

enum class BackupKind(val fileTag: String, val displayName: String) {
    CHANGE("change", "旧变更快照"),
    LATEST("latest", "最新快照"),
    DAILY("daily", "每日快照"),
    MILESTONE("milestone", "关键节点快照"),
    MANUAL("manual", "手动备份"),
}

enum class BackupReason(val fileTag: String, val displayName: String) {
    DATA_CHANGE("data-change", "数据变化"),
    DAILY_SCHEDULE("daily", "每日保护"),
    MANUAL("manual", "手动创建"),
    BEFORE_RESTORE("before-restore", "导入备份前"),
    BEFORE_BULK_DELETE("before-bulk-delete", "批量删除前"),
    BEFORE_DATABASE_UPGRADE("before-database-upgrade", "数据库升级前"),
}

data class PassportBackupSnapshot(
    val profile: PassportProfile,
    val soundVolume: Int,
    val screenTimeoutSeconds: Int,
    val destinations: List<Destination>,
    val qrs: List<PassportQr>,
    val places: List<SavedPlace> = emptyList(),
    val tracks: List<TrackPoint>,
)

internal fun PassportBackupSnapshot.requireValid() {
    require(soundVolume in 0..100) { "备份中的提示音量无效" }
    require(screenTimeoutSeconds in 15..600) { "备份中的亮屏时长无效" }
    require(profile.nickname.toByteArray(Charsets.UTF_8).size <= 48) { "备份中的昵称过长" }
    require(profile.bio.toByteArray(Charsets.UTF_8).size <= 96) { "备份中的简介过长" }
    require(profile.socialIds.size <= 4 && profile.socialIds.all { it.toByteArray(Charsets.UTF_8).size <= 48 }) {
        "备份中的社交账号无效"
    }
    require(destinations.size <= 64) { "备份中的清单超过 64 项" }
    require(destinations.map(Destination::id).distinct().size == destinations.size) { "备份中的清单 ID 重复" }
    require(destinations.all { it.name.isNotBlank() }) { "备份中存在空白清单名称" }
    require(qrs.map(PassportQr::id).distinct().size == qrs.size) { "备份中的二维码 ID 重复" }
    require(qrs.count(PassportQr::starred) <= 3) { "备份中的星标二维码超过 3 个" }
    require(qrs.all { it.name.isNotBlank() && it.payload.isNotBlank() && it.payload.toByteArray(Charsets.UTF_8).size <= 192 }) {
        "备份中的二维码无效"
    }
    val qrIds = qrs.map(PassportQr::id).toSet()
    require(destinations.all { it.qrId == null || it.qrId in qrIds }) { "备份中的清单关联了不存在的二维码" }
    require(tracks.map(TrackPoint::id).distinct().size == tracks.size) { "备份中的轨迹 ID 重复" }
    require(tracks.map(TrackPoint::requestId).distinct().size == tracks.size) { "备份中的轨迹请求 ID 重复" }
    require(places.map(SavedPlace::id).distinct().size == places.size) { "备份中的场所 ID 重复" }
    require(places.all {
        it.name.isNotBlank() && it.favoriteTier in 0..3 &&
            it.latitude.isFinite() && it.latitude in -90.0..90.0 &&
            it.longitude.isFinite() && it.longitude in -180.0..180.0
    }) { "备份中的场所数据无效" }
    val placeIds = places.map(SavedPlace::id).toSet()
    require(tracks.all {
        it.name.isNotBlank() && it.lat.isFinite() && it.lat in -90.0..90.0 &&
            it.lon.isFinite() && it.lon in -180.0..180.0 && it.accuracy >= 0f && it.favoriteTier in 0..3 &&
            (it.savedPlaceId == null || it.savedPlaceId in placeIds)
    }) { "备份中的轨迹数据无效" }
}

data class BackupPreferences(
    val folderUri: Uri?,
    val folderLabel: String?,
    val automaticEnabled: Boolean,
    val lastSuccessAt: Long,
    val lastDailySuccessAt: Long,
    val lastBackedRevision: Long,
    val lastFileName: String?,
    val lastKind: BackupKind?,
    val lastReason: BackupReason?,
    val lastError: String?,
)

data class BackupResult(
    val fileName: String,
    val completedAt: Long,
    val kind: BackupKind,
    val reason: BackupReason,
    val protectedRevision: Long,
)

data class BackupInspection(
    val createdAt: Long,
    val destinations: Int,
    val qrs: Int,
    val places: Int,
    val tracks: Int,
    val hasAvatar: Boolean,
    val kind: BackupKind?,
    val reason: BackupReason?,
    val checksumVerified: Boolean,
)

internal fun isBackupOverdue(
    lastSuccessAt: Long,
    now: Long,
    intervalMillis: Long = BACKUP_INTERVAL_MILLIS,
): Boolean = lastSuccessAt <= 0L || now - lastSuccessAt >= intervalMillis

internal fun automaticBackupKind(
    currentRevision: Long,
    lastBackedRevision: Long,
    lastDailySuccessAt: Long,
    now: Long,
): BackupKind? = when {
    isBackupOverdue(lastDailySuccessAt, now) -> BackupKind.DAILY
    currentRevision != lastBackedRevision -> BackupKind.LATEST
    else -> null
}

internal fun defaultBackupReason(kind: BackupKind): BackupReason = when (kind) {
    BackupKind.DAILY -> BackupReason.DAILY_SCHEDULE
    BackupKind.MANUAL -> BackupReason.MANUAL
    BackupKind.MILESTONE -> BackupReason.BEFORE_RESTORE
    BackupKind.CHANGE, BackupKind.LATEST -> BackupReason.DATA_CHANGE
}

internal fun archiveChecksum(manifest: ByteArray, avatar: ByteArray?): String =
    MessageDigest.getInstance("SHA-256").run {
        update(manifest)
        avatar?.let(::update)
        digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

internal data class BackupFileRecord(val id: String, val name: String, val lastModified: Long)

internal fun backupsToPrune(
    files: List<BackupFileRecord>,
    prefix: String,
    retention: Int,
): List<BackupFileRecord> = files
    .filter { it.name.startsWith(prefix) && it.name.endsWith(".bugchecking.zip") }
    .sortedWith(compareByDescending<BackupFileRecord> { it.lastModified }.thenByDescending { it.name })
    .drop(retention)

object AppDataBackup {
    private const val PREFS = "app_data_backup"
    private const val KEY_FOLDER_URI = "folder_uri"
    private const val KEY_FOLDER_LABEL = "folder_label"
    private const val KEY_AUTOMATIC = "automatic"
    private const val KEY_LAST_SUCCESS = "last_success"
    private const val KEY_LAST_DAILY_SUCCESS = "last_daily_success"
    private const val KEY_LAST_BACKED_REVISION = "last_backed_revision"
    private const val KEY_LAST_FILE = "last_file"
    private const val KEY_LAST_KIND = "last_kind"
    private const val KEY_LAST_REASON = "last_reason"
    private const val KEY_LAST_ERROR = "last_error"
    private const val PERIODIC_WORK = "bugchecking-daily-backup"
    private const val CATCH_UP_WORK = "bugchecking-backup-catch-up"
    private const val DATA_CHANGE_WORK = "bugchecking-backup-data-change"
    private const val DAILY_BACKUP_RETENTION = 30
    private const val STALE_PARTIAL_MILLIS = 24L * 60L * 60L * 1000L
    private val backupMutex = Mutex()

    fun preferences(context: Context): BackupPreferences {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return BackupPreferences(
            folderUri = prefs.getString(KEY_FOLDER_URI, null)?.let(Uri::parse),
            folderLabel = prefs.getString(KEY_FOLDER_LABEL, null),
            automaticEnabled = prefs.getBoolean(KEY_AUTOMATIC, false),
            lastSuccessAt = prefs.getLong(KEY_LAST_SUCCESS, 0L),
            lastDailySuccessAt = prefs.getLong(KEY_LAST_DAILY_SUCCESS, 0L),
            lastBackedRevision = prefs.getLong(KEY_LAST_BACKED_REVISION, 0L),
            lastFileName = prefs.getString(KEY_LAST_FILE, null),
            lastKind = prefs.getString(KEY_LAST_KIND, null)?.let { value ->
                enumValues<BackupKind>().firstOrNull { it.name == value }
            },
            lastReason = prefs.getString(KEY_LAST_REASON, null)?.let { value ->
                enumValues<BackupReason>().firstOrNull { it.name == value }
            },
            lastError = prefs.getString(KEY_LAST_ERROR, null),
        )
    }

    fun setFolder(context: Context, uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
        val old = preferences(context).folderUri
        val label = queryDisplayName(context, uri) ?: "已选择的文件夹"
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_FOLDER_URI, uri.toString())
            putString(KEY_FOLDER_LABEL, label)
            putBoolean(KEY_AUTOMATIC, true)
            remove(KEY_LAST_ERROR)
        }
        if (old != null && old != uri) runCatching {
            context.contentResolver.releasePersistableUriPermission(old, flags)
        }
        syncSchedule(context)
        enqueueCatchUp(context)
    }

    fun setAutomaticEnabled(context: Context, enabled: Boolean) {
        require(!enabled || preferences(context).folderUri != null) { "请先选择备份文件夹" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_AUTOMATIC, enabled)
        }
        syncSchedule(context)
        if (enabled) enqueueCatchUp(context)
    }

    fun syncSchedule(context: Context) {
        val manager = WorkManager.getInstance(context)
        val preferences = preferences(context)
        if (preferences.automaticEnabled && preferences.folderUri != null) {
            val request = PeriodicWorkRequestBuilder<AutomaticBackupWorker>(
                24, TimeUnit.HOURS, 1, TimeUnit.HOURS,
            ).build()
            manager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        } else {
            manager.cancelUniqueWork(PERIODIC_WORK)
            manager.cancelUniqueWork(CATCH_UP_WORK)
            manager.cancelUniqueWork(DATA_CHANGE_WORK)
        }
    }

    fun enqueueDataChange(context: Context) {
        val preferences = preferences(context)
        if (!preferences.automaticEnabled || preferences.folderUri == null) return
        val request = OneTimeWorkRequestBuilder<AutomaticBackupWorker>()
            .setInitialDelay(BACKUP_CHANGE_DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            DATA_CHANGE_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun enqueueCatchUp(context: Context) {
        val preferences = preferences(context)
        if (!preferences.automaticEnabled || preferences.folderUri == null) return
        WorkManager.getInstance(context).enqueueUniqueWork(
            CATCH_UP_WORK,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<AutomaticBackupWorker>().build(),
        )
    }

    suspend fun cleanStalePartialBackups(context: Context) = withContext(Dispatchers.IO) {
        val folder = preferences(context).folderUri ?: return@withContext
        runCatching { cleanStalePartialBackups(context, folder, System.currentTimeMillis()) }
    }

    suspend fun backupNow(
        context: Context,
        kind: BackupKind,
        reason: BackupReason = defaultBackupReason(kind),
    ): BackupResult = backupMutex.withLock {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val preferences = preferences(context)
            val folder = preferences.folderUri ?: error("请先选择备份文件夹")
            try {
                val db = PassportDb(context.applicationContext)
                val capture = try { db.backupCapture() } finally { db.close() }
                val snapshot = capture.snapshot
                snapshot.requireValid()
                val avatar = snapshot.profile.avatarPath?.let(::File)?.takeIf(File::isFile)?.readBytes()
                if (avatar != null) require(avatar.size <= MAX_AVATAR_BYTES) { "头像文件过大，无法备份" }
                val manifest = snapshotToJson(snapshot, now, avatar != null, kind, reason)
                    .toString().toByteArray(Charsets.UTF_8)
                require(manifest.size <= MAX_MANIFEST_BYTES) { "备份数据过大" }
                val checksum = archiveChecksum(manifest, avatar)
                val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date(now))
                val requestedName = when (kind) {
                    BackupKind.LATEST -> LATEST_BACKUP_NAME
                    BackupKind.DAILY -> "$DAILY_BACKUP_PREFIX$timestamp.bugchecking.zip"
                    BackupKind.MILESTONE -> "$MILESTONE_BACKUP_PREFIX${reason.fileTag}-$timestamp.bugchecking.zip"
                    BackupKind.MANUAL -> "${BACKUP_PREFIX}manual-$timestamp.bugchecking.zip"
                    BackupKind.CHANGE -> "$CHANGE_BACKUP_PREFIX$timestamp.bugchecking.zip"
                }
                runCatching { cleanStalePartialBackups(context, folder, now) }
                val partial = writePartialArchive(context, folder, now, manifest, avatar, checksum)
                val fileName = if (kind == BackupKind.LATEST) {
                    rotateLatestBackup(context, folder, partial)
                    LATEST_BACKUP_NAME
                } else {
                    finalizeDocument(context, partial, requestedName)
                    requestedName
                }
                when (kind) {
                    BackupKind.DAILY -> {
                        runCatching { pruneBackups(context, folder, DAILY_BACKUP_PREFIX, DAILY_BACKUP_RETENTION) }
                        val latestPartial = writePartialArchive(context, folder, now, manifest, avatar, checksum)
                        rotateLatestBackup(context, folder, latestPartial)
                    }
                    BackupKind.CHANGE, BackupKind.LATEST, BackupKind.MILESTONE, BackupKind.MANUAL -> Unit
                }
                val protectsRevision = kind == BackupKind.LATEST || kind == BackupKind.DAILY
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
                    putLong(KEY_LAST_SUCCESS, now)
                    if (kind == BackupKind.DAILY) putLong(KEY_LAST_DAILY_SUCCESS, now)
                    if (protectsRevision) putLong(
                            KEY_LAST_BACKED_REVISION,
                            maxOf(preferences.lastBackedRevision, capture.revision),
                        )
                    putString(KEY_LAST_FILE, fileName)
                    putString(KEY_LAST_KIND, kind.name)
                    putString(KEY_LAST_REASON, reason.name)
                    remove(KEY_LAST_ERROR)
                }
                BackupResult(fileName, now, kind, reason, capture.revision)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
                    putString(KEY_LAST_ERROR, error.message ?: "备份失败")
                }
                throw error
            }
        }
    }

    private fun writePartialArchive(
        context: Context,
        folder: Uri,
        now: Long,
        manifest: ByteArray,
        avatar: ByteArray?,
        checksum: String,
    ): Uri {
        val partialName = "$PARTIAL_BACKUP_PREFIX$now-${UUID.randomUUID()}.partial"
        val partial = createDocument(context, folder, partialName, BACKUP_MIME)
        try {
            context.contentResolver.openOutputStream(partial, "w")?.use { raw ->
                ZipOutputStream(raw.buffered()).use { zip ->
                    zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                    zip.write(manifest)
                    zip.closeEntry()
                    if (avatar != null) {
                        zip.putNextEntry(ZipEntry(AVATAR_ENTRY))
                        zip.write(avatar)
                        zip.closeEntry()
                    }
                    zip.putNextEntry(ZipEntry(CHECKSUM_ENTRY))
                    zip.write(checksum.toByteArray(Charsets.US_ASCII))
                    zip.closeEntry()
                }
            } ?: error("无法写入备份文件")
            return partial
        } catch (error: Throwable) {
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, partial) }
            throw error
        }
    }

    private fun finalizeDocument(context: Context, partial: Uri, fileName: String): Uri =
        try {
            DocumentsContract.renameDocument(context.contentResolver, partial, fileName)
                ?: error("无法完成备份文件写入")
        } catch (error: Throwable) {
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, partial) }
            throw error
        }

    suspend fun inspect(context: Context, uri: Uri): BackupInspection = withContext(Dispatchers.IO) {
        val decoded = readArchive(context, uri)
        BackupInspection(
            createdAt = decoded.createdAt,
            destinations = decoded.snapshot.destinations.size,
            qrs = decoded.snapshot.qrs.size,
            places = decoded.snapshot.places.size,
            tracks = decoded.snapshot.tracks.size,
            hasAvatar = decoded.avatar != null,
            kind = decoded.kind,
            reason = decoded.reason,
            checksumVerified = decoded.checksumVerified,
        )
    }

    suspend fun restore(context: Context, uri: Uri) {
        if (preferences(context).folderUri != null) {
            backupNow(context, BackupKind.MILESTONE, BackupReason.BEFORE_RESTORE)
        }
        backupMutex.withLock { withContext(Dispatchers.IO) {
            val decoded = readArchive(context, uri)
            val oldAvatar = PassportDb(context.applicationContext).use { it.profile().avatarPath }
            var newAvatar: File? = null
            try {
                if (decoded.avatar != null) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(decoded.avatar, 0, decoded.avatar.size, bounds)
                    require(bounds.outWidth in 1..12_000 && bounds.outHeight in 1..12_000) {
                        "备份中的头像图片无效"
                    }
                    require(bounds.outWidth.toLong() * bounds.outHeight.toLong() <= 64_000_000L) {
                        "备份中的头像分辨率过大"
                    }
                    val directory = File(context.filesDir, "profile").apply { mkdirs() }
                    newAvatar = File(directory, "avatar_imported_${System.currentTimeMillis()}.png")
                    FileOutputStream(newAvatar).use { it.write(decoded.avatar) }
                }
                val snapshot = decoded.snapshot.copy(
                    profile = decoded.snapshot.profile.copy(avatarPath = newAvatar?.absolutePath),
                )
                PassportDb(context.applicationContext).use { it.restoreBackup(snapshot) }
                oldAvatar?.let(::File)?.takeIf { it.absolutePath != newAvatar?.absolutePath }?.delete()
            } catch (error: Throwable) {
                newAvatar?.delete()
                throw error
            }
        } }
    }

    private fun createDocument(context: Context, treeUri: Uri, name: String, mimeType: String): Uri {
        val parent = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        return DocumentsContract.createDocument(context.contentResolver, parent, mimeType, name)
            ?: error("无法在所选文件夹中创建备份")
    }

    private fun folderDocuments(context: Context, treeUri: Uri): List<BackupFileRecord> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        return context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(1) ?: continue
                    add(BackupFileRecord(cursor.getString(0), name, cursor.getLong(2)))
                }
            }
        }.orEmpty()
    }

    private fun documentUri(treeUri: Uri, record: BackupFileRecord): Uri =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, record.id)

    private fun rotateLatestBackup(context: Context, treeUri: Uri, partial: Uri) {
        val resolver = context.contentResolver
        val before = folderDocuments(context, treeUri)
        val latest = before.filter { it.name == LATEST_BACKUP_NAME }
            .maxWithOrNull(compareBy<BackupFileRecord> { it.lastModified }.thenBy { it.id })
        val previous = before.filter { it.name == PREVIOUS_BACKUP_NAME }
            .maxWithOrNull(compareBy<BackupFileRecord> { it.lastModified }.thenBy { it.id })

        try {
            if (latest != null) {
                if (previous != null) {
                    require(DocumentsContract.deleteDocument(resolver, documentUri(treeUri, previous))) {
                        "无法轮换上一份备份"
                    }
                }
                require(DocumentsContract.renameDocument(
                    resolver,
                    documentUri(treeUri, latest),
                    PREVIOUS_BACKUP_NAME,
                ) != null) {
                    "无法保留上一份备份"
                }
            }
            finalizeDocument(context, partial, LATEST_BACKUP_NAME)
        } catch (error: Throwable) {
            runCatching { DocumentsContract.deleteDocument(resolver, partial) }
            throw error
        }

        // The first layered snapshot migrates one legacy change file into the previous slot.
        // Only after latest + previous are protected are the remaining legacy change files removed.
        val afterLatest = folderDocuments(context, treeUri)
        val hasPrevious = afterLatest.any { it.name == PREVIOUS_BACKUP_NAME }
        val legacyChanges = afterLatest
            .filter { it.name.startsWith(CHANGE_BACKUP_PREFIX) && it.name.endsWith(".bugchecking.zip") }
            .sortedWith(compareByDescending<BackupFileRecord> { it.lastModified }.thenByDescending { it.name })
        val previousReady = hasPrevious || legacyChanges.firstOrNull()?.let { legacy ->
            DocumentsContract.renameDocument(
                resolver,
                documentUri(treeUri, legacy),
                PREVIOUS_BACKUP_NAME,
            ) != null
        } == true
        if (previousReady) {
            folderDocuments(context, treeUri)
                .filter { it.name.startsWith(CHANGE_BACKUP_PREFIX) && it.name.endsWith(".bugchecking.zip") }
                .forEach { legacy ->
                    runCatching { DocumentsContract.deleteDocument(resolver, documentUri(treeUri, legacy)) }
                }
        }
    }

    private fun pruneBackups(context: Context, treeUri: Uri, prefix: String, retention: Int) {
        backupsToPrune(folderDocuments(context, treeUri), prefix, retention)
            .forEach { document ->
            runCatching {
                DocumentsContract.deleteDocument(
                    context.contentResolver,
                    DocumentsContract.buildDocumentUriUsingTree(treeUri, document.id),
                )
            }
        }
    }

    private fun cleanStalePartialBackups(context: Context, treeUri: Uri, now: Long) {
        folderDocuments(context, treeUri)
            .filter { document ->
                if (!document.name.startsWith(PARTIAL_BACKUP_PREFIX) || !document.name.endsWith(".partial")) {
                    false
                } else {
                    val createdAt = document.name.removePrefix(PARTIAL_BACKUP_PREFIX).substringBefore('-').toLongOrNull()
                    val timestamp = document.lastModified.takeIf { it > 0L } ?: createdAt ?: now
                    now - timestamp >= STALE_PARTIAL_MILLIS
                }
            }
            .forEach { document ->
                runCatching {
                    DocumentsContract.deleteDocument(
                        context.contentResolver,
                        DocumentsContract.buildDocumentUriUsingTree(treeUri, document.id),
                    )
                }
            }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        val documentUri = runCatching {
            if (DocumentsContract.isTreeUri(uri)) {
                DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
            } else {
                uri
            }
        }.getOrNull()
        val queriedName = documentUri?.let { target ->
            runCatching {
                context.contentResolver.query(
                    target,
                    arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null,
                    null,
                    null,
                )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            }.getOrNull()
        }
        return queriedName?.takeIf(String::isNotBlank)
            ?: runCatching { DocumentsContract.getTreeDocumentId(uri).substringAfterLast('/') }
                .getOrNull()?.takeIf(String::isNotBlank)
            ?: uri.lastPathSegment?.substringAfterLast(':')?.takeIf(String::isNotBlank)
    }

    private data class DecodedBackup(
        val snapshot: PassportBackupSnapshot,
        val createdAt: Long,
        val avatar: ByteArray?,
        val kind: BackupKind?,
        val reason: BackupReason?,
        val checksumVerified: Boolean,
    )

    private fun readArchive(context: Context, uri: Uri): DecodedBackup {
        require(queryDisplayName(context, uri)?.endsWith(".partial") != true) {
            "临时备份尚未写入完成"
        }
        var manifest: ByteArray? = null
        var avatar: ByteArray? = null
        var checksum: ByteArray? = null
        context.contentResolver.openInputStream(uri)?.use { raw ->
            ZipInputStream(raw.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(!entry.isDirectory && entry.name in setOf(MANIFEST_ENTRY, AVATAR_ENTRY, CHECKSUM_ENTRY)) {
                        "备份包包含未知内容"
                    }
                    when (entry.name) {
                        MANIFEST_ENTRY -> {
                            require(manifest == null) { "备份包清单重复" }
                            manifest = zip.readLimited(MAX_MANIFEST_BYTES)
                        }
                        AVATAR_ENTRY -> {
                            require(avatar == null) { "备份包头像重复" }
                            avatar = zip.readLimited(MAX_AVATAR_BYTES)
                        }
                        CHECKSUM_ENTRY -> {
                            require(checksum == null) { "备份包校验值重复" }
                            checksum = zip.readLimited(128)
                        }
                    }
                    zip.closeEntry()
                }
            }
        } ?: error("无法读取备份文件")
        val manifestBytes = manifest ?: error("备份包缺少数据清单")
        val root = JSONObject(manifestBytes.toString(Charsets.UTF_8))
        require(root.getInt("schemaVersion") in 1..BACKUP_SCHEMA_VERSION) { "不支持这个备份版本" }
        val hasAvatar = root.optBoolean("hasAvatar", false)
        require(hasAvatar == (avatar != null)) { "备份包头像状态不一致" }
        val checksumVerified = checksum?.toString(Charsets.US_ASCII)?.trim()?.let { expected ->
            require(expected.matches(Regex("[0-9a-f]{64}"))) { "备份包校验值无效" }
            require(expected == archiveChecksum(manifestBytes, avatar)) { "备份包校验失败，文件可能已损坏" }
            true
        } ?: false
        val snapshot = snapshotFromJson(root)
        snapshot.requireValid()
        return DecodedBackup(
            snapshot = snapshot,
            createdAt = root.getLong("createdAt"),
            avatar = avatar,
            kind = root.optString("backupKind").takeIf(String::isNotBlank)?.let { value ->
                enumValues<BackupKind>().firstOrNull { it.name == value }
            },
            reason = root.optString("triggerReason").takeIf(String::isNotBlank)?.let { value ->
                enumValues<BackupReason>().firstOrNull { it.name == value }
            },
            checksumVerified = checksumVerified,
        )
    }
}

class AutomaticBackupWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val preferences = AppDataBackup.preferences(applicationContext)
        if (!preferences.automaticEnabled || preferences.folderUri == null) return Result.success()
        AppDataBackup.cleanStalePartialBackups(applicationContext)
        val currentRevision = PassportDb(applicationContext).use(PassportDb::dataRevision)
        val kind = automaticBackupKind(
            currentRevision = currentRevision,
            lastBackedRevision = preferences.lastBackedRevision,
            lastDailySuccessAt = preferences.lastDailySuccessAt,
            now = System.currentTimeMillis(),
        ) ?: return Result.success()
        return runCatching { AppDataBackup.backupNow(applicationContext, kind) }
            .fold(
                onSuccess = { Result.success() },
                onFailure = { if (runAttemptCount < 3) Result.retry() else Result.failure() },
            )
    }
}

private fun ZipInputStream.readLimited(limit: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        require(total <= limit) { "备份包内容过大" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private fun snapshotToJson(
    snapshot: PassportBackupSnapshot,
    createdAt: Long,
    hasAvatar: Boolean,
    kind: BackupKind,
    reason: BackupReason,
): JSONObject =
    JSONObject().apply {
        put("schemaVersion", BACKUP_SCHEMA_VERSION)
        put("createdAt", createdAt)
        put("packageName", "com.bugchecking.app")
        put("backupKind", kind.name)
        put("triggerReason", reason.name)
        put("hasAvatar", hasAvatar)
        put("summary", JSONObject().apply {
            put("destinations", snapshot.destinations.size)
            put("qrs", snapshot.qrs.size)
            put("places", snapshot.places.size)
            put("tracks", snapshot.tracks.size)
        })
        put("profile", JSONObject().apply {
            put("nickname", snapshot.profile.nickname)
            put("bio", snapshot.profile.bio)
            put("socialIds", JSONArray().apply { snapshot.profile.socialIds.forEach(::put) })
        })
        put("settings", JSONObject().apply {
            put("soundVolume", snapshot.soundVolume)
            put("screenTimeoutSeconds", snapshot.screenTimeoutSeconds)
        })
        put("destinations", JSONArray().apply { snapshot.destinations.forEach { destination ->
            put(JSONObject().apply {
                put("id", destination.id); put("name", destination.name); put("order", destination.order)
                put("done", destination.done); putNullable("qrId", destination.qrId)
            })
        } })
        put("qrs", JSONArray().apply { snapshot.qrs.forEach { qr ->
            put(JSONObject().apply {
                put("id", qr.id); put("name", qr.name); put("payload", qr.payload); put("starred", qr.starred)
                put("createdOrder", qr.createdOrder); putNullable("starredOrder", qr.starredOrder)
            })
        } })
        put("places", JSONArray().apply { snapshot.places.forEach { place ->
            put(JSONObject().apply {
                put("id", place.id); put("name", place.name); put("favoriteTier", place.favoriteTier)
                put("provider", place.provider.name); putNullable("providerPlaceId", place.providerPlaceId)
                putNullable("originalUrl", place.originalUrl); putNullable("canonicalUrl", place.canonicalUrl)
                put("lat", place.latitude); put("lon", place.longitude)
                put("createdAt", place.createdAt); put("updatedAt", place.updatedAt)
            })
        } })
        put("tracks", JSONArray().apply { snapshot.tracks.forEach { track ->
            put(JSONObject().apply {
                put("id", track.id); put("requestId", track.requestId); putNullable("destinationId", track.destinationId)
                put("name", track.name); put("lat", track.lat); put("lon", track.lon); put("time", track.time)
                put("accuracy", track.accuracy.toDouble()); put("provider", track.provider.name)
                putNullable("address", track.address); putNullable("placeId", track.placeId)
                putNullable("originalUrl", track.originalUrl); putNullable("canonicalUrl", track.canonicalUrl)
                put("resolutionState", track.resolutionState.name); put("entrySource", track.entrySource.name)
                put("favoriteTier", track.favoriteTier)
                putNullable("savedPlaceId", track.savedPlaceId)
            })
        } })
    }

private fun snapshotFromJson(root: JSONObject): PassportBackupSnapshot {
    val profileJson = root.getJSONObject("profile")
    val settings = root.getJSONObject("settings")
    return PassportBackupSnapshot(
        profile = PassportProfile(
            nickname = profileJson.getString("nickname"),
            bio = profileJson.getString("bio"),
            socialIds = profileJson.getJSONArray("socialIds").objects { getString(it) },
            avatarPath = null,
        ),
        soundVolume = settings.getInt("soundVolume"),
        screenTimeoutSeconds = settings.getInt("screenTimeoutSeconds"),
        destinations = root.getJSONArray("destinations").objects { index -> getJSONObject(index).let { value ->
            Destination(
                id = value.getString("id"), name = value.getString("name"), order = value.getInt("order"),
                done = value.getBoolean("done"), qrId = value.nullableString("qrId"),
            )
        } },
        qrs = root.getJSONArray("qrs").objects { index -> getJSONObject(index).let { value ->
            PassportQr(
                id = value.getString("id"), name = value.getString("name"), payload = value.getString("payload"),
                starred = value.getBoolean("starred"), createdOrder = value.getLong("createdOrder"),
                starredOrder = value.nullableLong("starredOrder"),
            )
        } },
        places = root.optJSONArray("places")?.objects { index -> getJSONObject(index).let { value ->
            SavedPlace(
                id = value.getString("id"), name = value.getString("name"),
                favoriteTier = value.getInt("favoriteTier"),
                provider = PlaceProvider.valueOf(value.getString("provider")),
                providerPlaceId = value.nullableString("providerPlaceId"),
                originalUrl = value.nullableString("originalUrl"), canonicalUrl = value.nullableString("canonicalUrl"),
                latitude = value.getDouble("lat"), longitude = value.getDouble("lon"),
                createdAt = value.getLong("createdAt"), updatedAt = value.getLong("updatedAt"),
            )
        } } ?: emptyList(),
        tracks = root.getJSONArray("tracks").objects { index -> getJSONObject(index).let { value ->
            TrackPoint(
                id = value.getLong("id"), requestId = value.getString("requestId"),
                destinationId = value.nullableString("destinationId"), name = value.getString("name"),
                lat = value.getDouble("lat"), lon = value.getDouble("lon"), time = value.getLong("time"),
                accuracy = value.getDouble("accuracy").toFloat(), provider = PlaceProvider.valueOf(value.getString("provider")),
                address = value.nullableString("address"), placeId = value.nullableString("placeId"),
                originalUrl = value.nullableString("originalUrl"), canonicalUrl = value.nullableString("canonicalUrl"),
                resolutionState = PlaceResolutionState.valueOf(value.getString("resolutionState")),
                entrySource = TrackEntrySource.valueOf(value.getString("entrySource")),
                favoriteTier = value.getInt("favoriteTier"),
                savedPlaceId = value.nullableString("savedPlaceId"),
            )
        } },
    )
}

private fun JSONObject.putNullable(key: String, value: Any?) {
    put(key, value ?: JSONObject.NULL)
}

private fun JSONObject.nullableString(key: String): String? = if (!has(key) || isNull(key)) null else getString(key)
private fun JSONObject.nullableLong(key: String): Long? = if (!has(key) || isNull(key)) null else getLong(key)

private inline fun <T> JSONArray.objects(block: JSONArray.(Int) -> T): List<T> =
    buildList { repeat(length()) { add(block(it)) } }
