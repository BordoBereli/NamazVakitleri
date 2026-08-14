package com.kutluoglu.prayer_feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.designsystem.R
import com.kutluoglu.core.designsystem.components.LoadingIndicator
import com.kutluoglu.prayer_feature.settings.R as SettingsR
import com.kutluoglu.prayer_settings.domain.model.Settings
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToMyLocations: () -> Unit,
    onNavigateToCalculationMethod: () -> Unit,
    onNavigateToHijriAdjustment: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearCacheDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onEvent(SettingsEvent.LoadSettings)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is SettingsUiState.Loading -> {
                    LoadingIndicator()
                }
                is SettingsUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.onEvent(SettingsEvent.LoadSettings) }
                    )
                }
                is SettingsUiState.Success -> {
                    SettingsContent(
                        settings = state.settings,
                        onClearCacheClick = { showClearCacheDialog = true },
                        onNavigateToMyLocations = onNavigateToMyLocations,
                        onNavigateToCalculationMethod = onNavigateToCalculationMethod,
                        onNavigateToHijriAdjustment = onNavigateToHijriAdjustment,
                        onNavigateToLanguage = onNavigateToLanguage
                    )
                }
            }
        }
    }

    if (showClearCacheDialog) {
        ClearCacheDialog(
            onConfirm = {
                viewModel.onEvent(SettingsEvent.ClearCache)
                showClearCacheDialog = false
            },
            onDismiss = { showClearCacheDialog = false }
        )
    }
}

@Composable
private fun SettingsContent(
    settings: Settings,
    onClearCacheClick: () -> Unit,
    onNavigateToMyLocations: () -> Unit,
    onNavigateToCalculationMethod: () -> Unit,
    onNavigateToHijriAdjustment: () -> Unit,
    onNavigateToLanguage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                SettingsItem(
                    icon = Icons.Default.LocationOn,
                    title = stringResource(R.string.location),
                    subtitle = stringResource(SettingsR.string.manage_locations),
                    onClick = onNavigateToMyLocations
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsItem(
                    icon = Icons.Default.Public,
                    title = stringResource(R.string.calculation_method),
                    subtitle = getCalculationMethodName(settings.calculationMethod),
                    onClick = onNavigateToCalculationMethod
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsItem(
                    icon = Icons.Default.Schedule,
                    title = stringResource(R.string.hijri_adjustment),
                    subtitle = if (settings.hijriAdjustment == 0) {
                        "0 ${stringResource(R.string.days)}"
                    } else {
                        "${settings.hijriAdjustment} ${stringResource(R.string.days)}"
                    },
                    onClick = onNavigateToHijriAdjustment
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsItem(
                    icon = Icons.Default.Refresh,
                    title = stringResource(R.string.language),
                    subtitle = getLanguageName(settings.language),
                    onClick = onNavigateToLanguage
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClearCacheClick),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.clear_all),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun ClearCacheDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clear_all)) },
        text = { Text(stringResource(R.string.clear_cache_confirmation)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.back))
            }
        }
    )
}

@Composable
private fun getCalculationMethodName(method: String): String {
    return when (method) {
        "TURKEY_DIYANET" -> "Türkiye (Diyanet)"
        "MUSLIM_WORLD_LEAGUE" -> "Muslim World League"
        "EGYPT_SURVEY" -> "Egyptian Survey"
        "MAKKAH" -> "Umm Al-Qura University"
        "KARACHI" -> "University of Karachi"
        "TEHRAN" -> "Institute of Geophysics, University of Tehran"
        else -> method
    }
}

@Composable
private fun getLanguageName(language: String): String {
    return when (language) {
        "tr" -> "Türkçe"
        "en" -> "English"
        "ar" -> "العربية"
        "de" -> "Deutsch"
        "fr" -> "Français"
        "es" -> "Español"
        else -> language
    }
}
