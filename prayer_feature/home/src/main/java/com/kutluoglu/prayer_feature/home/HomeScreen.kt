package com.kutluoglu.prayer_feature.home

import android.app.Activity
import android.net.http.SslCertificate.restoreState
import android.net.http.SslCertificate.saveState
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.kutluoglu.core.ui.theme.components.PermissionHandler
import com.kutluoglu.prayer_feature.common.LocalIsLandscape
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.components.BottomContainer
import com.kutluoglu.prayer_feature.home.components.DailyPrayers
import com.kutluoglu.prayer_feature.home.components.HomeTopContainer
import com.kutluoglu.prayer_feature.home.feature.CustomBottomSheet
import com.kutluoglu.prayer_feature.home.feature.VerseDetailSheetContent
import com.kutluoglu.prayer_navigation.core.PrayerNestedGraph
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    uiState: HomeUiState,
    quranVerseFormatter: QuranVerseFormatter,
    onEvent: (HomeEvent) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        PermissionHandler(
            onPermissionsGranted = { onEvent(HomeEvent.OnPermissionsGranted) }
        ) {
            PrayerContent(navController, uiState, quranVerseFormatter, onEvent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerContent(
    navController: NavController,
    uiState: HomeUiState,
    quranVerseFormatter: QuranVerseFormatter,
    onEvent: (HomeEvent) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current.display

    val successState = uiState as? HomeUiState.Success

    val prayerState by remember(successState?.prayerState) {
        derivedStateOf { successState?.prayerState }
    }
    val showLocationUpdatePrompt by remember(successState?.showLocationUpdatePrompt) {
        derivedStateOf { successState?.showLocationUpdatePrompt ?: false }
    }
    val quranVerse by remember(successState?.quranVerse) {
        derivedStateOf { successState?.quranVerse }
    }
    val isVerseSheetVisible by remember(successState?.isVerseDetailSheetVisible) {
        derivedStateOf { successState?.isVerseDetailSheetVisible ?: false }
    }

    LaunchedEffect(showLocationUpdatePrompt) {
        if (showLocationUpdatePrompt) {
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
    /*val sheetState = rememberModalBottomSheetState()
    if (isVerseSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { onEvent(HomeEvent.OnVerseDetailDismissed) },
            sheetState = sheetState
        ) {
            quranVerse?.let { verse ->
                VerseDetailSheetContent(verse = verse, verseFormatter = quranVerseFormatter)
            }
        }
    }*/

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        val isRefreshing = uiState is HomeUiState.Loading
        val onPrayerTimesClick = {
            navController.navigate(PrayerNestedGraph.PRAYER_TIMES) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isLandscape = maxWidth > maxHeight
                val layoutDirection = LocalLayoutDirection.current // Get the layout direction here

                CompositionLocalProvider(LocalIsLandscape provides isLandscape) {
                    val topContainer = @Composable { modifier: Modifier ->
                        Box(modifier = modifier, contentAlignment = Alignment.Center) {
                            HomeTopContainer(
                                painter = painterResource(id = R.drawable.home_page_fallback),
                                successState = successState,
                                onStartCount = { onEvent(HomeEvent.OnCountDown) }
                            )
                        }
                    }

                    val dailyPrayers = @Composable { modifier: Modifier ->
                        Box(modifier = modifier) {
                            DailyPrayers(
                                prayerState = prayerState,
                                isRefreshing = isRefreshing,
                                onRefresh = { onEvent(HomeEvent.OnRefresh) },
                                onViewAllClicked = onPrayerTimesClick
                            )
                        }
                    }

                    val bottomContainer = @Composable { modifier: Modifier ->
                        Box(
                            modifier = modifier
                                .padding(8.dp)
                                .clickable(enabled = successState?.quranVerse != null) {
                                    onEvent(HomeEvent.OnVerseClicked)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            BottomContainer(
                                quranVerse = quranVerse,
                                verseFormatter = quranVerseFormatter
                            ) { onEvent(HomeEvent.OnLoadQuranVerse) }
                        }
                    }

                    if (isLandscape) {
                        LandscapeMode(
                            innerPadding = innerPadding,
                            topContainer = topContainer,
                            dailyPrayers = dailyPrayers,
                            bottomContainer = bottomContainer
                        )
                    } else {
                        PortraitMode(
                            innerPadding = innerPadding,
                            layoutDirection = layoutDirection,
                            topContainer = topContainer,
                            dailyPrayers = dailyPrayers,
                            bottomContainer = bottomContainer
                        )
                    }
                }
            }
            CustomBottomSheet(
                isVisible = isVerseSheetVisible,
                onDismiss = { onEvent(HomeEvent.OnVerseDetailDismissed) }
            ) {
                quranVerse?.let { verse ->
                    VerseDetailSheetContent(verse = verse, verseFormatter = quranVerseFormatter)
                }
            }
        }
    }
}

@Composable
private fun LandscapeMode(
        innerPadding: PaddingValues,
        topContainer: @Composable (Modifier) -> Unit,
        dailyPrayers: @Composable (Modifier) -> Unit,
        bottomContainer: @Composable (Modifier) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                bottom = innerPadding.calculateBottomPadding()
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        topContainer(Modifier.weight(0.4f))
        Column(
            modifier = Modifier.weight(0.6f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            dailyPrayers(Modifier.weight(0.73f))
            bottomContainer(Modifier.weight(0.27f))
        }
    }
}

@Composable
private fun PortraitMode(
        innerPadding: PaddingValues,
        layoutDirection: LayoutDirection,
        topContainer: @Composable (Modifier) -> Unit,
        dailyPrayers: @Composable (Modifier) -> Unit,
        bottomContainer: @Composable (Modifier) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = innerPadding.calculateBottomPadding()
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        topContainer(Modifier.weight(0.37f))
        dailyPrayers(Modifier.weight(0.50f))
        bottomContainer(Modifier.weight(0.13f))
    }
}