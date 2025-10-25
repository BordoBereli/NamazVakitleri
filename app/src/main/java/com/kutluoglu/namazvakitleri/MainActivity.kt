package com.kutluoglu.namazvakitleri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults.contentPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.kutluoglu.core.ui.theme.NamazVakitleriTheme
import com.kutluoglu.namazvakitleri.bottom_navigation.Destination
import com.kutluoglu.prayer_feature.home.navigation.homeGraph
import com.kutluoglu.prayer_navigation.core.Screen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kutluoglu.prayer_feature.prayertimes.navigation.prayerTimesGraph

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NamazVakitleriTheme {
                val modifier = Modifier
                val navController = rememberNavController()
                // Get the current back stack entry as state to observe changes
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                // Get the current route from the back stack entry
                val currentRoute = navBackStackEntry?.destination?.route
                // Find the current Destination enum based on the route
                val currentDestination = Destination.entries.find { it.route == currentRoute }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = modifier,
                        /*bottomBar = {
                            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                                Destination.entries.forEach { destination ->
                                    val isSelected = currentRoute == destination.route
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            // This ensures you don't build up a large back stack
                                            navController.navigate(route = destination.route) {
                                                // Pop up to the start destination of the graph to
                                                // avoid building up a large stack of destinations
                                                // on the back stack as users select items
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                // Avoid multiple copies of the same destination when
                                                // re-selecting the same item
                                                launchSingleTop = true
                                                // Restore state when re-selecting a previously selected item
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                ImageVector.vectorResource(destination.iconDrawable),
                                                contentDescription = destination.contentDescription
                                            )
                                        },
                                        label = { Text(destination.label) },
                                    )
                                }
                            }
                        }*/
                    ) { contentPadding ->
                        val padding = contentPadding
                        NavHost(
                            navController = navController,
                            startDestination = Screen.HomeScreen.route
                        ) {
                            homeGraph(navController)
                            prayerTimesGraph(navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
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