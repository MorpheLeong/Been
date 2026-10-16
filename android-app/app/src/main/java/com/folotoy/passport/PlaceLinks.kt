package com.folotoy.passport

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

enum class PlaceProvider { MANUAL, SYSTEM, AMAP, GOOGLE }
enum class PlaceResolutionState { RESOLVED, PARTIAL, MANUAL_CONFIRMATION }

data class PlaceResolution(
    val provider: PlaceProvider,
    val name: String?,
    val address: String? = null,
    val latitude: Double?,
    val longitude: Double?,
    val placeId: String? = null,
    val originalUrl: String? = null,
    val canonicalUrl: String? = null,
    val state: PlaceResolutionState,
)

object PlaceLinkParser {
    private val urlPattern = Regex("https://[^\\s<>]+", RegexOption.IGNORE_CASE)
    private val atCoordinates = Regex("@(-?\\d{1,3}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)")
    private val dataCoordinates = Regex("!3d(-?\\d{1,3}(?:\\.\\d+)?)!4d(-?\\d{1,3}(?:\\.\\d+)?)")
    private val plainCoordinates = Regex("(?<![\\d.])(-?\\d{1,3}(?:\\.\\d+)?)\\s*[,，]\\s*(-?\\d{1,3}(?:\\.\\d+)?)(?![\\d.])")
    private val supportedSuffixes = listOf("google.com", "goo.gl", "amap.com")
    private val shortHosts = setOf("maps.app.goo.gl", "goo.gl", "surl.amap.com")

    fun firstUrl(text: String): String? = urlPattern.find(text)?.value?.trimEnd('.', ',', '，', ')', '）')

    fun needsRedirect(url: String): Boolean = runCatching {
        URI(url).host?.lowercase() in shortHosts
    }.getOrDefault(false)

    fun isSupportedUrl(url: String): Boolean = runCatching {
        val host = URI(url).host?.lowercase() ?: return@runCatching false
        URI(url).scheme?.equals("https", true) == true && supportedSuffixes.any { host == it || host.endsWith(".$it") }
    }.getOrDefault(false)

    fun parse(text: String, expandedUrl: String? = null): PlaceResolution? {
        val original = firstUrl(text)
        val effective = expandedUrl ?: original
        val sharedName = text.lineSequence()
            .map(String::trim)
            .firstOrNull { it.isNotBlank() && !it.contains("https://", true) && it.length <= 100 }
            ?.removePrefix("地点：")?.removePrefix("位置：")?.trim()
        if (effective != null && isSupportedUrl(effective)) {
            val uri = runCatching { URI(effective) }.getOrNull()
            val host = uri?.host?.lowercase().orEmpty()
            return when {
                host == "uri.amap.com" || host.endsWith(".amap.com") || host == "amap.com" ->
                    parseAmap(uri!!, sharedName, original, effective)
                host == "goo.gl" || host.endsWith(".goo.gl") || host.endsWith(".google.com") ->
                    parseGoogle(uri!!, sharedName, original, effective)
                else -> null
            }
        }
        val pair = plainCoordinates.find(text)?.let { validPair(it.groupValues[1], it.groupValues[2]) }
        return pair?.let {
            PlaceResolution(PlaceProvider.MANUAL, sharedName, latitude = it.first, longitude = it.second,
                originalUrl = original, canonicalUrl = effective, state = PlaceResolutionState.MANUAL_CONFIRMATION)
        }
    }

