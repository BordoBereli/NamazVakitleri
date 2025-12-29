package com.kutluoglu.prayer_feature.prayertimes.components

/**
 * Created by F.K. on 20.12.2025.
 *
 */
/**
 * Copyright © 2025 F.K. All rights reserved.
 */
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.kutluoglu.prayer_feature.prayertimes.DailyPrayer
import com.kutluoglu.prayer_feature.prayertimes.PrayerTimesUiState
import com.kutluoglu.prayer_feature.prayertimes.R

@Composable
fun PrayerContainer(
        uiState: PrayerTimesUiState
) {
    when (uiState) {
        is PrayerTimesUiState.Loading -> LoadingIndicator()
        is PrayerTimesUiState.Error -> ErrorMessage(message = uiState.message)
        is PrayerTimesUiState.Success -> PrayerTimesContent(
            monthlyPrayers = uiState.monthlyPrayers,
            currentDayOfMonth = uiState.currentDayOfMonth,
            gregorianDate = uiState.timeState.gregorianShortDate,
        )
    }
}

@Composable
private fun PrayerTimesContent(
        monthlyPrayers: List<DailyPrayer>,
        currentDayOfMonth: Int,
        gregorianDate: String
) {
    val listState = rememberLazyListState()

    LaunchedEffect(monthlyPrayers) {
        // Find the index of the current day. The list is 0-indexed, so subtract 1.
        val todayIndex = currentDayOfMonth - 1
        // Make sure the index is valid before scrolling
        if (todayIndex in monthlyPrayers.indices) {
            // Animate the scroll to the item. Use scrollToItem for an instant jump.
            listState.animateScrollToItem(index = todayIndex)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary), // Apply background to the whole container
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Header Items ---
        TitleHeader(gregorianDate)
        PrayersHeader(monthlyPrayers.firstOrNull()?.prayers ?: emptyList())
        // --- Content Items ---
        PrayerList(monthlyPrayers, currentDayOfMonth, listState)
    }

}

@Composable
private fun TitleHeader(gregorianDate: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1F))
            .padding(vertical = 16.dp),
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
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        horizontalArrangement = Arrangement.SpaceAround, // Distributes space evenly
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(prayers, key = { it.name }) { prayer ->
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(getPrayerDrawableIdFrom(prayer.name)),
                    contentDescription = prayer.name,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = prayer.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PrayerList(
        monthlyPrayers: List<DailyPrayer>,
        currentDayOfMonth: Int,
        listState: LazyListState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(monthlyPrayers, key = { it.dayOfMonth }) { dailyPrayer ->
            val isToday = dailyPrayer.dayOfMonth == currentDayOfMonth
            DailyPrayerCard(
                dailyPrayer = dailyPrayer,
                isToday = isToday
            )
        }
    }
}

@Composable
private fun DailyPrayerCard(dailyPrayer: DailyPrayer, isToday: Boolean) {
    val borderColorFromTheme = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent
    val shapeCornerSize = 12.dp
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = borderColorFromTheme.copy(alpha = 0.7F),
                shape = RoundedCornerShape(corner = CornerSize(shapeCornerSize))
            )
        ,
        shape = RoundedCornerShape(shapeCornerSize),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            PrayersRow(dailyPrayer.prayers, isToday)
            Spacer(Modifier.height(12.dp))
            PrayerDateInfo(
                dayOfMonth = dailyPrayer.dayOfMonth,
                nameOfMonth = dailyPrayer.gregorianDate,
                hijriDate = dailyPrayer.hijriDate,
                isToday = isToday
            )
        }
    }
}

@Composable
private fun PrayersRow(prayers: List<Prayer>, isToday: Boolean) {
    val textColor = if (isToday) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround, // Distributes space evenly
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(prayers, key = { it.name }) { prayer ->
            Text(
                text = prayer.time.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
        }
    }
}

@Composable
private fun PrayerDateInfo(
        dayOfMonth: Int,
        nameOfMonth: String,
        hijriDate: String,
        isToday: Boolean,
) {
    val numberBackgroundColor = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val numberColor = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val dateColor = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val hijriDateColor = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = numberBackgroundColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$dayOfMonth",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = numberColor
                )
            }
            Text(
                text = nameOfMonth.split(" ").last(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = dateColor
            )
        }
        Text(
            text = hijriDate,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = hijriDateColor
        )
    }
}
