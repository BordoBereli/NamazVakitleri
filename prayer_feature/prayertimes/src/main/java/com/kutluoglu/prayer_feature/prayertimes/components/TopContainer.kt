package com.kutluoglu.prayer_feature.prayertimes.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
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
                    .weight(0.35F)
                    .padding(start = 16.dp, top = 24.dp)
            ) {
                PageTitleSection()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45F)
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
                    .testTag("location_date_box")
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .padding(vertical = 10.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(50)
                            )
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = timeState?.gregorianShortDate ?: "",
                            fontSize = 17.sp,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        timeState?.hijriDate?.let { hijri ->
                            Text(
                                text = hijri,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .padding(top = 3.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 1.dp)
                            )
                        }
                        timeState?.gregorianDayAndName?.let { day ->
                            Text(
                                text = day,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            locationState?.let {
                                LocationInfoSection(
                                    modifier = Modifier.wrapContentWidth(),
                                    locationState = it,
                                    textColor = Color.White,
                                    textSize = 11.sp
                                )
                            }
                            timeState?.currentTime?.let { currentTime ->
                                Text(
                                    text = currentTime,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 16.dp, end = 16.dp)
                    .weight(0.2F),
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