    private fun parseGoogle(uri: URI, sharedName: String?, original: String?, effective: String): PlaceResolution {
        val query = queryParameters(uri)
        val pathText = decode(uri.path.orEmpty())
        val rawLabel = query["query"]?.takeUnless(::looksLikeCoordinates)
            ?: Regex("/place/([^/]+)").find(pathText)?.groupValues?.get(1)?.replace('+', ' ')
        val name = rawLabel?.let(GooglePlaceNameExtractor::extract) ?: sharedName
        val coordinates = atCoordinates.find(effective)?.let { validPair(it.groupValues[1], it.groupValues[2]) }
            ?: dataCoordinates.find(effective)?.let { validPair(it.groupValues[1], it.groupValues[2]) }
            ?: query["query"]?.let(::parsePair)
        return PlaceResolution(
            provider = PlaceProvider.GOOGLE,
            name = name,
            latitude = coordinates?.first,
            longitude = coordinates?.second,
            placeId = query["query_place_id"],
            originalUrl = original,
            canonicalUrl = effective,
            state = if (coordinates != null) PlaceResolutionState.RESOLVED else PlaceResolutionState.PARTIAL,
        )
    }

    private fun parseAmap(uri: URI, sharedName: String?, original: String?, effective: String): PlaceResolution {
        val query = queryParameters(uri)
        val raw = query["position"]?.let(::parseLonLat)
        val coordinate = query["coordinate"]?.lowercase()
        val coordinates = if (raw != null && coordinate != "wgs84") {
            val wgs = CoordinateTransforms.gcj02ToWgs84(raw.first, raw.second)
            wgs.first to wgs.second
        } else raw
        return PlaceResolution(
            provider = PlaceProvider.AMAP,
            name = query["name"] ?: sharedName,
            latitude = coordinates?.first,
            longitude = coordinates?.second,
            placeId = query["poiid"],
            originalUrl = original,
            canonicalUrl = effective,
            state = if (coordinates != null) PlaceResolutionState.RESOLVED else PlaceResolutionState.PARTIAL,
        )
    }

    private fun queryParameters(uri: URI): Map<String, String> = uri.rawQuery.orEmpty().split('&')
        .mapNotNull { part ->
            val index = part.indexOf('=')
            if (index < 0) null else decode(part.substring(0, index)) to decode(part.substring(index + 1))
        }.toMap()

    private fun parsePair(value: String): Pair<Double, Double>? {
        val match = plainCoordinates.find(value) ?: return null
        return validPair(match.groupValues[1], match.groupValues[2])
    }

    private fun parseLonLat(value: String): Pair<Double, Double>? {
        val parts = value.split(',', '，').map(String::trim)
        if (parts.size != 2) return null
        val lon = parts[0].toDoubleOrNull() ?: return null
        val lat = parts[1].toDoubleOrNull() ?: return null
        return if (lat in -90.0..90.0 && lon in -180.0..180.0) lat to lon else null
    }

    private fun validPair(latText: String, lonText: String): Pair<Double, Double>? {
        val lat = latText.toDoubleOrNull() ?: return null
        val lon = lonText.toDoubleOrNull() ?: return null
        return if (lat in -90.0..90.0 && lon in -180.0..180.0) lat to lon else null
    }

    private fun looksLikeCoordinates(value: String): Boolean = parsePair(value) != null
    private fun decode(value: String): String = runCatching { URLDecoder.decode(value, Charsets.UTF_8.name()) }.getOrDefault(value)
}

object PlaceRedirectResolver {
    fun expand(url: String, maxRedirects: Int = 5): String {
        require(PlaceLinkParser.isSupportedUrl(url))
        var current = url
        repeat(maxRedirects) {
            var response = requestHeaders(current, "HEAD")
            if (response.first == HttpURLConnection.HTTP_BAD_METHOD) response = requestHeaders(current, "GET")
            val target = response.second
            if (response.first !in 300..399 || target.isNullOrBlank()) return current
            val resolved = URI(current).resolve(target).toString()
            require(PlaceLinkParser.isSupportedUrl(resolved))
            current = resolved
        }
        error("地图短链接重定向次数过多")
    }

    private fun requestHeaders(url: String, method: String): Pair<Int, String?> {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.requestMethod = method
            connection.setRequestProperty("User-Agent", "OtterChecking/0.1 map-link-resolver")
            if (method == "GET") connection.setRequestProperty("Range", "bytes=0-0")
            connection.responseCode to connection.getHeaderField("Location")
        } finally { connection.disconnect() }
    }
}

