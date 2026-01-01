package com.kutluoglu.prayer_feature.qibla.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kutluoglu.prayer_feature.qibla.QiblaScreen
import com.kutluoglu.prayer_feature.qibla.QiblaViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Created by F.K. on 1.01.2026.
 *
 */

@Composable
fun QiblaRoute(
        viewModel: QiblaViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    QiblaScreen(uiState)
}