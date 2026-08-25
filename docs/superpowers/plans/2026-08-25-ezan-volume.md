# Ezan Volume Control Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a 0–100% volume slider for the Ezan (adhan) sound in the notification settings, persisted via DataStore and applied through `MediaPlayer.setVolume()` at playback time. Default volume is 30%.

**Architecture:** `NotificationSettings` gains an `adhanVolume: Int = 30` field, persisted by `NotificationSettingsDataStore` (new `ADHAN_VOLUME` int key) and written by `UpdateNotificationSettingsUseCase`. `AdhanPlayer.play(prayerKey, volumePercent)` applies `setVolume(v, v)` where `v = volumePercent / 100f`. `AdhanService` injects the DataStore, reads the volume itself (the service owns its settings), and passes it to `play()` from a `serviceScope` coroutine. The settings UI exposes a `SetAdhanVolume` event and renders a `Slider` below the Ezan toggle only when `adhanEnabled` is true.

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose (Material3 `Slider`), Koin (KSP annotations), Jetpack DataStore Preferences, Robolectric + MockK + Truth + JUnit 5 for tests.

---

## Impact Analysis (read before starting)

GitNexus flags `NotificationSettings` and `NotificationSettingsDataStore` as **HIGH** risk because they are widely-used classes. **All changes in this plan are additive** — a new field with a default value, a new preference key, a new method, and one new line in `toSettings()` — so no existing callers or constructor call sites break. `AdhanPlayer` is **LOW** risk; its `play()` signature change is the only breaking edit and is handled atomically with its call site in Task 4. Proceed with normal caution; no behavioral changes to existing settings.

---

