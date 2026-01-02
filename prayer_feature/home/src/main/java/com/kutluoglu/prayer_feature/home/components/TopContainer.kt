package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.kutluoglu.prayer_feature.common.components.LocationInfoSection
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import com.kutluoglu.prayer_feature.common.prayerUtils.getPrayerDrawableIdFrom
import com.kutluoglu.prayer_feature.home.HomeUiState
import com.kutluoglu.prayer_feature.home.PrayerUiState
import com.kutluoglu.prayer_feature.home.R

@Composable
fun TopContainer(
        successState: HomeUiState.Success?,
        painter: Painter,
        onStartCount: () -> Unit
) {
    val locationState by remember(successState) {
        derivedStateOf { successState?.locationState }
    }
    val timeState by remember(successState) {
        derivedStateOf { successState?.timeState }
    }
    val prayerState by remember(successState) {
        derivedStateOf { successState?.prayerState }
    }

    val borderColorFromTheme = MaterialTheme.colorScheme.onSecondaryContainer

    LaunchedEffect(prayerState) { onStartCount() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = stringResource(id = R.string.home_page_fallback),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.9F
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Box(modifier = Modifier.weight(0.3F).padding(start = 16.dp, top = 24.dp)) {
                locationState?.let { LocationInfoSection(locationState = it) }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 16.dp, end = 16.dp)
                    .weight(0.4F),
                contentAlignment = Alignment.Center
            ) {
                timeState?.let { TimeInfoSection(timeState = it) }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4F)
                    .padding(start = 16.dp, end = 16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.3F),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .drawBehind {
                        drawPath(
                            path = defineCustomBorderShapeForCurrentPrayerContainer(),
                            color = borderColorFromTheme.copy(alpha = 0.7F),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                prayerState?.let { NextPrayerInfo(prayerState = it) }
            }
        }
    }
}

@Composable
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
            text = timeState.gregorianFullDate,
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
}

@Composable
private fun NextPrayerInfo(prayerState: PrayerUiState) {
    val nextPrayerNameRaw = prayerState.nextPrayer?.name ?: "İmsak"

    val nextPrayerDisplayName = when (nextPrayerNameRaw) {
        "İmsak" -> "Sabah"
        else -> nextPrayerNameRaw
    }

    val timeUntilText = when (nextPrayerNameRaw) {
        "Güneş" -> stringResource(id = R.string.time_until_sunrise)
        else -> stringResource(id = R.string.time_until_prayer_format, nextPrayerDisplayName)
    }
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
                text = timeUntilText,
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
}

private fun DrawScope.defineCustomBorderShapeForCurrentPrayerContainer(): Path = Path().apply {
    val cornerRadius = 16.dp.toPx()
    moveTo(size.width, size.height)
    lineTo(size.width, cornerRadius)
    arcTo(
        rect = Rect(
            Offset(size.width - 2 * cornerRadius, 0f),
            Size(2 * cornerRadius, 2 * cornerRadius)
        ),
        startAngleDegrees = 0f,
        sweepAngleDegrees = -90f,
        forceMoveTo = false
    )
    lineTo(cornerRadius, 0f)
    arcTo(
        rect = Rect(Offset(0f, 0f), Size(2 * cornerRadius, 2 * cornerRadius)),
        startAngleDegrees = 270f,
        sweepAngleDegrees = -90f,
        forceMoveTo = false
    )
    lineTo(0f, size.height)
}
