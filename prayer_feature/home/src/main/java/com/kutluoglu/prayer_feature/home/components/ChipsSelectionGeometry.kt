package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import kotlin.math.abs

data class ChipBounds(
    val offsetX: Float,
    val width: Float,
    val height: Float
)

data class IndicatorBounds(
    val offsetX: Float,
    val width: Float,
    val height: Float
)

object ChipsSelectionGeometry {

    fun selectionProgressFor(
        index: Int,
        currentPage: Int,
        currentPageOffsetFraction: Float
    ): Float {
        val fraction = currentPageOffsetFraction
        val absFraction = abs(fraction)
        return when (index) {
            currentPage -> 1f - absFraction
            currentPage + 1 -> if (fraction > 0) fraction else 0f
            currentPage - 1 -> if (fraction < 0) absFraction else 0f
            else -> 0f
        }
    }

    fun indicatorBoundsFor(
        currentPage: Int,
        currentPageOffsetFraction: Float,
        chipBounds: Map<Int, ChipBounds>,
        lastIndex: Int
    ): IndicatorBounds? {
        val fromIndex = currentPage.coerceIn(0, lastIndex)
        val toIndex = if (currentPageOffsetFraction > 0) {
            (currentPage + 1).coerceIn(0, lastIndex)
        } else {
            (currentPage - 1).coerceIn(0, lastIndex)
        }
        val fraction = abs(currentPageOffsetFraction).coerceIn(0f, 1f)

        val fromBounds = chipBounds[fromIndex] ?: return null
        val toBounds = chipBounds[toIndex]

        val offsetX = if (toBounds != null) {
            fromBounds.offsetX + (toBounds.offsetX - fromBounds.offsetX) * fraction
        } else {
            fromBounds.offsetX
        }
        val width = if (toBounds != null) {
            fromBounds.width + (toBounds.width - fromBounds.width) * fraction
        } else {
            fromBounds.width
        }
        return IndicatorBounds(offsetX = offsetX, width = width, height = fromBounds.height)
    }

    fun chipScrollTargetFor(page: Int, layoutInfo: LazyListLayoutInfo): Float? {
        val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == page } ?: return null
        return (item.offset + item.size / 2 - layoutInfo.viewportSize.width / 2).toFloat()
    }
}
