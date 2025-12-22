package com.kutluoglu.prayer_feature.prayertimes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.kutluoglu.prayer_feature.prayertimes.components.TopContainer

/**
 * Created by F.K. on 24.10.2025.
 *
 */

@Composable
fun PayerTimesScreen(){
    Box(
        modifier = Modifier
            .fillMaxSize()
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
                    onStartCount = { }
                )
            }
            // --- 2. Prayer Container (63%) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.63f),
                contentAlignment = Alignment.Center
            ){
//                PrayerContainer(uiState = PrayerTimesUiState())
            }
        }
    }
}