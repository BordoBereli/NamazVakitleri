package com.kutluoglu.prayer_feature.common.background

import androidx.compose.runtime.mutableStateOf

/**
 * Session-scoped record of the home screen's background banner aspect ratio
 * (width / height).
 *
 * The background adjust screen frames the user's crop with exactly this ratio,
 * so the baked image can be drawn by the home banner without an extra
 * [androidx.compose.ui.layout.ContentScale.Crop] pass trimming it again. Home
 * is the start destination, so the size is normally measured before the adjust
 * screen is opened; [DEFAULT_ASPECT_RATIO] only covers deep-link/edge cases.
 *
 * In-memory on purpose: the value depends on the current window size and
 * orientation, so persisting it would make it stale after a rotation.
 */
object BackgroundBannerMetrics {

    /** Fallback used when the home banner has not been measured yet (3:2). */
    const val DEFAULT_ASPECT_RATIO: Float = 1.5f

    private val measuredAspectRatio = mutableStateOf(DEFAULT_ASPECT_RATIO)

    /** Banner width / height in pixels, or [DEFAULT_ASPECT_RATIO] when unknown. */
    val aspectRatio: Float
        get() = measuredAspectRatio.value

    /** Records the banner size measured on the home screen. */
    fun update(widthPx: Int, heightPx: Int) {
        if (widthPx <= 0 || heightPx <= 0) return
        measuredAspectRatio.value = widthPx.toFloat() / heightPx
    }
}
