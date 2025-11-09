package com.kutluoglu.prayer_feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.kutluoglu.core.ui.theme.components.PermissionHandler
import com.kutluoglu.prayer_feature.home.components.BottomContainer
import com.kutluoglu.prayer_feature.home.components.DailyPrayers
import com.kutluoglu.prayer_feature.home.components.TopContainer
import com.kutluoglu.prayer_feature.home.feature.VerseDetailSheetContent
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerContent(
        navController: NavController,
        uiState: HomeUiState,
        onEvent: (HomeEvent) -> Unit
) {
    // 1. Remember SnackbarHostState and CoroutineScope
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 2. Add LaunchedEffect to show the snackbar when the state changes
    LaunchedEffect(uiState) {
        if (uiState is HomeUiState.Success && uiState.data.showLocationUpdatePrompt) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.location_update_prompt_message),
                    actionLabel = context.getString(R.string.location_update_action_label),
                    withDismissAction = true,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    onEvent(HomeEvent.OnUpdateLocationConfirmed)
                }
            }
        }
    }

    // --- Modal Bottom Sheet Logic ---
    val sheetState = rememberModalBottomSheetState()

    // Show the bottom sheet when the state says so
    if (uiState is HomeUiState.Success && uiState.data.isVerseDetailSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { onEvent(HomeEvent.OnVerseDetailDismissed) },
            sheetState = sheetState
        ) {
            // Pass the verse data to our new content composable
            uiState.data.quranVerse?.let { verse ->
                VerseDetailSheetContent(verse = verse)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        // Determine if the UI is in a refreshing state
        val isRefreshing = uiState is HomeUiState.Loading

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Apply padding from the Scaffold
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
                    .weight(0.50f)
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
                    .weight(0.15f)
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.secondary)
                    .clickable(
                        onClick = { onEvent(HomeEvent.OnVerseClicked) } // Trigger the sheet
                    ),
                contentAlignment = Alignment.Center
            ) {
                BottomContainer(uiState = uiState) {
                    onEvent(HomeEvent.OnLoadQuranVerse)
                }
            }
        }
    }
}