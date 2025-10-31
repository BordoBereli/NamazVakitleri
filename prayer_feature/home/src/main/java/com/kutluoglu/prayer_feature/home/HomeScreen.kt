package com.kutluoglu.prayer_feature.home

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kutluoglu.core.ui.theme.common.REQUIRED_LOCATION_PERMISSIONS
import com.kutluoglu.core.ui.theme.components.PermissionHandler
import com.kutluoglu.prayer_feature.home.components.BottomContainer
import com.kutluoglu.prayer_feature.home.components.DailyPrayers
import com.kutluoglu.prayer_feature.home.components.TopContainer

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
        navController: NavController,
        uiState: HomeUiState,
        onEvent: (HomeEvent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // This is the key modifier. It applies padding for any system bars
            // that are currently visible, like when the user swipes them into view.
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Use the PermissionHandler to wrap the main content.
        // It will only show PrayerContent when permissions are granted.
        PermissionHandler(
            onPermissionsGranted = { onEvent(HomeEvent.OnPermissionsGranted) }
        ) {
            PrayerContent(navController, uiState, onEvent)
        }
    }
}

@Composable
private fun PrayerContent(
        navController: NavController,
        uiState: HomeUiState,
        onEvent: (HomeEvent) -> Unit
) {
    // Determine if the UI is in a refreshing state
    val isRefreshing = uiState is HomeUiState.Loading

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // --- 1. Top Container (35%) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f),
            contentAlignment = Alignment.Center
        ) {
            TopContainer(
                painter = painterResource(id = R.drawable.home_page_fallback),
                uiState = uiState,
                onStartCount = { onEvent(HomeEvent.OnCountDown) }
            )
        }

        // --- 2. Middle Container (57%) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.64f)
                .background(Color.Transparent)
        ) {
            DailyPrayers(
                uiState = uiState,
                navController = navController,
                isRefreshing = isRefreshing,
                onRefresh = { onEvent(HomeEvent.OnRefresh) }
            )
        }

        // --- 3. Bottom Container (8%) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.01f)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            BottomContainer()
        }
    }
}
