package com.kutluoglu.prayer_feature.prayertimes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.android.material.loadingindicator.LoadingIndicator
import com.kutluoglu.core.ui.theme.components.ErrorMessage
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
    /*Box(
        modifier = Modifier.fillMaxSize()
            // This is the key modifier. It applies padding for any system bars
            // that are currently visible, like when the user swipes them into view.
            .windowInsetsPadding(WindowInsets.systemBars)
    ){
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- 1. Top Container (37%) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.37f),
                contentAlignment = Alignment.Center
            ) {
                TopContainer(
                    painter = painterResource(id = R.drawable.image_prayers),
                    uiState = uiState
                )
            }
            // --- 2. Prayer Container (63%) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.63f),
                contentAlignment = Alignment.Center
            ){
                PrayerContainer(uiState = uiState)
            }
        }
    }*/

    Box(modifier = modifier.fillMaxSize()) {
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
                   .fillMaxHeight(0.6f), // The card takes up the bottom 70% of the screen
               shape = RoundedCornerShape(24.dp), // Rounded top corners
               elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
           ) {
               PrayerContainer(modifier, uiState)
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
    }
}