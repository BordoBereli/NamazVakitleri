package com.kutluoglu.prayer_feature.prayertimes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kutluoglu.prayer_feature.prayertimes.PayerTimesScreen
import com.kutluoglu.prayer_feature.prayertimes.PrayerTimesViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Created by F.K. on 24.12.2025.
 *
 */

@Composable
fun PrayerTimesRoute(
        viewModel: PrayerTimesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadMonthlyPrayerTimes() }
    PayerTimesScreen(uiState = uiState, onEvent = viewModel::onEvent)
}
