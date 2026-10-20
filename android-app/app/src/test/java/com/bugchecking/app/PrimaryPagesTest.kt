package com.bugchecking.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Calendar

class PrimaryPagesTest {
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

    @Test fun unknownAccuracyUsesManualCoordinateLabel() {
        val manual = TrackPoint(1, "manual-1", null, "外滩", 31.23, 121.47, 1L, 0f)
        assertEquals("手动坐标", trackAccuracyLabel(manual))
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
}