### Task 1: Add `adhanVolume` to `NotificationSettings` model

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/NotificationSettings.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/domain/NotificationSettingsTest.kt`

- [ ] **Step 1: Write the failing test**

Add this test to `NotificationSettingsTest.kt` (after the existing `adhan defaults to off` test):

```kotlin
@Test
fun `adhan volume defaults to 30`() {
    assertThat(NotificationSettings().adhanVolume).isEqualTo(30)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.domain.NotificationSettingsTest"`
Expected: COMPILATION FAILURE — `adhanVolume` does not exist on `NotificationSettings`.

- [ ] **Step 3: Implement the field**

In `NotificationSettings.kt`, add `val adhanVolume: Int = 30` after `adhanEnabled`:

```kotlin
data class NotificationSettings(
    val enabled: Boolean = false,
    val prayerToggles: Map<String, Boolean> = defaultPrayerToggles(),
    val adhanEnabled: Boolean = false,
    val adhanVolume: Int = 30,
    val countdownEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderHour: Int = 8,
    val dailyReminderMinute: Int = 0,
    val prePrayerReminderEnabled: Boolean = false,
    val prePrayerMinutes: Int = 15,
    val jumuahEnabled: Boolean = true,
    val specialDaysEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
) {
    companion object {
        val PRAYER_KEYS = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

        fun defaultPrayerToggles(): Map<String, Boolean> =
            PRAYER_KEYS.associateWith { true }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.domain.NotificationSettingsTest"`
Expected: PASS (both tests).

- [ ] **Step 5: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/NotificationSettings.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/domain/NotificationSettingsTest.kt
git commit -m "feat(notifications): add adhanVolume to NotificationSettings"
```

---

### Task 2: Persist `adhanVolume` in `NotificationSettingsDataStore`

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/data/NotificationSettingsDataStore.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/data/NotificationSettingsDataStoreTest.kt`

- [ ] **Step 1: Write the failing tests**

Add these two tests to `NotificationSettingsDataStoreTest.kt` (after the last test):

```kotlin
@Test
fun `adhan volume defaults to 30`() = runTest {
    val store = freshStore()
    assertThat(store.getSettings().adhanVolume).isEqualTo(30)
}

@Test
fun `updateAdhanVolume persists`() = runTest {
    val store = freshStore()
    store.updateAdhanVolume(50)
    assertThat(store.getSettings().adhanVolume).isEqualTo(50)
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStoreTest"`
Expected: COMPILATION FAILURE — `updateAdhanVolume` does not exist, and `adhanVolume` is not a property of `NotificationSettings` returned by `getSettings()`.

- [ ] **Step 3: Implement the key, method, and read**

In `NotificationSettingsDataStore.kt`:

1. Add the key inside the `Keys` object (after `ADHAN_ENABLED`):

```kotlin
val ADHAN_VOLUME = intPreferencesKey("adhan_volume")
```

2. Add the update method (after `updateAdhanEnabled`):

```kotlin
suspend fun updateAdhanVolume(volume: Int) = dataStore.edit { it[Keys.ADHAN_VOLUME] = volume }
```

3. Add the read in `toSettings()` (after `adhanEnabled = ...`):

```kotlin
adhanVolume = this[Keys.ADHAN_VOLUME] ?: 30,
```

The `toSettings()` return block should now read:

```kotlin
return NotificationSettings(
    enabled = this[Keys.ENABLED] ?: false,
    prayerToggles = toggles,
    adhanEnabled = this[Keys.ADHAN_ENABLED] ?: false,
    adhanVolume = this[Keys.ADHAN_VOLUME] ?: 30,
    countdownEnabled = this[Keys.COUNTDOWN_ENABLED] ?: true,
    dailyReminderEnabled = this[Keys.DAILY_REMINDER_ENABLED] ?: false,
    dailyReminderHour = this[Keys.DAILY_REMINDER_HOUR] ?: 8,
    dailyReminderMinute = this[Keys.DAILY_REMINDER_MINUTE] ?: 0,
    prePrayerReminderEnabled = this[Keys.PRE_PRAYER_ENABLED] ?: false,
    prePrayerMinutes = this[Keys.PRE_PRAYER_MINUTES] ?: 15,
    jumuahEnabled = this[Keys.JUMUAH_ENABLED] ?: true,
    specialDaysEnabled = this[Keys.SPECIAL_DAYS_ENABLED] ?: true,
    soundEnabled = this[Keys.SOUND_ENABLED] ?: true,
    vibrationEnabled = this[Keys.VIBRATION_ENABLED] ?: true
)
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStoreTest"`
Expected: PASS (all tests, including the 2 new ones).

- [ ] **Step 5: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/data/NotificationSettingsDataStore.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/data/NotificationSettingsDataStoreTest.kt
git commit -m "feat(notifications): persist adhanVolume in DataStore"
```

---

### Task 3: Persist `adhanVolume` in `UpdateNotificationSettingsUseCase`

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/usecases/UpdateNotificationSettingsUseCase.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/domain/usecases/NotificationUseCasesTest.kt`

- [ ] **Step 1: Write the failing test**

Add this test to `NotificationUseCasesTest.kt` (after the existing `UpdateNotificationSettingsUseCase persists and reschedules` test):

```kotlin
@Test
fun `UpdateNotificationSettingsUseCase persists adhan volume`() = runTest {
    val scheduler = mockk<PrayerNotificationScheduler>(relaxed = true)
    val useCase = UpdateNotificationSettingsUseCase(dataStore, scheduler, notificationManager)
    useCase(NotificationSettings(enabled = true, adhanVolume = 50))
    coVerify { dataStore.updateAdhanVolume(50) }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.domain.usecases.NotificationUseCasesTest"`
Expected: FAIL — `dataStore.updateAdhanVolume(50)` is never called (MockK `coVerify` throws).

- [ ] **Step 3: Implement the persistence call**

In `UpdateNotificationSettingsUseCase.kt`, add the call right after `updateAdhanEnabled`:

```kotlin
dataStore.updateAdhanEnabled(settings.adhanEnabled)
dataStore.updateAdhanVolume(settings.adhanVolume)
```

The `invoke` body should now read:

```kotlin
suspend operator fun invoke(settings: NotificationSettings) {
    dataStore.updateEnabled(settings.enabled)
    dataStore.updateAdhanEnabled(settings.adhanEnabled)
    dataStore.updateAdhanVolume(settings.adhanVolume)
    dataStore.updateCountdownEnabled(settings.countdownEnabled)
    dataStore.updateDailyReminder(
        settings.dailyReminderEnabled,
        settings.dailyReminderHour,
        settings.dailyReminderMinute
    )
    dataStore.updatePrePrayerReminder(
        settings.prePrayerReminderEnabled,
        settings.prePrayerMinutes
    )
    dataStore.updateJumuahEnabled(settings.jumuahEnabled)
    dataStore.updateSpecialDaysEnabled(settings.specialDaysEnabled)
    dataStore.updateSoundEnabled(settings.soundEnabled)
    dataStore.updateVibrationEnabled(settings.vibrationEnabled)
    settings.prayerToggles.forEach { (key, enabled) ->
        dataStore.updatePrayerToggle(key, enabled)
    }
    if (settings.enabled) {
        notificationManager.createChannels(settings)
        scheduler.scheduleAll()
    } else {
        scheduler.cancelAll()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.domain.usecases.NotificationUseCasesTest"`
Expected: PASS (all tests).

- [ ] **Step 5: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/usecases/UpdateNotificationSettingsUseCase.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/domain/usecases/NotificationUseCasesTest.kt
git commit -m "feat(notifications): persist adhanVolume in UpdateNotificationSettingsUseCase"
```

---

### Task 4: Add volume parameter to `AdhanPlayer.play`

This task changes the `play()` signature, so the `AdhanService` call site and its test are updated in the same task to keep the module compiling. `AdhanService` temporarily passes a hardcoded `30`; Task 5 replaces that with the real DataStore read.

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayer.kt`
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AdhanService.kt` (call site only)
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayerTest.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/AdhanServiceTest.kt` (verify call only)

- [ ] **Step 1: Write the failing test**

Add this test to `AdhanPlayerTest.kt` (after the last test):

```kotlin
@Test
fun `play applies volume as a fraction`() {
    val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_fajr}")
    ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, uri), ShadowMediaPlayer.MediaInfo())
    var createdPlayer: MediaPlayer? = null
    ShadowMediaPlayer.setCreateListener { player, _ -> createdPlayer = player }
    val player = AdhanPlayer(context)
    player.play("Fajr", 30)
    val mediaPlayer = createdPlayer ?: error("no player created")
    assertThat(shadowOf(mediaPlayer).getLeftVolume()).isEqualTo(0.3f)
    assertThat(shadowOf(mediaPlayer).getRightVolume()).isEqualTo(0.3f)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.manager.AdhanPlayerTest"`
Expected: COMPILATION FAILURE — `play` takes 1 argument but 2 were provided.

- [ ] **Step 3: Update the existing `AdhanPlayerTest` calls**

Update every existing `play(...)` call in `AdhanPlayerTest.kt` to pass a volume:

- `player.play("Fajr")` → `player.play("Fajr", 30)` (in `play and stop do not throw`, `play does not throw when playback fails`, `repeated play calls do not throw`, `completion listener is invoked when playback completes`)
- `player.play("Dhuhr")` → `player.play("Dhuhr", 30)` (in `stop is idempotent`)
- `player.play("Isha")` → `player.play("Isha", 30)` (in `repeated play calls do not throw`)

- [ ] **Step 4: Update the `AdhanService` call site and `AdhanServiceTest` verify**

In `AdhanService.kt`, change the call in `onStartCommand`:

```kotlin
adhanPlayer.play(prayerKey, 30)
```

In `AdhanServiceTest.kt`, change the verify in `onStartCommand plays adhan and shows foreground notification`:

```kotlin
verify { adhanPlayer.play("Fajr", 30) }
```

- [ ] **Step 5: Implement the volume in `AdhanPlayer`**

In `AdhanPlayer.kt`, change the `play` signature and apply `setVolume` after `start()`:

```kotlin
fun play(prayerKey: String, volumePercent: Int) {
    stop()
    val volume = volumePercent.coerceIn(0, 100) / 100f
    val resId = when (prayerKey) {
        "Fajr" -> R.raw.adhan_fajr
        "Dhuhr" -> R.raw.adhan_dhuhr
        "Asr" -> R.raw.adhan_asr
        "Maghrib" -> R.raw.adhan_maghrib
        "Isha" -> R.raw.adhan_isha
        else -> R.raw.adhan_fajr
    }
    val player = MediaPlayer()
    try {
        player.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(context, android.net.Uri.parse("android.resource://${context.packageName}/$resId"))
            setOnCompletionListener { onCompletion?.invoke() }
            prepare()
            start()
            setVolume(volume, volume)
        }
        mediaPlayer = player
    } catch (e: Exception) {
        runCatching { player.release() }
        Log.e("AdhanPlayer", "Failed to play adhan -> ${e.message}")
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.manager.AdhanPlayerTest" --tests="com.kutluoglu.prayer_notifications.scheduler.AdhanServiceTest"`
Expected: PASS (all AdhanPlayer tests including the new volume test, and all AdhanService tests).

- [ ] **Step 7: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayer.kt prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AdhanService.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayerTest.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/AdhanServiceTest.kt
git commit -m "feat(notifications): apply volume to AdhanPlayer playback"
```

---

### Task 5: Read volume in `AdhanService` and pass to `AdhanPlayer`

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AdhanService.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/AdhanServiceTest.kt`

- [ ] **Step 1: Write the failing test**

Update `AdhanServiceTest.kt`:

1. Add imports:

```kotlin
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import io.mockk.coEvery
```

2. Add the mock field (after `notificationManager`):

```kotlin
private val dataStore = mockk<NotificationSettingsDataStore>(relaxed = true)
```

3. Register it in the Koin module in `setUp`:

```kotlin
modules(module {
    single { adhanPlayer }
    single { notificationManager }
    single { dataStore }
})
```

4. Stub the volume read in `setUp` (after the `every { notificationManager.buildAdhanNotification(any()) }` line):

```kotlin
coEvery { dataStore.getSettings() } returns NotificationSettings(adhanVolume = 50)
```

5. Update the verify in `onStartCommand plays adhan and shows foreground notification` to prove the volume comes from the DataStore:

```kotlin
verify { adhanPlayer.play("Fajr", 50) }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.scheduler.AdhanServiceTest"`
Expected: FAIL — `adhanPlayer.play("Fajr", 50)` was not called (AdhanService still passes hardcoded `30`).

- [ ] **Step 3: Implement the DataStore read in `AdhanService`**

In `AdhanService.kt`:

1. Add imports:

```kotlin
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
```

2. Inject the DataStore and add the service scope (after the `notificationManager` injection):

```kotlin
private val adhanPlayer: AdhanPlayer by inject()
private val notificationManager: PrayerNotificationManager by inject()
private val dataStore: NotificationSettingsDataStore by inject()

private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
```

3. Rewrite `onStartCommand` so `startForeground` stays synchronous (satisfies the 5-second rule) and the play call reads the volume inside the coroutine:

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val prayerKey = intent?.getStringExtra(AlarmReceiver.EXTRA_PRAYER_KEY)
    if (prayerKey == null) {
        stopSelf()
        return START_NOT_STICKY
    }
    startForeground(
        PrayerNotificationManager.NOTIFICATION_ID_ADHAN,
        notificationManager.buildAdhanNotification(prayerKey)
    )
    serviceScope.launch {
        val volume = dataStore.getSettings().adhanVolume
        adhanPlayer.play(prayerKey, volume)
    }
    lastAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
    registerVolumeObserver()
    return START_NOT_STICKY
}
```

4. Cancel the scope in `onDestroy` (before the existing cleanup):

```kotlin
override fun onDestroy() {
    serviceScope.cancel()
    unregisterVolumeObserver()
    adhanPlayer.stop()
    stopForeground(STOP_FOREGROUND_REMOVE)
    super.onDestroy()
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.scheduler.AdhanServiceTest"`
Expected: PASS (all 3 tests). `Dispatchers.Main.immediate` runs the coroutine synchronously because Robolectric tests execute on the main thread, so `adhanPlayer.play("Fajr", 50)` is called during `onStartCommand`.

- [ ] **Step 5: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AdhanService.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/AdhanServiceTest.kt
git commit -m "feat(notifications): read adhan volume in AdhanService"
```

---

### Task 6: Add `SetAdhanVolume` event to the ViewModel

**Files:**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsContract.kt`
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsViewModel.kt`
- Test: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

Add this test to `NotificationsViewModelTest.kt` (after `toggling adhan persists`):

```kotlin
@Test
fun `setting adhan volume persists`() = runTest {
    coEvery { getUseCase() } returns NotificationSettings()

    val viewModel = NotificationsViewModel(getUseCase, updateUseCase, notificationManager)
    viewModel.onEvent(NotificationsEvent.SetAdhanVolume(50))

    coVerify { updateUseCase(match { it.adhanVolume == 50 }) }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.settings.notifications.NotificationsViewModelTest"`
Expected: COMPILATION FAILURE — `SetAdhanVolume` does not exist on `NotificationsEvent`.

- [ ] **Step 3: Add the event to the contract**

In `NotificationsContract.kt`, add the event to the sealed class (after `SetAdhanEnabled`):

```kotlin
data class SetAdhanVolume(val volume: Int) : NotificationsEvent()
```

- [ ] **Step 4: Handle the event in the ViewModel**

In `NotificationsViewModel.kt`, add the branch in `onEvent` (after the `SetAdhanEnabled` branch):

```kotlin
is NotificationsEvent.SetAdhanVolume -> update { it.copy(adhanVolume = event.volume) }
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.settings.notifications.NotificationsViewModelTest"`
Expected: PASS (all tests).

- [ ] **Step 6: Commit**

```bash
git add prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsContract.kt prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsViewModel.kt prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsViewModelTest.kt
git commit -m "feat(settings): add SetAdhanVolume event to notifications"
```

---

### Task 7: Add the volume slider to `NotificationsScreen`

**Files:**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreen.kt`
- Test: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreenTest.kt`

- [ ] **Step 1: Write the failing tests**

Add these three tests to `NotificationsScreenTest.kt`, and add the imports:

```kotlin
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.performSemanticsAction
```

```kotlin
@Test
fun `shows adhan volume slider when adhan enabled`() {
    launchScreen(
        NotificationSettings(enabled = true, adhanEnabled = true, adhanVolume = 30)
    )

    composeTestRule.onNodeWithText(
        composeTestRule.activity.getString(R.string.adhan_volume)
    ).assertIsDisplayed()
    composeTestRule.onNodeWithText("30%").assertIsDisplayed()
}

@Test
fun `hides adhan volume slider when adhan disabled`() {
    launchScreen(NotificationSettings(enabled = true, adhanEnabled = false))

    composeTestRule.onNodeWithText(
        composeTestRule.activity.getString(R.string.adhan_volume)
    ).assertDoesNotExist()
}

@Test
fun `adjusting adhan volume slider emits SetAdhanVolume`() {
    launchScreen(
        NotificationSettings(enabled = true, adhanEnabled = true, adhanVolume = 30)
    )

    composeTestRule.onNode(hasProgressBarRangeInfo())
        .performSemanticsAction(SemanticsActions.SetProgress) { it(50f) }
    composeTestRule.waitForIdle()

    coVerify { updateUseCase(match { it.adhanVolume == 50 }) }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.settings.notifications.NotificationsScreenTest"`
Expected: FAIL — `adhan_volume` text is not found (no slider rendered yet).

- [ ] **Step 3: Add the slider to the screen**

In `NotificationsScreen.kt`:

1. Add the import:

```kotlin
import androidx.compose.material3.Slider
```

2. Add the conditional block in `NotificationsContent` immediately after the Ezan `ToggleRow` (the one with `R.string.adhan`):

```kotlin
if (settings.adhanEnabled) {
    AdhanVolumeSlider(
        volume = settings.adhanVolume,
        onVolumeChange = { onEvent(NotificationsEvent.SetAdhanVolume(it)) }
    )
}
```

3. Add the `AdhanVolumeSlider` composable (after the `ToggleRow` composable):

```kotlin
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.settings.notifications.NotificationsScreenTest"`
Expected: PASS (all tests, including the 3 new ones).

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreen.kt prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreenTest.kt
git commit -m "feat(settings): add adhan volume slider to notifications screen"
```

---

### Task 8: Add `adhan_volume` string to all locales

**Files:**
- Modify: all 15 files in `prayer_feature/settings/src/main/res/values*/strings.xml`

- [ ] **Step 1: Add the string to each locale file**

In each file, add the `<string name="adhan_volume">...</string>` line immediately after the `<string name="adhan">...</string>` line:

| File | Value |
|------|-------|
| `values/strings.xml` | `<string name="adhan_volume">Adhan volume</string>` |
| `values-ar/strings.xml` | `<string name="adhan_volume">حجم الأذان</string>` |
| `values-bn/strings.xml` | `<string name="adhan_volume">আজানের ভলিউম</string>` |
| `values-de/strings.xml` | `<string name="adhan_volume">Adhan-Lautstärke</string>` |
| `values-es/strings.xml` | `<string name="adhan_volume">Volumen del Adhan</string>` |
| `values-fa/strings.xml` | `<string name="adhan_volume">صدای اذان</string>` |
| `values-fr/strings.xml` | `<string name="adhan_volume">Volume de l\'Adhan</string>` |
| `values-hi/strings.xml` | `<string name="adhan_volume">अज़ान की आवाज़</string>` |
| `values-id/strings.xml` | `<string name="adhan_volume">Volume Adzan</string>` |
| `values-ms/strings.xml` | `<string name="adhan_volume">Kelantangan Azan</string>` |
| `values-ru/strings.xml` | `<string name="adhan_volume">Громкость азана</string>` |
| `values-ta/strings.xml` | `<string name="adhan_volume">அதான் ஒலி அளவு</string>` |
| `values-th/strings.xml` | `<string name="adhan_volume">ระดับเสียงอาซาน</string>` |
| `values-tr/strings.xml` | `<string name="adhan_volume">Ezan sesi</string>` |
| `values-ur/strings.xml` | `<string name="adhan_volume">اذان کی آواز</string>` |

- [ ] **Step 2: Verify the resource compiles**

Run: `./gradlew :prayer_feature:settings:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (no missing-resource or XML errors).

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/settings/src/main/res/values/strings.xml prayer_feature/settings/src/main/res/values-ar/strings.xml prayer_feature/settings/src/main/res/values-bn/strings.xml prayer_feature/settings/src/main/res/values-de/strings.xml prayer_feature/settings/src/main/res/values-es/strings.xml prayer_feature/settings/src/main/res/values-fa/strings.xml prayer_feature/settings/src/main/res/values-fr/strings.xml prayer_feature/settings/src/main/res/values-hi/strings.xml prayer_feature/settings/src/main/res/values-id/strings.xml prayer_feature/settings/src/main/res/values-ms/strings.xml prayer_feature/settings/src/main/res/values-ru/strings.xml prayer_feature/settings/src/main/res/values-ta/strings.xml prayer_feature/settings/src/main/res/values-th/strings.xml prayer_feature/settings/src/main/res/values-tr/strings.xml prayer_feature/settings/src/main/res/values-ur/strings.xml
git commit -m "feat(settings): add adhan_volume string to all locales"
```

---

## Final Verification

After all tasks are complete, run the full affected test suites:

```bash
./gradlew :prayer_notifications:testDebugUnitTest :prayer_feature:settings:testDebugUnitTest
```

Expected: all tests pass. Then run `gitnexus_detect_changes()` (per AGENTS.md) to confirm the changes only affect the expected symbols before merging.
