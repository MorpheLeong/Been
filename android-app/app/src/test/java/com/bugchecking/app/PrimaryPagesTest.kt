package com.bugchecking.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar

class PrimaryPagesTest {
    @Test fun reconnectPolicyBoundsEachCycleToThreeAttemptsWithinOneMinute() {
        assertEquals(2_000L, reconnectDelayMs(0))
        assertEquals(8_000L, reconnectDelayMs(1))
        assertNull(reconnectDelayMs(2))
        assertEquals(2_000L, reconnectDelayMs(-1))
    }

    @Test fun deviceActionMapsConnectionAndSyncStates() {
        assertEquals(
            DeviceActionState.DISCONNECTED,
            resolveDeviceActionState(false, PassportSyncStatus.DISCONNECTED, "未连接"),
        )
        assertEquals(
            DeviceActionState.CONNECTING,
            resolveDeviceActionState(false, PassportSyncStatus.DISCONNECTED, "正在查找 Passport"),
        )
        assertEquals(
            DeviceActionState.SYNCING,
            resolveDeviceActionState(true, PassportSyncStatus.SYNCING, "正在同步"),
        )
        assertEquals(
            DeviceActionState.FAILED,
            resolveDeviceActionState(true, PassportSyncStatus.FAILED, "同步失败"),
        )
    }

    @Test fun syncStatusDistinguishesAllUserVisibleStates() {
        assertEquals(PassportSyncStatus.DISCONNECTED, resolveSyncStatus(false, "未连接", true))
        assertEquals(PassportSyncStatus.LOCAL_SAVED, resolveSyncStatus(true, "已安全连接", false))
        assertEquals(PassportSyncStatus.PENDING, resolveSyncStatus(true, "已安全连接", true))
        assertEquals(PassportSyncStatus.SYNCING, resolveSyncStatus(true, "正在写入", true))
        assertEquals(PassportSyncStatus.SYNCED, resolveSyncStatus(true, "清单、二维码、头像和设备设置已同步", true))
        assertEquals(PassportSyncStatus.FAILED, resolveSyncStatus(true, "同步失败，请重试", true))
    }

