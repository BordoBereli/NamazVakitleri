package com.kutluoglu.prayer_feature.settings.notifications

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kutluoglu.core.designsystem.components.LoadingIndicator
import com.kutluoglu.prayer_feature.settings.BuildConfig
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
    var showNotificationRationale by remember { mutableStateOf(false) }
    var notificationPermanentlyDenied by remember { mutableStateOf(false) }
    var pendingPermissionAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    var exactAlarmDialogDismissed by remember { mutableStateOf(false) }
    var pendingExactAlarmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    var batteryDialogDismissed by remember { mutableStateOf(false) }
    var pendingBatteryAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val context = LocalContext.current
    val activity = LocalActivity.current

    fun checkNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun checkExactAlarmPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

    fun checkBatteryOptimization(): Boolean =
        context.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName) == true

    var ignoresBatteryOptimization by remember { mutableStateOf(checkBatteryOptimization()) }
    var hasNotificationPermission by remember { mutableStateOf(checkNotificationPermission()) }
    var canScheduleExactAlarms by remember { mutableStateOf(checkExactAlarmPermission()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = checkNotificationPermission()
                canScheduleExactAlarms = checkExactAlarmPermission()
                ignoresBatteryOptimization = checkBatteryOptimization()
                if (canScheduleExactAlarms) {
                    pendingExactAlarmAction?.invoke()
                }
                pendingExactAlarmAction = null
                if (ignoresBatteryOptimization) {
                    pendingBatteryAction?.invoke()
                }
                pendingBatteryAction = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(settings.enabled, canScheduleExactAlarms, exactAlarmDialogDismissed) {
        if (settings.enabled && !canScheduleExactAlarms && !exactAlarmDialogDismissed) {
            showExactAlarmDialog = true
        }
    }

    LaunchedEffect(
        settings.enabled,
        canScheduleExactAlarms,
        ignoresBatteryOptimization,
        batteryDialogDismissed
    ) {
        if (settings.enabled && canScheduleExactAlarms &&
            !ignoresBatteryOptimization && !batteryDialogDismissed
        ) {
            showBatteryDialog = true
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingPermissionAction?.invoke()
            pendingPermissionAction = null
            showNotificationRationale = false
        } else {
            notificationPermanentlyDenied = activity == null ||
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            showNotificationRationale = true
        }
    }

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
            onCheckedChange = { enabled ->
                if (enabled && !hasNotificationPermission) {
                    pendingPermissionAction = { onEvent(NotificationsEvent.SetEnabled(true)) }
                    showNotificationRationale = false
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else if (enabled && !canScheduleExactAlarms) {
                    pendingExactAlarmAction = { onEvent(NotificationsEvent.SetEnabled(true)) }
                    showExactAlarmDialog = true
                } else if (enabled && !ignoresBatteryOptimization) {
                    pendingBatteryAction = { onEvent(NotificationsEvent.SetEnabled(true)) }
                    showBatteryDialog = true
                } else {
                    onEvent(NotificationsEvent.SetEnabled(enabled))
                }
            }
        )
        NotificationPermissionRationale(
            showRationale = showNotificationRationale,
            hasPermission = hasNotificationPermission,
            permanentlyDenied = notificationPermanentlyDenied,
            onGrantPermission = {
                showNotificationRationale = false
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onOpenSettings = {
                showNotificationRationale = false
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            }
        )
        if (!canScheduleExactAlarms) {
            PermissionHintRow(
                text = stringResource(R.string.exact_alarm_hint),
                actionText = stringResource(R.string.open_settings),
                onAction = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        )
                    }
                }
            )
        }
        if (!ignoresBatteryOptimization) {
            PermissionHintRow(
                text = stringResource(R.string.battery_optimization_hint),
                actionText = stringResource(R.string.open_settings),
                onAction = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                    }
                }
            )
        }
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
            onCheckedChange = { enabled ->
                if (enabled && !hasNotificationPermission) {
                    pendingPermissionAction = { onEvent(NotificationsEvent.SetAdhanEnabled(true)) }
                    showNotificationRationale = false
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else if (enabled && !canScheduleExactAlarms) {
                    pendingExactAlarmAction = { onEvent(NotificationsEvent.SetAdhanEnabled(true)) }
                    showExactAlarmDialog = true
                } else if (enabled && !ignoresBatteryOptimization) {
                    pendingBatteryAction = { onEvent(NotificationsEvent.SetAdhanEnabled(true)) }
                    showBatteryDialog = true
                } else {
                    onEvent(NotificationsEvent.SetAdhanEnabled(enabled))
                }
            }
        )
        if (settings.adhanEnabled) {
            AdhanVolumeSlider(
                volume = settings.adhanVolume,
                onVolumeChange = { onEvent(NotificationsEvent.SetAdhanVolume(it)) }
            )
        }
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
        if (BuildConfig.DEBUG) {
            var testAdhanDelayMinutes by remember { mutableStateOf(5) }
            HorizontalDivider()
            Text(
                text = stringResource(R.string.test_adhan),
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = testAdhanDelayMinutes == 0,
                    onClick = { testAdhanDelayMinutes = 0 },
                    label = { Text(stringResource(R.string.test_adhan_instant)) }
                )
                listOf(1, 5, 10, 15).forEach { minutes ->
                    FilterChip(
                        selected = testAdhanDelayMinutes == minutes,
                        onClick = { testAdhanDelayMinutes = minutes },
                        label = { Text("${minutes}m") }
                    )
                }
            }
            if (!settings.adhanEnabled) {
                Text(
                    text = stringResource(R.string.test_adhan_adhan_off_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = {
                    if (checkExactAlarmPermission()) {
                        onEvent(NotificationsEvent.ScheduleTestAdhan(testAdhanDelayMinutes))
                    } else {
                        pendingExactAlarmAction = {
                            onEvent(NotificationsEvent.ScheduleTestAdhan(testAdhanDelayMinutes))
                        }
                        showExactAlarmDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.schedule_adhan_test))
            }
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

    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = {
                showExactAlarmDialog = false
                exactAlarmDialogDismissed = true
                pendingExactAlarmAction = null
            },
            title = { Text(stringResource(R.string.exact_alarm_dialog_title)) },
            text = { Text(stringResource(R.string.exact_alarm_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showExactAlarmDialog = false
                    exactAlarmDialogDismissed = true
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        )
                    }
                }) {
                    Text(stringResource(R.string.exact_alarm_grant))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExactAlarmDialog = false
                    exactAlarmDialogDismissed = true
                    pendingExactAlarmAction = null
                }) {
                    Text(stringResource(R.string.exact_alarm_not_now))
                }
            }
        )
    }

    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = {
                showBatteryDialog = false
                batteryDialogDismissed = true
                pendingBatteryAction = null
            },
            title = { Text(stringResource(R.string.battery_optimization_dialog_title)) },
            text = { Text(stringResource(R.string.battery_optimization_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showBatteryDialog = false
                    batteryDialogDismissed = true
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                    }
                }) {
                    Text(stringResource(R.string.battery_optimization_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBatteryDialog = false
                    batteryDialogDismissed = true
                    pendingBatteryAction = null
                }) {
                    Text(stringResource(R.string.battery_optimization_not_now))
                }
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

@Composable
private fun AdhanVolumeSlider(
    volume: Int,
    onVolumeChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.adhan_volume),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$volume%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = volume.toFloat(),
            onValueChange = { onVolumeChange(it.toInt()) },
            valueRange = 0f..100f
        )
    }
}

@Composable
private fun PermissionHintRow(
    text: String,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAction) {
            Text(actionText)
        }
    }
}

internal fun shouldShowNotificationRationale(
    showRationale: Boolean,
    hasPermission: Boolean
): Boolean = showRationale && !hasPermission

@Composable
internal fun NotificationPermissionRationale(
    showRationale: Boolean,
    hasPermission: Boolean,
    permanentlyDenied: Boolean,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (shouldShowNotificationRationale(showRationale, hasPermission)) {
        if (permanentlyDenied) {
            PermissionHintRow(
                text = stringResource(R.string.notification_permission_rationale),
                actionText = stringResource(R.string.open_settings),
                onAction = onOpenSettings
            )
        } else {
            PermissionHintRow(
                text = stringResource(R.string.notification_permission_rationale),
                actionText = stringResource(R.string.grant_permission),
                onAction = onGrantPermission
            )
        }
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
