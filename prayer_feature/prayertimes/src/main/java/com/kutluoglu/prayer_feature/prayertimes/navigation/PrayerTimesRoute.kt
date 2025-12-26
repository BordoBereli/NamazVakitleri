package com.kutluoglu.prayer_feature.prayertimes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.kutluoglu.prayer_feature.prayertimes.PayerTimesScreen
import com.kutluoglu.prayer_feature.prayertimes.PrayerTimesViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.runtime.getValue

/**
 * Created by F.K. on 24.12.2025.
 *
 */

@Composable
fun PrayerTimesRoute(
        viewModel: PrayerTimesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    PayerTimesScreen(uiState = uiState)
}