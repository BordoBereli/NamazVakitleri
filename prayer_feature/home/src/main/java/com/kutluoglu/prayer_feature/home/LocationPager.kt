package com.kutluoglu.prayer_feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.designsystem.components.LoadingIndicator
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer.model.location.LocationNameLocalizer
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_feature.home.R
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.components.BottomContainer
import com.kutluoglu.prayer_feature.home.components.DailyPrayers
import com.kutluoglu.prayer_feature.home.components.HomeEmptyContent
import com.kutluoglu.prayer_feature.home.components.HomeErrorContent
import com.kutluoglu.prayer_feature.home.components.HomeTopContainer
import com.kutluoglu.prayer_feature.home.components.LocationChipsRow
import com.kutluoglu.prayer_feature.home.domain.LoadedPrayerData
import com.kutluoglu.prayer_feature.home.feature.CustomBottomSheet
import com.kutluoglu.prayer_feature.home.feature.VerseDetailSheetContent
import com.kutluoglu.prayer_feature.home.layout.HomeResponsiveLayout
import com.kutluoglu.prayer_feature.home.state.CountdownUiState
import com.kutluoglu.prayer_feature.home.state.HomeUiState
import com.kutluoglu.prayer_feature.home.state.PrayerUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.time.ZoneId

@Composable
fun LocationPager(
    entries: List<LocationEntry>,
    selectedId: String?,
    activeLocationId: String?,
    uiState: HomeUiState,
    prayerDataByLocation: Map<String, LoadedPrayerData>,
    quranVerseFormatter: QuranVerseFormatter,
    onPrayerTimesClick: () -> Unit,
    onAddLocation: () -> Unit,
    onChooseLocation: () -> Unit,
    onUseMyLocation: () -> Unit,
    permissionDenied: Boolean,
    onEvent: (HomeEvent) -> Unit,
    calculator: PrayerLogicEngine,
    formatter: PrayerFormatter,
    languageCode: String
) {
    if (entries.isEmpty()) {
        if (uiState is HomeUiState.Loading) {
            LoadingIndicator()
        } else {
            HomeEmptyContent(
                onAddLocation = onAddLocation,
                onUseMyLocation = onUseMyLocation,
                permissionDenied = permissionDenied
            )
        }
        return
    }

    val selectedIndex = entries.indexOfFirst { it.id == selectedId }
        .coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { entries.size.coerceAtLeast(1) }
    )

    LaunchedEffect(selectedId) {
        val index = entries.indexOfFirst { it.id == selectedId }
        if (index >= 0 && index != pagerState.currentPage) {
            pagerState.animateScrollToPage(index)
        }
    }

    val currentEntries by rememberUpdatedState(entries)
    val currentSelectedId by rememberUpdatedState(selectedId)
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
            selectedId = selectedId,
            pagerState = pagerState,
            onLocationSelected = { id -> onEvent(HomeEvent.OnLocationSelected(id)) },
            onAddLocation = onAddLocation,
            languageCode = languageCode
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
                    uiState = uiState,
                    quranVerseFormatter = quranVerseFormatter,
                    isAutoGps = entry.isAutoGps,
                    onPrayerTimesClick = onPrayerTimesClick,
                    onChooseLocation = onChooseLocation,
                    onEvent = onEvent
                )
                data != null -> LocationPagePreview(
                    data = data,
                    isAutoGps = entry.isAutoGps,
                    onViewAllClicked = onPrayerTimesClick,
                    calculator = calculator,
                    formatter = formatter
                )
                else -> LocationPlaceholder(entry, languageCode)
            }
        }
    }
}

@Composable
private fun LocationPlaceholder(entry: LocationEntry?, languageCode: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = entry?.let { LocationNameLocalizer.localized(it, languageCode) } ?: "",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun LocationPagePreview(
    data: LoadedPrayerData,
    isAutoGps: Boolean,
    onViewAllClicked: () -> Unit,
    calculator: PrayerLogicEngine,
    formatter: PrayerFormatter
) {
    val countdownState = rememberLocationCountdown(
        prayerState = data.prayerState,
        zoneId = data.zoneId,
        calculator = calculator,
        formatter = formatter
    )
    val successState = HomeUiState.Success(
        timeState = data.timeState,
        prayerState = data.prayerState,
        locationState = data.locationState,
        countdownState = countdownState,
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

/**
 * Computes a live per-second countdown for a non-active location page so the
 * preview shows the real time remaining instead of the "--:--:--" placeholder.
 */
@Composable
private fun rememberLocationCountdown(
    prayerState: PrayerUiState,
    zoneId: ZoneId,
    calculator: PrayerLogicEngine,
    formatter: PrayerFormatter
): CountdownUiState {
    var countdown by remember(prayerState, zoneId) {
        mutableStateOf(CountdownUiState())
    }
    LaunchedEffect(prayerState, zoneId) {
        while (true) {
            val nextPrayer = prayerState.nextPrayer
            countdown = CountdownUiState(
                timeRemaining = if (nextPrayer != null) {
                    formatter.formatTimeRemaining(
                        calculator.calculateTimeRemaining(nextPrayer.time, zoneId)
                    )
                } else {
                    "--:--:--"
                },
                currentTime = formatter.getFormattedCurrentTime(zoneId)
            )
            delay(1_000)
        }
    }
    return countdown
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerContent(
    uiState: HomeUiState,
    quranVerseFormatter: QuranVerseFormatter,
    isAutoGps: Boolean = false,
    onPrayerTimesClick: () -> Unit,
    onChooseLocation: () -> Unit,
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
                onRetry = { onEvent(HomeEvent.OnRefresh) },
                onChooseLocation = onChooseLocation
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
                    VerseDetailSheetContent(
                        verse = verse,
                        verseFormatter = quranVerseFormatter,
                        isSaved = successState?.isVerseSaved == true,
                        onToggleSaved = { onEvent(HomeEvent.OnToggleVerseSaved) }
                    )
                }
            }
        }
    }
}
