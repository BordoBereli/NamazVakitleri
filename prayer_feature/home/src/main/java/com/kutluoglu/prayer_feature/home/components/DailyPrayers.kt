package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.kutluoglu.core.ui.theme.components.ErrorMessage
import com.kutluoglu.core.ui.theme.components.LoadingIndicator
import com.kutluoglu.prayer_navigation.core.PrayerNestedGraph
import com.kutluoglu.prayer.model.Prayer
import com.kutluoglu.prayer_feature.home.HomeUiState
import com.kutluoglu.prayer_feature.home.R

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DailyPrayers(
        uiState: HomeUiState,
        navController: NavController,
        isRefreshing: Boolean,
        onRefresh: () -> Unit
) {
    // Create the state for the pull-to-refresh component
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh // This calls the lambda that triggers the ViewModel event
    )
    Box(modifier = Modifier.pullRefresh(pullRefreshState)) {

        when (val state = uiState) {
            is HomeUiState.Loading -> {
                // Show a centered indicator only on initial load, not on pull-refresh
                // The PullRefreshIndicator will show for subsequent loads.
                if (!isRefreshing) { // This check can be refined if needed
                    LoadingIndicator()
                }
            }

            is HomeUiState.Error   -> {
                // A reusable composable for showing an error message.
                ErrorMessage(message = state.message)
            }

            is HomeUiState.Success -> {
                // The success state delegates the complex grid logic to a dedicated composable.
                PrayerGrid(
                    prayers = state.data.prayers,
                    navController = navController
                )
            }
        }
        // Place the indicator at the top center of the Box
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun PrayerGrid(prayers: List<Prayer>, navController: NavController) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // The header is now in its own item with a clear name.
        item(span = { GridItemSpan(3) }) {
            PrayerGridHeader(navController = navController)
        }
        // The list of prayers remains clean and simple.
        items(prayers, key = { it.name }) { prayer ->
            PrayerCard(
                prayer = prayer,
                isCurrent = prayer.isCurrent
            )
        }
    }
}

@Composable
private fun PrayerGridHeader(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp), // Add padding for alignment
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.prayer_times_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        )
        Text(
            text = stringResource(id = R.string.view_all_prayers),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .clickable {
                    navController.navigate(PrayerNestedGraph.PRAYER_TIMES) {
                        // This navigation logic is standard and correct.
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
        )
    }
}
