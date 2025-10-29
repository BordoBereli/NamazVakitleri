package com.kutluoglu.namazvakitleri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.kutluoglu.core.ui.theme.NamazVakitleriTheme
import com.kutluoglu.prayer_navigation.core.Destination
import com.kutluoglu.prayer_feature.home.navigation.homeGraph
import com.kutluoglu.prayer_navigation.core.Screen
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import com.kutluoglu.prayer_navigation.core.PrayerNestedGraph
import com.kutluoglu.prayer_navigation.core.NavButton
import com.kutluoglu.prayer_feature.prayertimes.navigation.prayerTimesGraph

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NamazVakitleriTheme(darkTheme = true) {
               MainAppScreen()
            }
        }
    }
}

@Composable
private fun MainAppScreen() {
    val modifier = Modifier
    val navController = rememberNavController()
    // Get the current back stack entry as state to observe changes
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    // Get the current route from the back stack entry
    val currentGraph = navBackStackEntry?.destination?.parent?.route
    // Find the current Destination enum based on the route
    val currentDestination = Destination.entries.find { it.graph == currentGraph }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {},
        bottomBar = {
            NavigationBar(
                // Manually set the height to a smaller value. 56.dp is a common choice.
                modifier = Modifier.height(56.dp),
                // By providing empty WindowInsets, we stop the NavigationBar
                // from adding extra padding at the bottom.
                windowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = MaterialTheme.colorScheme.background
            ) {
                // Add a spacer to create padding from the screen edges
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
                    // Add spacing between the buttons
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
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NamazVakitleriTheme {
        Greeting("Android")
    }
}