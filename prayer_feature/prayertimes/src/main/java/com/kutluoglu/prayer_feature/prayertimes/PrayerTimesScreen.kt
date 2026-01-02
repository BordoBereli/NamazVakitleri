package com.kutluoglu.prayer_feature.prayertimes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.prayertimes.components.PrayerContainer
import com.kutluoglu.prayer_feature.prayertimes.components.TopContainer

/**
 * Created by F.K. on 24.10.2025.
 *
 */

@Composable
fun PayerTimesScreen(
    modifier: Modifier = Modifier,
    uiState: PrayerTimesUiState
){
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                TopContainer(
                    modifier = Modifier.weight(0.43f),
                    painter = painterResource(id = R.drawable.image_prayers),
                    uiState = uiState
                )
                Card(
                    modifier = Modifier
                        .weight(0.57f)
                        .fillMaxHeight()
                        .padding(8.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    PrayerContainer(uiState)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                TopContainer(
                    modifier = Modifier.fillMaxHeight(0.35f), // Top takes 30% of the height
                    painter = painterResource(id = R.drawable.image_prayers),
                    uiState = uiState
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f) // The card column starts from the bottom and overlaps a bit
                        .align(Alignment.BottomCenter)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        PrayerContainer(uiState)
                    }
                }
            }
        }
    }
}