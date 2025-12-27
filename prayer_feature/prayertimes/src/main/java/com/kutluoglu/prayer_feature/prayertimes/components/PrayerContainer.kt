package com.kutluoglu.prayer_feature.prayertimes.components

/**
 * Created by F.K. on 20.12.2025.
 *
 */
/**
 * Copyright © 2025 F.K. All rights reserved.
 */
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.ui.theme.components.ErrorMessage
import com.kutluoglu.core.ui.theme.components.LoadingIndicator
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.common.prayerUtils.getPrayerDrawableIdFrom
import com.kutluoglu.prayer_feature.prayertimes.PrayerTimesUiState
import com.kutluoglu.prayer_feature.prayertimes.R

@Composable
fun PrayerContainer(
        modifier: Modifier = Modifier,
        uiState: PrayerTimesUiState
) {
    when (uiState) {
        is PrayerTimesUiState.Loading -> LoadingIndicator()
        is PrayerTimesUiState.Error -> ErrorMessage(message = uiState.message)
        is PrayerTimesUiState.Success -> PrayerTimesContent(
            prayers = uiState.prayers,
            gregorianDate = uiState.timeState.gregorianShortDate
        )
    }
}

@Composable
private fun PrayerTimesContent(prayers: List<Prayer>, gregorianDate: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- Header Item ---
        item {
            TitleHeader(gregorianDate)
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                PrayersHeader(prayers)
            }
        }

        // --- Prayer List Items ---
        items(prayers, key = { it.name }) { prayer ->
            PrayerRow(prayer = prayer)
        }
    }
}

@Composable
private fun TitleHeader(gregorianDate: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.btn_left),
            contentDescription = "Prayer Times Icon"
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = gregorianDate,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.page_sub_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.btn_right),
            contentDescription = "Prayer Times Icon"
        )
    }
}

@Composable
fun PrayersHeader(prayers: List<Prayer>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround, // Distributes space evenly
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 'items' is the correct way to build a list in LazyRow/LazyColumn
        items(prayers, key = { it.name }) { prayer ->
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(getPrayerDrawableIdFrom(prayer.name)),
                    contentDescription = prayer.name,
                    tint = MaterialTheme.colorScheme.primary // Use a theme color for the icon
                )
                Text(
                    text = prayer.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary // Use a theme color for the text
                )
            }
        }
    }
}

@Composable
private fun PrayerRow(prayer: Prayer) {
    val isCurrent = prayer.isCurrent
    val containerColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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