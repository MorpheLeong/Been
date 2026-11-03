package com.bugchecking.app

import java.net.URI
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class SavedPlace(
    val id: String,
    val name: String,
    val favoriteTier: Int,
    val provider: PlaceProvider,
    val providerPlaceId: String? = null,
    val originalUrl: String? = null,
    val canonicalUrl: String? = null,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long,
    val updatedAt: Long,
    val visitCount: Int = 0,
    val lastVisitedAt: Long? = null,
) {
    init { require(favoriteTier in 0..3) }
}

enum class PlaceMatchConfidence { EXACT_IDENTIFIER, NAME_AND_DISTANCE }

data class SavedPlaceMatch(
    val place: SavedPlace,
    val confidence: PlaceMatchConfidence,
)

internal fun normalizePlaceName(value: String): String = value
    .trim()
    .lowercase(Locale.ROOT)
    .filter(Char::isLetterOrDigit)

internal fun normalizePlaceUrl(value: String?): String? {
    val raw = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
    return runCatching {
        val uri = URI(raw)
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return@runCatching raw
        val host = uri.host?.lowercase(Locale.ROOT) ?: return@runCatching raw
        val path = uri.rawPath.orEmpty().removeSuffix("/")
        URI(scheme, uri.userInfo, host, uri.port, path.ifEmpty { null }, uri.rawQuery, null).toASCIIString()
    }.getOrDefault(raw)
}

internal fun placeDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val latDelta = (lat2 - lat1) * PI / 180.0
    val lonDelta = (lon2 - lon1) * PI / 180.0
    val first = lat1 * PI / 180.0
    val second = lat2 * PI / 180.0
    val a = sin(latDelta / 2) * sin(latDelta / 2) +
        cos(first) * cos(second) * sin(lonDelta / 2) * sin(lonDelta / 2)
    return 6_371_000.0 * 2 * atan2(sqrt(a), sqrt(1 - a))
}

/**
 * Stable provider identifiers and canonical URLs win. Name matching is only accepted when the
 * normalized name is identical, the visit is close to the saved anchor, and exactly one place
 * qualifies. This deliberately refuses ambiguous same-building matches.
 */
internal fun findSavedPlaceMatch(track: TrackPoint, places: List<SavedPlace>): SavedPlaceMatch? {
    val providerPlaceId = track.placeId?.trim()?.takeIf(String::isNotEmpty)
    if (providerPlaceId != null) {
        places.firstOrNull {
            it.provider == track.provider && it.providerPlaceId == providerPlaceId
        }?.let { return SavedPlaceMatch(it, PlaceMatchConfidence.EXACT_IDENTIFIER) }
    }

    val urls = sequenceOf(track.canonicalUrl, track.originalUrl)
        .mapNotNull(::normalizePlaceUrl)
        .toSet()
    if (urls.isNotEmpty()) {
        places.firstOrNull { place ->
            sequenceOf(place.canonicalUrl, place.originalUrl)
                .mapNotNull(::normalizePlaceUrl)
                .any(urls::contains)
        }?.let { return SavedPlaceMatch(it, PlaceMatchConfidence.EXACT_IDENTIFIER) }
    }

    val normalizedName = normalizePlaceName(track.name)
    if (normalizedName.isEmpty()) return null
    val distanceLimit = (track.accuracy.toDouble() * 1.5).coerceIn(60.0, 150.0)
    val candidates = places.filter {
        normalizePlaceName(it.name) == normalizedName &&
            placeDistanceMeters(track.lat, track.lon, it.latitude, it.longitude) <= distanceLimit
    }
    return candidates.singleOrNull()?.let {
        SavedPlaceMatch(it, PlaceMatchConfidence.NAME_AND_DISTANCE)
    }
}

internal fun savedPlaceLinksForUnlinkedTracks(
    tracks: List<TrackPoint>,
    places: List<SavedPlace>,
): Map<Long, String> = tracks.asSequence()
    .filter { it.savedPlaceId == null }
    .mapNotNull { track ->
        findSavedPlaceMatch(track, places)?.place?.id?.let { placeId -> track.id to placeId }
    }
    .toMap()

internal fun filterSavedPlaces(
    places: List<SavedPlace>,
    selectedTiers: Set<Int>,
    query: String,
): List<SavedPlace> {
    if (selectedTiers.isEmpty()) return emptyList()
    val needle = query.trim()
    return places.asSequence()
        .filter { it.favoriteTier in selectedTiers }
        .filter { place ->
            needle.isEmpty() || listOf(place.name, place.originalUrl, place.canonicalUrl, place.providerPlaceId)
                .filterNotNull()
                .any { it.contains(needle, ignoreCase = true) }
        }
        .sortedWith(compareByDescending<SavedPlace> { it.lastVisitedAt ?: Long.MIN_VALUE }.thenBy { it.name })
        .toList()
}