object MapLinks {
    fun amap(name: String, latitude: Double, longitude: Double): String =
        "https://uri.amap.com/marker?position=${longitude},${latitude}&name=${encode(name)}&src=otter-checking&coordinate=wgs84&callnative=1"

    fun google(name: String, latitude: Double, longitude: Double, placeId: String? = null): String = buildString {
        append("https://www.google.com/maps/search/?api=1&query=")
        append(encode("$latitude,$longitude"))
        if (!placeId.isNullOrBlank()) append("&query_place_id=").append(encode(placeId))
        append("&utm_source=otter-checking&utm_campaign=place_details_search")
    }

    fun amapApp(name: String, latitude: Double, longitude: Double): String =
        "androidamap://viewMap?sourceApplication=otter-checking&poiname=${encode(name)}&lat=$latitude&lon=$longitude&dev=1&style=2"

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
}

internal object GooglePlaceNameExtractor {
    private val japaneseFloor = Regex("(?:\\d+\\s*)?(?:階|F)\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val leadingCountry = Regex("^(?:日本|Japan)\\s*〒?\\d{3}-?\\d{4}\\s*", RegexOption.IGNORE_CASE)
    private val addressWords = Regex("(?:〒\\d{3}-?\\d{4}|\\b(?:street|st\\.?|road|rd\\.?|avenue|ave\\.?|city|district|prefecture|ward)\\b|路|街|道|区|县|縣|市|町|丁目|番地|号)", RegexOption.IGNORE_CASE)

    fun extract(raw: String): String {
        val label = raw.replace('+', ' ').replace(Regex("\\s+"), " ").trim()
        japaneseFloor.find(label)?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)?.let { return it }

        val commaParts = label.split(Regex("[,，]\\s*")).map(String::trim).filter(String::isNotBlank)
        if (commaParts.size >= 2) {
            val first = commaParts.first()
            val rest = commaParts.drop(1).joinToString(" ")
            if (!addressWords.containsMatchIn(first) && addressWords.containsMatchIn(rest)) return first
            val last = commaParts.last()
            if (addressWords.containsMatchIn(commaParts.dropLast(1).joinToString(" ")) && !addressWords.containsMatchIn(last)) return last
        }

        val withoutCountry = leadingCountry.replace(label, "")
        return withoutCountry.ifBlank { label }.take(100)
    }
}

/** Coordinate transform used only for imported AMap GCJ-02 links. Stored coordinates remain WGS-84. */
object CoordinateTransforms {
    private const val A = 6378245.0
    private const val EE = 0.006693421622965943

    fun gcj02ToWgs84(latitude: Double, longitude: Double): Pair<Double, Double> {
        if (outsideChina(latitude, longitude)) return latitude to longitude
        val converted = wgs84ToGcj02(latitude, longitude)
        return latitude * 2 - converted.first to longitude * 2 - converted.second
    }

    private fun wgs84ToGcj02(latitude: Double, longitude: Double): Pair<Double, Double> {
        if (outsideChina(latitude, longitude)) return latitude to longitude
        var dLat = transformLat(longitude - 105.0, latitude - 35.0)
        var dLon = transformLon(longitude - 105.0, latitude - 35.0)
        val radLat = latitude / 180.0 * PI
        var magic = sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = dLat * 180.0 / ((A * (1 - EE)) / (magic * sqrtMagic) * PI)
        dLon = dLon * 180.0 / (A / sqrtMagic * kotlin.math.cos(radLat) * PI)
        return latitude + dLat to longitude + dLon
    }

    private fun outsideChina(lat: Double, lon: Double) = lon < 72.004 || lon > 137.8347 || lat < 0.8293 || lat > 55.8271
    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }
    private fun transformLon(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }
}
