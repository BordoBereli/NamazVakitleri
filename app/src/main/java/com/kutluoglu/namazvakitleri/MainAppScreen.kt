package com.kutluoglu.namazvakitleri

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kutluoglu.core.common.analytics.AnalyticsEvents
import com.kutluoglu.core.common.analytics.AnalyticsParams
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.app_update.ui.ForceUpdateDialog
import com.kutluoglu.app_update.ui.OptionalUpdateDialog
import com.kutluoglu.app_update.ui.UpdateUiState
import com.kutluoglu.app_update.ui.UpdateViewModel
import com.kutluoglu.prayer_navigation.core.Destination
import com.kutluoglu.prayer_navigation.core.NavButton
import com.kutluoglu.prayer_navigation.core.PrayerNestedGraph
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import com.kutluoglu.prayer_feature.home.navigation.homeGraph
import com.kutluoglu.prayer_navigation.core.Screen
import androidx.navigation.compose.navigation
import com.kutluoglu.prayer_feature.prayertimes.navigation.prayerTimesGraph
import com.kutluoglu.prayer_feature.qibla.navigation.qiblaGraph
import com.kutluoglu.prayer_feature.settings.settingsGraph
import com.kutluoglu.namazvakitleri.locale.LocaleManager
import org.koin.android.ext.android.get
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject


@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    // Get the current back stack entry as state to observe changes
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    // Get the current route from the back stack entry
    val currentGraph = navBackStackEntry?.destination?.parent?.route
    val currentRoute = navBackStackEntry?.destination?.route
    val analyticsTracker: AnalyticsTracker = koinInject()

    // Track screen views for analytics
    LaunchedEffect(currentRoute) {
        if (currentRoute != null) {
            analyticsTracker.logEvent(
                AnalyticsEvents.SCREEN_VIEW,
                mapOf(AnalyticsParams.SCREEN_NAME to currentRoute)
            )
        }
    }

    val context = LocalContext.current
    val activity = context.findActivity()

    val updateViewModel: UpdateViewModel = koinViewModel()
    val updateState by updateViewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updateViewModel.checkForUpdate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun applyLanguage(language: String) {
        activity?.get<LocaleManager>()?.setLanguage(language)
        activity?.recreate()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                // Manually set the height to a smaller value. 56.dp is a common choice.
                modifier = Modifier.height(56.dp),
                // By providing empty WindowInsets, we stop the NavigationBar
                // from adding extra padding at the bottom.
                windowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                Destination.entries.forEach { destination ->
                    val isSelected = currentGraph == destination.graph
                    // NEW CUSTOM BUTTON
                    NavButton(
                        destination = destination,
                        isSelected = isSelected,
                        onClick = {
                            navController.navigate(route = destination.graph) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    ) { contentPadding ->
        NavHost(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            navController = navController,
            startDestination = PrayerNestedGraph.HOME
        ) {
            // Define the nested graphs with their new routes
            navigation(
                route = PrayerNestedGraph.HOME,
                startDestination = Screen.HomeScreen.route
            ) {
                homeGraph(navController)
            }
            navigation(
                route = PrayerNestedGraph.PRAYER_TIMES,
                startDestination = Screen.PayerTimesScreen.route
            ) {
                prayerTimesGraph(navController)
            }
            navigation(
                route = PrayerNestedGraph.QIBLA,
                startDestination = Screen.QiblaScreen.route
            ) {
                qiblaGraph(navController)
            }
            navigation(
                route = PrayerNestedGraph.SETTINGS,
                startDestination = Screen.SettingsScreen.route
            ) {
                settingsGraph(navController, onLanguageSelected = ::applyLanguage)
            }
        }
    }

    when (val state = updateState) {
        is UpdateUiState.ForceUpdate -> ForceUpdateDialog(
            info = state.info,
            urlOpenFailed = state.urlOpenFailed,
            onUpdateClick = updateViewModel::onUpdateClicked,
        )
        is UpdateUiState.OptionalUpdate -> OptionalUpdateDialog(
            info = state.info,
            onUpdateClick = updateViewModel::onUpdateClicked,
            onLaterClick = updateViewModel::onOptionalUpdateDismissed,
        )
        UpdateUiState.NoUpdate -> Unit
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
