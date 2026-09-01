package com.kutluoglu.prayer_feature.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer.model.location.LocationNameLocalizer
import kotlinx.coroutines.flow.first

@Composable
fun LocationChipsRow(
    entries: List<LocationEntry>,
    selectedId: String?,
    pagerState: PagerState,
    onLocationSelected: (String) -> Unit,
    onAddLocation: () -> Unit,
    languageCode: String,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val currentEntries by rememberUpdatedState(entries)
    val currentPage = pagerState.currentPage
    val currentPageOffsetFraction = pagerState.currentPageOffsetFraction

    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val chipBounds = remember { mutableStateMapOf<Int, ChipBounds>() }

    val indicatorOffsetX = remember { Animatable(0f) }
    val indicatorWidth = remember { Animatable(0f) }

    val indicatorBounds by remember(entries.size) {
        derivedStateOf {
            ChipsSelectionGeometry.indicatorBoundsFor(
                currentPage = pagerState.currentPage,
                currentPageOffsetFraction = pagerState.currentPageOffsetFraction,
                chipBounds = chipBounds,
                lastIndex = (entries.size - 1).coerceAtLeast(0)
            )
        }
    }

    LaunchedEffect(indicatorBounds) {
        val bounds = indicatorBounds ?: return@LaunchedEffect
        indicatorOffsetX.snapTo(bounds.offsetX)
        indicatorWidth.snapTo(bounds.width)
    }

    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage to pagerState.currentPageOffsetFraction }
        .collect { (page, _) ->
            val index = page.coerceIn(0, (currentEntries.size - 1).coerceAtLeast(0))
            if (index < 0) return@collect
                if (listState.isScrollInProgress) {
                    snapshotFlow { listState.isScrollInProgress }.first { !it }
                }
                val info = listState.layoutInfo
                val delta = ChipsSelectionGeometry.chipScrollTargetFor(index, info)
                if (delta != null) {
                    if (delta != 0f) listState.animateScrollBy(delta)
                } else {
                    listState.scrollToItem(index)
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords -> containerCoords = coords }
    ) {
        indicatorBounds?.let { bounds ->
            SlidingChipIndicator(
                offsetX = indicatorOffsetX.value,
                width = indicatorWidth.value,
                height = bounds.height
            )
        }
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 68.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                LocationChip(
                    text = LocationNameLocalizer.localized(entry, languageCode),
                    selectionProgress = ChipsSelectionGeometry.selectionProgressFor(
                        index = index,
                        currentPage = currentPage,
                        currentPageOffsetFraction = currentPageOffsetFraction
                    ),
                    isGps = entry.isAutoGps,
                    onClick = { onLocationSelected(entry.id) },
                    modifier = Modifier.onGloballyPositioned { coords ->
                        val container = containerCoords ?: return@onGloballyPositioned
                        val position = container.localPositionOf(coords, Offset.Zero)
                        val newBounds = ChipBounds(
                            offsetX = with(density) { position.x.toDp().value },
                            width = with(density) { coords.size.width.toDp().value },
                            height = with(density) { coords.size.height.toDp().value }
                        )
                        if (chipBounds[index] != newBounds) {
                            chipBounds[index] = newBounds
                        }
                    }
                )
            }
            item {
                AddLocationChip(onClick = onAddLocation)
            }
        }
    }
}
