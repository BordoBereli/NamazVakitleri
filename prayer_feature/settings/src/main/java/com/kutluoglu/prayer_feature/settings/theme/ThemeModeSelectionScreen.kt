package com.kutluoglu.prayer_feature.settings.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.designsystem.components.BackNavigationIcon
import com.kutluoglu.core.designsystem.components.LoadingIndicator
import com.kutluoglu.core.designsystem.components.RoundedPageTitleBar
import com.kutluoglu.prayer_feature.settings.R as SettingsR
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

data class ThemeMode(
    val id: String,
    val nameRes: Int
)

val themeModes = listOf(
    ThemeMode("dark", SettingsR.string.theme_dark),
    ThemeMode("light", SettingsR.string.theme_light),
    ThemeMode("system", SettingsR.string.theme_system)
)

sealed class ThemeModeUiState {
    data object Loading : ThemeModeUiState()
    data class ThemeModesLoaded(
        val themeModes: List<ThemeMode>,
        val selectedThemeMode: String
    ) : ThemeModeUiState()
    data class Error(val message: String) : ThemeModeUiState()
}

sealed class ThemeModeEvent {
    data class SelectThemeMode(val themeMode: ThemeMode) : ThemeModeEvent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeModeSelectionRoute(
    onNavigateBack: () -> Unit,
    onThemeModeSelected: (String) -> Unit,
    viewModel: ThemeModeSelectionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.selectedThemeMode.collectLatest { mode ->
            onThemeModeSelected(mode)
        }
    }

    Scaffold(
        topBar = {
            RoundedPageTitleBar(
                title = stringResource(SettingsR.string.select_theme),
                navigationIcon = { BackNavigationIcon(onClick = onNavigateBack) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ThemeModeUiState.Loading -> {
                    LoadingIndicator()
                }
                is ThemeModeUiState.ThemeModesLoaded -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.themeModes) { themeMode ->
                            ThemeModeItem(
                                themeMode = themeMode,
                                isSelected = themeMode.id == state.selectedThemeMode,
                                onClick = { viewModel.onEvent(ThemeModeEvent.SelectThemeMode(themeMode)) }
                            )
                        }
                    }
                }
                is ThemeModeUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeItem(
    themeMode: ThemeMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Text(
                text = stringResource(themeMode.nameRes),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
