package com.kutluoglu.prayer_feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.common.formatter
import com.kutluoglu.prayer.model.Prayer

// PrayerCard.kt - Reusable prayer display component
@Composable
fun PrayerCard(
    modifier: Modifier = Modifier,
    prayer: Prayer,
    isCurrent: Boolean = false,
    timeRemaining: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent)
                Color.Yellow
            else
                Color.DarkGray
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = prayer.arabicName,
//                    style = NamazVakitleriTheme.typography.headlineSmall,
//                    color = NamazVakitleriTheme.colors.primary
                )
                Text(
                    text = formatter.format(prayer.time),
//                    style = NamazVakitleriTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isCurrent && timeRemaining != null) {
//                CountdownTimer(timeRemaining = timeRemaining)
            }
        }
    }
}