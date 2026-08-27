# Ezan Reliability, Volume Control & Easy Stop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Ezan play exactly at each prayer time (blocking scheduling when the exact-alarm permission is missing and prompting the user), cover the next 24h of prayers so no day is skipped, let the volume buttons adjust the Ezan (volume-down to 0 stops it), and stop a playing Ezan when notifications are disabled.

**Architecture:** `PrayerNotificationScheduler` stops silently falling back to inexact alarms and instead blocks scheduling when `canScheduleExactAlarms()` is false on Android 12+. `SchedulePlan.buildDailyAlarms` schedules today's remaining prayers plus tomorrow's full day. `AdhanService`'s volume observer stops only at alarm-volume 0 (the MediaPlayer follows the alarm stream via `USAGE_ALARM`). `cancelAll()` also stops `AdhanService`. The Notifications screen shows an exact-alarm permission dialog.

**Tech Stack:** Kotlin, Android AlarmManager, MediaPlayer, Compose, Robolectric, MockK, Truth, Koin.

**Spec:** `docs/superpowers/specs/2026-08-27-ezan-reliability-design.md`

---

### Task 1: Scheduler — block inexact scheduling

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt:238-246`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationSchedulerTest.kt`

- [ ] **Step 1: Add `@Before` to the existing test so it grants the exact-alarm permission**

In `PrayerNotificationSchedulerTest.kt`, add these imports at the top (after the existing `org.robolectric.annotation.Config` import):

```kotlin
import org.junit.Before
import org.robolectric.shadows.ShadowAlarmManager
```

Add this method right after the `scheduler(...)` helper (after line 67):

```kotlin
    @Before
    fun grantExactAlarmPermission() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }
```

- [ ] **Step 2: Add the failing test for the no-permission case**

Add this test at the end of `PrayerNotificationSchedulerTest` (before the closing brace):

```kotlin
    @Test
    fun `scheduleAll without exact alarm permission schedules nothing`() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        coEvery { dataStore.getSettings() } returns NotificationSettings(enabled = true)
        coEvery { locationsCoordinator.resolveSelected() } returns LocationData(
            latitude = 41.0082,
            longitude = 28.9784,
            country = "Turkey",
            countryCode = "TR",
            city = "Istanbul",
            county = null
        )
        coEvery { getSettingsUseCase() } returns Settings(
            location = LocationSettings(timeZone = "Europe/Istanbul"),
            calculationMethod = "TURKEY_DIYANET"
        )
        coEvery { getPrayerTimesUseCase(any(), any(), any(), any(), any(), any()) } returns Result.success(
            listOf(
                Prayer(
                    name = "Fajr",
                    arabicName = "الفجر",
                    time = LocalTime(23, 59),
                    date = LocalDate(2026, 8, 22)
                )
            )
        )

        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleAll()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        assertThat(shadowOf(alarmManager).scheduledAlarms).isEmpty()
    }
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.scheduler.PrayerNotificationSchedulerTest"`
Expected: the new test FAILS because the scheduler still schedules inexact alarms (the assertion `scheduledAlarms.isEmpty()` is false).

- [ ] **Step 4: Implement the block in `PrayerNotificationScheduler`**

In `PrayerNotificationScheduler.kt`, replace `setExactAlarm` (lines 238-246):

```kotlin
    private fun setExactAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            Log.w("PrayerNotificationScheduler", "Exact alarm permission missing; skipping alarm")
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
```

In `scheduleAllSuspending`, insert this block immediately after the `if (!settings.enabled) { ... }` block (after line 68):

```kotlin
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            Log.w("PrayerNotificationScheduler", "Exact alarm permission missing; cancelling all")
            cancelAll()
            cancelDailyReschedule()
            return
        }
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.scheduler.PrayerNotificationSchedulerTest"`
Expected: PASS (all tests).

- [ ] **Step 6: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationSchedulerTest.kt
git commit -m "fix(notifications): block scheduling when exact alarm permission missing"
```

---

### Task 2: Add exact-alarm dialog strings to all 15 locales

**Files:**
- Modify: `prayer_feature/settings/src/main/res/values/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-ar/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-bn/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-de/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-es/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-fa/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-fr/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-hi/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-id/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-ms/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-ru/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-ta/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-th/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-tr/strings.xml`
- Modify: `prayer_feature/settings/src/main/res/values-ur/strings.xml`

- [ ] **Step 1: Add the four strings to each locale file**

In each `strings.xml`, add these four lines directly after the `exact_alarm_hint` line:

`values/strings.xml` (en):
```xml
    <string name="exact_alarm_dialog_title">Alarms &amp; reminders permission</string>
    <string name="exact_alarm_dialog_body">To play the Ezan exactly on time, grant the "Alarms &amp; reminders" permission.</string>
    <string name="exact_alarm_grant">Grant</string>
    <string name="exact_alarm_not_now">Not now</string>
