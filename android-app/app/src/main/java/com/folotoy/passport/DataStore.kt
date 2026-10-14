package com.folotoy.passport

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

data class TrackPoint(val id: Long, val requestId: String, val destinationId: String?, val name: String, val lat: Double, val lon: Double, val time: Long, val accuracy: Float)
data class Destination(val id: String, val name: String, val order: Int, val done: Boolean, val qrId: String?)
data class PassportQr(val id: String, val name: String, val payload: String, val starred: Boolean)
data class PassportProfile(val nickname: String = "", val bio: String = "", val socialIds: List<String> = emptyList())

/** Local-only store: QR payloads and precise coordinates are never uploaded to Passport. */
class PassportDb(context: Context) : SQLiteOpenHelper(context, "passport.db", null, 3) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE tracks(id INTEGER PRIMARY KEY AUTOINCREMENT, request_id TEXT UNIQUE NOT NULL, destination_id TEXT, name TEXT NOT NULL, lat REAL NOT NULL, lon REAL NOT NULL, time INTEGER NOT NULL, accuracy REAL NOT NULL)")
        db.execSQL("CREATE TABLE destinations(id TEXT PRIMARY KEY, name TEXT NOT NULL, sort_order INTEGER NOT NULL, done INTEGER NOT NULL DEFAULT 0, qr_id TEXT)")
        db.execSQL("CREATE TABLE qrs(id TEXT PRIMARY KEY, name TEXT NOT NULL, payload TEXT NOT NULL, starred INTEGER NOT NULL DEFAULT 0)")
        createSettingsTables(db)
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
    }

    private fun createSettingsTables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS profile(id INTEGER PRIMARY KEY CHECK(id=1), nickname TEXT NOT NULL DEFAULT '', bio TEXT NOT NULL DEFAULT '', social_ids TEXT NOT NULL DEFAULT '')")
        db.execSQL("INSERT OR IGNORE INTO profile(id,nickname,bio,social_ids) VALUES(1,'','','')")
        db.execSQL("CREATE TABLE IF NOT EXISTS device_settings(id INTEGER PRIMARY KEY CHECK(id=1), sound_volume INTEGER NOT NULL DEFAULT 50, screen_timeout_seconds INTEGER NOT NULL DEFAULT 60)")
        db.execSQL("INSERT OR IGNORE INTO device_settings(id,sound_volume) VALUES(1,50)")
    }

    @Synchronized fun profile(): PassportProfile = readableDatabase.rawQuery(
        "SELECT nickname,bio,social_ids FROM profile WHERE id=1", null
    ).use { c ->
        if (!c.moveToFirst()) PassportProfile() else PassportProfile(
            c.getString(0), c.getString(1), c.getString(2).lineSequence().filter(String::isNotBlank).take(4).toList()
        )
    }
    @Synchronized fun saveProfile(value: PassportProfile) {
        val nickname = truncateUtf8(value.nickname.trim(), 48)
        val bio = truncateUtf8(value.bio.trim(), 96)
        val social = value.socialIds.map { truncateUtf8(it.trim(), 48) }.filter(String::isNotEmpty).take(4)
        writableDatabase.execSQL("UPDATE profile SET nickname=?,bio=?,social_ids=? WHERE id=1",
            arrayOf(nickname, bio, social.joinToString("\n")))
    }
    @Synchronized fun soundVolume(): Int = readableDatabase.rawQuery(
        "SELECT sound_volume FROM device_settings WHERE id=1", null
    ).use { c -> if (c.moveToFirst()) c.getInt(0).coerceIn(0, 100) else 50 }
    @Synchronized fun setSoundVolume(value: Int) {
        writableDatabase.execSQL("UPDATE device_settings SET sound_volume=? WHERE id=1", arrayOf(value.coerceIn(0, 100)))
    }
    @Synchronized fun screenTimeoutSeconds(): Int = readableDatabase.rawQuery(
        "SELECT screen_timeout_seconds FROM device_settings WHERE id=1", null
    ).use { c -> if (c.moveToFirst()) c.getInt(0).coerceIn(15, 600) else 60 }
    @Synchronized fun setScreenTimeoutSeconds(value: Int) {
        require(value in 15..600)
        writableDatabase.execSQL("UPDATE device_settings SET screen_timeout_seconds=? WHERE id=1", arrayOf(value))
    }

    @Synchronized fun destinations(): List<Destination> = readableDatabase.rawQuery("SELECT id,name,sort_order,done,qr_id FROM destinations ORDER BY sort_order", null).use { c ->
        buildList { while (c.moveToNext()) add(Destination(c.getString(0), c.getString(1), c.getInt(2), c.getInt(3) != 0, c.getString(4))) }
    }
    @Synchronized fun qrs(): List<PassportQr> = readableDatabase.rawQuery("SELECT id,name,payload,starred FROM qrs ORDER BY name COLLATE NOCASE", null).use { c ->
        buildList { while (c.moveToNext()) add(PassportQr(c.getString(0), c.getString(1), c.getString(2), c.getInt(3) != 0)) }
    }
    @Synchronized fun tracks(): List<TrackPoint> = readableDatabase.rawQuery("SELECT id,request_id,destination_id,name,lat,lon,time,accuracy FROM tracks ORDER BY time DESC LIMIT 300", null).use { c ->
        buildList { while (c.moveToNext()) add(TrackPoint(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getDouble(4), c.getDouble(5), c.getLong(6), c.getFloat(7))) }
    }
    @Synchronized fun hasTrack(requestId: String): Boolean = readableDatabase.rawQuery("SELECT 1 FROM tracks WHERE request_id=?", arrayOf(requestId)).use { it.moveToFirst() }
    @Synchronized fun updateTrack(id: Long, name: String, lat: Double, lon: Double) {
        require(name.trim().isNotEmpty() && lat in -90.0..90.0 && lon in -180.0..180.0)
        writableDatabase.execSQL("UPDATE tracks SET name=?,lat=?,lon=? WHERE id=?",
            arrayOf(name.trim().take(100), lat, lon, id))
    }
    @Synchronized fun saveTrackOnce(requestId: String, destinationId: String?, name: String, lat: Double, lon: Double, time: Long, accuracy: Float): Boolean {
        val values = ContentValues().apply {
            put("request_id", requestId); put("destination_id", destinationId); put("name", name)
            put("lat", lat); put("lon", lon); put("time", time); put("accuracy", accuracy)
        }
        return writableDatabase.insertWithOnConflict("tracks", null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1L
    }
    @Synchronized fun addDestination(name: String) {
        val clean = truncateUtf8(name.trim(), 47); if (clean.isEmpty()) return
        if (destinations().size >= 64) error("Passport 单次最多同步 64 个目的地")
        val values = ContentValues().apply { put("id", UUID.randomUUID().toString()); put("name", clean); put("sort_order", destinations().size) }
        writableDatabase.insert("destinations", null, values)
    }
    @Synchronized fun deleteDestination(id: String) { writableDatabase.delete("destinations", "id=?", arrayOf(id)); normalizeOrder() }
    @Synchronized fun reorder(from: Int, to: Int) {
        val list = destinations().toMutableList(); if (from !in list.indices || to !in list.indices || from == to) return
        val moved = list.removeAt(from); list.add(to, moved)
        writableDatabase.beginTransaction(); try {
            list.forEachIndexed { index, item -> writableDatabase.execSQL("UPDATE destinations SET sort_order=? WHERE id=?", arrayOf(index, item.id)) }
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }
    @Synchronized fun bindQrToDestination(qrId: String, destinationId: String?) {
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
        if (starred && qrs().count { it.starred && it.id != id } >= 3) error("最多只能星标 3 个二维码")
        val key = id ?: UUID.randomUUID().toString()
        val values = ContentValues().apply { put("id", key); put("name", cleanName); put("payload", cleanPayload); put("starred", if (starred) 1 else 0) }
        writableDatabase.insertWithOnConflict("qrs", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return key
    }
    @Synchronized fun deleteQr(id: String) {
        writableDatabase.delete("qrs", "id=?", arrayOf(id)); writableDatabase.execSQL("UPDATE destinations SET qr_id=NULL WHERE qr_id=?", arrayOf(id))
    }
    @Synchronized fun markDestinationDone(id: String) = setDestinationDone(id, true)
    @Synchronized fun setDestinationDone(id: String, done: Boolean) {
        writableDatabase.execSQL("UPDATE destinations SET done=? WHERE id=?", arrayOf(if (done) 1 else 0, id))
    }
    private fun normalizeOrder() {
        destinations().forEachIndexed { index, item -> writableDatabase.execSQL("UPDATE destinations SET sort_order=? WHERE id=?", arrayOf(index, item.id)) }
    }
    internal fun truncateUtf8(value: String, maxBytes: Int): String {
        val out = StringBuilder(); var bytes = 0
        value.codePoints().forEach { cp ->
            val part = String(Character.toChars(cp)); val size = part.toByteArray(Charsets.UTF_8).size
            if (bytes + size <= maxBytes) { out.append(part); bytes += size }
        }
        return out.toString().trim()
    }
}
