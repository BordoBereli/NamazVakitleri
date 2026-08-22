package com.kutluoglu.prayer_feature.settings.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.settings.R
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.manager.PrayerNotificationManager
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsRoute(
    onNavigateBack: () -> Unit,
    viewModel: NotificationsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text(stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        val settings = (uiState as? NotificationsUiState.Success)?.settings
            ?: NotificationSettings()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToggleRow(
                title = stringResource(R.string.notifications_enabled),
                checked = settings.enabled,
                onCheckedChange = { viewModel.onEvent(NotificationsEvent.SetEnabled(it)) }
            )
            HorizontalDivider()
            NotificationSettings.PRAYER_KEYS.forEach { key ->
                ToggleRow(
                    title = stringResource(prayerNameRes(key)),
                    checked = settings.prayerToggles[key] ?: true,
                    onCheckedChange = {
                        viewModel.onEvent(NotificationsEvent.SetPrayerToggle(key, it))
                    }
                )
            }
            HorizontalDivider()
            ToggleRow(
                title = stringResource(R.string.adhan),
                checked = settings.adhanEnabled,
                onCheckedChange = { viewModel.onEvent(NotificationsEvent.SetAdhanEnabled(it)) }
            )
            ToggleRow(
                title = stringResource(R.string.countdown),
                checked = settings.countdownEnabled,
                onCheckedChange = { viewModel.onEvent(NotificationsEvent.SetCountdownEnabled(it)) }
            )
            ToggleRow(
                title = stringResource(R.string.pre_prayer_reminder),
                checked = settings.prePrayerReminderEnabled,
                onCheckedChange = {
                    viewModel.onEvent(
                        NotificationsEvent.SetPrePrayerReminder(it, settings.prePrayerMinutes)
                    )
                }
            )
            ToggleRow(
                title = stringResource(R.string.jumuah),
                checked = settings.jumuahEnabled,
                onCheckedChange = { viewModel.onEvent(NotificationsEvent.SetJumuahEnabled(it)) }
            )
            ToggleRow(
                title = stringResource(R.string.special_days),
                checked = settings.specialDaysEnabled,
                onCheckedChange = { viewModel.onEvent(NotificationsEvent.SetSpecialDaysEnabled(it)) }
            )
            ToggleRow(
                title = stringResource(R.string.sound),
                checked = settings.soundEnabled,
                onCheckedChange = { viewModel.onEvent(NotificationsEvent.SetSoundEnabled(it)) }
            )
            ToggleRow(
                title = stringResource(R.string.vibration),
                checked = settings.vibrationEnabled,
                onCheckedChange = { viewModel.onEvent(NotificationsEvent.SetVibrationEnabled(it)) }
            )
            Button(
                onClick = {
                    PrayerNotificationManager(context).createChannels(settings)
                    PrayerNotificationManager(context).showTestNotification()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.send_test_notification))
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun prayerNameRes(key: String): Int = when (key) {
    "Fajr" -> R.string.prayer_fajr
    "Dhuhr" -> R.string.prayer_dhuhr
    "Asr" -> R.string.prayer_asr
    "Maghrib" -> R.string.prayer_maghrib
    "Isha" -> R.string.prayer_isha
    else -> R.string.notifications
}