```

`values-ar/strings.xml`:
```xml
    <string name="exact_alarm_dialog_title">إذن المنبهات والتذكيرات</string>
    <string name="exact_alarm_dialog_body">لتشغيل الأذان في وقته بالضبط، امنح إذن "المنبهات والتذكيرات".</string>
    <string name="exact_alarm_grant">منح</string>
    <string name="exact_alarm_not_now">ليس الآن</string>
```

`values-bn/strings.xml`:
```xml
    <string name="exact_alarm_dialog_title">অ্যালার্ম ও রিমাইন্ডার অনুমতি</string>
    <string name="exact_alarm_dialog_body">ঠিক সময়ে আজান বাজাতে "অ্যালার্ম ও রিমাইন্ডার" অনুমতি দিন।</string>
    <string name="exact_alarm_grant">অনুমতি দিন</string>
    <string name="exact_alarm_not_now">এখন নয়</string>
```

`values-de/strings.xml`:
```xml
    <string name="exact_alarm_dialog_title">Berechtigung für Alarme und Erinnerungen</string>
    <string name="exact_alarm_dialog_body">Damit der Adhan pünktlich ertönt, erteilen Sie die Berechtigung "Alarme und Erinnerungen".</string>
    <string name="exact_alarm_grant">Erlauben</string>
    <string name="exact_alarm_not_now">Später</string>
```

`values-es/strings.xml`:
```xml
    <string name="exact_alarm_dialog_title">Permiso de alarmas y recordatorios</string>
    <string name="exact_alarm_dialog_body">Para reproducir el adhan exactamente a tiempo, conceda el permiso "Alarmas y recordatorios".</string>
    <string name="exact_alarm_grant">Conceder</string>
    <string name="exact_alarm_not_now">Ahora no</string>
```

`values-fa/strings.xml`:
```xml
    <string name="exact_alarm_dialog_title">مجوز زنگ‌ها و یادآورها</string>
    <string name="exact_alarm_dialog_body">برای پخش اذان دقیقاً به‌موقع، مجوز «زنگ‌ها و یادآورها» را بدهید.</string>
    <string name="exact_alarm_grant">اعطا</string>
    <string name="exact_alarm_not_now">اکنون نه</string>
```

`values-fr/strings.xml`:
```xml
    <string name="exact_alarm_dialog_title">Autorisation des alarmes et rappels</string>
    <string name="exact_alarm_dialog_body">Pour jouer l'adhan à l'heure exacte, accordez l'autorisation "Alarmes et rappels".</string>
    <string name="exact_alarm_grant">Autoriser</string>
    <string name="exact_alarm_not_now">Pas maintenant</string>
```

`values-hi/strings.xml`:
```xml
    <string name="exact_alarm_dialog_title">अलार्म और रिमाइंडर अनुमति</string>
    <string name="exact_alarm_dialog_body">अज़ान को ठीक समय पर बजाने के लिए "अलार्म और रिमाइंडर" अनुमति दें।</string>
    <string name="exact_alarm_grant">अनुमति दें</string>
    <string name="exact_alarm_not_now">अभी नहीं</string>
```

`values-id/strings.xml`:
```xml
    <string name="exact_alarm_dialog_title">Izin alarm dan pengingat</string>
    <string name="exact_alarm_dialog_body">Untuk memutar azan tepat waktu, berikan izin "Alarm dan pengingat".</string>
    <string name="exact_alarm_grant">Izinkan</string>
    <string name="exact_alarm_not_now">Nanti saja</string>
```

`values-ms/strings.xml`:
```xml
    <string name="exact_alarm_dialog_title">Kebenaran penggera dan peringatan</string>
    <string name="exact_alarm_dialog_body">Untuk memainkan azan tepat pada masanya, berikan kebenaran "Penggera dan peringatan".</string>
    <string name="exact_alarm_grant">Benarkan</string>
    <string name="exact_alarm_not_now">Tidak sekarang</string>
