package com.bugchecking.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeocoderAndLocationPolicyTest {
    @Test fun roadAndHouseNumberRankBeforeAdministrativeAreas() {
        val values = GeocoderLabelPolicy.candidates(
            GeocoderAddressParts(
                thoroughfare = "世纪大道",
                subThoroughfare = "1294号",
                subLocality = "浦东新区",
                locality = "上海市",
                adminArea = "上海市",
                addressLine = "上海市浦东新区世纪大道1294号",
            )
        )

        assertEquals("世纪大道1294号", values.first().text)
        assertEquals(GeocoderLabelKind.ROAD_NUMBER, values.first().kind)
        assertTrue(values.indexOfFirst { it.text == "浦东新区" } > values.indexOfFirst { it.text == "世纪大道" })
        assertTrue(values.indexOfFirst { it.text == "上海市" } > values.indexOfFirst { it.text == "浦东新区" })
    }

    @Test fun extractsSpecificPlaceAfterRoadContext() {
        val values = GeocoderLabelPolicy.candidates(
            GeocoderAddressParts(
                featureName = "金汇镇万顺路天和前滩时代",
                thoroughfare = "万顺路",
                subThoroughfare = "2388号",
                subLocality = "奉贤区",
                locality = "上海市",
            )
        )

        assertEquals("天和前滩时代", values.first().text)
        assertEquals(GeocoderLabelKind.FEATURE, values.first().kind)
    }

    @Test fun rejectsAdministrativeCompoundButKeepsRoadNumber() {
        val best = GeocoderLabelPolicy.best(
            listOf(
                GeocoderAddressParts(
                    featureName = "金汇镇万顺路中国(上海)自由贸易试验区临港新片区",
                    thoroughfare = "万顺路",
                    subThoroughfare = "2388号",
                    locality = "上海市",
                )
            )
        )

        assertEquals("万顺路2388号", best?.text)
        assertEquals(GeocoderLabelKind.ROAD_NUMBER, best?.kind)
    }

    @Test fun preservesCompoundHotelNameWithoutSuffixSplitting() {
        val best = GeocoderLabelPolicy.best(
            listOf(GeocoderAddressParts(featureName = "上海外滩老码头亚朵酒店秀水苑"))
        )

        assertEquals("上海外滩老码头亚朵酒店秀水苑", best?.text)
    }

    @Test fun nearbyCandidatesSortBySpecificityBeforeDistance() {
        val values = NearbyPlaceCandidates.prepare(
            listOf(
                RawNearbyPlace("奉贤区", latitude = 31.0001, longitude = 121.0, specificity = 200),
                RawNearbyPlace("世纪大道1294号", latitude = 31.001, longitude = 121.0, specificity = 500),
            ),
            originLatitude = 31.0,
            originLongitude = 121.0,
        )

        assertEquals(listOf("世纪大道1294号", "奉贤区"), values.map { it.name })
    }

    @Test fun locationPolicyBalancesFreshnessAndAccuracy() {
        val freshNetwork = LocationFixQuality(accuracyMeters = 100f, ageMillis = 0L)
        val staleGps = LocationFixQuality(accuracyMeters = 15f, ageMillis = 60_000L)

        assertTrue(LocationFixPolicy.score(freshNetwork) < LocationFixPolicy.score(staleGps))
        assertTrue(LocationFixPolicy.isImmediate(LocationFixQuality(50f, 10_000L)))
        assertFalse(LocationFixPolicy.isImmediate(LocationFixQuality(50f, 10_001L)))
        assertFalse(LocationFixPolicy.isImmediate(LocationFixQuality(50.1f, 0L)))
    }
}
