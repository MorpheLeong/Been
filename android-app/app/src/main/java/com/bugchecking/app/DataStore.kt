package com.bugchecking.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import java.util.UUID

private const val TRACK_COLUMNS =
    "id,request_id,destination_id,name,lat,lon,time,accuracy,provider,address,place_id,original_url,canonical_url,resolution_state,entry_source,favorite_tier,saved_place_id"
private val BACKUP_TRACKED_TABLES = listOf("tracks", "places", "destinations", "qrs", "profile", "device_settings")

internal fun backupTrackingTriggerStatements(): List<String> = BACKUP_TRACKED_TABLES.flatMap { table ->
    listOf("insert", "update", "delete").map { action ->
        """CREATE TRIGGER IF NOT EXISTS backup_revision_${table}_$action
            AFTER ${action.uppercase()} ON $table
            WHEN (SELECT tracking_suspended FROM backup_state WHERE id=1)=0
            BEGIN
                UPDATE backup_state
                SET revision=revision+1, changed_at=CAST(strftime('%s','now') AS INTEGER)*1000
                WHERE id=1;
            END""".trimIndent()
    }
}

data class TrackPoint(
    val id: Long,
    val requestId: String,
    val destinationId: String?,
    val name: String,
    val lat: Double,
    val lon: Double,
    val time: Long,
    val accuracy: Float,
    val provider: PlaceProvider = PlaceProvider.MANUAL,
    val address: String? = null,
    val placeId: String? = null,
    val originalUrl: String? = null,
    val canonicalUrl: String? = null,
    val resolutionState: PlaceResolutionState = PlaceResolutionState.MANUAL_CONFIRMATION,
    val entrySource: TrackEntrySource = TrackEntrySource.CHECK_IN,
    val favoriteTier: Int = 0,
    val savedPlaceId: String? = null,
)
enum class TrackEntrySource { CHECK_IN, MANUAL }
data class Destination(val id: String, val name: String, val order: Int, val done: Boolean, val qrId: String?)
data class PassportQr(
    val id: String,
    val name: String,
    val payload: String,
    val starred: Boolean,
    val createdOrder: Long = 0L,
    val starredOrder: Long? = null,
)

internal fun orderQrsForDisplay(values: List<PassportQr>): List<PassportQr> = values.sortedWith(
    compareBy<PassportQr>(
        { if (it.starred) 0 else 1 },
        { if (it.starred) it.starredOrder ?: it.createdOrder else it.createdOrder },
    ),
)
data class PassportProfile(
    val nickname: String = "",
    val bio: String = "",
    val socialIds: List<String> = emptyList(),
    val avatarPath: String? = null,
)

data class PassportBackupCapture(
    val snapshot: PassportBackupSnapshot,
    val revision: Long,
)

/** Local-only store: QR payloads and precise coordinates are never uploaded to Passport. */
class PassportDb(context: Context) : SQLiteOpenHelper(context, "passport.db", null, 10) {
    private val appContext = context.applicationContext