```

`values-ru/strings.xml`:
```xml
    <string name="exact_alarm_dialog_title">Разрешение на будильники и напоминания</string>
    <string name="exact_alarm_dialog_body">Чтобы азан звучал точно вовремя, предоставьте разрешение «Будильники и напоминания».</string>
    <string name="exact_alarm_grant">Разрешить</string>
    <string name="exact_alarm_not_now">Не сейчас</string>
```

`values-ta/strings.xml`:
```xml
    <string name="exact_alarm_dialog_title">அலாரம் மற்றும் நினைவூட்டல் அனுமதி</string>
    <string name="exact_alarm_dialog_body">அதானை சரியான நேரத்தில் இயக்க "அலாரம் மற்றும் நினைவூட்டல்" அனுமதியை வழங்கவும்.</string>
    <string name="exact_alarm_grant">அனுமதி</string>
    <string name="exact_alarm_not_now">இப்போது இல்லை</string>
```

`values-th/strings.xml`:
```xml
    <string name="exact_alarm_dialog_title">สิทธิ์ปลุกและเตือนความจำ</string>
    <string name="exact_alarm_dialog_body">เพื่อเล่นอาซานตรงเวลา โปรดให้สิทธิ์ "ปลุกและเตือนความจำ"</string>
    <string name="exact_alarm_grant">อนุญาต</string>
    <string name="exact_alarm_not_now">ไม่ใช่ตอนนี้</string>
```

`values-tr/strings.xml`:
```xml
    <string name="exact_alarm_dialog_title">Alarmlar ve hatırlatıcılar izni</string>
    <string name="exact_alarm_dialog_body">Ezanın tam vaktinde çalması için "Alarmlar ve hatırlatıcılar" iznini verin.</string>
    <string name="exact_alarm_grant">İzin Ver</string>
    <string name="exact_alarm_not_now">Şimdi Değil</string>
```

`values-ur/strings.xml`:
```xml
    <string name="exact_alarm_dialog_title">الارم اور یاد دہانی کی اجازت</string>
    <string name="exact_alarm_dialog_body">اذان کو عین وقت پر چلانے کے لیے "الارم اور یاد دہانی" کی اجازت دیں۔</string>
    <string name="exact_alarm_grant">اجازت دیں</string>
    <string name="exact_alarm_not_now">ابھی نہیں</string>
```

- [ ] **Step 2: Verify the resources compile**

Run: `./gradlew :prayer_feature:settings:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (no resource errors).

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/settings/src/main/res
git commit -m "feat(notifications): add exact alarm dialog strings in all locales"
```

---

### Task 3: Notifications screen — exact-alarm permission dialog

**Files:**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreen.kt`
- Test: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreenTest.kt`

- [ ] **Step 1: Add `@Before` to the existing test so it grants the exact-alarm permission**

In `NotificationsScreenTest.kt`, add these imports:

```kotlin
import org.junit.Before
import org.robolectric.shadows.ShadowAlarmManager
```

Add this method right after the `launchScreen(...)` helper (after line 51):

```kotlin
    @Before
    fun grantExactAlarmPermission() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }
```

- [ ] **Step 2: Add the failing tests for the dialog**

Add these tests at the end of `NotificationsScreenTest` (before the closing brace):

```kotlin
    @Test
    fun `enabling notifications without exact alarm permission shows dialog`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        shadowOf(composeTestRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = false))

        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.exact_alarm_dialog_title)
        ).assertIsDisplayed()
        coVerify(exactly = 0) { updateUseCase(any()) }
    }

    @Test
    fun `granting exact alarm permission opens settings intent`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        shadowOf(composeTestRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = false))

        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.exact_alarm_grant)
        ).performClick()
        composeTestRule.waitForIdle()

        val startedIntent = shadowOf(composeTestRule.activity).nextStartedActivity
        assertThat(startedIntent?.action).isEqualTo(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
    }

    @Test
    fun `not now dismisses exact alarm dialog`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        shadowOf(composeTestRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = false))

        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.exact_alarm_not_now)
        ).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.exact_alarm_dialog_title)
        ).assertDoesNotExist()
    }
```

In `NotificationsScreenTest.kt`, add the import for `Settings` at the top (the screen file already imports it):

```kotlin
import android.provider.Settings
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.settings.notifications.NotificationsScreenTest"`
Expected: the three new tests FAIL (dialog never appears).

- [ ] **Step 4: Implement the dialog in `NotificationsScreen`**

In `NotificationsContent` (in `NotificationsScreen.kt`), add these state variables right after the existing `pendingPermissionAction` declaration (after line 128):

