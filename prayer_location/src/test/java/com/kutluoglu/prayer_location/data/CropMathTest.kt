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
            imageAspectRatio = 4f / 3f, frameAspectRatio = 2f
        )
        assertThat(scale).isEqualTo(1f)
        assertThat(offset.x).isAtMost(0f)
        assertThat(offset.y).isAtMost(0f)
    }

    @Test
    fun `clampTransform enforces cover scale for wider images and clamps pan`() {
        // 3:1 image in a 2:1 frame: cover scale = (3/1)/(2/1) = 1.5;
        // image spans 1.5 x 0.5 in frame units, so the image's top-left corner
        // (the offset) clamps to [-1.5, 0] x [-0.5, 0]. Offset(-2, -1) is out
        // of bounds and clamps to the corner (-1.5, -0.5).
        val (scale, offset) = CropMath.clampTransform(
            scale = 1f, offset = Offset(-2f, -1f),
            imageAspectRatio = 3f, frameAspectRatio = 2f
        )
        assertThat(scale).isEqualTo(1.5f)
        assertThat(offset.x).isEqualTo(-1.5f)
        assertThat(offset.y).isEqualTo(-0.5f)
    }

    @Test
    fun `clampTransform keeps a valid transform unchanged`() {
        val (scale, offset) = CropMath.clampTransform(
            scale = 2f, offset = Offset(-0.5f, -0.25f),
            imageAspectRatio = 4f / 3f, frameAspectRatio = 2f
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
