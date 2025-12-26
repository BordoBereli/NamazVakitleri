package com.kutluoglu.prayer_feature.prayertimes.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kutluoglu.prayer_feature.common.components.LocationInfoSection
import com.kutluoglu.prayer_feature.prayertimes.R
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import com.kutluoglu.prayer_feature.prayertimes.PrayerTimesUiState

@Composable
fun TopContainer(
        modifier: Modifier = Modifier,
        painter: Painter,
        uiState: PrayerTimesUiState
) {

    val locationState by remember(uiState) {
        derivedStateOf { (uiState as? PrayerTimesUiState.Success)?.locationState }
    }
    val timeState by remember(uiState) {
        derivedStateOf { (uiState as? PrayerTimesUiState.Success)?.timeState }
    }
    val borderColorFromTheme = MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = stringResource(id = R.string.image_desc),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.9F),
                            Color.Transparent
                        ),
                        tileMode = TileMode.Decal
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .weight(0.3F)
                    .padding(start = 16.dp, top = 16.dp)
            ) {
                PageTitleSection()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3F)
                    .padding(start = 16.dp, end = 16.dp)
                    .background(
                        Color.Transparent.copy(alpha = 0.1F),
                        shape = RoundedCornerShape(corner = CornerSize(16.dp)),
                    )
                    .border(
                        width = 1.dp,
                        color = borderColorFromTheme.copy(alpha = 0.7F),
                        shape = RoundedCornerShape(corner = CornerSize(16.dp))
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    locationState?.let {
                        LocationInfoSection(
                            modifier = Modifier.wrapContentWidth(),
                            locationState = it,
                            textColor = Color.White,
                            textSize = 12.sp
                        )
                    }
                    timeState?.let {
                        TimeInfoSection(it)
                    }
                }
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

@Composable
fun PageTitleSection() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Icon on the left
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                // Using a material icon for demonstration. Replace with your own if needed.
                painter = painterResource(R.drawable.times),
                contentDescription = "Page Title Icon",
                tint = MaterialTheme.colorScheme.primary, // Set icon color
                modifier = Modifier.size(20.dp)

            )
        }
        // Two texts on the right, stacked vertically
        Column {
            Text(
                text = stringResource(R.string.page_title), // Main title
                fontSize = 20.sp
            )
            Text(
                text = stringResource(R.string.page_sub_title), // Subtitle
                color = MaterialTheme.colorScheme.onSurfaceVariant, // Slightly transparent
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun TimeInfoSection(timeState: TimeUiState) {
    Column(
        modifier = Modifier.fillMaxHeight().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.takvim),
                contentDescription = "Calendar Icon",
                tint = MaterialTheme.colorScheme.primary, // Set icon color
                modifier = Modifier.size(16.dp)

            )
            Text(
                text = timeState.gregorianShortDate,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = timeState.hijriDate,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

