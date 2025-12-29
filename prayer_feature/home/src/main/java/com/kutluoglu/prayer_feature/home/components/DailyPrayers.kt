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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.ui.theme.components.ErrorMessage
import com.kutluoglu.core.ui.theme.components.LoadingIndicator
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.common.LocalIsLandscape
import com.kutluoglu.prayer_feature.home.HomeUiState
import com.kutluoglu.prayer_feature.home.PrayerUiState
import com.kutluoglu.prayer_feature.home.R

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DailyPrayers(
        prayerState: PrayerUiState?,
        isRefreshing: Boolean,
        onRefresh: () -> Unit,
        onViewAllClicked: () -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    Box(modifier = Modifier
        .fillMaxSize()
        .pullRefresh(pullRefreshState)
    ) {
        prayerState?.let {
            PrayerGrid(
                prayers = it.prayers,
                onViewAllClicked = onViewAllClicked
            )
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
private fun PrayerGrid(
        prayers: List<Prayer>,
        onViewAllClicked: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = { GridItemSpan(3) }) {
            PrayerGridHeader(onViewAllClicked = onViewAllClicked)
        }
        items(prayers, key = { it.name }) { prayer ->
            PrayerCard(
                prayer = prayer,
                isCurrent = prayer.isCurrent
            )
        }
    }
}

@Composable
private fun PrayerGridHeader(onViewAllClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
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
                .clickable { onViewAllClicked() }
        )
    }
}
