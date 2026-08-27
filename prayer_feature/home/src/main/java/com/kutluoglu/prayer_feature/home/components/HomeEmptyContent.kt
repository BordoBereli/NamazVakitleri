package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.designsystem.components.EmptyStateContent
import com.kutluoglu.prayer_feature.home.R

@Composable
fun HomeEmptyContent(
    onAddLocation: () -> Unit,
    onUseMyLocation: () -> Unit,
    permissionDenied: Boolean = false
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EmptyStateContent(
                icon = Icons.Default.LocationOn,
                text = stringResource(R.string.no_location_selected)
            )
            Button(onClick = onAddLocation) {
                Text(stringResource(R.string.add_location))
            }
            OutlinedButton(onClick = onUseMyLocation) {
                Text(stringResource(R.string.use_my_location))
            }
            if (permissionDenied) {
                Text(
                    text = stringResource(R.string.permission_denied_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
