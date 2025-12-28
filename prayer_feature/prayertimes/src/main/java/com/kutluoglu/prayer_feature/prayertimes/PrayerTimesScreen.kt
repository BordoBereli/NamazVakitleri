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
    /*Box(modifier = modifier.fillMaxSize()) {
        // Assume TopContainer is your header with the image.
        // It takes up the top portion of the screen.
        TopContainer(
            modifier = Modifier
                .fillMaxHeight(0.3f), // Top container takes 40% of the height
            painter = painterResource(id = R.drawable.image_prayers),
            uiState = uiState
        )

       Column(
           modifier = Modifier
               .fillMaxWidth()
               .fillMaxHeight(0.75f)
               .align(Alignment.BottomCenter) // Align the card to the bottom
       ) {
           // The content is now wrapped in a Card
           Card(
               modifier = Modifier
                   .fillMaxWidth().padding(8.dp)
                   .fillMaxHeight(1f), // The card takes up the bottom 70% of the screen
               shape = RoundedCornerShape(24.dp), // Rounded top corners
               elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
           ) {
               PrayerContainer(uiState)
           }

           // The content is now wrapped in a Card
           Card(
               modifier = Modifier
                   .fillMaxWidth().padding(8.dp),
               shape = RoundedCornerShape(24.dp), // Rounded top corners
               elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
           ) {
               PrayerContainer(modifier, uiState)
           }
       }
    }*/
    // BoxWithConstraints gives us the screen dimensions to decide the layout.
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            // In Landscape, use a Row to place items side-by-side.
            Row(modifier = Modifier.fillMaxSize()) {
                TopContainer(
                    modifier = Modifier.weight(0.43f), // Left side takes 40% of the width
                    painter = painterResource(id = R.drawable.image_prayers),
                    uiState = uiState
                )
                Card(
                    modifier = Modifier
                        .weight(0.57f) // Right side takes 60% of the width
                        .fillMaxHeight()
                        .padding(8.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    PrayerContainer(uiState)
                }
            }
        } else {
            // In Portrait, use the original Box layout.
            Box(modifier = Modifier.fillMaxSize()) {
                TopContainer(
                    modifier = Modifier.fillMaxHeight(0.3f), // Top takes 30% of the height
                    painter = painterResource(id = R.drawable.image_prayers),
                    uiState = uiState
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.75f) // The card column starts from the bottom and overlaps a bit
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