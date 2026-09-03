package com.kutluoglu.prayer_feature.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.kutluoglu.core.designsystem.utils.LanguageProvider
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_feature.home.HomeScreen
import com.kutluoglu.prayer_feature.home.HomeViewModel
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.state.mergeToHomeUiState
import com.kutluoglu.prayer_navigation.core.Screen
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
        calculator: PrayerLogicEngine = koinInject(),
        formatter: PrayerFormatter = koinInject(),
        navController: NavController
) {
    val languageProvider = koinInject<LanguageProvider>()
    val languageCode = languageProvider.getLanguageCode()
    val gate by viewModel.screenGate.collectAsState()
    val countdown by viewModel.countdownState.collectAsState()
    val quran by viewModel.quranState.collectAsState()
    val locations by viewModel.locationsState.collectAsState()
    val prayerData by viewModel.prayerDataByLocation.collectAsState()
    val activeLocationId by viewModel.activeLocationId.collectAsState()

    val activeData = prayerData[activeLocationId]

    val uiState = remember(gate, activeData, countdown, quran) {
        mergeToHomeUiState(
            gate = gate,
            location = activeData?.locationState,
            time = activeData?.timeState,
            prayer = activeData?.prayerState,
            countdown = countdown,
            quran = quran,
            nextImsakTime = activeData?.nextImsakTime
        )
    }

    HomeScreen(
        navController = navController,
        uiState = uiState,
        locationsState = locations,
        prayerDataByLocation = prayerData,
        activeLocationId = activeLocationId,
        languageCode = languageCode,
        quranVerseFormatter = verseFormatter,
        calculator = calculator,
        formatter = formatter,
        onNavigateToSavedVerses = {
            navController.navigate(Screen.SavedVersesScreen.route)
        },
        onEvent = { event -> viewModel.onEvent(event) }
    )
}
