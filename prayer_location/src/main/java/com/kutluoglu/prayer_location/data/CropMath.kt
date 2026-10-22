package com.kutluoglu.prayer_location.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import android.graphics.RectF

/**
 * Pure math helpers for the background image adjust screen and [BackgroundImageStore].
 *
 * Coordinate convention: the image is displayed centered in the frame at the given
 * [scale] (relative to the displayed image size) with [offset] in frame pixels,
 * where the image's top-left corner sits at (offset.x, offset.y).
 */
object CropMath {

    /**
     * Clamps a pan/zoom transform so the frame is always covered (no gaps).
     *
     * @param scale current scale relative to the displayed image size
     * @param offset current offset of the image's top-left corner in frame pixels
     * @param imageAspectRatio displayed image width / height
     * @param frameAspectRatio frame width / height
     * @return the clamped (scale, offset); scale is at least the cover scale
     */
    fun clampTransform(
        scale: Float,
        offset: Offset,
        imageAspectRatio: Float,
        frameAspectRatio: Float
    ): Pair<Float, Offset> {
        require(imageAspectRatio > 0f) { "imageAspectRatio must be positive" }
        require(frameAspectRatio > 0f) { "frameAspectRatio must be positive" }

        // Work in a normalized frame of width 1; the displayed image is then
        // 1 wide and 1/imageAspectRatio tall at scale 1.
        val coverScale = maxOf(1f, imageAspectRatio / frameAspectRatio)
        val clampedScale = maxOf(scale, coverScale)

        // Image size in frame pixels at the clamped scale.
        val imageWidth = clampedScale
        val imageHeight = clampedScale / imageAspectRatio

        // The image's top-left corner must stay within [-imageSize, 0] so the
        // frame is fully covered on both axes.
        val clampedX = offset.x.coerceIn(-imageWidth, 0f)
        val clampedY = offset.y.coerceIn(-imageHeight, 0f)

        return clampedScale to Offset(clampedX, clampedY)
    }

    /**
     * Converts an on-screen transform into a normalized crop rect
     * (0..1 in displayed-image coordinates) for the visible frame.
     */
    fun normalizedCropRect(
        scale: Float,
        offset: Offset,
        frameSize: Size,
        displayedImageSize: Size
    ): RectF {
        require(scale > 0f) { "scale must be positive" }
        require(displayedImageSize.width > 0f && displayedImageSize.height > 0f) {
            "displayedImageSize must be positive"
        }

        // The image's top-left corner in displayed-image pixels is
        // (-offset / scale); the frame spans frameSize / scale image pixels.
        val left = (-offset.x / scale) / displayedImageSize.width
        val top = (-offset.y / scale) / displayedImageSize.height
        val right = left + frameSize.width / scale / displayedImageSize.width
        val bottom = top + frameSize.height / scale / displayedImageSize.height

        // Clamp to image bounds so the baked crop never exits the bitmap.
        return RectF(
            left.coerceIn(0f, 1f),
            top.coerceIn(0f, 1f),
            right.coerceIn(0f, 1f),
            bottom.coerceIn(0f, 1f)
        )
    }
}