    override fun onCreate(db: SQLiteDatabase) {
        createPlacesTable(db)
        db.execSQL("CREATE TABLE tracks(id INTEGER PRIMARY KEY AUTOINCREMENT, request_id TEXT UNIQUE NOT NULL, destination_id TEXT, name TEXT NOT NULL, lat REAL NOT NULL, lon REAL NOT NULL, time INTEGER NOT NULL, accuracy REAL NOT NULL, provider TEXT NOT NULL DEFAULT 'MANUAL', address TEXT, place_id TEXT, original_url TEXT, canonical_url TEXT, resolution_state TEXT NOT NULL DEFAULT 'MANUAL_CONFIRMATION', entry_source TEXT NOT NULL DEFAULT 'CHECK_IN', favorite_tier INTEGER NOT NULL DEFAULT 0, saved_place_id TEXT)")
        db.execSQL("CREATE INDEX idx_tracks_favorite_time ON tracks(favorite_tier, time DESC)")
        db.execSQL("CREATE INDEX idx_tracks_saved_place_time ON tracks(saved_place_id, time DESC)")
        db.execSQL("CREATE TABLE destinations(id TEXT PRIMARY KEY, name TEXT NOT NULL, sort_order INTEGER NOT NULL, done INTEGER NOT NULL DEFAULT 0, qr_id TEXT)")
        db.execSQL("CREATE TABLE qrs(id TEXT PRIMARY KEY, name TEXT NOT NULL, payload TEXT NOT NULL, starred INTEGER NOT NULL DEFAULT 0, created_order INTEGER NOT NULL DEFAULT 0, starred_order INTEGER)")
        createSettingsTables(db)
        createBackupTracking(db)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        createSettingsTables(db)
        // Existing v2 tables need the new column; v1 upgrades may have just created it.
        val hasTimeout = db.rawQuery("PRAGMA table_info(device_settings)", null).use { c ->
            var found = false
            while (c.moveToNext()) if (c.getString(c.getColumnIndexOrThrow("name")) == "screen_timeout_seconds") found = true
            found
        }
        if (!hasTimeout) db.execSQL("ALTER TABLE device_settings ADD COLUMN screen_timeout_seconds INTEGER NOT NULL DEFAULT 60")
        if (oldVersion < 4) {
            val existing = db.rawQuery("PRAGMA table_info(tracks)", null).use { c ->
                buildSet { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("name"))) }
            }
            fun add(name: String, sql: String) { if (name !in existing) db.execSQL(sql) }
            add("provider", "ALTER TABLE tracks ADD COLUMN provider TEXT NOT NULL DEFAULT 'MANUAL'")
            add("address", "ALTER TABLE tracks ADD COLUMN address TEXT")
            add("place_id", "ALTER TABLE tracks ADD COLUMN place_id TEXT")
            add("original_url", "ALTER TABLE tracks ADD COLUMN original_url TEXT")
            add("canonical_url", "ALTER TABLE tracks ADD COLUMN canonical_url TEXT")
            add("resolution_state", "ALTER TABLE tracks ADD COLUMN resolution_state TEXT NOT NULL DEFAULT 'MANUAL_CONFIRMATION'")
        }
        if (oldVersion < 6) {
            val trackColumns = db.rawQuery("PRAGMA table_info(tracks)", null).use { c ->
                buildSet { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("name"))) }
            }
            if ("entry_source" !in trackColumns) {
                db.execSQL("ALTER TABLE tracks ADD COLUMN entry_source TEXT NOT NULL DEFAULT 'CHECK_IN'")
            }
        }
        if (oldVersion < 7) {
            val qrColumns = db.rawQuery("PRAGMA table_info(qrs)", null).use { c ->
                buildSet { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("name"))) }
            }
            if ("created_order" !in qrColumns) {
                db.execSQL("ALTER TABLE qrs ADD COLUMN created_order INTEGER NOT NULL DEFAULT 0")
            }
            if ("starred_order" !in qrColumns) {
                db.execSQL("ALTER TABLE qrs ADD COLUMN starred_order INTEGER")
            }
            db.execSQL("UPDATE qrs SET created_order=rowid WHERE created_order=0")
            db.execSQL("UPDATE qrs SET starred_order=created_order WHERE starred=1 AND starred_order IS NULL")
        }
        if (oldVersion < 8) {
            val trackColumns = db.rawQuery("PRAGMA table_info(tracks)", null).use { c ->
                buildSet { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("name"))) }
            }
            if ("favorite_tier" !in trackColumns) {
                db.execSQL("ALTER TABLE tracks ADD COLUMN favorite_tier INTEGER NOT NULL DEFAULT 0")
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_favorite_time ON tracks(favorite_tier, time DESC)")
        }
        if (oldVersion < 10) {
            createPlacesTable(db)
            val trackColumns = db.rawQuery("PRAGMA table_info(tracks)", null).use { c ->
                buildSet { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("name"))) }
            }
            if ("saved_place_id" !in trackColumns) {
                db.execSQL("ALTER TABLE tracks ADD COLUMN saved_place_id TEXT")
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_tracks_saved_place_time ON tracks(saved_place_id, time DESC)")
            migrateLegacyFavoriteTracks(db)
        }
        val profileColumns = db.rawQuery("PRAGMA table_info(profile)", null).use { c ->
            buildSet { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("name"))) }
        }
        if ("avatar_path" !in profileColumns) db.execSQL("ALTER TABLE profile ADD COLUMN avatar_path TEXT")
        createBackupTracking(db)
    }

    private fun createSettingsTables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS profile(id INTEGER PRIMARY KEY CHECK(id=1), nickname TEXT NOT NULL DEFAULT '', bio TEXT NOT NULL DEFAULT '', social_ids TEXT NOT NULL DEFAULT '', avatar_path TEXT)")
        db.execSQL("INSERT OR IGNORE INTO profile(id,nickname,bio,social_ids) VALUES(1,'','','')")
        db.execSQL("CREATE TABLE IF NOT EXISTS device_settings(id INTEGER PRIMARY KEY CHECK(id=1), sound_volume INTEGER NOT NULL DEFAULT 50, screen_timeout_seconds INTEGER NOT NULL DEFAULT 60)")
        db.execSQL("INSERT OR IGNORE INTO device_settings(id,sound_volume) VALUES(1,50)")
    }

    private fun createPlacesTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS places(id TEXT PRIMARY KEY, name TEXT NOT NULL, favorite_tier INTEGER NOT NULL DEFAULT 0, provider TEXT NOT NULL DEFAULT 'MANUAL', provider_place_id TEXT, original_url TEXT, canonical_url TEXT, lat REAL NOT NULL, lon REAL NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_places_favorite_updated ON places(favorite_tier, updated_at DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_places_provider_id ON places(provider, provider_place_id)")
    }

    private fun migrateLegacyFavoriteTracks(db: SQLiteDatabase) {
        val placeIds = mutableMapOf<String, String>()
        db.rawQuery(
            "SELECT id,name,lat,lon,time,provider,place_id,original_url,canonical_url,favorite_tier FROM tracks WHERE favorite_tier BETWEEN 1 AND 3 AND saved_place_id IS NULL ORDER BY time",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val trackId = cursor.getLong(0)
                val provider = enumOrDefault(cursor.getString(5), PlaceProvider.MANUAL)
                val providerPlaceId = cursor.getString(6)?.trim()?.takeIf(String::isNotEmpty)
                val originalUrl = cursor.getString(7)
                val canonicalUrl = cursor.getString(8)
                val stableUrl = normalizePlaceUrl(canonicalUrl) ?: normalizePlaceUrl(originalUrl)
                val stableKey = when {
                    providerPlaceId != null -> "provider:${provider.name}:$providerPlaceId"
                    stableUrl != null -> "url:$stableUrl"
                    else -> "track:$trackId"
                }
                val placeId = placeIds.getOrPut(stableKey) {
                    UUID.randomUUID().toString().also { id ->
                        val time = cursor.getLong(4)
                        db.insertOrThrow("places", null, ContentValues().apply {
                            put("id", id)
                            put("name", cursor.getString(1))
                            put("favorite_tier", cursor.getInt(9).coerceIn(1, 3))
                            put("provider", provider.name)
                            put("provider_place_id", providerPlaceId)
                            put("original_url", originalUrl)
                            put("canonical_url", canonicalUrl)
                            put("lat", cursor.getDouble(2))
                            put("lon", cursor.getDouble(3))
                            put("created_at", time)
                            put("updated_at", time)
                        })
                    }
                }
                db.execSQL(
                    "UPDATE places SET favorite_tier=MAX(favorite_tier,?),updated_at=MAX(updated_at,?) WHERE id=?",
                    arrayOf<Any>(cursor.getInt(9).coerceIn(1, 3), cursor.getLong(4), placeId),
                )
                db.execSQL("UPDATE tracks SET saved_place_id=? WHERE id=?", arrayOf<Any>(placeId, trackId))
            }
        }
    }

    private fun createBackupTracking(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS backup_state(id INTEGER PRIMARY KEY CHECK(id=1), revision INTEGER NOT NULL DEFAULT 0, changed_at INTEGER NOT NULL DEFAULT 0, tracking_suspended INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("INSERT OR IGNORE INTO backup_state(id) VALUES(1)")
        backupTrackingTriggerStatements().forEach(db::execSQL)
    }

    private fun prepareBackup() {
        AppDataBackup.enqueueDataChange(appContext)
    }

    @Synchronized fun profile(): PassportProfile = readableDatabase.rawQuery(
        "SELECT nickname,bio,social_ids,avatar_path FROM profile WHERE id=1", null
    ).use { c ->
        if (!c.moveToFirst()) PassportProfile() else PassportProfile(
            c.getString(0), c.getString(1), c.getString(2).lineSequence().filter(String::isNotBlank).take(4).toList(), c.getString(3)
        )
    }
    @Synchronized fun saveProfile(value: PassportProfile) {
        val nickname = truncateUtf8(value.nickname.trim(), 48)
        val bio = truncateUtf8(value.bio.trim(), 96)
        val social = value.socialIds.map { truncateUtf8(it.trim(), 48) }.filter(String::isNotEmpty).take(4)
        prepareBackup()
        writableDatabase.execSQL("UPDATE profile SET nickname=?,bio=?,social_ids=?,avatar_path=? WHERE id=1",
            arrayOf(nickname, bio, social.joinToString("\n"), value.avatarPath))
    }
    @Synchronized fun setAvatarPath(path: String?) {
        prepareBackup()
        writableDatabase.execSQL("UPDATE profile SET avatar_path=? WHERE id=1", arrayOf(path))
    }
    @Synchronized fun soundVolume(): Int = readableDatabase.rawQuery(
        "SELECT sound_volume FROM device_settings WHERE id=1", null
    ).use { c -> if (c.moveToFirst()) c.getInt(0).coerceIn(0, 100) else 50 }
    @Synchronized fun setSoundVolume(value: Int) {
        prepareBackup()
        writableDatabase.execSQL("UPDATE device_settings SET sound_volume=? WHERE id=1", arrayOf(value.coerceIn(0, 100)))
    }
    @Synchronized fun screenTimeoutSeconds(): Int = readableDatabase.rawQuery(
        "SELECT screen_timeout_seconds FROM device_settings WHERE id=1", null
    ).use { c -> if (c.moveToFirst()) c.getInt(0).coerceIn(15, 600) else 60 }
    @Synchronized fun setScreenTimeoutSeconds(value: Int) {
        require(value in 15..600)
        prepareBackup()
        writableDatabase.execSQL("UPDATE device_settings SET screen_timeout_seconds=? WHERE id=1", arrayOf(value))
    }

    @Synchronized fun destinations(): List<Destination> = readableDatabase.rawQuery("SELECT id,name,sort_order,done,qr_id FROM destinations ORDER BY sort_order", null).use { c ->
        buildList { while (c.moveToNext()) add(Destination(c.getString(0), c.getString(1), c.getInt(2), c.getInt(3) != 0, c.getString(4))) }
    }
    @Synchronized fun qrs(): List<PassportQr> = readableDatabase.rawQuery(
        "SELECT id,name,payload,starred,created_order,starred_order FROM qrs",
        null,
    ).use { c ->
        orderQrsForDisplay(buildList {
            while (c.moveToNext()) add(
                PassportQr(
                    id = c.getString(0),
                    name = c.getString(1),
                    payload = c.getString(2),
                    starred = c.getInt(3) != 0,
                    createdOrder = c.getLong(4),
                    starredOrder = if (c.isNull(5)) null else c.getLong(5),
                ),
            )
        })
    }
    @Synchronized fun tracks(): List<TrackPoint> = queryTracks(
        "SELECT $TRACK_COLUMNS FROM tracks ORDER BY time DESC LIMIT 300",
    )
    @Synchronized fun dataRevision(): Long = readableDatabase.rawQuery(
        "SELECT revision FROM backup_state WHERE id=1",
        null,
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }

    @Synchronized fun backupCapture(): PassportBackupCapture = readableDatabase.transaction {
        PassportBackupCapture(
            snapshot = PassportBackupSnapshot(
                profile = profile(),
                soundVolume = soundVolume(),
                screenTimeoutSeconds = screenTimeoutSeconds(),
                destinations = destinations(),
                qrs = qrs(),
                places = savedPlaces(favoritesOnly = false),
                tracks = queryTracks("SELECT $TRACK_COLUMNS FROM tracks ORDER BY time DESC"),
            ),
            revision = dataRevision(),
        )
    }

    @Synchronized fun backupSnapshot(): PassportBackupSnapshot = backupCapture().snapshot

    @Synchronized fun restoreBackup(snapshot: PassportBackupSnapshot) {
        snapshot.requireValid()
        prepareBackup()
        val db = writableDatabase
        db.transaction {
            db.execSQL("UPDATE backup_state SET tracking_suspended=1 WHERE id=1")
            db.delete("destinations", null, null)
            db.delete("qrs", null, null)
            db.delete("tracks", null, null)
            db.delete("places", null, null)
            snapshot.qrs.forEach { qr ->
                db.insertOrThrow("qrs", null, ContentValues().apply {
                    put("id", qr.id)
                    put("name", qr.name)
                    put("payload", qr.payload)
                    put("starred", if (qr.starred) 1 else 0)
                    put("created_order", qr.createdOrder)
                    if (qr.starredOrder == null) putNull("starred_order") else put("starred_order", qr.starredOrder)
                })
            }
            snapshot.destinations.forEach { destination ->
                db.insertOrThrow("destinations", null, ContentValues().apply {
                    put("id", destination.id)
                    put("name", destination.name)
                    put("sort_order", destination.order)
                    put("done", if (destination.done) 1 else 0)
                    if (destination.qrId == null) putNull("qr_id") else put("qr_id", destination.qrId)
                })
            }
            snapshot.places.forEach { place ->
                db.insertOrThrow("places", null, place.toContentValues())
            }
            snapshot.tracks.forEach { track ->
                db.insertOrThrow("tracks", null, ContentValues().apply {
                    put("id", track.id)
                    put("request_id", track.requestId)
                    if (track.destinationId == null) putNull("destination_id") else put("destination_id", track.destinationId)
                    put("name", track.name)
                    put("lat", track.lat)
                    put("lon", track.lon)
                    put("time", track.time)
                    put("accuracy", track.accuracy)
                    put("provider", track.provider.name)
                    if (track.address == null) putNull("address") else put("address", track.address)
                    if (track.placeId == null) putNull("place_id") else put("place_id", track.placeId)
                    if (track.originalUrl == null) putNull("original_url") else put("original_url", track.originalUrl)
                    if (track.canonicalUrl == null) putNull("canonical_url") else put("canonical_url", track.canonicalUrl)
                    put("resolution_state", track.resolutionState.name)
                    put("entry_source", track.entrySource.name)
                    put("favorite_tier", track.favoriteTier)
                    if (track.savedPlaceId == null) putNull("saved_place_id") else put("saved_place_id", track.savedPlaceId)
                })
            }
            migrateLegacyFavoriteTracks(db)
            db.execSQL(
                "UPDATE profile SET nickname=?,bio=?,social_ids=?,avatar_path=? WHERE id=1",
                arrayOf(
                    snapshot.profile.nickname,
                    snapshot.profile.bio,
                    snapshot.profile.socialIds.joinToString("\n"),
                    snapshot.profile.avatarPath,
                ),
            )
            db.execSQL(
                "UPDATE device_settings SET sound_volume=?,screen_timeout_seconds=? WHERE id=1",
                arrayOf(snapshot.soundVolume, snapshot.screenTimeoutSeconds),
            )
            db.execSQL(
                "UPDATE backup_state SET tracking_suspended=0,revision=revision+1,changed_at=? WHERE id=1",
                arrayOf(System.currentTimeMillis()),
            )
        }
    }
    @Synchronized fun favoriteTracks(): List<TrackPoint> = queryTracks(
        "SELECT $TRACK_COLUMNS FROM tracks WHERE favorite_tier BETWEEN 1 AND 3 ORDER BY time DESC",
    )
    @Synchronized fun savedPlaces(favoritesOnly: Boolean = true): List<SavedPlace> = querySavedPlaces(
        if (favoritesOnly) "WHERE p.favorite_tier BETWEEN 1 AND 3" else "",
    )
    @Synchronized fun savedPlace(id: String): SavedPlace? = querySavedPlaces(
        "WHERE p.id=?",
        arrayOf(id),
    ).firstOrNull()
    @Synchronized fun tracksForSavedPlace(placeId: String): List<TrackPoint> = queryTracks(
        "SELECT $TRACK_COLUMNS FROM tracks WHERE saved_place_id=? ORDER BY time DESC",
        arrayOf(placeId),
    )
    @Synchronized fun track(id: Long): TrackPoint? = queryTracks(
        "SELECT $TRACK_COLUMNS FROM tracks WHERE id=? LIMIT 1",
        arrayOf(id.toString()),
    ).firstOrNull()
    @Synchronized fun trackByRequestId(requestId: String): TrackPoint? = queryTracks(
        "SELECT $TRACK_COLUMNS FROM tracks WHERE request_id=? LIMIT 1",
        arrayOf(requestId),
    ).firstOrNull()
    @Synchronized fun deviceTracksForNameSync(limit: Int = 32): List<TrackPoint> {
        require(limit in 1..32)
        return queryTracks(
            "SELECT $TRACK_COLUMNS FROM tracks WHERE request_id LIKE 'device-%' ORDER BY time DESC LIMIT ?",
            arrayOf(limit.toString()),
        )
    }
    private fun queryTracks(sql: String, args: Array<String>? = null): List<TrackPoint> =
        readableDatabase.rawQuery(sql, args).use { c ->
            buildList { while (c.moveToNext()) add(TrackPoint(
                c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getDouble(4), c.getDouble(5),
                c.getLong(6), c.getFloat(7), enumOrDefault(c.getString(8), PlaceProvider.MANUAL), c.getString(9),
                c.getString(10), c.getString(11), c.getString(12),
                enumOrDefault(c.getString(13), PlaceResolutionState.MANUAL_CONFIRMATION),
                enumOrDefault(c.getString(14), TrackEntrySource.CHECK_IN),
                c.getInt(15).coerceIn(0, 3),
                c.getString(16),
            )) }
        }

    private fun querySavedPlaces(where: String, args: Array<String>? = null): List<SavedPlace> =
        readableDatabase.rawQuery(
            """SELECT p.id,p.name,p.favorite_tier,p.provider,p.provider_place_id,p.original_url,p.canonical_url,
                p.lat,p.lon,p.created_at,p.updated_at,COUNT(t.id),MAX(t.time)
                FROM places p LEFT JOIN tracks t ON t.saved_place_id=p.id
                $where GROUP BY p.id ORDER BY COALESCE(MAX(t.time),p.updated_at) DESC""".trimIndent(),
            args,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(
                    SavedPlace(
                        id = cursor.getString(0),
                        name = cursor.getString(1),
                        favoriteTier = cursor.getInt(2).coerceIn(0, 3),
                        provider = enumOrDefault(cursor.getString(3), PlaceProvider.MANUAL),
                        providerPlaceId = cursor.getString(4),
                        originalUrl = cursor.getString(5),
                        canonicalUrl = cursor.getString(6),
                        latitude = cursor.getDouble(7),
                        longitude = cursor.getDouble(8),
                        createdAt = cursor.getLong(9),
                        updatedAt = cursor.getLong(10),
                        visitCount = cursor.getInt(11),
                        lastVisitedAt = if (cursor.isNull(12)) null else cursor.getLong(12),
                    ),
                )
            }
        }
    @Synchronized fun setTrackFavoriteTier(id: Long, tier: Int): Boolean {
        require(tier in 0..3) { "收藏档位必须在 0 到 3 之间" }
        val track = track(id) ?: return false
        prepareBackup()
        val db = writableDatabase
        db.transaction {
            val linked = track.savedPlaceId?.let(::savedPlace)
            if (tier == 0) {
                if (linked != null) {
                    db.execSQL("UPDATE places SET favorite_tier=0,updated_at=? WHERE id=?", arrayOf<Any>(System.currentTimeMillis(), linked.id))
                    db.execSQL("UPDATE tracks SET favorite_tier=0 WHERE saved_place_id=?", arrayOf(linked.id))
                } else {
                    db.execSQL("UPDATE tracks SET favorite_tier=0 WHERE id=?", arrayOf(id))
                }
                return@transaction
            }

            val place = linked ?: findSavedPlaceMatch(track, savedPlaces(favoritesOnly = false))?.place
            val placeId = place?.id ?: UUID.randomUUID().toString().also { newId ->
                db.insertOrThrow(
                    "places",
                    null,
                    SavedPlace(
                        id = newId,
                        name = track.name,
                        favoriteTier = tier,
                        provider = track.provider,
                        providerPlaceId = track.placeId,
                        originalUrl = track.originalUrl,
                        canonicalUrl = track.canonicalUrl,
                        latitude = track.lat,
                        longitude = track.lon,
                        createdAt = track.time,
                        updatedAt = System.currentTimeMillis(),
                    ).toContentValues(),
                )
            }
            db.execSQL(
                "UPDATE places SET favorite_tier=?,updated_at=? WHERE id=?",
                arrayOf<Any>(tier, System.currentTimeMillis(), placeId),
            )
            db.execSQL("UPDATE tracks SET saved_place_id=?,favorite_tier=? WHERE id=?", arrayOf<Any>(placeId, tier, id))
            db.execSQL("UPDATE tracks SET favorite_tier=? WHERE saved_place_id=?", arrayOf<Any>(tier, placeId))
        }
        return true
    }

    @Synchronized fun updateSavedPlace(
        id: String,
        name: String,
        tier: Int,
        originalUrl: String?,
        canonicalUrl: String?,
    ): Boolean {
        require(tier in 0..3)
        val cleanName = truncateUtf8(name.trim(), 100)
        require(cleanName.isNotEmpty())
        if (savedPlace(id) == null) return false
        prepareBackup()
        val db = writableDatabase
        db.transaction {
            db.execSQL(
                "UPDATE places SET name=?,favorite_tier=?,original_url=?,canonical_url=?,updated_at=? WHERE id=?",
                arrayOf<Any?>(cleanName, tier, originalUrl?.trim()?.takeIf(String::isNotEmpty),
                    canonicalUrl?.trim()?.takeIf(String::isNotEmpty), System.currentTimeMillis(), id),
            )
            db.execSQL("UPDATE tracks SET favorite_tier=? WHERE saved_place_id=?", arrayOf<Any>(tier, id))
        }
        return true
    }

    @Synchronized fun unlinkTrackFromSavedPlace(trackId: Long): Boolean {
        if (track(trackId) == null) return false
        prepareBackup()
        return writableDatabase.update(
            "tracks",
            ContentValues().apply { putNull("saved_place_id"); put("favorite_tier", 0) },
            "id=?",
            arrayOf(trackId.toString()),
        ) == 1
    }

    @Synchronized fun linkTrackToSavedPlace(trackId: Long, placeId: String): Boolean {
        val place = savedPlace(placeId) ?: return false
        if (track(trackId) == null) return false
        prepareBackup()
        return writableDatabase.update(
            "tracks",
            ContentValues().apply { put("saved_place_id", place.id); put("favorite_tier", place.favoriteTier) },
            "id=?",
            arrayOf(trackId.toString()),
        ) == 1
    }

    private fun reconcileTrackWithSavedPlace(id: Long) {
        val track = track(id) ?: return
        val match = findSavedPlaceMatch(track, savedPlaces(favoritesOnly = false)) ?: return
        writableDatabase.execSQL(
            "UPDATE tracks SET saved_place_id=?,favorite_tier=? WHERE id=?",
            arrayOf<Any>(match.place.id, match.place.favoriteTier, id),
        )
    }
    @Synchronized fun hasTrack(requestId: String): Boolean = readableDatabase.rawQuery("SELECT 1 FROM tracks WHERE request_id=?", arrayOf(requestId)).use { it.moveToFirst() }
    @Synchronized fun updateTrackName(id: Long, name: String) {
        require(name.trim().isNotEmpty())
        prepareBackup()
        writableDatabase.execSQL(
            "UPDATE tracks SET name=?,provider=?,resolution_state=? WHERE id=?",
            arrayOf<Any>(name.trim().take(100), PlaceProvider.MANUAL.name, PlaceResolutionState.MANUAL_CONFIRMATION.name, id)
        )
        reconcileTrackWithSavedPlace(id)
    }
    @Synchronized fun updateTrackPlace(id: Long, value: PlaceResolution) {
        val cleanName = truncateUtf8(value.name?.trim().orEmpty(), 100)
        require(cleanName.isNotEmpty())
        prepareBackup()
        writableDatabase.execSQL(
            "UPDATE tracks SET name=?,provider=?,address=?,place_id=?,original_url=?,canonical_url=?,resolution_state=? WHERE id=?",
            arrayOf<Any?>(cleanName, value.provider.name, value.address, value.placeId, value.originalUrl,
                value.canonicalUrl, value.state.name, id)
        )
        reconcileTrackWithSavedPlace(id)
    }
    @Synchronized fun enrichTrackFromSystem(id: Long, name: String, address: String?) {
        val clean = truncateUtf8(name.trim(), 100)
        if (clean.isEmpty()) return
        prepareBackup()
        writableDatabase.execSQL(
            "UPDATE tracks SET name=?,address=?,provider=?,resolution_state=? WHERE id=? AND provider IN ('MANUAL','SYSTEM')",
            arrayOf<Any?>(clean, address, PlaceProvider.SYSTEM.name, PlaceResolutionState.PARTIAL.name, id)
        )
        reconcileTrackWithSavedPlace(id)
    }
    @Synchronized fun enrichTrackFromSystem(requestId: String, name: String, address: String?) {
        val clean = truncateUtf8(name.trim(), 100)
        if (clean.isEmpty()) return
        prepareBackup()
        writableDatabase.execSQL(
            "UPDATE tracks SET name=?,address=?,provider=?,resolution_state=? WHERE request_id=? AND provider IN ('MANUAL','SYSTEM')",
            arrayOf<Any?>(clean, address, PlaceProvider.SYSTEM.name, PlaceResolutionState.PARTIAL.name, requestId)
        )
        trackByRequestId(requestId)?.let { reconcileTrackWithSavedPlace(it.id) }
    }
    @Synchronized fun deleteTrack(id: Long) {
        prepareBackup()
        writableDatabase.delete("tracks", "id=?", arrayOf(id.toString()))
    }
    @Synchronized fun addManualTrack(value: ManualTrackDraft): Long {
        value.requireValid()
        prepareBackup()
        val values = ContentValues().apply {
            put("request_id", "manual-${UUID.randomUUID()}")
            putNull("destination_id")
            put("name", truncateUtf8(value.name.trim(), 100))
            put("lat", value.latitude)
            put("lon", value.longitude)
            put("time", value.time)
            put("accuracy", value.accuracy.coerceAtLeast(0f))
            put("provider", value.provider.name)
            put("address", value.address)
            put("place_id", value.placeId)
            put("original_url", value.originalUrl)
            put("canonical_url", value.canonicalUrl)
            put("resolution_state", value.resolutionState.name)
            put("entry_source", TrackEntrySource.MANUAL.name)
        }
        return writableDatabase.insertOrThrow("tracks", null, values).also(::reconcileTrackWithSavedPlace)
    }
    @Synchronized fun updateManualTrack(id: Long, value: ManualTrackDraft): Boolean {
        value.requireValid()
        prepareBackup()
        val values = ContentValues().apply {
            put("name", truncateUtf8(value.name.trim(), 100))
            put("lat", value.latitude)
            put("lon", value.longitude)
            put("time", value.time)
            put("accuracy", value.accuracy.coerceAtLeast(0f))
            put("provider", value.provider.name)
            put("address", value.address)
            put("place_id", value.placeId)
            put("original_url", value.originalUrl)
            put("canonical_url", value.canonicalUrl)
            put("resolution_state", value.resolutionState.name)
        }
        return writableDatabase.update(
            "tracks",
            values,
            "id=? AND entry_source=?",
            arrayOf(id.toString(), TrackEntrySource.MANUAL.name),
        ).let { updated ->
            if (updated == 1) reconcileTrackWithSavedPlace(id)
            updated == 1
        }
    }
    @Synchronized fun saveTrackOnce(requestId: String, destinationId: String?, name: String, lat: Double, lon: Double, time: Long, accuracy: Float): Boolean {
        prepareBackup()
        val values = ContentValues().apply {
            put("request_id", requestId); put("destination_id", destinationId); put("name", name)
            put("lat", lat); put("lon", lon); put("time", time); put("accuracy", accuracy)
            put("provider", PlaceProvider.SYSTEM.name); put("resolution_state", PlaceResolutionState.PARTIAL.name)
        }
        val rowId = writableDatabase.insertWithOnConflict("tracks", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        if (rowId != -1L) reconcileTrackWithSavedPlace(rowId)
        return rowId != -1L
    }
    @Synchronized fun addDestination(name: String) {
        val clean = truncateUtf8(name.trim(), 47); if (clean.isEmpty()) return
        if (destinations().size >= 64) error("Passport 单次最多同步 64 个目的地")
        prepareBackup()
        val values = ContentValues().apply { put("id", UUID.randomUUID().toString()); put("name", clean); put("sort_order", destinations().size) }
        writableDatabase.insert("destinations", null, values)
    }
    @Synchronized fun addDestinations(names: List<String>): Int {
        val existing = destinations()
        val capacity = (64 - existing.size).coerceAtLeast(0)
        val clean = names.map { truncateUtf8(it.trim(), 47) }.filter(String::isNotEmpty).take(capacity)
        if (clean.isEmpty()) return 0
        prepareBackup()
        writableDatabase.beginTransaction()
        return try {
            clean.forEachIndexed { index, name ->
                val values = ContentValues().apply {
                    put("id", UUID.randomUUID().toString()); put("name", name); put("sort_order", existing.size + index)
                }
                writableDatabase.insertOrThrow("destinations", null, values)
            }
            writableDatabase.setTransactionSuccessful()
            clean.size
        } finally { writableDatabase.endTransaction() }
    }
    @Synchronized fun deleteDestination(id: String) {
        prepareBackup()
        writableDatabase.delete("destinations", "id=?", arrayOf(id))
        normalizeOrder()
    }
    @Synchronized fun updateDestination(id: String, name: String) {
        val clean = truncateUtf8(name.trim(), 47)
        require(clean.isNotEmpty())
        prepareBackup()
        writableDatabase.execSQL("UPDATE destinations SET name=? WHERE id=?", arrayOf(clean, id))
    }
    @Synchronized fun reorder(from: Int, to: Int) {
        val list = destinations().toMutableList(); if (from !in list.indices || to !in list.indices || from == to) return
        val moved = list.removeAt(from); list.add(to, moved)
        prepareBackup()
        writableDatabase.beginTransaction(); try {
            list.forEachIndexed { index, item -> writableDatabase.execSQL("UPDATE destinations SET sort_order=? WHERE id=?", arrayOf<Any>(index, item.id)) }
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }
    @Synchronized fun bindQrToDestination(qrId: String, destinationId: String?) {
        prepareBackup()
        writableDatabase.beginTransaction()
        try {
            writableDatabase.execSQL("UPDATE destinations SET qr_id=NULL WHERE qr_id=?", arrayOf(qrId))
            if (destinationId != null) {
                writableDatabase.execSQL("UPDATE destinations SET qr_id=? WHERE id=?", arrayOf(qrId, destinationId))
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }
    @Synchronized fun addQr(name: String, payload: String, starred: Boolean, id: String? = null): String {
        val cleanName = truncateUtf8(name.trim(), 48); val cleanPayload = payload.trim()
        require(cleanName.isNotEmpty() && cleanPayload.isNotEmpty() && cleanPayload.toByteArray(Charsets.UTF_8).size <= 192)
        val existing = id?.let { key -> qrs().firstOrNull { it.id == key } }
        if (starred && qrs().count { it.starred && it.id != id } >= 3) error("最多只能星标 3 个二维码")
        prepareBackup()
        val key = id ?: UUID.randomUUID().toString()
        val createdOrder = existing?.createdOrder ?: nextQrOrder("created_order")
        val starredOrder = when {
            !starred -> null
            existing?.starred == true -> existing.starredOrder ?: nextQrOrder("starred_order")
            else -> nextQrOrder("starred_order")
        }
        val values = ContentValues().apply {
            put("id", key)
            put("name", cleanName)
            put("payload", cleanPayload)
            put("starred", if (starred) 1 else 0)
            put("created_order", createdOrder)
            if (starredOrder == null) putNull("starred_order") else put("starred_order", starredOrder)
        }
        writableDatabase.insertWithOnConflict("qrs", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return key
    }
    @Synchronized fun deleteQr(id: String) {
        prepareBackup()
        writableDatabase.delete("qrs", "id=?", arrayOf(id)); writableDatabase.execSQL("UPDATE destinations SET qr_id=NULL WHERE qr_id=?", arrayOf(id))
    }
    @Synchronized fun markDestinationDone(id: String) = setDestinationDone(id, true)
    @Synchronized fun setDestinationDone(id: String, done: Boolean) {
        prepareBackup()
        writableDatabase.execSQL("UPDATE destinations SET done=? WHERE id=?", arrayOf<Any>(if (done) 1 else 0, id))
    }
    private fun normalizeOrder() {
        destinations().forEachIndexed { index, item -> writableDatabase.execSQL("UPDATE destinations SET sort_order=? WHERE id=?", arrayOf<Any>(index, item.id)) }
    }
    private fun nextQrOrder(column: String): Long {
        require(column == "created_order" || column == "starred_order")
        return readableDatabase.rawQuery("SELECT COALESCE(MAX($column), 0) + 1 FROM qrs", null).use { c ->
            if (c.moveToFirst()) c.getLong(0) else 1L
        }
    }
    internal fun truncateUtf8(value: String, maxBytes: Int): String {
        val out = StringBuilder(); var bytes = 0
        value.codePoints().forEach { cp ->
            val part = String(Character.toChars(cp)); val size = part.toByteArray(Charsets.UTF_8).size
            if (bytes + size <= maxBytes) { out.append(part); bytes += size }
        }
        return out.toString().trim()
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback
}

private fun SavedPlace.toContentValues(): ContentValues = ContentValues().apply {
    put("id", id)
    put("name", name)
    put("favorite_tier", favoriteTier)
    put("provider", provider.name)
    if (providerPlaceId == null) putNull("provider_place_id") else put("provider_place_id", providerPlaceId)
    if (originalUrl == null) putNull("original_url") else put("original_url", originalUrl)
    if (canonicalUrl == null) putNull("canonical_url") else put("canonical_url", canonicalUrl)
    put("lat", latitude)
    put("lon", longitude)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}
