package com.bugchecking.app

data class TrackFavoriteList(
    val tier: Int,
    val name: String,
) {
    init { require(tier in 1..3) }
}

val trackFavoriteLists = listOf(
    TrackFavoriteList(1, "我的遗产"),
    TrackFavoriteList(2, "不错食堂"),
    TrackFavoriteList(3, "余味绕梁"),
)

fun trackFavoriteList(tier: Int): TrackFavoriteList? = trackFavoriteLists.firstOrNull { it.tier == tier }

internal fun filterFavoriteTracks(
    tracks: List<TrackPoint>,
    selectedTiers: Set<Int>,
    query: String,
): List<TrackPoint> {
    if (selectedTiers.isEmpty()) return emptyList()
    val needle = query.trim()
    return tracks.asSequence()
        .filter { it.favoriteTier in selectedTiers }
        .filter { track ->
            needle.isEmpty() || listOf(track.name, track.originalUrl, track.canonicalUrl)
                .filterNotNull()
                .any { it.contains(needle, ignoreCase = true) }
        }
        .sortedByDescending(TrackPoint::time)
        .toList()
}

/** Pure timing policy shared by the gesture UI and unit tests. */
internal fun favoriteTierAt(elapsedMillis: Long, longPressTimeoutMillis: Long): Int = when {
    elapsedMillis >= longPressTimeoutMillis + 3_000L -> 3
    elapsedMillis >= longPressTimeoutMillis + 2_000L -> 2
    elapsedMillis >= longPressTimeoutMillis + 1_000L -> 1
    else -> 0
}

