package com.kutluoglu.prayer_feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.kutluoglu.core.designsystem.components.PermissionHandler
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_feature.common.LocalIsLandscape
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.components.BottomContainer
import com.kutluoglu.prayer_feature.home.components.DailyPrayers
import com.kutluoglu.prayer_feature.home.components.HomeTopContainer
import com.kutluoglu.prayer_feature.home.components.LocationChipsRow
import com.kutluoglu.prayer_feature.home.feature.CustomBottomSheet
import com.kutluoglu.prayer_feature.home.feature.VerseDetailSheetContent
import com.kutluoglu.prayer_feature.home.state.HomeUiState
import com.kutluoglu.prayer_location.data.LocationsState
import com.kutluoglu.prayer_navigation.core.PrayerNestedGraph
import com.kutluoglu.prayer_navigation.core.Screen

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    uiState: HomeUiState,
    locationsState: LocationsState,
    quranVerseFormatter: QuranVerseFormatter,
    onEvent: (HomeEvent) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        PermissionHandler(
            onPermissionsGranted = { onEvent(HomeEvent.OnPermissionsGranted) }
        ) {
            val entries = locationsState.entries
            val selectedIndex = entries.indexOfFirst { it.id == locationsState.selectedId }
                .coerceAtLeast(0)
            val pagerState = rememberPagerState(
                initialPage = selectedIndex,
                pageCount = { entries.size.coerceAtLeast(1) }
            )

            LaunchedEffect(locationsState.selectedId) {
                val index = entries.indexOfFirst { it.id == locationsState.selectedId }
                if (index >= 0 && index != pagerState.currentPage) {
                    pagerState.animateScrollToPage(index)
                }
            }

            LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                val entry = entries.getOrNull(pagerState.currentPage)
                if (entry != null && entry.id != locationsState.selectedId) {
                    onEvent(HomeEvent.OnLocationSelected(entry.id))
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                LocationChipsRow(
                    entries = entries,
                    selectedId = locationsState.selectedId,
                    onLocationSelected = { id -> onEvent(HomeEvent.OnLocationSelected(id)) },
                    onAddLocation = {
                        navController.navigate(Screen.SettingsScreen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                HorizontalPager(state = pagerState) { page ->
                    val entry = entries.getOrNull(page)
                    if (entry != null && entry.id == locationsState.selectedId) {
                        PrayerContent(navController, uiState, quranVerseFormatter, onEvent)
                    } else {
                        LocationPlaceholder(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationPlaceholder(entry: LocationEntry?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = entry?.displayName ?: "",
            style = MaterialTheme.typography.titleLarge
        )
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
    val successState = uiState as? HomeUiState.Success

    val prayerState = successState?.prayerState
    val quranVerse = successState?.quranVerse
    val isVerseSheetVisible = successState?.isVerseDetailSheetVisible ?: false

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        val errorState = uiState as? HomeUiState.Error
        if (errorState != null) {
            ErrorContent(
                message = errorState.message,
                onRetry = { onEvent(HomeEvent.OnRefresh) }
            )
            return@Scaffold
        }

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
                                successState = successState
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
            dailyPrayers(Modifier.weight(0.8f))
            bottomContainer(Modifier.weight(0.2f))
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

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}