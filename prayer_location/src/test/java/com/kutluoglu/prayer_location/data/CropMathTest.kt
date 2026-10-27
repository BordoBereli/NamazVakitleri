package com.kutluoglu.prayer_location.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CropMathTest {

    @Test
    fun `clampTransform raises sub-cover scale to cover scale and clamps pan`() {
        // 4:3 image in a 2:1 frame already covers at scale 1 (width-fit):
        // cover scale = max(1, (4/3)/2) = 1. The image is drawn 400x300 px in
        // a 400x200 frame, so the center offset clamps to
        // [-0, 0] x [-50, 50].
        val (scale, offset) = CropMath.clampTransform(
            scale = 0.5f, offset = Offset(1000f, 1000f),
            imageAspectRatio = 4f / 3f, frameAspectRatio = 2f,
            frameSize = Size(400f, 200f)
        )
        assertThat(scale).isEqualTo(1f)
        assertThat(offset.x).isEqualTo(0f)
        assertThat(offset.y).isEqualTo(50f)
    }

    @Test
    fun `clampTransform enforces cover scale for wider images and clamps pan`() {
        // 3:1 image in a 2:1 frame: cover scale = (3/1)/(2/1) = 1.5; the image
        // is drawn 600x200 px, so the center offset clamps to
        // [-100, 100] x [0, 0].
        val (scale, offset) = CropMath.clampTransform(
            scale = 1f, offset = Offset(-1000f, -1000f),
            imageAspectRatio = 3f, frameAspectRatio = 2f,
            frameSize = Size(400f, 200f)
        )
        assertThat(scale).isEqualTo(1.5f)
        assertThat(offset.x).isEqualTo(-100f)
        assertThat(offset.y).isEqualTo(0f)
    }

    @Test
    fun `clampTransform keeps a valid transform unchanged`() {
        val (scale, offset) = CropMath.clampTransform(
            scale = 2f, offset = Offset(-0.5f, -0.25f),
            imageAspectRatio = 4f / 3f, frameAspectRatio = 2f,
            frameSize = Size(400f, 200f)
        )
        assertThat(scale).isEqualTo(2f)
        assertThat(offset.x).isEqualTo(-0.5f)
        assertThat(offset.y).isEqualTo(-0.25f)
    }

    @Test
    fun `normalizedCropRect maps on-screen transform to image rect`() {
        // scale 2 doubles the 800x400 drawn image to 1600x800; the frame
        // (400x200) is centered in it, then the image is shifted by offset
        // (-100, -50) (image moves left/up): the frame's top-left in image px
        // is ((1600-400)/2 + 100, (800-200)/2 + 50) / 2 = (700, 350) scaled px
        // = (350, 175) image px, i.e. (0.4375, 0.4375) normalized; the frame
        // spans 0.25 of the image.
        val rect = CropMath.normalizedCropRect(
            scale = 2f, offset = Offset(-100f, -50f),
            frameSize = Size(400f, 200f), displayedImageSize = Size(800f, 400f)
        )
        assertThat(rect.left).isWithin(1e-6f).of(0.4375f)
        assertThat(rect.top).isWithin(1e-6f).of(0.4375f)
        assertThat(rect.width()).isWithin(1e-6f).of(0.25f)
        assertThat(rect.height()).isWithin(1e-6f).of(0.25f)
    }

    @Test
    fun `normalizedCropRect maps the visible frame of a width-fit image`() {
        // Portrait image (1:2) drawn width-fit in a 400x600 frame is 400x800,
        // centered shows y 100..700; panning 100 px up shows y 200..800 of
        // the image (the bottom half).
        val rect = CropMath.normalizedCropRect(
            scale = 1f, offset = Offset(0f, -100f),
            frameSize = Size(400f, 600f), displayedImageSize = Size(400f, 800f)
        )
        assertThat(rect.left).isWithin(1e-6f).of(0f)
        assertThat(rect.top).isWithin(1e-6f).of(0.25f)
        assertThat(rect.right).isWithin(1e-6f).of(1f)
        assertThat(rect.bottom).isWithin(1e-6f).of(1f)
    }

    @Test
    fun `normalizedCropRect at rest returns the centered crop`() {
        // Offset zero must map to the centered visible region — this is the
        // regression test for the preview/crop mismatch: the preview renders
        // the image centered, so the baked crop must be the centered region
        // too, not the top-left corner.
        val rect = CropMath.normalizedCropRect(
            scale = 1f, offset = Offset.Zero,
            frameSize = Size(400f, 600f), displayedImageSize = Size(400f, 800f)
        )
        assertThat(rect.left).isWithin(1e-6f).of(0f)
        assertThat(rect.top).isWithin(1e-6f).of(0.125f)
        assertThat(rect.right).isWithin(1e-6f).of(1f)
        assertThat(rect.bottom).isWithin(1e-6f).of(0.875f)
    }

    @Test
    fun `normalizedCropRect clamps to image bounds`() {
        // Positive offset shifts the image right/down, leaving a gap — the
        // rect must clamp to the image bounds so the baked crop never exits
        // the bitmap.
        val rect = CropMath.normalizedCropRect(
            scale = 1f, offset = Offset(50f, 50f),
            frameSize = Size(400f, 200f), displayedImageSize = Size(400f, 200f)
        )
        assertThat(rect.left).isEqualTo(0f)
        assertThat(rect.top).isEqualTo(0f)
        assertThat(rect.right).isAtMost(1f)
        assertThat(rect.bottom).isAtMost(1f)
    }

    @Test
    fun `cover-clamped crop keeps the frame aspect ratio`() {
        // 16:9 image in the home-banner-shaped frame; at scale 1 the frame is
        // not covered, so clampTransform raises it to the cover scale. The
        // resulting crop must keep the frame's aspect ratio, otherwise the
        // home banner's ContentScale.Crop would trim the confirmed view again.
        val frameSize = Size(660f, 446f)
        val imageAspect = 1600f / 900f
        val displayed = Size(660f, 660f / imageAspect)
        val (scale, offset) = CropMath.clampTransform(
            scale = 1f, offset = Offset.Zero,
            imageAspectRatio = imageAspect,
            frameAspectRatio = frameSize.width / frameSize.height,
            frameSize = frameSize
        )

        val rect = CropMath.normalizedCropRect(
            scale = scale, offset = offset,
            frameSize = frameSize, displayedImageSize = displayed
        )

        val cropAspect = (rect.width() * displayed.width) / (rect.height() * displayed.height)
        assertThat(cropAspect).isWithin(1e-3f).of(frameSize.width / frameSize.height)
    }
}