    @Test fun generatedQrRoundTripsUnicodePayload() {
        val payload = "BugChecking：栗Cafe（长宁馥邦苑店）"
        val matrix = QrCodeImage.encode(payload, 512)
        val pixels = IntArray(matrix.width * matrix.height) { index ->
            if (matrix[index % matrix.width, index / matrix.width]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val decoded = QRCodeReader().decode(
            BinaryBitmap(HybridBinarizer(RGBLuminanceSource(matrix.width, matrix.height, pixels))),
        )
        assertEquals(payload, decoded.text)
    }

    @Test fun qrDisplayOrderPinsStarsAndRestoresCreationOrder() {
        val original = listOf(
            PassportQr("a", "第一张", "a", starred = false, createdOrder = 1),
            PassportQr("b", "第二张", "b", starred = true, createdOrder = 2, starredOrder = 2),
            PassportQr("c", "第三张", "c", starred = true, createdOrder = 3, starredOrder = 1),
            PassportQr("d", "第四张", "d", starred = false, createdOrder = 4),
        )
        assertEquals(listOf("c", "b", "a", "d"), orderQrsForDisplay(original).map { it.id })

        val newlyStarred = original.map {
            if (it.id == "a") it.copy(starred = true, starredOrder = 3) else it
        }
        assertEquals(listOf("c", "b", "a", "d"), orderQrsForDisplay(newlyStarred).map { it.id })

        val unstarredAgain = newlyStarred.map {
            if (it.id == "a") it.copy(starred = false, starredOrder = null) else it
        }
        assertEquals(listOf("c", "b", "a", "d"), orderQrsForDisplay(unstarredAgain).map { it.id })
    }

    @Test fun checklistProgressIsAlwaysBounded() {
        assertEquals(0f, checklistProgress(0, 0))
        assertEquals(0.5f, checklistProgress(2, 4))
        assertEquals(1f, checklistProgress(7, 4))
        assertEquals(0, checklistProgressPercent(0, 0))
        assertEquals(33, checklistProgressPercent(1, 3))
        assertEquals(67, checklistProgressPercent(2, 3))
        assertEquals(100, checklistProgressPercent(7, 4))
    }

    @Test fun dragReorderTargetsOnlyTheCurrentGroup() {
        val all = listOf("pending-a", "done-a", "pending-b", "done-b")
        val pending = listOf("pending-a", "pending-b")

        assertEquals(0 to 2, reorderIndicesWithinGroup(all, pending, "pending-a", 1))
        assertNull(reorderIndicesWithinGroup(all, pending, "pending-a", 0))
        assertNull(reorderIndicesWithinGroup(all, pending, "done-a", 1))
    }

    @Test fun manualTrackDraftValidatesCoordinates() {
        ManualTrackDraft("外滩", 31.23, 121.47, 1L, 8f).requireValid()
        assertThrows(IllegalArgumentException::class.java) {
            ManualTrackDraft("外滩", 91.0, 121.47, 1L, 0f).requireValid()
        }
        assertThrows(IllegalArgumentException::class.java) {
            ManualTrackDraft("外滩", 31.23, -181.0, 1L, 0f).requireValid()
        }
    }

    @Test fun trackEditorModesShareOneEntityLanguageButKeepSourceCapabilities() {
        assertEquals("添加轨迹", TrackEditorMode.CREATE_MANUAL.title())
        assertEquals("编辑轨迹", TrackEditorMode.EDIT_MANUAL.title())
        assertEquals("编辑轨迹", TrackEditorMode.EDIT_CHECK_IN.title())
        assertEquals("保存更改", TrackEditorMode.EDIT_CHECK_IN.primaryActionLabel())

        val checkIn = TrackEditorMode.EDIT_CHECK_IN.capabilities()
        assertEquals(false, checkIn.canEditTimestamp)
        assertEquals(false, checkIn.canEditCoordinates)
        assertEquals(true, checkIn.canDelete)

        val create = TrackEditorMode.CREATE_MANUAL.capabilities()
        assertEquals(true, create.canEditTimestamp)
        assertEquals(true, create.canEditCoordinates)
        assertEquals(true, create.autoLocateOnOpen)
        assertEquals(false, create.canDelete)
    }

    @Test fun trackEditorFieldPairsStackForNarrowOrLargeTextLayouts() {
        assertEquals(false, shouldStackTrackFields(maxWidthDp = 400f, fontScale = 1f))
        assertEquals(true, shouldStackTrackFields(maxWidthDp = 359f, fontScale = 1f))
        assertEquals(true, shouldStackTrackFields(maxWidthDp = 400f, fontScale = 2f))
    }

    @Test fun longTrackEditorValuesUseATrailingEllipsisOnlyWhenNeeded() {
        assertEquals("外滩", collapsedEditorValue("外滩", maxCharacters = 6))
        assertEquals("上海外…", collapsedEditorValue("上海外滩老码头", maxCharacters = 4))
        assertEquals(18, collapsedEditorCharacterLimit(fontScale = 1f))
        assertEquals(12, collapsedEditorCharacterLimit(fontScale = 1.5f))
        assertEquals(9, collapsedEditorCharacterLimit(fontScale = 2f))
    }

    @Test fun trackHomeSignatureTargetsAndCardTitleContractStayStable() {
        assertEquals(48.dp, TrackHomeActionTokens.signatureTargetSize)
        assertEquals(48.dp, PrimaryTopActionTokens.targetSize)
        assertEquals(8.dp, PrimaryTopActionTokens.safeAreaGap)
        assertEquals(64.dp, PrimaryTopActionTokens.primaryContentBreathingRoom)
        assertEquals(16.dp, PrimaryTopActionTokens.detailContentGap)
        assertEquals(8.dp, PrimaryTopActionTokens.minimumGroupGap)
        assertTrue(TrackHomeActionTokens.safeBottomReserve >= 72.dp)
        assertEquals(1, TrackHomeActionTokens.cardTitleMaxLines)
    }

    @Test fun primaryNavigationMovesMeToTheAvatarAction() {
        assertEquals(listOf(Tab.Tracks, Tab.Checklist, Tab.Qr), primaryNavigationTabs)
        assertTrue(Tab.Me !in primaryNavigationTabs)
    }

    @Test fun deviceCapsulePreservesTheCenteredTrackActions() {
        assertEquals(108f, deviceCapsuleMaxWidthDp(328f, true, 132f))
        assertEquals(132f, deviceCapsuleMaxWidthDp(328f, false, 132f))
        assertEquals(220f, deviceCapsuleMaxWidthDp(792f, true, 220f))
        assertEquals(48f, deviceCapsuleMaxWidthDp(160f, true, 132f))
    }

    @Test fun unknownAccuracyUsesManualCoordinateLabel() {
        val manual = TrackPoint(1, "manual-1", null, "外滩", 31.23, 121.47, 1L, 0f)
        assertEquals(null, trackPrecisionLabel(0f))
        assertEquals("精度 30\u00A0m", trackPrecisionLabel(30f))
        assertEquals("手动坐标", trackAccuracyLabel(manual))
        assertEquals("精度 30\u00A0m", trackAccuracyLabel(manual.copy(accuracy = 30f)))
    }

    @Test fun mapTrackLinesAreSplitByLocalDateAndOrderedWithinEachDay() {
        val zone = ZoneId.of("Asia/Shanghai")
        fun point(id: Long, day: Int, hour: Int) = TrackPoint(
            id = id,
            requestId = "track-$id",
            destinationId = null,
            name = "地点$id",
            lat = 31.0 + id / 1000.0,
            lon = 121.0 + id / 1000.0,
            time = ZonedDateTime.of(2026, 10, day, hour, 0, 0, 0, zone).toInstant().toEpochMilli(),
            accuracy = 10f,
        )
        val segments = dailyTrackSegments(
            listOf(point(3, 20, 9), point(2, 19, 18), point(1, 19, 8)),
            zone,
        )

        assertEquals(listOf(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 20)), segments.map { it.date })
        assertEquals(listOf(1L, 2L), segments[0].tracks.map { it.id })
        assertEquals(listOf(3L), segments[1].tracks.map { it.id })
        assertEquals(2, segments.size)
    }

    @Test fun mapRouteGeoJsonKeepsOrderedGeographicVertices() {
        val zone = ZoneId.of("UTC")
        fun point(id: Long, time: String, lat: Double, lon: Double) = TrackPoint(
            id = id,
            requestId = "track-$id",
            destinationId = null,
            name = "地点$id",
            lat = lat,
            lon = lon,
            time = ZonedDateTime.parse(time).toInstant().toEpochMilli(),
            accuracy = 10f,
        )
        val collection = trackRouteFeatureCollection(
            listOf(
                point(2, "2026-10-19T18:00:00Z", 31.2, 121.2),
                point(1, "2026-10-19T08:00:00Z", 31.1, 121.1),
                point(3, "2026-10-20T08:00:00Z", 31.3, 121.3),
            ),
            zone,
        )

        val line = collection.features().orEmpty().single().geometry() as org.maplibre.geojson.LineString
        assertEquals(listOf(121.1, 121.2), line.coordinates().map { it.longitude() })
        assertEquals(listOf(31.1, 31.2), line.coordinates().map { it.latitude() })
    }

    @Test fun reorderHelperMovesOneStableIdWithoutDroppingItems() {
        assertEquals(listOf("b", "c", "a"), moveChecklistId(listOf("a", "b", "c"), "a", "c"))
        assertEquals(listOf("a", "b", "c"), moveChecklistId(listOf("a", "b", "c"), "x", "c"))
    }

    @Test fun wheelDateAndTimePreserveSecondPrecision() {
        assertEquals(29, daysInMonth(2024, 2))
        assertEquals(28, daysInMonth(2025, 2))
        val initial = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 19, 19, 56, 22)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val changed = applyPickerTime(applyPickerDate(initial, 2027, 2, 3), 8, 9, 47)
        val result = Calendar.getInstance().apply { timeInMillis = changed }
        assertEquals(2027, result.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, result.get(Calendar.MONTH))
        assertEquals(3, result.get(Calendar.DAY_OF_MONTH))
        assertEquals(8, result.get(Calendar.HOUR_OF_DAY))
        assertEquals(9, result.get(Calendar.MINUTE))
        assertEquals(47, result.get(Calendar.SECOND))
    }

