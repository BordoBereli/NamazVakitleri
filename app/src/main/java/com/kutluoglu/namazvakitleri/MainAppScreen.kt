package com.kutluoglu.namazvakitleri

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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


@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    // Get the current back stack entry as state to observe changes
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    // Get the current route from the back stack entry
    val currentGraph = navBackStackEntry?.destination?.parent?.route

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
                settingsGraph(navController)
            }
        }
    }
}