```kotlin
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    var exactAlarmDialogDismissed by remember { mutableStateOf(false) }
    var pendingExactAlarmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
```

Add this `LaunchedEffect` right after the `DisposableEffect(lifecycleOwner) { ... }` block (after line 156) so returning users with notifications enabled but no permission see the dialog once per visit:

```kotlin
    LaunchedEffect(settings.enabled, canScheduleExactAlarms, exactAlarmDialogDismissed) {
        if (settings.enabled && !canScheduleExactAlarms && !exactAlarmDialogDismissed) {
            showExactAlarmDialog = true
        }
    }
```

Add the import for `LaunchedEffect` at the top:

```kotlin
import androidx.compose.runtime.LaunchedEffect
```

In the existing `DisposableEffect` ON_RESUME observer, apply any pending exact-alarm action once the permission is granted. Replace the observer body (lines 148-153):

```kotlin
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = checkNotificationPermission()
                canScheduleExactAlarms = checkExactAlarmPermission()
                if (canScheduleExactAlarms) {
                    pendingExactAlarmAction?.invoke()
                    pendingExactAlarmAction = null
                }
            }
        }
```

Update the Notifications toggle handler (lines 185-193) to check the exact-alarm permission after the notification permission:

```kotlin
            onCheckedChange = { enabled ->
                if (enabled && !hasNotificationPermission) {
                    pendingPermissionAction = { onEvent(NotificationsEvent.SetEnabled(true)) }
                    showNotificationRationale = false
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else if (enabled && !canScheduleExactAlarms) {
                    pendingExactAlarmAction = { onEvent(NotificationsEvent.SetEnabled(true)) }
                    showExactAlarmDialog = true
                } else {
                    onEvent(NotificationsEvent.SetEnabled(enabled))
                }
            }
```

Update the Ezan toggle handler (lines 241-249) the same way:

```kotlin
            onCheckedChange = { enabled ->
                if (enabled && !hasNotificationPermission) {
                    pendingPermissionAction = { onEvent(NotificationsEvent.SetAdhanEnabled(true)) }
                    showNotificationRationale = false
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else if (enabled && !canScheduleExactAlarms) {
                    pendingExactAlarmAction = { onEvent(NotificationsEvent.SetAdhanEnabled(true)) }
                    showExactAlarmDialog = true
                } else {
                    onEvent(NotificationsEvent.SetAdhanEnabled(enabled))
                }
            }
```

Add the dialog at the end of `NotificationsContent`, right after the `if (showTimePicker) { ... }` block (after line 337):

```kotlin
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
```

`AlertDialog`, `TextButton`, `Settings`, `Uri`, `Intent`, `stringResource` are already imported in this file.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.settings.notifications.NotificationsScreenTest"`
Expected: PASS (all tests, including the three new ones).

- [ ] **Step 6: Commit**

```bash
git add prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreen.kt prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreenTest.kt
git commit -m "feat(notifications): prompt for exact alarm permission in settings"
```

---

### Task 4: SchedulePlan — 24-hour coverage

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/SchedulePlan.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/domain/SchedulePlanTest.kt`

- [ ] **Step 1: Add the failing tests for 24-hour coverage**

Add these tests to `SchedulePlanTest.kt` (before the closing brace), and add the import at the top:

```kotlin
import java.time.LocalDate
```

```kotlin
    @Test
    fun `schedules today's remaining and tomorrow's full day`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            tomorrowPrayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T15:00:00Z"), // 18:00 Istanbul
            enabledPrayers = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        assertThat(alarms.map { it.prayerKey })
            .containsExactly("Maghrib", "Isha", "Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    }

    @Test
    fun `marks tomorrow's Friday Dhuhr as jumuah`() {
        // 2026-08-21 is a Friday. now = 2026-08-20 15:00 UTC (18:00 Istanbul), after today's Dhuhr.
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            tomorrowPrayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-20T15:00:00Z"),
            enabledPrayers = setOf("Dhuhr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false,
            jumuahEnabled = true
        )
        assertThat(alarms.single().prayerKey).isEqualTo("Dhuhr")
        assertThat(alarms.single().isJumuah).isTrue()
    }

    @Test
    fun `tomorrow's Fajr carries today's last prayer as previous`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            tomorrowPrayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T20:00:00Z"), // 23:00 Istanbul, after Isha
            enabledPrayers = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        val tomorrowFajr = alarms.first { it.prayerKey == "Fajr" }
        val todayIshaTrigger = java.time.LocalTime.of(21, 15)
            .atDate(LocalDate.of(2026, 8, 22))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        assertThat(tomorrowFajr.previousPrayerTimeMillis).isEqualTo(todayIshaTrigger)
    }

    @Test
    fun `request codes stay within the cancel range for two days`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            tomorrowPrayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"),
            prePrayerMinutes = 15,
            prePrayerEnabled = true
        )
        val codes = alarms.map { it.requestCode }
        assertThat(codes.min()).isGreaterThanOrEqualTo(1000)
        assertThat(codes.max()).isLessThan(1020)
    }
