package com.bugchecking.app

internal enum class TrackEditorMode {
    CREATE_MANUAL,
    EDIT_MANUAL,
    EDIT_CHECK_IN,
}

internal data class TrackEditorCapabilities(
    val canEditTimestamp: Boolean,
    val canEditCoordinates: Boolean,
    val autoLocateOnOpen: Boolean,
    val canDelete: Boolean,
)

internal fun TrackEditorMode.capabilities() = when (this) {
    TrackEditorMode.CREATE_MANUAL -> TrackEditorCapabilities(
        canEditTimestamp = true,
        canEditCoordinates = true,
        autoLocateOnOpen = true,
        canDelete = false,
    )
    TrackEditorMode.EDIT_MANUAL -> TrackEditorCapabilities(
        canEditTimestamp = true,
        canEditCoordinates = true,
        autoLocateOnOpen = false,
        canDelete = true,
    )
    TrackEditorMode.EDIT_CHECK_IN -> TrackEditorCapabilities(
        canEditTimestamp = false,
        canEditCoordinates = false,
        autoLocateOnOpen = false,
        canDelete = true,
    )
}

internal fun TrackEditorMode.title(): String = when (this) {
    TrackEditorMode.CREATE_MANUAL -> "添加轨迹"
    TrackEditorMode.EDIT_MANUAL,
    TrackEditorMode.EDIT_CHECK_IN,
    -> "编辑轨迹"
}

internal fun TrackEditorMode.primaryActionLabel(): String = when (this) {
    TrackEditorMode.CREATE_MANUAL -> "添加轨迹"
    TrackEditorMode.EDIT_MANUAL,
    TrackEditorMode.EDIT_CHECK_IN,
    -> "保存更改"
}

internal fun shouldStackTrackFields(maxWidthDp: Float, fontScale: Float): Boolean =
    maxWidthDp < 360f || fontScale >= 1.6f
