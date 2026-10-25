package com.bugchecking.app

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceAndImportTest {
    @Test fun parsesGoogleMapsUrlWithoutNetwork() {
        val value = PlaceLinkParser.parse(
            "故宫博物院\nhttps://www.google.com/maps/search/?api=1&query=39.9163%2C116.3972&query_place_id=abc"
        )
        assertNotNull(value)
        assertEquals(PlaceProvider.GOOGLE, value?.provider)
        assertEquals("故宫博物院", value?.name)
        assertEquals("abc", value?.placeId)
        assertEquals(39.9163, value?.latitude!!, 0.000001)
        assertEquals(116.3972, value.longitude!!, 0.000001)
        assertEquals(PlaceResolutionState.RESOLVED, value.state)
    }

    @Test fun parsesAmapUriAndNormalizesCoordinates() {
        val value = PlaceLinkParser.parse("https://uri.amap.com/marker?position=116.3972,39.9163&name=%E6%95%85%E5%AE%AB&poiid=B0FF")!!
        assertEquals(PlaceProvider.AMAP, value.provider)
        assertEquals("故宫", value.name)
        assertEquals("B0FF", value.placeId)
        assertTrue(value.latitude!! in 39.90..39.93)
        assertTrue(value.longitude!! in 116.38..116.41)
        assertEquals(PlaceResolutionState.RESOLVED, value.state)
    }

    @Test fun usesAmapShareTitleWithoutFollowingTheShortLink() {
        val value = PlaceLinkParser.parse(
            "新健身NEW FITNESS游泳健身瑜伽舞蹈(贤浦路店)\n¥5759/人 · 游泳馆\n沿港河路478号\nhttps://surl.amap.com/3kdhw5fMz8bl"
        )!!
        assertEquals(PlaceProvider.AMAP, value.provider)
        assertEquals("新健身NEW FITNESS游泳健身瑜伽舞蹈(贤浦路店)", value.name)
        assertEquals(PlaceResolutionState.PARTIAL, value.state)
    }

    @Test fun extractsGoogleBusinessNameFromJapaneseAndChineseLabels() {
        assertEquals(
            "M.O.DTOKYO【モッズ】池袋店 エクステ",
            GooglePlaceNameExtractor.extract("日本〒170-0013 Tokyo, Toshima City, Higashiikebukuro, 1 Chome-42-14 2階 M.O.DTOKYO【モッズ】池袋店 エクステ"),
        )
        assertEquals(
            "IAPM Mall",
            GooglePlaceNameExtractor.extract("IAPM Mall, 999 淮海中路徐汇区 200020"),
        )
    }

    @Test fun buildsNativeAmapUriWithWgs84Coordinates() {
        val uri = MapLinks.amapApp("测试地点", 30.1, 121.2)
        assertTrue(uri.startsWith("androidamap://viewMap?"))
        assertTrue(uri.contains("dev=1"))
        assertTrue(uri.contains("lat=30.1&lon=121.2"))
    }

    @Test fun nearbyPlacesAreSortedByDistanceAndDeduplicated() {
        val values = NearbyPlaceCandidates.prepare(
            listOf(
                RawNearbyPlace("远处咖啡", latitude = 31.001, longitude = 121.0),
                RawNearbyPlace("  近处咖啡  ", latitude = 31.0001, longitude = 121.0),
                RawNearbyPlace("近处 咖啡", latitude = 31.0002, longitude = 121.0),
                RawNearbyPlace("没有坐标的地点"),
            ),
            originLatitude = 31.0,
            originLongitude = 121.0,
        )

        assertEquals(listOf("近处咖啡", "远处咖啡", "没有坐标的地点"), values.map { it.name })
        assertTrue(values[0].distanceMeters!! < values[1].distanceMeters!!)
        assertEquals(null, values[2].distanceMeters)
    }

    @Test fun nearbyPlaceDistanceUsesStraightLineMeters() {
        val distance = NearbyPlaceCandidates.distanceMeters(31.0, 121.0, 31.001, 121.0)
        assertTrue(distance in 110..112)
    }

    @Test fun limitsMapRedirectHostsToHttpsProviders() {
        assertTrue(PlaceLinkParser.isSupportedUrl("https://maps.app.goo.gl/abc"))
        assertTrue(PlaceLinkParser.needsRedirect("https://surl.amap.com/abc"))
        assertFalse(PlaceLinkParser.isSupportedUrl("http://www.google.com/maps"))
        assertFalse(PlaceLinkParser.isSupportedUrl("https://google.com.example.org/maps"))
    }

    @Test fun clipboardPromptAcceptsShareTextContainingAUrl() {
        val shared = "外滩源\nhttps://surl.amap.com/example\n值得一去"
        assertEquals(shared, clipboardMapShareCandidate("  $shared  "))
    }

    @Test fun clipboardPromptIgnoresPlainTextAndEmptyContent() {
        assertEquals(null, clipboardMapShareCandidate("外滩源，值得一去"))
        assertEquals(null, clipboardMapShareCandidate("   "))
        assertEquals(null, clipboardMapShareCandidate(null))
    }

    @Test fun parsesQuotedCsvAndChoosesNamedColumn() {
        val rows = ImportEngine.parseDelimited("编号,景点,说明\r\n1,故宫博物院,上午\r\n2,\"颐和园,东宫门\",下午", ',')
        assertEquals("颐和园,东宫门", rows[2][1])
        val candidates = CandidateClassifier.fromTable(rows, "CSV", 1)
        assertEquals(listOf("故宫博物院", "颐和园,东宫门"), candidates.map { it.text })
        assertTrue(candidates.all { it.source.startsWith("CSV · 第") })
    }

    @Test fun filtersNoiseButKeepsLowConfidenceCandidates() {
        val values = CandidateClassifier.fromLines(listOf("第 2 页", "2026-10-15", "¥88", "□ 西湖", "小众地点"), "图片")
        assertEquals(listOf("西湖", "小众地点"), values.map { it.text })
        assertTrue(values.any { it.confidence == ImportConfidence.LOW || it.confidence == ImportConfidence.MEDIUM })
    }

    @Test fun pastedWhitespaceTableSelectsAllDestinationsWithoutEatingDates() {
        val input = """
            日期    目的地
            09.25    万州
            09.26    成都
            09.27    甲居藏寨
            09.28    炉霍
            9.29    新龙
            9.30    巴塘
            10.1    则巴村
            10.2    下则通村
            10.3    雅江
            10.4    新都桥
            10.5    资阳
            10.6    武汉
            10.7    黄山
        """.trimIndent()

        val result = ImportEngine.extractStructuredText(input, "粘贴文本")

        assertEquals("目的地", result.selectedColumn)
        assertEquals(
            listOf("万州", "成都", "甲居藏寨", "炉霍", "新龙", "巴塘", "则巴村", "下则通村", "雅江", "新都桥", "资阳", "武汉", "黄山"),
            result.candidates.map { it.text },
        )
        assertTrue(result.candidates.all { it.source.contains("目的地列") })
    }

    @Test fun pastedTabTableAndNumberedListUseDifferentPrefixRules() {
        val table = ImportEngine.extractStructuredText("日期\t目的地\n09.25\t万州\n10.1\t则巴村", "粘贴文本")
        assertEquals(listOf("万州", "则巴村"), table.candidates.map { it.text })

        val list = CandidateClassifier.fromLines(listOf("1.栗Cafe（长宁馥邦苑店）", "09.25"), "粘贴文本")
        assertEquals(listOf("栗Cafe（长宁馥邦苑店）"), list.map { it.text })
    }

    @Test fun pastedTablesSupportCommonDelimitersButNotSingleSpaces() {
        listOf(',', ';', '|').forEach { delimiter ->
            val result = ImportEngine.extractStructuredText(
                "日期${delimiter}目的地\n09.25${delimiter}万州\n09.26${delimiter}成都",
                "粘贴文本",
            )
            assertEquals(listOf("万州", "成都"), result.candidates.map { it.text })
        }

        val ordinary = ImportEngine.extractStructuredText("M.O.D TOKYO\nIAPM Mall", "粘贴文本")
        assertTrue(ordinary.tables.isEmpty())
        assertEquals(listOf("M.O.D TOKYO", "IAPM Mall"), ordinary.candidates.map { it.text })
    }

    @Test fun aSinglePastedPlaceStillEntersCandidatePreview() {
        val result = ImportEngine.extractStructuredText("朝阳公园", "粘贴文本")
        assertEquals(listOf("朝阳公园"), result.candidates.map { it.text })
    }

    @Test fun switchingTableColumnReusesRowsAndPreservesOrder() {
        val initial = ImportEngine.extractStructuredText(
            "日期\t目的地\t景点\n09.25\t万州\t长江夜景\n09.26\t成都\t大源中央公园",
            "粘贴文本",
        )

        val switched = ImportEngine.selectColumn(initial, "景点")

        assertEquals(listOf("长江夜景", "大源中央公园"), switched.candidates.map { it.text })
        assertEquals("景点", switched.selectedColumn)
        assertTrue(switched.candidates.all { it.source.contains("景点列") })
    }

    @Test fun imageLayoutKeepsProminentItineraryPlacesAndDropsInterfaceText() {
        val lines = listOf(
            OcrLine("泰州", 24, 80, 100),
            OcrLine("04.03周五", 42, 380, 60),
            OcrLine("吃喝", 20, 520, 260),
            OcrLine("1.栗Cafe（长宁馥邦苑店）", 42, 570, 260),
            OcrLine("备注", 22, 640, 260),
            OcrLine("258.6公里·3小时1分钟", 20, 710, 260),
            OcrLine("其他", 20, 820, 260),
            OcrLine("2.栖霞坑村", 40, 870, 260),
            OcrLine("住宿", 20, 1020, 260),
            OcrLine("宁波沄野和光·栖霞坑民宿", 40, 1070, 260),
            OcrLine("问AI", 28, 1500, 700),
        )
        assertEquals(
            listOf("栗Cafe（长宁馥邦苑店）", "栖霞坑村", "宁波沄野和光·栖霞坑民宿"),
            CandidateClassifier.fromOcrLines(lines, "图片").map { it.text },
        )
    }

    @Test fun tableScreenshotRebuildsRowsAndReadsDestinationColumnInOrder() {
        val lines = listOf(
            // Deliberately shuffled like OCR text blocks; geometry, not input order, is authoritative.
            OcrLine("445公里", 24, 300, 330, 70), OcrLine("甲居藏寨", 24, 300, 210, 90),
            OcrLine("黄山", 24, 100, 110, 60), OcrLine("目的地", 26, 100, 210, 80),
            OcrLine("时间", 26, 100, 10, 60), OcrLine("里程", 26, 100, 330, 70),
            OcrLine("出发", 26, 100, 110, 60), OcrLine("09.25", 24, 180, 10, 60),
            OcrLine("万州", 24, 180, 210, 60), OcrLine("1153公里", 24, 180, 330, 80),
            OcrLine("09.26", 24, 240, 10, 60), OcrLine("万州", 24, 240, 110, 60),
            OcrLine("成都", 24, 240, 210, 60), OcrLine("527公里", 24, 240, 330, 70),
            OcrLine("09.27", 24, 300, 10, 60), OcrLine("成都", 24, 300, 110, 60),
            OcrLine("黄山", 24, 180, 110, 60),
        )

        val result = CandidateClassifier.fromOcrLines(lines, "图片")

        assertEquals(listOf("万州", "成都", "甲居藏寨"), result.map { it.text })
        assertTrue(result.all { it.source.contains("目的地列") })
    }

    @Test fun tableScreenshotKeepsEmptyCellsOutOfAdjacentColumns() {
        val recognized = RecognizedText(
            lines = emptyList(),
            elements = listOf(
                OcrLine("日期", 24, 100, 10, 60), OcrLine("目的地", 24, 102, 150, 80), OcrLine("景点", 24, 99, 300, 60),
                OcrLine("09.25", 22, 180, 10, 60), OcrLine("万州", 22, 182, 150, 60), OcrLine("长江夜景", 22, 179, 300, 90),
                OcrLine("09.26", 22, 240, 10, 60), OcrLine("大源中央公园", 22, 241, 300, 110),
                OcrLine("09.27", 22, 300, 10, 60), OcrLine("成都", 22, 302, 150, 60),
            ),
        )

        val result = CandidateClassifier.fromOcrText(recognized, "图片")

        assertEquals(listOf("万州", "成都"), result.candidates.map { it.text })
        assertEquals(listOf("日期", "目的地", "景点"), result.availableColumns)
    }

    @Test fun clustersDensePointsButLeavesDistantPointSeparate() {
        fun track(id: Long, lat: Double, lon: Double) = TrackPoint(id, "$id", null, "地点$id", lat, lon, 0, 5f)
        val result = clusterTracks(
            listOf(track(1, 39.91630, 116.39720), track(2, 39.91631, 116.39721), track(3, 31.2304, 121.4737)),
            zoom = 12.0,
        )
        assertEquals(2, result.size)
        assertTrue(result.any { it.tracks.size == 2 })
    }

    @Test fun connectsOnlyTracksFromTheSameDayInTimeOrder() {
        fun track(id: Long, instant: String) = TrackPoint(id, "$id", null, "地点$id", 30.0 + id, 120.0, Instant.parse(instant).toEpochMilli(), 5f)
        val segments = dailyTrackSegments(
            listOf(
                track(2, "2026-10-15T03:00:00Z"), track(1, "2026-10-15T01:00:00Z"),
                track(3, "2026-10-16T01:00:00Z"),
            ),
            ZoneId.of("UTC"),
        )
        val drawableSegments = segments.filter { it.tracks.size > 1 }
        assertEquals(1, drawableSegments.size)
        assertEquals(listOf(1L, 2L), drawableSegments.single().tracks.map { it.id })
        assertEquals(listOf(3L), segments.single { it.tracks.size == 1 }.tracks.map { it.id })
    }
}