```

- [ ] **Step 2: Rework the two `nextDayFajrTimeMillis` tests**

Replace the `last enabled prayer points to tomorrow's Fajr when provided` test (lines 238-254) with:

```kotlin
    @Test
    fun `last enabled prayer points to tomorrow's Fajr when provided`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            tomorrowPrayers = listOf(prayer("Fajr", LocalTime(4, 30))),
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        val isha = alarms.first { it.prayerKey == "Isha" }
        val tomorrowFajr = java.time.LocalTime.of(4, 30)
            .atDate(LocalDate.of(2026, 8, 23))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        assertThat(isha.nextPrayerName).isEqualTo("Fajr")
        assertThat(isha.nextPrayerTimeMillis).isEqualTo(tomorrowFajr)
    }
```

Replace the `last enabled prayer has no next when nextDayFajr not provided` test (lines 256-270) with:

```kotlin
    @Test
    fun `last enabled prayer has no next when no tomorrow prayers`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        val isha = alarms.first { it.prayerKey == "Isha" }
        assertThat(isha.nextPrayerName).isNull()
        assertThat(isha.nextPrayerTimeMillis).isNull()
    }
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.domain.SchedulePlanTest"`
Expected: COMPILATION FAILURE — `buildDailyAlarms` has no `tomorrowPrayers` parameter.

- [ ] **Step 4: Implement 24-hour coverage in `SchedulePlan`**

In `SchedulePlan.kt`, change the `buildDailyAlarms` signature (lines 34-49) to add `tomorrowPrayers` and remove `nextDayFajrTimeMillis`:

```kotlin
    fun buildDailyAlarms(
        prayers: List<Prayer>,
        tomorrowPrayers: List<Prayer> = emptyList(),
        zoneId: ZoneId,
        now: Instant,
        enabledPrayers: Set<String>,
        prePrayerMinutes: Int,
        prePrayerEnabled: Boolean,
        dailyReminderEnabled: Boolean = false,
        dailyReminderHour: Int = 8,
        dailyReminderMinute: Int = 0,
        dailySummary: String = "",
        specialDayToday: SpecialDay? = null,
        specialDayTomorrow: SpecialDay? = null,
        jumuahEnabled: Boolean = true
    ): List<ScheduledAlarm> {
```

Replace the body (lines 50-155) with:

