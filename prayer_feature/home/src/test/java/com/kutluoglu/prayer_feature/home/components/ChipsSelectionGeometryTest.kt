package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class ChipsSelectionGeometryTest {

    // --- selectionProgressFor ---

    @Test
    fun `current page has progress 1 when not swiping`() {
        assertThat(ChipsSelectionGeometry.selectionProgressFor(2, 2, 0f)).isEqualTo(1f)
    }

    @Test
    fun `current page progress decreases as user swipes forward`() {
        assertThat(ChipsSelectionGeometry.selectionProgressFor(2, 2, 0.4f)).isEqualTo(0.6f)
    }

    @Test
    fun `current page progress decreases as user swipes backward`() {
        assertThat(ChipsSelectionGeometry.selectionProgressFor(2, 2, -0.3f)).isEqualTo(0.7f)
    }

    @Test
    fun `next page progress increases when swiping forward`() {
        assertThat(ChipsSelectionGeometry.selectionProgressFor(3, 2, 0.4f)).isEqualTo(0.4f)
    }

    @Test
    fun `next page has zero progress when swiping backward`() {
        assertThat(ChipsSelectionGeometry.selectionProgressFor(3, 2, -0.4f)).isEqualTo(0f)
    }

    @Test
    fun `previous page progress increases when swiping backward`() {
        assertThat(ChipsSelectionGeometry.selectionProgressFor(1, 2, -0.4f)).isEqualTo(0.4f)
    }

    @Test
    fun `previous page has zero progress when swiping forward`() {
        assertThat(ChipsSelectionGeometry.selectionProgressFor(1, 2, 0.4f)).isEqualTo(0f)
    }

    @Test
    fun `non adjacent page always has zero progress`() {
        assertThat(ChipsSelectionGeometry.selectionProgressFor(5, 2, 0.4f)).isEqualTo(0f)
        assertThat(ChipsSelectionGeometry.selectionProgressFor(5, 2, -0.4f)).isEqualTo(0f)
    }

    // --- indicatorBoundsFor ---

    private val bounds = mapOf(
        0 to ChipBounds(offsetX = 0f, width = 100f, height = 28f),
        1 to ChipBounds(offsetX = 120f, width = 80f, height = 28f)
    )

    @Test
    fun `indicator sits on from chip when not swiping`() {
        val result = ChipsSelectionGeometry.indicatorBoundsFor(0, 0f, bounds, lastIndex = 1)
        assertThat(result).isEqualTo(IndicatorBounds(offsetX = 0f, width = 100f, height = 28f))
    }

    @Test
    fun `indicator interpolates between from and to chips`() {
        val result = ChipsSelectionGeometry.indicatorBoundsFor(0, 0.5f, bounds, lastIndex = 1)
        assertThat(result).isEqualTo(IndicatorBounds(offsetX = 60f, width = 90f, height = 28f))
    }

    @Test
    fun `indicator clamps to last chip when swiping past end`() {
        val result = ChipsSelectionGeometry.indicatorBoundsFor(1, 0.5f, bounds, lastIndex = 1)
        assertThat(result).isEqualTo(IndicatorBounds(offsetX = 120f, width = 80f, height = 28f))
    }

    @Test
    fun `indicator returns null when from chip has no bounds`() {
        val result = ChipsSelectionGeometry.indicatorBoundsFor(0, 0f, emptyMap(), lastIndex = 1)
        assertThat(result).isNull()
    }

    // --- chipScrollTargetFor ---

    @Test
    fun `chip scroll target returns null when chip not visible`() {
        val layoutInfo = mockk<LazyListLayoutInfo> {
            every { visibleItemsInfo } returns emptyList()
        }
        assertThat(ChipsSelectionGeometry.chipScrollTargetFor(0, layoutInfo)).isNull()
    }

    @Test
    fun `chip scroll target returns centering delta when chip visible`() {
        val item = mockk<LazyListItemInfo> {
            every { index } returns 0
            every { offset } returns 50
            every { size } returns 100
        }
        val layoutInfo = mockk<LazyListLayoutInfo> {
            every { visibleItemsInfo } returns listOf(item)
            every { viewportSize } returns IntSize(400, 100)
        }
        assertThat(ChipsSelectionGeometry.chipScrollTargetFor(0, layoutInfo)).isEqualTo(-100f)
    }
}
