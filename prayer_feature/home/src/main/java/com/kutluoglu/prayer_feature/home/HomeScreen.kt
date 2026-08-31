package com.kutluoglu.prayer_feature.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    onNavigateToSavedVerses: () -> Unit = {},
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
    val onChooseLocation = {
        navController.navigate(Screen.LocationSelectionScreen.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val context = LocalContext.current

    var pendingUseMyLocation by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        PermissionHandler(
            onPermissionsGranted = {
                if (pendingUseMyLocation) {
                    pendingUseMyLocation = false
                    onEvent(HomeEvent.OnUseMyLocation)
                } else {
                    onEvent(HomeEvent.OnPermissionsGranted)
                }
            },
            onChooseLocation = onChooseLocation,
            canProceedWithoutPermission = true
        ) { permissionActions ->
            LaunchedEffect(permissionActions.permanentlyDenied) {
                if (permissionActions.permanentlyDenied) pendingUseMyLocation = false
            }
            val openAppSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    this.data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
            LocationPager(
                entries = locationsState.entries,
                selectedId = locationsState.selectedId,
                activeLocationId = activeLocationId,
                uiState = uiState,
                prayerDataByLocation = prayerDataByLocation,
                quranVerseFormatter = quranVerseFormatter,
                onPrayerTimesClick = onPrayerTimesClick,
                onAddLocation = onAddLocation,
                onChooseLocation = onChooseLocation,
                onUseMyLocation = {
                    resolveUseMyLocationAction(
                        allPermissionsGranted = permissionActions.allPermissionsGranted,
                        permanentlyDenied = permissionActions.permanentlyDenied,
                        onUseMyLocation = { onEvent(HomeEvent.OnUseMyLocation) },
                        openSettings = openAppSettings,
                        requestPermission = {
                            pendingUseMyLocation = true
                            permissionActions.requestPermission()
                        }
                    )
                },
                permissionDenied = permissionActions.permanentlyDenied,
                onEvent = onEvent
            )
        }
        IconButton(
            onClick = onNavigateToSavedVerses,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(
                Icons.Outlined.BookmarkBorder,
                contentDescription = stringResource(R.string.saved_verses)
            )
        }
    }
}

internal fun resolveUseMyLocationAction(
    allPermissionsGranted: Boolean,
    permanentlyDenied: Boolean,
    onUseMyLocation: () -> Unit,
    openSettings: () -> Unit,
    requestPermission: () -> Unit
) {
    when {
        allPermissionsGranted -> onUseMyLocation()
        permanentlyDenied -> openSettings()
        else -> requestPermission()
    }
}
