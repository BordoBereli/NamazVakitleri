package com.kutluoglu.prayer_feature.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel
import androidx.compose.runtime.getValue
import com.kutluoglu.prayer_feature.home.HomeScreen
import com.kutluoglu.prayer_feature.home.HomeViewModel
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
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
    val uiState by viewModel.uiState.collectAsState()
    HomeScreen(
        navController = navController,
        uiState = uiState,
        quranVerseFormatter = verseFormatter,
        onEvent = { event -> viewModel.onEvent(event) }
    )
}