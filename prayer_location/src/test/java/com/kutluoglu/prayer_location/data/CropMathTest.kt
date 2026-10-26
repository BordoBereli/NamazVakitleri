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
        // cover scale = max(1, (4/3)/2) = 1
        val (scale, offset) = CropMath.clampTransform(
            scale = 0.5f, offset = Offset(1000f, 1000f),
            imageAspectRatio = 4f / 3f, frameAspectRatio = 2f,
            frameSize = Size(400f, 200f)
        )
        assertThat(scale).isEqualTo(1f)
        assertThat(offset.x).isEqualTo(0f)
        assertThat(offset.y).isEqualTo(0f)
    }

    @Test
    fun `clampTransform enforces cover scale for wider images and clamps pan`() {
        // 3:1 image in a 2:1 frame: cover scale = (3/1)/(2/1) = 1.5; the image
        // is drawn 600x200 px, so its top-left corner clamps to
        // [400-600, 0] x [200-200, 0] = [-200, 0] x [0, 0].
        val (scale, offset) = CropMath.clampTransform(
            scale = 1f, offset = Offset(-1000f, -1000f),
            imageAspectRatio = 3f, frameAspectRatio = 2f,
            frameSize = Size(400f, 200f)
        )
        assertThat(scale).isEqualTo(1.5f)
        assertThat(offset.x).isEqualTo(-200f)
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
        val rect = CropMath.normalizedCropRect(
            scale = 2f, offset = Offset(-100f, -50f),
            frameSize = Size(400f, 200f), displayedImageSize = Size(800f, 400f)
        )
        assertThat(rect.left).isEqualTo(0.0625f)
        assertThat(rect.top).isEqualTo(0.0625f)
        assertThat(rect.width()).isEqualTo(0.25f)
        assertThat(rect.height()).isEqualTo(0.25f)
    }

    @Test
    fun `normalizedCropRect maps the visible frame of a width-fit image`() {
        // Portrait image (1:2) drawn width-fit in a 400x600 frame is 400x800,
        // panned 100 px up: the visible frame is y 100..700 of the image.
        val rect = CropMath.normalizedCropRect(
            scale = 1f, offset = Offset(0f, -100f),
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
}
