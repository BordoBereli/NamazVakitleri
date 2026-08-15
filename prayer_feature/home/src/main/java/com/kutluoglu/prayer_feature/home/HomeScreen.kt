package com.kutluoglu.prayer_feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.kutluoglu.core.designsystem.components.PermissionHandler
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.components.BottomContainer
import com.kutluoglu.prayer_feature.home.components.DailyPrayers
import com.kutluoglu.prayer_feature.home.components.HomeErrorContent
import com.kutluoglu.prayer_feature.home.components.HomeTopContainer
import com.kutluoglu.prayer_feature.home.components.LocationChipsRow
import com.kutluoglu.prayer_feature.home.domain.LoadedPrayerData
import com.kutluoglu.prayer_feature.home.feature.CustomBottomSheet
import com.kutluoglu.prayer_feature.home.feature.VerseDetailSheetContent
import com.kutluoglu.prayer_feature.home.layout.HomeResponsiveLayout
import com.kutluoglu.prayer_feature.home.state.CountdownUiState
import com.kutluoglu.prayer_feature.home.state.HomeUiState
import com.kutluoglu.prayer_location.data.LocationsState
import com.kutluoglu.prayer_navigation.core.PrayerNestedGraph
import com.kutluoglu.prayer_navigation.core.Screen
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    uiState: HomeUiState,
    locationsState: LocationsState,
    prayerDataByLocation: Map<String, LoadedPrayerData>,
    activeLocationId: String?,
    quranVerseFormatter: QuranVerseFormatter,
    onEvent: (HomeEvent) -> Unit
) {
    val onPrayerTimesClick = {
        navController.navigate(PrayerNestedGraph.PRAYER_TIMES) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

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

            val currentEntries by rememberUpdatedState(entries)
            val currentSelectedId by rememberUpdatedState(locationsState.selectedId)
            LaunchedEffect(Unit) {
                snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
                    .filter { (_, isScrolling) -> !isScrolling }
                    .map { (page, _) -> page }
                    .distinctUntilChanged()
                    .collect { page ->
                        val entry = currentEntries.getOrNull(page)
                        if (entry != null && entry.id != currentSelectedId) {
                            onEvent(HomeEvent.OnLocationSelected(entry.id))
                        }
                    }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                LocationChipsRow(
                    entries = entries,
                    selectedId = locationsState.selectedId,
                    pagerState = pagerState,
                    onLocationSelected = { id -> onEvent(HomeEvent.OnLocationSelected(id)) },
                    onAddLocation = {
                        navController.navigate(Screen.SettingsScreen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1
                ) { page ->
                    val entry = entries.getOrNull(page)
                    if (entry == null) return@HorizontalPager
                    val isActive = entry.id == activeLocationId
                    val data = prayerDataByLocation[entry.id]
                    when {
                        isActive -> PrayerContent(
                            navController = navController,
                            uiState = uiState,
                            quranVerseFormatter = quranVerseFormatter,
                            isAutoGps = entry.isAutoGps,
                            onPrayerTimesClick = onPrayerTimesClick,
                            onEvent = onEvent
                        )
                        data != null -> LocationPagePreview(
                            data = data,
                            isAutoGps = entry.isAutoGps,
                            onViewAllClicked = onPrayerTimesClick
                        )
                        else -> LocationPlaceholder(entry)
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

@Composable
private fun LocationPagePreview(
    data: LoadedPrayerData,
    isAutoGps: Boolean,
    onViewAllClicked: () -> Unit
) {
    val successState = HomeUiState.Success(
        timeState = data.timeState,
        prayerState = data.prayerState,
        locationState = data.locationState,
        countdownState = CountdownUiState(),
        quranVerse = null,
        isVerseDetailSheetVisible = false
    )
    HomeResponsiveLayout(
        innerPadding = PaddingValues(0.dp),
        topContainer = { modifier ->
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                HomeTopContainer(
                    painter = painterResource(id = R.drawable.home_page_fallback),
                    successState = successState
                )
            }
        },
        dailyPrayers = { modifier ->
            Box(modifier = modifier) {
                DailyPrayers(
                    prayerState = data.prayerState,
                    isRefreshing = false,
                    onRefresh = {},
                    onViewAllClicked = onViewAllClicked,
                    isAutoGps = isAutoGps
                )
            }
        },
        bottomContainer = { modifier -> Box(modifier = modifier) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerContent(
    navController: NavController,
    uiState: HomeUiState,
    quranVerseFormatter: QuranVerseFormatter,
    isAutoGps: Boolean = false,
    onPrayerTimesClick: () -> Unit,
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
            HomeErrorContent(
                message = errorState.message,
                onRetry = { onEvent(HomeEvent.OnRefresh) }
            )
            return@Scaffold
        }

        val isRefreshing = uiState is HomeUiState.Loading

        Box(modifier = Modifier.fillMaxSize()) {
            HomeResponsiveLayout(
                innerPadding = innerPadding,
                topContainer = { modifier ->
                    Box(modifier = modifier, contentAlignment = Alignment.Center) {
                        HomeTopContainer(
                            painter = painterResource(id = R.drawable.home_page_fallback),
                            successState = successState
                        )
                    }
                },
                dailyPrayers = { modifier ->
                    Box(modifier = modifier) {
                        DailyPrayers(
                            prayerState = prayerState,
                            isRefreshing = isRefreshing,
                            onRefresh = { onEvent(HomeEvent.OnRefresh) },
                            onViewAllClicked = onPrayerTimesClick,
                            isAutoGps = isAutoGps
                        )
                    }
                },
                bottomContainer = { modifier ->
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
            )
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
