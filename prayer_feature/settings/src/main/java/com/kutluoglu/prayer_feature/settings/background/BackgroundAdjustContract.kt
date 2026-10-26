package com.kutluoglu.prayer_feature.settings.background

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

data class BackgroundAdjustUiState(
    val uri: String?,
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero,
    /**
     * Decoded preview bitmap (EXIF-rotated, sampled). The screen draws it
     * directly via Canvas + drawImage with an explicit dstSize, so the drawn
     * geometry always matches the crop math. Null while loading.
     */
    val previewBitmap: android.graphics.Bitmap? = null,
    /**
     * Intrinsic width / height of the loaded image, or 0 while unknown.
     *
     * The preview lays the image out at its full drawn size (frame width and
     * width / aspect height) so panning never exposes a gap; 0 means "use the
     * frame's aspect ratio until the image is measured".
     */
    val imageAspectRatio: Float = 0f,
    val isSaving: Boolean = false
)

sealed class BackgroundAdjustEvent {
    /** Pan/zoom gesture update; clamped so the frame stays covered. */
    data class OnTransform(val scale: Float, val offset: Offset) : BackgroundAdjustEvent()

    /** Double-tap: reset zoom to the cover scale and re-center. */
    data object OnDoubleTap : BackgroundAdjustEvent()

    /** The preview frame was laid out; size is in pixels. */
    data class FrameMeasured(val size: Size) : BackgroundAdjustEvent()

    /** Bake the framed crop, persist it as the location's background. */
    data object Confirm : BackgroundAdjustEvent()
}