    @Test fun favoriteHoldTimingStartsOneSecondAfterSystemLongPress() {
        val systemLongPress = 500L
        assertEquals(0, favoriteTierAt(499L, systemLongPress))
        assertEquals(0, favoriteTierAt(1_499L, systemLongPress))
        assertEquals(1, favoriteTierAt(1_500L, systemLongPress))
        assertEquals(1, favoriteTierAt(2_499L, systemLongPress))
        assertEquals(2, favoriteTierAt(2_500L, systemLongPress))
        assertEquals(2, favoriteTierAt(3_499L, systemLongPress))
        assertEquals(3, favoriteTierAt(3_500L, systemLongPress))
        assertEquals(3, favoriteTierAt(30_000L, systemLongPress))
    }

    @Test fun favoriteFiltersByMultipleListsNameAndStoredUrls() {
        fun favorite(
            id: Long,
            name: String,
            tier: Int,
            time: Long,
            originalUrl: String? = null,
            canonicalUrl: String? = null,
        ) = TrackPoint(
            id, "track-$id", null, name, 31.0, 121.0, time, 10f,
            originalUrl = originalUrl,
            canonicalUrl = canonicalUrl,
            favoriteTier = tier,
        )
        val tracks = listOf(
            favorite(1, "栗Cafe", 1, 100, originalUrl = "https://amap.com/place/coffee"),
            favorite(2, "外滩", 3, 300, canonicalUrl = "https://maps.google.com/BUND"),
            favorite(3, "面馆", 2, 200),
            favorite(4, "未收藏", 0, 400),
        )

        assertEquals(listOf(2L, 3L, 1L), filterFavoriteTracks(tracks, setOf(1, 2, 3), "").map { it.id })
        assertEquals(listOf(3L, 1L), filterFavoriteTracks(tracks, setOf(1, 2), "").map { it.id })
        assertEquals(listOf(1L), filterFavoriteTracks(tracks, setOf(1), "cafe").map { it.id })
        assertEquals(listOf(2L), filterFavoriteTracks(tracks, setOf(3), "bund").map { it.id })
        assertEquals(emptyList<Long>(), filterFavoriteTracks(tracks, emptySet(), "").map { it.id })
    }

