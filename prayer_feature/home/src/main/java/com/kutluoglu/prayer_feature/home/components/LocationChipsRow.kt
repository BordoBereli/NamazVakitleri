package com.kutluoglu.prayer_feature.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer.model.location.LocationEntry
import kotlin.math.abs

private fun selectionProgressFor(
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

@Composable
fun LocationChipsRow(
    entries: List<LocationEntry>,
    selectedId: String?,
    pagerState: PagerState,
    onLocationSelected: (String) -> Unit,
    onAddLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val currentPage = pagerState.currentPage
    val currentPageOffsetFraction = pagerState.currentPageOffsetFraction

    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val chipBounds = remember { mutableStateMapOf<Int, ChipBounds>() }

    val indicatorOffsetX = remember { Animatable(0f) }
    val indicatorWidth = remember { Animatable(0f) }

    val lastIndex = (entries.size - 1).coerceAtLeast(0)
    val fromIndex = currentPage.coerceIn(0, lastIndex)
    val toIndex = if (currentPageOffsetFraction > 0) {
        (currentPage + 1).coerceIn(0, lastIndex)
    } else {
        (currentPage - 1).coerceIn(0, lastIndex)
    }
    val fraction = abs(currentPageOffsetFraction).coerceIn(0f, 1f)

    val fromBounds = chipBounds[fromIndex]
    val toBounds = chipBounds[toIndex]

    val targetOffsetX = if (fromBounds != null && toBounds != null) {
        fromBounds.offsetX + (toBounds.offsetX - fromBounds.offsetX) * fraction
    } else {
        fromBounds?.offsetX ?: 0f
    }
    val targetWidth = if (fromBounds != null && toBounds != null) {
        fromBounds.width + (toBounds.width - fromBounds.width) * fraction
    } else {
        fromBounds?.width ?: 0f
    }

    LaunchedEffect(targetOffsetX, targetWidth, fromBounds != null) {
        if (fromBounds == null) return@LaunchedEffect
        indicatorOffsetX.snapTo(targetOffsetX)
        indicatorWidth.snapTo(targetWidth)
    }

    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage to pagerState.currentPageOffsetFraction }
            .collect { (page, _) ->
                val index = page.coerceIn(0, (entries.size - 1).coerceAtLeast(0))
                if (index < 0) return@collect
                val info = listState.layoutInfo
                val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                if (item != null) {
                    val delta = item.offset + item.size / 2 - info.viewportSize.width / 2
                    if (delta != 0) listState.animateScrollBy(delta.toFloat())
                } else {
                    listState.scrollToItem(index)
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                containerCoords = coords
            }
    ) {
        if (fromBounds != null) {
            SlidingChipIndicator(
                offsetX = indicatorOffsetX.value,
                width = indicatorWidth.value,
                height = fromBounds.height
            )
        }
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                LocationChip(
                    text = entry.displayName,
                    selectionProgress = selectionProgressFor(index, currentPage, currentPageOffsetFraction),
                    isGps = entry.isAutoGps,
                    onClick = { onLocationSelected(entry.id) },
                    modifier = Modifier.onGloballyPositioned { coords ->
                        val container = containerCoords ?: return@onGloballyPositioned
                        val position = container.localPositionOf(coords, Offset.Zero)
                        chipBounds[index] = ChipBounds(
                            offsetX = with(density) { position.x.toDp().value },
                            width = with(density) { coords.size.width.toDp().value },
                            height = with(density) { coords.size.height.toDp().value }
                        )
                    }
                )
            }
            item {
                AddLocationChip(onClick = onAddLocation)
            }
        }
    }
}
