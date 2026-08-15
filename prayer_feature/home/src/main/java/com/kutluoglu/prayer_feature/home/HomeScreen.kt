package com.kutluoglu.prayer_feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.kutluoglu.core.designsystem.components.PermissionHandler
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.domain.LoadedPrayerData
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
    val onAddLocation = {
        navController.navigate(Screen.SettingsScreen.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PermissionHandler(
            onPermissionsGranted = { onEvent(HomeEvent.OnPermissionsGranted) }
        ) {
            LocationPager(
                entries = locationsState.entries,
                selectedId = locationsState.selectedId,
                activeLocationId = activeLocationId,
                uiState = uiState,
                prayerDataByLocation = prayerDataByLocation,
                quranVerseFormatter = quranVerseFormatter,
                onPrayerTimesClick = onPrayerTimesClick,
                onAddLocation = onAddLocation,
                onEvent = onEvent
            )
        }
    }
}
