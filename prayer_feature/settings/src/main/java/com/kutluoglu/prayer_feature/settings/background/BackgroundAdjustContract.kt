package com.kutluoglu.prayer_feature.settings.background

import androidx.compose.ui.geometry.Offset

data class BackgroundAdjustUiState(
    val uri: String?,
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero,
    val isSaving: Boolean = false
)

sealed class BackgroundAdjustEvent {
    /** Pan/zoom gesture update; clamped so the frame stays covered. */
    data class OnTransform(val scale: Float, val offset: Offset) : BackgroundAdjustEvent()

    /** Double-tap: reset zoom to the cover scale and re-center. */
    data object OnDoubleTap : BackgroundAdjustEvent()

    /** Bake the framed crop, persist it as the location's background. */
    data object Confirm : BackgroundAdjustEvent()
}
