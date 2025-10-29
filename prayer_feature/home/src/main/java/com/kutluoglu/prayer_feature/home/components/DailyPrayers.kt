package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.kutluoglu.core.ui.theme.navigation.NestedGraph
import com.kutluoglu.prayer.model.Prayer
import com.kutluoglu.prayer_feature.home.HomeUiState
import com.kutluoglu.prayer_feature.home.R

@Composable
fun DailyPrayers(uiState: HomeUiState, navController: NavController) {
    when (val state = uiState) {
        is HomeUiState.Loading -> {
            // A simple, reusable composable for showing a loading indicator.
            LoadingIndicator()
        }
        is HomeUiState.Error -> {
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
                    navController.navigate(NestedGraph.PRAYER_TIMES) {
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
