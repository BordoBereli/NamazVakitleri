package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer.model.location.LocationEntry
import kotlinx.coroutines.flow.first

@Composable
fun LocationChipsRow(
    entries: List<LocationEntry>,
    selectedId: String?,
    onLocationSelected: (String) -> Unit,
    onAddLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val selectedIndex = entries.indexOfFirst { it.id == selectedId }

    LaunchedEffect(selectedId, entries.size) {
        if (selectedIndex < 0) return@LaunchedEffect
        if (!listState.layoutInfo.visibleItemsInfo.any { it.index == selectedIndex }) {
            listState.scrollToItem(selectedIndex)
        }
        snapshotFlow { listState.layoutInfo }
            .first { info -> info.visibleItemsInfo.any { it.index == selectedIndex } }
            .let { info ->
                val item = info.visibleItemsInfo.first { it.index == selectedIndex }
                val delta = item.offset + item.size / 2 - info.viewportSize.width / 2
                if (delta != 0) listState.animateScrollBy(delta.toFloat())
            }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(entries, key = { it.id }) { entry ->
            FilterChip(
                selected = entry.id == selectedId,
                onClick = { onLocationSelected(entry.id) },
                leadingIcon = {
                    if (entry.isAutoGps) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = entry.displayName,
                        color = if (entry.isAutoGps) MaterialTheme.colorScheme.primary else Color.Unspecified
                    )
                }
            )
        }
        item {
            FilterChip(
                selected = false,
                onClick = onAddLocation,
                leadingIcon = {
                    Icon(Icons.Default.Add, contentDescription = "Add location")
                },
                label = { Text("") }
            )
        }
    }
}
