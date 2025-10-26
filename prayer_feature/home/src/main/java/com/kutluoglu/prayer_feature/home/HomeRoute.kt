package com.kutluoglu.prayer_feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel
import androidx.compose.runtime.getValue

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
    // The Route connects the ViewModel to the stateless UI
    HomeScreen(
        navController = navController,
        uiState = uiState
    )
}