    @Test fun avatarGestureKeepsTheTouchedImagePointUnderTheFingers() {
        val viewport = IntSize(400, 400)
        val centroid = Offset(300f, 240f)
        val next = AvatarImage.offsetAfterGesture(
            viewport = viewport,
            currentScale = 1f,
            nextScale = 2f,
            currentOffset = Offset.Zero,
            centroid = centroid,
            pan = Offset.Zero,
        )
        assertEquals(Offset(-100f, -40f), next)
    }

    @Test fun backupCatchUpRunsWhenMissingOrDailyIntervalElapsed() {
        val now = 10L * BACKUP_INTERVAL_MILLIS
        assertEquals(true, isBackupOverdue(0L, now))
        assertEquals(false, isBackupOverdue(now - BACKUP_INTERVAL_MILLIS + 1L, now))
        assertEquals(true, isBackupOverdue(now - BACKUP_INTERVAL_MILLIS, now))
    }

    @Test fun automaticBackupChoosesDailyBeforePendingChange() {
        val now = 10L * BACKUP_INTERVAL_MILLIS
        assertEquals(
            BackupKind.DAILY,
            automaticBackupKind(8, 7, now - BACKUP_INTERVAL_MILLIS, now),
        )
        assertEquals(BackupKind.LATEST, automaticBackupKind(8, 7, now, now))
        assertNull(automaticBackupKind(8, 8, now, now))
        assertEquals(BackupReason.DATA_CHANGE, defaultBackupReason(BackupKind.LATEST))
        assertEquals(BackupReason.DAILY_SCHEDULE, defaultBackupReason(BackupKind.DAILY))
    }

