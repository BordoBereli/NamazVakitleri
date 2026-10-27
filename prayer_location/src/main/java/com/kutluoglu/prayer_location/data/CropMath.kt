package com.kutluoglu.prayer_location.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import android.graphics.RectF

/**
 * Pure math helpers for the background image adjust screen and [BackgroundImageStore].
 *
 * Coordinate convention: the image is displayed **centered** in the frame at the
 * given [scale] (relative to the displayed image size) with [offset] in frame
 * pixels, where offset (0, 0) means the image is exactly centered in the frame
 * and the image's center sits at (frameSize/2 + offset.x, frameSize/2 + offset.y).
 * This matches what the preview renders: at rest the visible region is the
 * centered part of the image.
 */
object CropMath {

    /**
     * Clamps a pan/zoom transform so the frame is always covered (no gaps).
     *
     * @param scale current scale relative to the width-fit drawn image size
     * @param offset current offset of the image's center from the frame's center, in frame pixels
     * @param imageAspectRatio displayed image width / height
     * @param frameAspectRatio frame width / height
     * @param frameSize frame size in pixels
     * @return the clamped (scale, offset); scale is at least the cover scale
     */
    fun clampTransform(
        scale: Float,
        offset: Offset,
        imageAspectRatio: Float,
        frameAspectRatio: Float,
        frameSize: Size
    ): Pair<Float, Offset> {
        require(imageAspectRatio > 0f) { "imageAspectRatio must be positive" }
        require(frameAspectRatio > 0f) { "frameAspectRatio must be positive" }

        // The image is drawn width-fit: at scale 1 it spans the frame width and
        // 1/imageAspectRatio times the frame width in height.
        val coverScale = maxOf(1f, imageAspectRatio / frameAspectRatio)
        val clampedScale = maxOf(scale, coverScale)

        // The image's center must stay within [frameSize/2 - imageSize/2,
        // frameSize/2 + imageSize/2] so the frame is fully covered on both axes.
        val imageWidth = clampedScale * frameSize.width
        val imageHeight = imageWidth / imageAspectRatio
        val halfOverflowX = (imageWidth - frameSize.width) / 2f
        val halfOverflowY = (imageHeight - frameSize.height) / 2f

        return clampedScale to Offset(
            offset.x.coerceIn(-halfOverflowX, halfOverflowX) + 0f,
            offset.y.coerceIn(-halfOverflowY, halfOverflowY) + 0f
        )
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

        // The scaled image is centered in the frame; offset shifts it. The
        // image's top-left corner in frame pixels is
        // ((frameSize - scaledImageSize)/2 + offset), so the frame's top-left
        // corner in scaled-image pixels is the negation of that; divide by
        // scale for displayed-image pixels, then by displayedImageSize for 0..1.
        val scaledWidth = displayedImageSize.width * scale
        val scaledHeight = displayedImageSize.height * scale
        val frameLeftInImagePx = (scaledWidth - frameSize.width) / 2f - offset.x
        val frameTopInImagePx = (scaledHeight - frameSize.height) / 2f - offset.y
        val left = frameLeftInImagePx / scale / displayedImageSize.width
        val top = frameTopInImagePx / scale / displayedImageSize.height
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
