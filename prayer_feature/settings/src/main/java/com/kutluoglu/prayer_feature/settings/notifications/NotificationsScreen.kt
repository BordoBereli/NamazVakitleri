package com.kutluoglu.prayer_feature.settings.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.designsystem.components.LoadingIndicator
import com.kutluoglu.prayer_feature.settings.R
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsRoute(
    onNavigateBack: () -> Unit,
    viewModel: NotificationsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                is NotificationsUiState.Loading -> {
                    LoadingIndicator()
                }
                is NotificationsUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.onEvent(NotificationsEvent.Load) }
                    )
                }
                is NotificationsUiState.Success -> {
                    NotificationsContent(
                        settings = state.settings,
                        onEvent = viewModel::onEvent
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsContent(
    settings: NotificationSettings,
    onEvent: (NotificationsEvent) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToggleRow(
            title = stringResource(R.string.notifications_enabled),
            checked = settings.enabled,
            onCheckedChange = { onEvent(NotificationsEvent.SetEnabled(it)) }
        )
        HorizontalDivider()
        NotificationSettings.PRAYER_KEYS.forEach { key ->
            ToggleRow(
                title = prayerNameRes(key),
                checked = settings.prayerToggles[key] ?: true,
                onCheckedChange = {
                    onEvent(NotificationsEvent.SetPrayerToggle(key, it))
                }
            )
        }
        HorizontalDivider()
        ToggleRow(
            title = stringResource(R.string.adhan),
            checked = settings.adhanEnabled,
            onCheckedChange = { onEvent(NotificationsEvent.SetAdhanEnabled(it)) }
        )
        ToggleRow(
            title = stringResource(R.string.countdown),
            checked = settings.countdownEnabled,
            onCheckedChange = { onEvent(NotificationsEvent.SetCountdownEnabled(it)) }
        )
        ToggleRow(
            title = stringResource(R.string.pre_prayer_reminder),
            checked = settings.prePrayerReminderEnabled,
            onCheckedChange = {
                onEvent(
                    NotificationsEvent.SetPrePrayerReminder(it, settings.prePrayerMinutes)
                )
            }
        )
        if (settings.prePrayerReminderEnabled) {
            PrePrayerMinutesSelector(
                selectedMinutes = settings.prePrayerMinutes,
                onMinutesSelected = { minutes ->
                    onEvent(NotificationsEvent.SetPrePrayerReminder(true, minutes))
                }
            )
        }
        ToggleRow(
            title = stringResource(R.string.daily_reminder),
            checked = settings.dailyReminderEnabled,
            onCheckedChange = { enabled ->
                onEvent(
                    NotificationsEvent.SetDailyReminder(
                        enabled,
                        settings.dailyReminderHour,
                        settings.dailyReminderMinute
                    )
                )
            }
        )
        if (settings.dailyReminderEnabled) {
            DailyReminderTimeRow(
                hour = settings.dailyReminderHour,
                minute = settings.dailyReminderMinute,
                onClick = { showTimePicker = true }
            )
        }
        ToggleRow(
            title = stringResource(R.string.jumuah),
            checked = settings.jumuahEnabled,
            onCheckedChange = { onEvent(NotificationsEvent.SetJumuahEnabled(it)) }
        )
        ToggleRow(
            title = stringResource(R.string.special_days),
            checked = settings.specialDaysEnabled,
            onCheckedChange = { onEvent(NotificationsEvent.SetSpecialDaysEnabled(it)) }
        )
        ToggleRow(
            title = stringResource(R.string.sound),
            checked = settings.soundEnabled,
            onCheckedChange = { onEvent(NotificationsEvent.SetSoundEnabled(it)) }
        )
        ToggleRow(
            title = stringResource(R.string.vibration),
            checked = settings.vibrationEnabled,
            onCheckedChange = { onEvent(NotificationsEvent.SetVibrationEnabled(it)) }
        )
        Button(
            onClick = { onEvent(NotificationsEvent.SendTest) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.send_test_notification))
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = settings.dailyReminderHour,
            initialMinute = settings.dailyReminderMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                onEvent(NotificationsEvent.SetDailyReminder(true, hour, minute))
                showTimePicker = false
            }
        )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrePrayerMinutesSelector(
    selectedMinutes: Int,
    onMinutesSelected: (Int) -> Unit
) {
    val options = listOf(5, 10, 15, 30, 60)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.minutes_before),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { minutes ->
                FilterChip(
                    selected = selectedMinutes == minutes,
                    onClick = { onMinutesSelected(minutes) },
                    label = { Text("$minutes") }
                )
            }
        }
    }
}

@Composable
private fun DailyReminderTimeRow(
    hour: Int,
    minute: Int,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
    ) {
        Text(formatTime(hour, minute))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.daily_reminder)) },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
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
private fun prayerNameRes(key: String): String = when (key) {
    "Fajr" -> stringResource(R.string.prayer_fajr)
    "Dhuhr" -> stringResource(R.string.prayer_dhuhr)
    "Asr" -> stringResource(R.string.prayer_asr)
    "Maghrib" -> stringResource(R.string.prayer_maghrib)
    "Isha" -> stringResource(R.string.prayer_isha)
    else -> key
}

private fun formatTime(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