```kotlin
        val nowZoned = now.atZone(zoneId)
        val today = nowZoned.toLocalDate()
        val tomorrow = today.plusDays(1)
        val result = mutableListOf<ScheduledAlarm>()
        var requestCode = 1000

        val enabledToday = prayers.filter { it.name in enabledPrayers }
        val enabledTomorrow = tomorrowPrayers.filter { it.name in enabledPrayers }

        fun triggerFor(prayer: Prayer, date: java.time.LocalDate): Instant =
            LocalTime.of(prayer.time.hour, prayer.time.minute)
                .atDate(date)
                .atZone(zoneId)
                .toInstant()

        enabledToday.forEachIndexed { index, prayer ->
            val trigger = triggerFor(prayer, today)
            if (trigger.isAfter(now)) {
                val next = enabledToday.getOrNull(index + 1)
                val isLast = index == enabledToday.lastIndex
                val effectiveNextTime = if (isLast) {
                    enabledTomorrow.firstOrNull()?.let { triggerFor(it, tomorrow).toEpochMilli() }
                } else {
                    next?.let { triggerFor(it, today).toEpochMilli() }
                }
                val effectiveNextName = if (isLast && enabledTomorrow.isNotEmpty()) {
                    enabledTomorrow.first().name
                } else {
                    next?.name
                }
                val previous = enabledToday.getOrNull(index - 1)?.let {
                    triggerFor(it, today).toEpochMilli()
                }
                result += ScheduledAlarm(
                    prayerKey = prayer.name,
                    triggerAtMillis = trigger.toEpochMilli(),
                    requestCode = requestCode++,
                    type = AlarmType.PRAYER,
                    isJumuah = jumuahEnabled &&
                        prayer.name == "Dhuhr" &&
                        today.dayOfWeek == DayOfWeek.FRIDAY,
                    nextPrayerTimeMillis = effectiveNextTime,
                    nextPrayerName = effectiveNextName,
                    previousPrayerTimeMillis = previous
                )
            }
            if (prePrayerEnabled) {
                val preTrigger = trigger.minusSeconds(prePrayerMinutes * 60L)
                if (preTrigger.isAfter(now)) {
                    result += ScheduledAlarm(
                        prayerKey = "${prayer.name}_pre",
                        triggerAtMillis = preTrigger.toEpochMilli(),
                        requestCode = requestCode++,
                        type = AlarmType.PRE_PRAYER,
                        prePrayerMinutes = prePrayerMinutes
                    )
                }
            }
        }

        enabledTomorrow.forEachIndexed { index, prayer ->
            val trigger = triggerFor(prayer, tomorrow)
            val next = enabledTomorrow.getOrNull(index + 1)
            val previous = enabledTomorrow.getOrNull(index - 1)?.let {
                triggerFor(it, tomorrow).toEpochMilli()
            } ?: enabledToday.lastOrNull()?.let {
                triggerFor(it, today).toEpochMilli()
            }
            result += ScheduledAlarm(
                prayerKey = prayer.name,
                triggerAtMillis = trigger.toEpochMilli(),
                requestCode = requestCode++,
                type = AlarmType.PRAYER,
                isJumuah = jumuahEnabled &&
                    prayer.name == "Dhuhr" &&
                    tomorrow.dayOfWeek == DayOfWeek.FRIDAY,
                nextPrayerTimeMillis = next?.let { triggerFor(it, tomorrow).toEpochMilli() },
                nextPrayerName = next?.name,
                previousPrayerTimeMillis = previous
            )
            if (prePrayerEnabled) {
                val preTrigger = trigger.minusSeconds(prePrayerMinutes * 60L)
                result += ScheduledAlarm(
                    prayerKey = "${prayer.name}_pre",
                    triggerAtMillis = preTrigger.toEpochMilli(),
                    requestCode = requestCode++,
                    type = AlarmType.PRE_PRAYER,
                    prePrayerMinutes = prePrayerMinutes
                )
            }
        }

        if (dailyReminderEnabled) {
            val reminderTrigger = LocalTime.of(dailyReminderHour, dailyReminderMinute)
                .atDate(today)
                .atZone(zoneId)
                .toInstant()
            if (reminderTrigger.isAfter(now)) {
                result += ScheduledAlarm(
                    prayerKey = "daily_reminder",
                    triggerAtMillis = reminderTrigger.toEpochMilli(),
                    requestCode = REQUEST_CODE_DAILY_REMINDER,
                    type = AlarmType.DAILY_REMINDER,
                    dailySummary = dailySummary
                )
            }
        }

        specialDayToday?.let { day ->
            val specialTrigger = LocalTime.of(8, 0)
                .atDate(today)
                .atZone(zoneId)
                .toInstant()
            if (specialTrigger.isAfter(now)) {
                result += ScheduledAlarm(
                    prayerKey = "special_day",
                    triggerAtMillis = specialTrigger.toEpochMilli(),
                    requestCode = REQUEST_CODE_SPECIAL_DAY,
                    type = AlarmType.SPECIAL_DAY,
                    specialDay = day
                )
            }
        }

        specialDayTomorrow?.let { day ->
            val specialTrigger = LocalTime.of(8, 0)
                .atDate(tomorrow)
                .atZone(zoneId)
                .toInstant()
            result += ScheduledAlarm(
                prayerKey = "pre_special_day",
                triggerAtMillis = specialTrigger.toEpochMilli(),
                requestCode = REQUEST_CODE_PRE_SPECIAL_DAY,
                type = AlarmType.PRE_SPECIAL_DAY,
                specialDay = day
            )
        }

        return result
    }
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.domain.SchedulePlanTest"`
Expected: PASS (all tests).

- [ ] **Step 6: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/SchedulePlan.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/domain/SchedulePlanTest.kt
git commit -m "feat(notifications): schedule 24 hours of prayer alarms"
```

---

### Task 5: Scheduler — wire 24-hour coverage

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationSchedulerTest.kt`

