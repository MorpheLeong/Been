package com.bugchecking.app

import android.location.Address

internal data class GeocoderAddressParts(
    val featureName: String? = null,
    val premises: String? = null,
    val thoroughfare: String? = null,
    val subThoroughfare: String? = null,
    val subLocality: String? = null,
    val locality: String? = null,
    val adminArea: String? = null,
    val countryName: String? = null,
    val addressLine: String? = null,
)

internal enum class GeocoderLabelKind(val specificity: Int, val showNearby: Boolean = true) {
    FEATURE(600),
    PREMISES(550),
    ROAD_NUMBER(500),
    ROAD(400),
    FULL_ADDRESS(300, showNearby = false),
    SUB_LOCALITY(200),
    LOCALITY(100),
    ADMIN_AREA(50),
}

internal data class GeocoderLabelCandidate(
    val text: String,
    val kind: GeocoderLabelKind,
)

internal data class GeocoderAddressLabel(
    val address: Address,
    val candidate: GeocoderLabelCandidate,
)

internal object GeocoderLabelPolicy {
    private val administrativePhrase = Regex(
        "(?:自由贸易试验区|新片区|经济开发区|高新技术产业开发区|街道办事处|居民委员会|村民委员会)$"
    )

    fun candidates(parts: GeocoderAddressParts): List<GeocoderLabelCandidate> {
        val road = normalize(parts.thoroughfare)
        val number = normalize(parts.subThoroughfare)
        val roadNumber = when {
            road == null -> null
            number == null || road.contains(number) -> road
            else -> road + number
        }
        val administrativeValues = listOf(
            parts.countryName,
            parts.adminArea,
            parts.locality,
            parts.subLocality,
        ).mapNotNull(::normalize).toSet()

        val values = buildList {
            normalizeSpecific(parts.featureName, road, number, administrativeValues)?.let {
                add(GeocoderLabelCandidate(it, GeocoderLabelKind.FEATURE))
            }
            normalizeSpecific(parts.premises, road, number, administrativeValues)?.let {
                add(GeocoderLabelCandidate(it, GeocoderLabelKind.PREMISES))
            }
            roadNumber?.let {
                add(GeocoderLabelCandidate(it, if (number == null) GeocoderLabelKind.ROAD else GeocoderLabelKind.ROAD_NUMBER))
            }
            if (road != null && road != roadNumber) add(GeocoderLabelCandidate(road, GeocoderLabelKind.ROAD))
            normalize(parts.addressLine)?.let { add(GeocoderLabelCandidate(it, GeocoderLabelKind.FULL_ADDRESS)) }
            normalize(parts.subLocality)?.let { add(GeocoderLabelCandidate(it, GeocoderLabelKind.SUB_LOCALITY)) }
            normalize(parts.locality)?.let { add(GeocoderLabelCandidate(it, GeocoderLabelKind.LOCALITY)) }
            normalize(parts.adminArea)?.let { add(GeocoderLabelCandidate(it, GeocoderLabelKind.ADMIN_AREA)) }
        }
        return values
            .filter { it.text.length <= 100 && it.text.any(Char::isLetter) }
            .distinctBy { normalizeKey(it.text) }
            .sortedByDescending { it.kind.specificity }
    }

    fun best(parts: Iterable<GeocoderAddressParts>): GeocoderLabelCandidate? =
        parts.flatMap(::candidates)
            .distinctBy { normalizeKey(it.text) }
            .maxByOrNull { it.kind.specificity }

    private fun normalizeSpecific(
        value: String?,
        road: String?,
        number: String?,
        administrativeValues: Set<String>,
    ): String? {
        val normalized = normalize(value) ?: return null
        if (normalized in administrativeValues || administrativePhrase.containsMatchIn(normalized)) return null

        val afterRoad = road?.let { roadName ->
            normalized.indexOf(roadName).takeIf { it >= 0 }
                ?.let { index -> normalized.substring(index + roadName.length).trim() }
        }
        val concise = afterRoad?.takeIf { it.isNotBlank() } ?: normalized
        if (concise == number || concise in administrativeValues || administrativePhrase.containsMatchIn(concise)) return null
        return concise
    }

    private fun normalize(value: String?): String? = value
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    private fun normalizeKey(value: String): String = value
        .lowercase()
        .replace(Regex("[\\s·・,，()（）-]+"), "")
}

internal fun Address.toGeocoderAddressParts(): GeocoderAddressParts = GeocoderAddressParts(
    featureName = featureName,
    premises = premises,
    thoroughfare = thoroughfare,
    subThoroughfare = subThoroughfare,
    subLocality = subLocality,
    locality = locality,
    adminArea = adminArea,
    countryName = countryName,
    addressLine = getAddressLine(0),
)

internal fun Address.toRawNearbyPlaces(): List<RawNearbyPlace> =
    GeocoderLabelPolicy.candidates(toGeocoderAddressParts())
        .filter { it.kind.showNearby }
        .map { candidate ->
            RawNearbyPlace(
                name = candidate.text,
                address = getAddressLine(0),
                latitude = if (hasLatitude()) latitude else null,
                longitude = if (hasLongitude()) longitude else null,
                specificity = candidate.kind.specificity,
            )
        }

internal fun Iterable<Address>.bestGeocoderLabel(): GeocoderAddressLabel? =
    flatMap { address ->
        GeocoderLabelPolicy.candidates(address.toGeocoderAddressParts())
            .map { candidate -> GeocoderAddressLabel(address, candidate) }
    }.maxByOrNull { it.candidate.kind.specificity }
