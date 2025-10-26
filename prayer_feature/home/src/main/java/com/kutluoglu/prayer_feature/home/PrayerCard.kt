package com.kutluoglu.prayer_feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.ui.R.*
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
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            // Use MaterialTheme.colorScheme to access the theme's colors
            containerColor = if (isCurrent)
                MaterialTheme.colorScheme.primaryContainer // A good choice for the current item
            else
                MaterialTheme.colorScheme.surface, // Standard card background
            contentColor = MaterialTheme.colorScheme.onSurface // Text color on top of surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // New Row for Icon and Arabic name
            /*Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = prayer.arabicName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    painter = painterResource(getPrayerDrawableIdFrom(prayer.name)),
                    contentDescription = prayer.name,
                    tint = MaterialTheme.colorScheme.primary
                )
            }*/
            Icon(
                painter = painterResource(getPrayerDrawableIdFrom(prayer.name)),
                contentDescription = prayer.name
            )
            Text(
                text = prayer.name,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "${prayer.time}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun getPrayerDrawableIdFrom(drawableName: String): Int {
    println("drawableName: $drawableName")
    val prayerNames = stringArrayResource(array.prayers)
    return when(drawableName) {
        prayerNames[0] -> return R.drawable.facr
        prayerNames[1] -> return R.drawable.sunrise
        prayerNames[2] -> return R.drawable.dhuhr
        prayerNames[3] -> return R.drawable.asr
        prayerNames[4] -> return R.drawable.magrip
        prayerNames[5] -> return R.drawable.isha
        else -> -1
    }
}