- [ ] **Step 1: Add the failing test for tomorrow's coverage**

Add this test to `PrayerNotificationSchedulerTest.kt` (before the closing brace):

```kotlin
    @Test
    fun `scheduleAll schedules tomorrow's prayers too`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings(enabled = true)
        coEvery { locationsCoordinator.resolveSelected() } returns LocationData(
            latitude = 41.0082,
            longitude = 28.9784,
            country = "Turkey",
            countryCode = "TR",
            city = "Istanbul",
            county = null
        )
        coEvery { getSettingsUseCase() } returns Settings(
            location = LocationSettings(timeZone = "Europe/Istanbul"),
            calculationMethod = "TURKEY_DIYANET"
        )
        coEvery { getPrayerTimesUseCase(any(), any(), any(), any(), any(), any()) } returns Result.success(
            listOf(
                Prayer(
                    name = "Fajr",
                    arabicName = "الفجر",
                    time = LocalTime(23, 59),
                    date = LocalDate(2026, 8, 22)
                )
            )
        )

        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleAll()

        val zoneId = java.time.ZoneId.of("Europe/Istanbul")
        val tomorrow = java.time.LocalDate.now(zoneId).plusDays(1)
        val tomorrowFajr = java.time.LocalTime.of(23, 59)
            .atDate(tomorrow)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val tomorrowAlarms = shadowOf(alarmManager).scheduledAlarms.filter { alarm ->
            shadowOf(alarm.operation).savedIntent
                .getLongExtra(AlarmReceiver.EXTRA_ALARM_TRIGGER_TIME, 0L) == tomorrowFajr
        }
        assertThat(tomorrowAlarms).isNotEmpty()
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.scheduler.PrayerNotificationSchedulerTest"`
Expected: the new test FAILS (only today's alarms are scheduled).

- [ ] **Step 3: Implement 24-hour wiring in `PrayerNotificationScheduler`**

In `PrayerNotificationScheduler.kt`:

1. Change `REQUEST_CODE_END` from `1010` to `1020` (line 51):

```kotlin
        const val REQUEST_CODE_END = 1020
```

2. Replace the block from `val today = LocalDate.now(zoneId)` through the end of the `nextDayFajrTimeMillis` computation (lines 82-116) with:

```kotlin
        val today = LocalDate.now(zoneId)
        val tomorrow = today.plusDays(1)
        val method = CalculationMethod.fromSettingsId(appSettings.calculationMethod)
        val prayers = getPrayerTimesUseCase(
            date = LocalDateTime.now(zoneId),
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = zoneId,
            calculationMethod = method
        ).getOrNull() ?: run {
            cancelAll()
            cancelDailyReschedule()
            return
        }
        val tomorrowPrayers = getPrayerTimesUseCase(
            date = LocalDateTime(tomorrow.year, tomorrow.monthValue, tomorrow.dayOfMonth, 0, 0),
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = zoneId,
            calculationMethod = method,
            persistDailyCache = false
        ).getOrNull() ?: run {
            cancelAll()
            cancelDailyReschedule()
            return
        }
```

3. In the `buildDailyAlarms` call (lines 130-145), add `tomorrowPrayers = tomorrowPrayers,` after `prayers = prayers,` and remove the `nextDayFajrTimeMillis = nextDayFajrTimeMillis` argument:

```kotlin
        val alarms = schedulePlan.buildDailyAlarms(
            prayers = prayers,
            tomorrowPrayers = tomorrowPrayers,
            zoneId = zoneId,
            now = Instant.now(),
            enabledPrayers = enabled,
            prePrayerMinutes = settings.prePrayerMinutes,
            prePrayerEnabled = settings.prePrayerReminderEnabled,
            dailyReminderEnabled = settings.dailyReminderEnabled,
            dailyReminderHour = settings.dailyReminderHour,
            dailyReminderMinute = settings.dailyReminderMinute,
            dailySummary = summary,
            specialDayToday = specialDayToday,
            specialDayTomorrow = specialDayTomorrow,
            jumuahEnabled = settings.jumuahEnabled
        )
```

4. Replace the countdown block (lines 148-169) with:

```kotlin
        if (settings.countdownEnabled) {
            val nextPrayer = alarms.firstOrNull { it.type == AlarmType.PRAYER }
            if (nextPrayer != null) {
                updateCountdown(
                    nextPrayer.triggerAtMillis,
                    nextPrayer.prayerKey,
                    nextPrayer.previousPrayerTimeMillis
                )
            }
        }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.scheduler.PrayerNotificationSchedulerTest"`
Expected: PASS (all tests, including the new one).

- [ ] **Step 5: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationSchedulerTest.kt
git commit -m "feat(notifications): wire tomorrow's prayers into scheduling"
```

---

### Task 6: AdhanService — volume buttons adjust, volume 0 stops

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AdhanService.kt:41-49`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/AdhanServiceTest.kt`

- [ ] **Step 1: Update the failing tests**

In `AdhanServiceTest.kt`, replace the `volume decrease stops the service` test (lines 83-94) with these two tests:

```kotlin
    @Test
    fun `volume decrease to non-zero does not stop the service`() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 5, 0)
        val controller = startService("Fajr")
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 4, 0)
        val uri = Settings.System.getUriFor("volume_alarm_sound")
        shadowOf(context.contentResolver).getContentObservers(uri).forEach { it.onChange(false) }
        assertThat(shadowOf(controller.get()).isStoppedBySelf()).isFalse()
        controller.destroy()
        verify(exactly = 0) { adhanPlayer.stop() }
    }

    @Test
    fun `volume to zero stops the service`() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 5, 0)
        val controller = startService("Fajr")
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
        val uri = Settings.System.getUriFor("volume_alarm_sound")
        shadowOf(context.contentResolver).getContentObservers(uri).forEach { it.onChange(false) }
        assertThat(shadowOf(controller.get()).isStoppedBySelf()).isTrue()
        controller.destroy()
        verify { adhanPlayer.stop() }
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.scheduler.AdhanServiceTest"`
Expected: `volume decrease to non-zero does not stop the service` FAILS (current code stops on any decrease).