    @Test fun backupTrackingCoversEveryMutableDataTable() {
        val statements = backupTrackingTriggerStatements()
        assertEquals(18, statements.size)
        listOf("tracks", "places", "destinations", "qrs", "profile", "device_settings").forEach { table ->
            listOf("INSERT", "UPDATE", "DELETE").forEach { action ->
                assertEquals(1, statements.count { "AFTER $action ON $table" in it })
            }
        }
    }

    @Test fun placeMatchingPrefersStableIdentityAndRejectsAmbiguousNames() {
        fun place(id: String, name: String, lat: Double = 31.0, lon: Double = 121.0) = SavedPlace(
            id = id,
            name = name,
            favoriteTier = 2,
            provider = PlaceProvider.AMAP,
            providerPlaceId = if (id == "stable") "B001" else null,
            canonicalUrl = if (id == "url") "https://uri.amap.com/marker?poiid=B002" else null,
            latitude = lat,
            longitude = lon,
            createdAt = 1,
            updatedAt = 1,
        )
        val stable = place("stable", "旧名称")
        val byId = TrackPoint(
            1, "track-1", null, "完全不同的名称", 0.0, 0.0, 1, 10f,
            provider = PlaceProvider.AMAP,
            placeId = "B001",
        )
        assertEquals("stable", findSavedPlaceMatch(byId, listOf(stable))?.place?.id)
        assertEquals(PlaceMatchConfidence.EXACT_IDENTIFIER, findSavedPlaceMatch(byId, listOf(stable))?.confidence)

        val byUrl = TrackPoint(
            2, "track-2", null, "其他名称", 0.0, 0.0, 1, 10f,
            canonicalUrl = "https://uri.amap.com/marker?poiid=B002#ignored",
        )
        assertEquals("url", findSavedPlaceMatch(byUrl, listOf(place("url", "链接地点")))?.place?.id)

        val byName = TrackPoint(3, "track-3", null, "同名餐厅", 31.0001, 121.0001, 1, 20f)
        assertEquals("near", findSavedPlaceMatch(byName, listOf(place("near", "同名餐厅")))?.place?.id)
        assertNull(findSavedPlaceMatch(byName, listOf(place("near", "同名餐厅"), place("also", "同名餐厅", 31.0002, 121.0002))))
    }

    @Test fun placeLibrarySearchesNameAndMapIdentity() {
        val places = listOf(
            SavedPlace("a", "青禾餐厅", 1, PlaceProvider.AMAP, canonicalUrl = "https://amap.com/a",
                latitude = 31.0, longitude = 121.0, createdAt = 1, updatedAt = 1, lastVisitedAt = 5),
            SavedPlace("b", "咖啡馆", 3, PlaceProvider.GOOGLE, providerPlaceId = "coffee-id",
                latitude = 31.1, longitude = 121.1, createdAt = 1, updatedAt = 1, lastVisitedAt = 8),
        )
        assertEquals(listOf("a"), filterSavedPlaces(places, setOf(1, 2, 3), "青禾").map(SavedPlace::id))
        assertEquals(listOf("b"), filterSavedPlaces(places, setOf(3), "coffee-id").map(SavedPlace::id))
        assertEquals(emptyList<String>(), filterSavedPlaces(places, emptySet(), "").map(SavedPlace::id))
    }

