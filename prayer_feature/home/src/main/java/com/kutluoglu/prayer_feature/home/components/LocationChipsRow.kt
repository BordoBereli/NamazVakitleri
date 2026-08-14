package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer.model.location.LocationEntry

@Composable
fun LocationChipsRow(
    entries: List<LocationEntry>,
    selectedId: String?,
    onLocationSelected: (String) -> Unit,
    onAddLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        entries.forEach { entry ->
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
