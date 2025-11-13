package com.kutluoglu.prayer_feature.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel
import androidx.compose.runtime.getValue
import com.kutluoglu.prayer_feature.home.HomeEvent
import com.kutluoglu.prayer_feature.home.HomeScreen
import com.kutluoglu.prayer_feature.home.HomeViewModel

/**
 * Created by F.K. on 24.10.2025.
 *
 */

@Composable
fun HomeRoute(
        viewModel: HomeViewModel = koinViewModel(),
        navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    HomeScreen(
        navController = navController,
        uiState = uiState,
        onEvent = { event -> viewModel.onEvent(event) }
    )
}