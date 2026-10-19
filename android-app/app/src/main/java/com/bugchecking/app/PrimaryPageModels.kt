package com.bugchecking.app

import kotlin.math.roundToInt

internal enum class DeviceActionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    SYNCING,
    FAILED,
}

internal fun resolveDeviceActionState(
    connected: Boolean,
    syncStatus: PassportSyncStatus,
    message: String,
): DeviceActionState = when {
    connected && syncStatus == PassportSyncStatus.SYNCING -> DeviceActionState.SYNCING
    connected && syncStatus == PassportSyncStatus.FAILED -> DeviceActionState.FAILED
    connected -> DeviceActionState.CONNECTED
    message.contains("正在") || message.contains("查找") || message.contains("已发现") ||
        message.contains("读取设备") -> DeviceActionState.CONNECTING
    else -> DeviceActionState.DISCONNECTED
}

data class ManualTrackDraft(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val time: Long,
    val accuracy: Float,
    val provider: PlaceProvider = PlaceProvider.MANUAL,
    val address: String? = null,
    val placeId: String? = null,
    val originalUrl: String? = null,
    val canonicalUrl: String? = null,
    val resolutionState: PlaceResolutionState = PlaceResolutionState.MANUAL_CONFIRMATION,
)

internal fun ManualTrackDraft.requireValid() {
    require(name.trim().isNotEmpty()) { "请输入地点名称" }
    require(latitude.isFinite() && latitude in -90.0..90.0) { "纬度需在 -90 到 90 之间" }
    require(longitude.isFinite() && longitude in -180.0..180.0) { "经度需在 -180 到 180 之间" }
    require(time > 0L) { "请选择有效时间" }
}

internal fun trackAccuracyLabel(track: TrackPoint): String =
    if (track.accuracy > 0f) "精度 ${track.accuracy.toInt()} m" else "手动坐标"

internal fun checklistProgress(completed: Int, total: Int): Float =
    if (total <= 0) 0f else (completed.toFloat() / total).coerceIn(0f, 1f)

internal fun checklistProgressPercent(completed: Int, total: Int): Int =
    if (total <= 0) 0 else (completed.coerceIn(0, total) * 100.0 / total).roundToInt()

internal fun moveChecklistId(ids: List<String>, movedId: String, targetId: String): List<String> {
    val from = ids.indexOf(movedId)
    val to = ids.indexOf(targetId)
    if (from < 0 || to < 0 || from == to) return ids
    return ids.toMutableList().apply { add(to, removeAt(from)) }
}

internal fun reorderIndicesWithinGroup(
    allIds: List<String>,
    originalGroupIds: List<String>,
    movedId: String,
    finalGroupIndex: Int,
): Pair<Int, Int>? {
    if (movedId !in originalGroupIds || finalGroupIndex !in originalGroupIds.indices) return null
    val from = allIds.indexOf(movedId)
    val targetId = originalGroupIds[finalGroupIndex]
    val to = allIds.indexOf(targetId)
    return if (from >= 0 && to >= 0 && from != to) from to to else null
}
