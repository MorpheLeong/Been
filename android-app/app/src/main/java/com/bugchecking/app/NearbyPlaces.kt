package com.bugchecking.app

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class RawNearbyPlace(
    val name: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class NearbyPlaceCandidate(
    val name: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val distanceMeters: Int?,
)

object NearbyPlaceCandidates {
    fun prepare(
        values: List<RawNearbyPlace>,
        originLatitude: Double,
        originLongitude: Double,
        limit: Int = 10,
    ): List<NearbyPlaceCandidate> {
        if (limit <= 0) return emptyList()
        return values.mapNotNull { value ->
            val name = value.name.replace(Regex("\\s+"), " ").trim().take(100)
            if (name.isBlank()) return@mapNotNull null
            val distance = if (
                value.latitude != null && value.longitude != null &&
                value.latitude in -90.0..90.0 && value.longitude in -180.0..180.0
            ) distanceMeters(originLatitude, originLongitude, value.latitude, value.longitude) else null
            NearbyPlaceCandidate(name, value.address, value.latitude, value.longitude, distance)
        }.sortedWith(compareBy<NearbyPlaceCandidate> { it.distanceMeters == null }.thenBy { it.distanceMeters ?: Int.MAX_VALUE })
            .distinctBy { it.name.lowercase().replace(Regex("[\\s·・,，()（）-]+"), "") }
            .take(limit)
    }

    internal fun distanceMeters(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Int {
        val earthRadiusMeters = 6_371_000.0
        val lat1 = Math.toRadians(fromLat)
        val lat2 = Math.toRadians(toLat)
        val deltaLat = lat2 - lat1
        val deltaLon = Math.toRadians(toLon - fromLon)
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
        return (2 * earthRadiusMeters * asin(sqrt(a.coerceIn(0.0, 1.0)))).roundToInt()
    }
}
