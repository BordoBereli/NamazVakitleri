package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.home.common.getPrayerDrawableIdFrom

@Composable
fun PrayerCard(
        modifier: Modifier = Modifier,
        prayer: Prayer,
        isCurrent: Boolean = false
) {
    // Determine colors based on the current prayer state
    val contentColor =
        if (isCurrent) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface
    val backgroundColor =
        if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else MaterialTheme.colorScheme.secondary
    // Define the border
    val border =
        if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        else null

    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor, // Standard card background
            contentColor = contentColor // Text color on top of surface
        ),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(getPrayerDrawableIdFrom(prayer.name)),
                contentDescription = prayer.name
            )
            Text(
                text = prayer.name,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "${prayer.time}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}