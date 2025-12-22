package com.kutluoglu.prayer_feature.prayertimes.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopContainer(
        modifier: Modifier = Modifier,
        painter: Painter,
        onStartCount: () -> Unit
) {

    val borderColorFromTheme = MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
//            contentDescription = stringResource(id = R.string.home_page_fallback),
            contentDescription = "",
            alpha = 0.9F,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .weight(0.3F)
                    .padding(start = 16.dp, top = 16.dp)
            ) {

            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3F)
                    .padding(start = 16.dp, end = 16.dp)
                    .background(
                        Color.Transparent.copy(alpha = 0.2F),
                        shape = RoundedCornerShape(corner = CornerSize(16.dp)),
                    ),
                contentAlignment = Alignment.Center,
            ) {

            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 16.dp, end = 16.dp)
                    .weight(0.4F),
                contentAlignment = Alignment.Center
            ) {

            }
        }
    }
}

/*@Composable
private fun LocationInfoSection(locationState: LocationUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(R.drawable.konum),
            contentDescription = stringResource(id = R.string.home_page_fallback),
            tint = Color.Unspecified
        )
        Text(
            text = locationState.locationInfoText,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}*/

/*@Composable
private fun TimeInfoSection(timeState: TimeUiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = timeState.currentTime,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = timeState.gregorianDate,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = timeState.hijriDate,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}*/

/*@Composable
private fun NextPrayerInfo(prayerState: PrayerUiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = getPrayerDrawableIdFrom(
                        prayerState.currentPrayer?.name ?: ""
                    )
                ),
                contentDescription = stringResource(id = R.string.time_until_message),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Time until ${prayerState.nextPrayer?.name ?: ""}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = prayerState.timeRemaining,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}*/
