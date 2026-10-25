package com.bugchecking.app

import android.content.Context
import android.location.Address
import android.os.Build
import android.os.Bundle
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object GeocoderDiagnostics {
    private const val TAG = "GeocoderDiagnostics"
    private const val DIRECTORY = "diagnostics"
    private const val FILE_NAME = "geocoder-raw.jsonl"

    @Synchronized
    fun record(
        context: Context,
        source: String,
        latitude: Double,
        longitude: Double,
        requestId: String?,
        addresses: List<Address> = emptyList(),
        error: Throwable? = null,
    ) {
        runCatching {
            val directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }
            val output = File(directory, FILE_NAME)
            val capturedAt = System.currentTimeMillis()
            val entry = JSONObject()
                .put("schemaVersion", 1)
                .put("capturedAtEpochMs", capturedAt)
                .put("capturedAtIso8601", iso8601(capturedAt))
                .put("source", source)
                .put("requestId", requestId ?: JSONObject.NULL)
                .put("queryLatitude", latitude)
                .put("queryLongitude", longitude)
                .put("resultCount", addresses.size)
                .put("deviceManufacturer", Build.MANUFACTURER)
                .put("deviceModel", Build.MODEL)
                .put("androidSdk", Build.VERSION.SDK_INT)
                .put("defaultLocale", Locale.getDefault().toLanguageTag())
                .put("results", JSONArray().apply {
                    addresses.forEachIndexed { index, address -> put(address.toJson(index)) }
                })
            if (error != null) {
                entry.put("errorType", error.javaClass.name)
                entry.put("errorMessage", error.message ?: JSONObject.NULL)
            }
            output.appendText(entry.toString() + "\n", Charsets.UTF_8)
            Log.i(TAG, "saved ${addresses.size} raw result(s) from $source to ${output.absolutePath}")
        }.onFailure { failure ->
            Log.e(TAG, "failed to save raw geocoder results", failure)
        }
    }

    fun file(context: Context): File = File(File(context.filesDir, DIRECTORY), FILE_NAME)

    private fun iso8601(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(timestamp))

    private fun Address.toJson(index: Int): JSONObject {
        val extrasJson = extras.toJson()
        val extrasRaw = extras?.toString()
        return JSONObject()
            .put("index", index)
            .put("implementationClass", javaClass.name)
            .put("locale", locale?.toLanguageTag() ?: JSONObject.NULL)
            .put("addressLines", JSONArray().apply {
                if (maxAddressLineIndex >= 0) {
                    for (lineIndex in 0..maxAddressLineIndex) put(getAddressLine(lineIndex))
                }
            })
            .putNullable("featureName", featureName)
            .putNullable("adminArea", adminArea)
            .putNullable("subAdminArea", subAdminArea)
            .putNullable("locality", locality)
            .putNullable("subLocality", subLocality)
            .putNullable("thoroughfare", thoroughfare)
            .putNullable("subThoroughfare", subThoroughfare)
            .putNullable("premises", premises)
            .putNullable("postalCode", postalCode)
            .putNullable("countryCode", countryCode)
            .putNullable("countryName", countryName)
            .put("hasLatitude", hasLatitude())
            .put("latitude", if (hasLatitude()) latitude else JSONObject.NULL)
            .put("hasLongitude", hasLongitude())
            .put("longitude", if (hasLongitude()) longitude else JSONObject.NULL)
            .putNullable("phone", phone)
            .putNullable("url", url)
            .put("extrasRawToString", extrasRaw ?: JSONObject.NULL)
            .put("extras", extrasJson)
            .put("rawToString", toString())
    }

    private fun JSONObject.putNullable(name: String, value: String?): JSONObject =
        put(name, value ?: JSONObject.NULL)

    private fun Bundle?.toJson(): JSONObject {
        if (this == null) return JSONObject()
        return JSONObject().apply {
            this@toJson.keySet().sorted().forEach { key ->
                val read = runCatching { this@toJson.get(key) }
                val value = read.getOrNull()
                put(key, when {
                    read.isFailure -> JSONObject().put(
                        "readError",
                        read.exceptionOrNull()?.let { "${it.javaClass.name}: ${it.message}" },
                    )
                    else -> when (value) {
                    null -> JSONObject.NULL
                    is Bundle -> value.toJson()
                    is Array<*> -> JSONArray(value.map(::safeValue))
                    is BooleanArray -> JSONArray(value.toList())
                    is ByteArray -> JSONArray(value.toList())
                    is CharArray -> JSONArray(value.map(Char::toString))
                    is DoubleArray -> JSONArray(value.toList())
                    is FloatArray -> JSONArray(value.toList())
                    is IntArray -> JSONArray(value.toList())
                    is LongArray -> JSONArray(value.toList())
                    is ShortArray -> JSONArray(value.toList())
                    is Iterable<*> -> JSONArray(value.map(::safeValue))
                    else -> safeValue(value)
                    }
                })
            }
        }
    }

    private fun safeValue(value: Any?): Any = when (value) {
        null -> JSONObject.NULL
        is Boolean, is Byte, is Short, is Int, is Long, is Float, is Double, is String -> value
        is Char -> value.toString()
        else -> value.toString()
    }
}
