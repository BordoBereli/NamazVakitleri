package com.kutluoglu.prayer_feature.prayertimes.components

/**
 * Created by F.K. on 20.12.2025.
 *
 */
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.ui.theme.components.ErrorMessage
import com.kutluoglu.core.ui.theme.components.LoadingIndicator
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.common.getPrayerDrawableIdFrom
import com.kutluoglu.prayer_feature.prayertimes.PrayerTimesUiState

/**
 * Copyright © 2025 F.K. All rights reserved.
 */

@Composable
fun PrayerContainer(
        modifier: Modifier = Modifier,
        uiState: PrayerTimesUiState
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is PrayerTimesUiState.Loading -> LoadingIndicator()
            is PrayerTimesUiState.Error   -> ErrorMessage(message = uiState.message)
            is PrayerTimesUiState.Success -> PrayerTimesContent(
                prayers = uiState.prayers,
                gregorianDate = uiState.gregorianDate
            )
        }
    }
}

@Composable
private fun PrayerTimesContent(prayers: List<Prayer>, gregorianDate: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Header Item ---
        item {
            Column {
                Text(
                    text = "Prayer Times",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = gregorianDate,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- Prayer List Items ---
        items(prayers, key = { it.name }) { prayer ->
            PrayerRow(prayer = prayer)
        }
    }
}

@Composable
private fun PrayerRow(prayer: Prayer) {
    val isCurrent = prayer.isCurrent
    val containerColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    val contentColor = if (isCurrent) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left side: Icon and Prayer Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(id = getPrayerDrawableIdFrom(prayer.name)),
                    contentDescription = prayer.name,
                    tint = contentColor
                )
                Text(
                    text = prayer.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            }
            // Right side: Prayer Time
            Text(
                text = prayer.time.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}