package com.kutluoglu.prayer_feature.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.kutluoglu.prayer_feature.home.HomeScreen
import com.kutluoglu.prayer_feature.home.HomeViewModel
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.mergeToHomeUiState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Created by F.K. on 24.10.2025.
 *
 */

@Composable
fun HomeRoute(
        viewModel: HomeViewModel = koinViewModel(),
        verseFormatter: QuranVerseFormatter = koinInject<QuranVerseFormatter>(),
        navController: NavController
) {
    val gate by viewModel.screenGate.collectAsState()
    val time by viewModel.timeState.collectAsState()
    val location by viewModel.locationState.collectAsState()
    val prayer by viewModel.prayerState.collectAsState()
    val countdown by viewModel.countdownState.collectAsState()
    val quran by viewModel.quranState.collectAsState()
    val prompt by viewModel.promptState.collectAsState()

    val uiState = remember(gate, time, location, prayer, countdown, quran, prompt) {
        mergeToHomeUiState(gate, location, time, prayer, countdown, quran, prompt)
    }

    HomeScreen(
        navController = navController,
        uiState = uiState,
        quranVerseFormatter = verseFormatter,
        onEvent = { event -> viewModel.onEvent(event) }
    )
}