    @Test fun newlySavedPlaceBackfillsUnlinkedHistoricalTracksWithoutOverridingExistingLinks() {
        val place = SavedPlace(
            "saved", "同一家店", 2, PlaceProvider.AMAP,
            latitude = 31.0, longitude = 121.0, createdAt = 1, updatedAt = 1,
        )
        val tracks = listOf(
            TrackPoint(1, "one", null, "同一家店", 31.0001, 121.0001, 1, 20f),
            TrackPoint(2, "two", null, "另一家店", 31.0001, 121.0001, 1, 20f),
            TrackPoint(
                3, "three", null, "同一家店", 31.0001, 121.0001, 1, 20f,
                savedPlaceId = "existing",
            ),
        )

        assertEquals(mapOf(1L to "saved"), savedPlaceLinksForUnlinkedTracks(tracks, listOf(place)))
    }

    @Test fun historicalBackfillStillRejectsAmbiguousSameNamePlaces() {
        fun place(id: String, lat: Double, lon: Double) = SavedPlace(
            id, "同名场所", 1, PlaceProvider.AMAP,
            latitude = lat, longitude = lon, createdAt = 1, updatedAt = 1,
        )
        val track = TrackPoint(4, "four", null, "同名场所", 31.0, 121.0, 1, 30f)

        assertEquals(
            emptyMap<Long, String>(),
            savedPlaceLinksForUnlinkedTracks(
                listOf(track),
                listOf(place("first", 31.0, 121.0), place("second", 31.0001, 121.0001)),
            ),
        )
    }

    @Test fun dailyRetentionNeverDeletesLayeredOrManualBackups() {
        val daily = (1..32).map { index ->
            BackupFileRecord("daily-$index", "BugChecking-daily-$index.bugchecking.zip", index.toLong())
        }
        val files = daily + listOf(
            BackupFileRecord("latest", "BugChecking-latest.bugchecking.zip", 1),
            BackupFileRecord("previous", "BugChecking-previous.bugchecking.zip", 1),
            BackupFileRecord("manual", "BugChecking-manual-1.bugchecking.zip", 1),
            BackupFileRecord("milestone", "BugChecking-milestone-before-restore-1.bugchecking.zip", 1),
        )
        assertEquals(
            listOf("daily-2", "daily-1"),
            backupsToPrune(files, "BugChecking-daily-", 30).map { it.id },
        )
    }

    @Test fun archiveChecksumCoversManifestAndAvatar() {
        val manifest = "manifest".toByteArray()
        val checksum = archiveChecksum(manifest, null)
        assertEquals(checksum, archiveChecksum(manifest, null))
        assertTrue(checksum.matches(Regex("[0-9a-f]{64}")))
        assertTrue(archiveChecksum(manifest, null) != archiveChecksum(manifest, "avatar".toByteArray()))
        assertTrue(archiveChecksum(manifest, null) != archiveChecksum("changed".toByteArray(), null))
    }

    @Test fun backupValidationRejectsDanglingQrAndInvalidFavoriteTier() {
        fun snapshot(destinationQrId: String?, favoriteTier: Int) = PassportBackupSnapshot(
            profile = PassportProfile("机主", "简介", listOf("account")),
            soundVolume = 50,
            screenTimeoutSeconds = 60,
            destinations = listOf(Destination("destination", "地点", 0, false, destinationQrId)),
            qrs = listOf(PassportQr("qr", "凭证", "payload", false, createdOrder = 1)),
            tracks = listOf(
                TrackPoint(1, "track", null, "地点", 31.0, 121.0, 1L, 10f, favoriteTier = favoriteTier),
            ),
        )

        snapshot("qr", 3).requireValid()
        assertThrows(IllegalArgumentException::class.java) { snapshot("missing", 3).requireValid() }
        assertThrows(IllegalArgumentException::class.java) { snapshot("qr", 4).requireValid() }
    }
}
