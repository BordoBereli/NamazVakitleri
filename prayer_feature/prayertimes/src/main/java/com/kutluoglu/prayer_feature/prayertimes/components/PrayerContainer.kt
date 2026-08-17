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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.common.extractWeekdayName
import com.kutluoglu.core.common.gregorianShortFormatter
import com.kutluoglu.core.designsystem.components.ErrorMessage
import com.kutluoglu.core.designsystem.components.LoadingIndicator
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.common.prayerUtils.getPrayerDrawableIdFrom
import com.kutluoglu.prayer_feature.prayertimes.PrayerTimesEvent
import com.kutluoglu.prayer_feature.prayertimes.PrayerTimesUiState
import com.kutluoglu.prayer_feature.prayertimes.R
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number

private val CARD_CORNER_SIZE = 12.dp
private val HEADER_BACKGROUND_ALPHA = 0.1F
private val TODAY_BORDER_ALPHA = 0.7F
private val HEADER_SURFACE_ALPHA = 0.5f
private val TODAY_HIJRI_ALPHA = 0.7f

@Composable
fun PrayerContainer(
        uiState: PrayerTimesUiState,
        onEvent: (PrayerTimesEvent) -> Unit
) {
    when (uiState) {
        is PrayerTimesUiState.Loading -> LoadingIndicator()
        is PrayerTimesUiState.Error -> ErrorMessage(message = uiState.message)
        is PrayerTimesUiState.Success -> PrayerTimesContent(
            monthlyPrayers = uiState.monthlyPrayers,
            currentDayOfMonth = uiState.currentDayOfMonth,
            selectedMonth = uiState.selectedMonth,
            isCurrentMonth = uiState.isCurrentMonth,
            onEvent = onEvent
        )
    }
}

@Composable
private fun PrayerTimesContent(
        monthlyPrayers: List<DailyPrayer>,
        currentDayOfMonth: Int,
        selectedMonth: YearMonth,
        isCurrentMonth: Boolean,
        onEvent: (PrayerTimesEvent) -> Unit
) {
    val listState = rememberLazyListState()
    val scrollController = remember { PrayerListScrollController(listState::animateScrollToItem) }

    LaunchedEffect(selectedMonth, monthlyPrayers) {
        scrollController.onMonthChanged(
            month = selectedMonth,
            isCurrentMonth = isCurrentMonth,
            todayIndex = currentDayOfMonth - 1,
            itemCount = monthlyPrayers.size
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Header Items ---
        TitleHeader(
            selectedMonthLabel = selectedMonthLabel(selectedMonth),
            isCurrentMonth = isCurrentMonth,
            onPrevious = { onEvent(PrayerTimesEvent.OnPreviousMonth) },
            onNext = { onEvent(PrayerTimesEvent.OnNextMonth) },
            onToday = { onEvent(PrayerTimesEvent.OnToday) }
        )
        PrayersHeader(monthlyPrayers.firstOrNull()?.prayers ?: emptyList())
        // --- Content Items ---
        PrayerList(monthlyPrayers, currentDayOfMonth, isCurrentMonth, listState)
    }

}

private fun selectedMonthLabel(month: YearMonth): String =
    java.time.YearMonth.of(month.year, month.month.number).format(gregorianShortFormatter)

@Composable
private fun TitleHeader(
        selectedMonthLabel: String,
        isCurrentMonth: Boolean,
        onPrevious: () -> Unit,
        onNext: () -> Unit,
        onToday: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.primary.copy(alpha = HEADER_BACKGROUND_ALPHA))
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                painter = painterResource(id = R.drawable.btn_left),
                contentDescription = stringResource(R.string.previous_month)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = selectedMonthLabel,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isCurrentMonth) {
                Text(
                    text = stringResource(R.string.page_sub_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                TextButton(onClick = onToday) {
                    Text(text = stringResource(R.string.today))
                }
            }
        }
        IconButton(onClick = onNext) {
            Icon(
                painter = painterResource(id = R.drawable.btn_right),
                contentDescription = stringResource(R.string.next_month)
            )
        }
    }
}

@Composable
private fun PrayersHeader(prayers: List<Prayer>) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(CARD_CORNER_SIZE))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = HEADER_SURFACE_ALPHA)),
        horizontalArrangement = Arrangement.SpaceAround, // Distributes space evenly
        verticalAlignment = Alignment.CenterVertically
    ) {
        prayers.forEach { prayer ->
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
        isCurrentMonth: Boolean,
        listState: LazyListState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(monthlyPrayers, key = { it.dayOfMonth }) { dailyPrayer ->
            val isToday = isCurrentMonth && dailyPrayer.dayOfMonth == currentDayOfMonth
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
    val cardShape = RoundedCornerShape(CARD_CORNER_SIZE)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = borderColorFromTheme.copy(alpha = TODAY_BORDER_ALPHA),
                shape = cardShape
            ),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            PrayersRow(dailyPrayer.prayers, isToday)
            Spacer(Modifier.height(12.dp))
            val weekdayName = remember(dailyPrayer.gregorianDate) { extractWeekdayName(dailyPrayer.gregorianDate) }
            PrayerDateInfo(
                dayOfMonth = dailyPrayer.dayOfMonth,
                weekdayName = weekdayName,
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround, // Distributes space evenly
        verticalAlignment = Alignment.CenterVertically
    ) {
        prayers.forEach { prayer ->
            val timeText = remember(prayer) { prayer.time.toString() }
            Text(
                text = timeText,
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
        }
    }
}

@Composable
private fun PrayerDateInfo(
        dayOfMonth: Int,
        weekdayName: String,
        hijriDate: String,
        isToday: Boolean,
) {
    val numberBackgroundColor = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val numberColor = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val dateColor = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val hijriDateColor = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = TODAY_HIJRI_ALPHA) else MaterialTheme.colorScheme.onSurfaceVariant

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
                text = weekdayName,
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