- [ ] **Step 3: Implement the volume behavior in `AdhanService`**

In `AdhanService.kt`, replace the `volumeObserver` (lines 41-49):

```kotlin
    private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            if (current <= 0) {
                stopSelf()
            }
            lastAlarmVolume = current
        }
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.scheduler.AdhanServiceTest"`
Expected: PASS (all tests).

- [ ] **Step 5: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AdhanService.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/AdhanServiceTest.kt
git commit -m "feat(notifications): volume buttons adjust ezan, volume 0 stops it"
```

---

### Task 7: cancelAll stops the AdhanService

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt:188-218`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationSchedulerTest.kt`

- [ ] **Step 1: Add the failing test**

Add this test to `PrayerNotificationSchedulerTest.kt` (before the closing brace), and add the imports at the top:

```kotlin
import android.app.Application
```

```kotlin
    @Test
    fun `cancelAll stops the adhan service`() = runTest {
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.cancelAll()
        val stopped = shadowOf(context as Application).getNextStoppedService()
        assertThat(stopped?.component?.className).isEqualTo(AdhanService::class.java.name)
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.scheduler.PrayerNotificationSchedulerTest"`
Expected: the new test FAILS (no service is stopped).

- [ ] **Step 3: Implement the stop in `cancelAll`**

In `PrayerNotificationScheduler.kt`, add the `stopService` call as the first line of `cancelAll()` (line 189):

```kotlin
    fun cancelAll() {
        context.stopService(Intent(context, AdhanService::class.java))
        notificationManager.cancelCountdown()
        // Cancel all pending alarms by re-issuing the same PendingIntents with FLAG_NO_CREATE.
        ...
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.scheduler.PrayerNotificationSchedulerTest"`
Expected: PASS (all tests).

- [ ] **Step 5: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationSchedulerTest.kt
git commit -m "fix(notifications): stop adhan service when notifications are disabled"
```

---

### Task 8: Full verification

- [ ] **Step 1: Run the notification module tests**

Run: `./gradlew :prayer_notifications:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 2: Run the settings module tests**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 3: Run the full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

- [ ] **Step 4: Run `gitnexus_detect_changes` to confirm the blast radius**

Run: `gitnexus_detect_changes({repo: "NamazVakitleri", scope: "all"})`
Expected: changed symbols limited to `PrayerNotificationScheduler`, `SchedulePlan`, `AdhanService`, `NotificationsScreen` and their tests. No unexpected execution flows affected.

## Verification

After all tasks, run the full test suite to confirm nothing is broken:

```bash
./gradlew :prayer_notifications:testDebugUnitTest
./gradlew :prayer_feature:settings:testDebugUnitTest
./gradlew testDebugUnitTest
```

Expected: all tests pass.
