package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kutluoglu.prayer_feature.home.HomeEvent
import com.kutluoglu.prayer_feature.home.HomeUiState
import com.kutluoglu.prayer_feature.home.R
import com.kutluoglu.prayer_feature.home.TimeInfo
import com.kutluoglu.prayer_feature.home.common.getPrayerDrawableIdFrom

@Composable
fun TopContainer(
        modifier: Modifier = Modifier,
        painter: Painter,
        uiState: HomeUiState,
        onStartCount: () -> Unit
) {

    val borderColorFromTheme = MaterialTheme.colorScheme.onSecondaryContainer
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (uiState is HomeUiState.Success) {
            onStartCount()
        }
        Image(
            painter = painter,
            contentDescription = stringResource(id = R.string.home_page_fallback),
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
                    .weight(0.2F)
                    .padding(start = 16.dp, top = 16.dp)
            ) {
                LocationInfoSection()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 16.dp, end = 16.dp)
                    .weight(0.4F),
                contentAlignment = Alignment.Center
            ) {
                TimeInfo(uiState = uiState)
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
                NextPrayerInfo(uiState = uiState)
            }
        }
    }
}

@Composable
fun LocationInfoSection() {
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
            text = "Konum Bilgisi",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun TimeInfo(uiState: HomeUiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (uiState is HomeUiState.Success) {
            Text(
                text = uiState.data.timeInfo.currentTime,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.data.timeInfo.gregorianDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = uiState.data.timeInfo.hijriDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
        // You can add placeholders for Loading/Error state here if desired
    }
}

@Composable
fun NextPrayerInfo(uiState: HomeUiState) {
    if (uiState is HomeUiState.Success) {
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
                            uiState.data.currentPrayer?.name ?: ""
                        )
                    ),
                    contentDescription = stringResource(id = R.string.time_until_message),
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Time until ${uiState.data.nextPrayer?.name ?: ""}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = uiState.data.timeRemaining,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
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
