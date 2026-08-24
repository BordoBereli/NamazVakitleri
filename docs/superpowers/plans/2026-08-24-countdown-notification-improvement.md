# Persistent Countdown Notification Improvement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the persistent countdown notification more meaningful: show the next prayer name and its clock time in the title, a clear "Xh Ym remaining" body, and a progress bar spanning the previous→next prayer gap. Keep the countdown alive overnight after Isha, targeting tomorrow's Fajr.

**Architecture:** Extend the existing single-scheduler design. `SchedulePlan` computes each prayer's `previousPrayerTimeMillis` (the prayer before it) and points the last enabled prayer's `nextPrayer*` at tomorrow's Fajr. `PrayerNotificationScheduler` threads the previous time through the countdown tick intent and starts the countdown (including the overnight case). `AlarmReceiver` passes the firing prayer's trigger as the progress-bar gap start. `PrayerNotificationManager` renders title/body/progress.

**Tech Stack:** Kotlin, AlarmManager exact alarms, Robolectric, MockK, Truth.

**Spec:** `docs/superpowers/specs/2026-08-24-countdown-notification-improvement-design.md`

---

## File Map

| File | Action |
|---|---|
| `prayer_notifications/src/main/res/values/strings.xml` + 14 locale files | Modify — add `notification_countdown_title`, `notification_remaining`; remove `notification_next_prayer` |
| `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/SchedulePlan.kt` | Modify — `previousPrayerTimeMillis` on `ScheduledAlarm`; `nextDayFajrTimeMillis` param; overnight next |
| `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManager.kt` | Modify — new `showCountdownNotification` signature + title/body/progress |
| `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt` | Modify — `updateCountdown` signature, tick intent, overnight Fajr start |
| `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiver.kt` | Modify — thread previous time through PRAYER + COUNTDOWN_TICK |
| Tests: `SchedulePlanTest.kt`, `PrayerNotificationManagerTest.kt`, `PrayerNotificationSchedulerTest.kt`, `AlarmReceiverTest.kt` | Modify |
| `TODO.md` | Modify — mark countdown improvement done |

---

## Task 1: Notification strings (all 15 locales)

**Files:**
- Modify: `prayer_notifications/src/main/res/values/strings.xml`
- Modify: `prayer_notifications/src/main/res/values-ar/strings.xml`, `values-ta`, `values-fa`, `values-es`, `values-ur`, `values-th`, `values-tr`, `values-fr`, `values-de`, `values-ru`, `values-bn`, `values-hi`, `values-id`, `values-ms`

- [ ] **Step 1: Add the two new strings and remove `notification_next_prayer` in `values/strings.xml`**

In `prayer_notifications/src/main/res/values/strings.xml`, replace line 9 (`<string name="notification_next_prayer">Next prayer: %1$s</string>`) with the two new strings:

```xml
    <string name="notification_countdown_title">%1$s · %2$s</string>
    <string name="notification_remaining">%1$s remaining</string>
```

The file should now have (lines 8-15 region):

```xml
    <string name="notification_prayer_time">%1$s time is now</string>
    <string name="notification_countdown_title">%1$s · %2$s</string>
    <string name="notification_remaining">%1$s remaining</string>
    <string name="notification_stop">Stop</string>
    <string name="notification_test_title">Test notification</string>
    <string name="notification_test_body">Notifications are working</string>

    <string name="notification_remaining_hours_minutes">%1$dh %2$dm</string>
    <string name="notification_remaining_minutes">%1$dm</string>
```

- [ ] **Step 2: Apply the same change to the 14 locale files**

For each locale file, replace the `<string name="notification_next_prayer">...</string>` line with the two new strings. Use these translations (initial translations — verify with a native speaker if available):

| Locale | `notification_countdown_title` | `notification_remaining` |
|---|---|---|
| ar | `%1$s · %2$s` | `متبقي %1$s` |
| ta | `%1$s · %2$s` | `%1$s மீதம்` |
| fa | `%1$s · %2$s` | `%1$s باقی مانده` |
| es | `%1$s · %2$s` | `Quedan %1$s` |
| ur | `%1$s · %2$s` | `%1$s باقی ہے` |
| th | `%1$s · %2$s` | `เหลือ %1$s` |
| tr | `%1$s · %2$s` | `%1$s kaldı` |
| fr | `%1$s · %2$s` | `Encore %1$s` |
| de | `%1$s · %2$s` | `Noch %1$s` |
| ru | `%1$s · %2$s` | `Осталось %1$s` |
| bn | `%1$s · %2$s` | `%1$s বাকি` |
| hi | `%1$s · %2$s` | `%1$s शेष` |
| id | `%1$s · %2$s` | `%1$s tersisa` |
| ms | `%1$s · %2$s` | `%1$s lagi` |

- [ ] **Step 3: Verify no remaining references to `notification_next_prayer`**

Run: `rg "notification_next_prayer" prayer_notifications`
Expected: no matches (the only previous use was `PrayerNotificationManager.kt:113`, which Task 3 rewrites).

- [ ] **Step 4: Commit**

```bash
git add prayer_notifications/src/main/res
git commit -m "feat(notifications): add countdown title and remaining strings across locales"
```

---

## Task 2: `ScheduledAlarm.previousPrayerTimeMillis` + overnight Fajr in `SchedulePlan`

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/SchedulePlan.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/domain/SchedulePlanTest.kt`

- [ ] **Step 1: Write the failing tests**

Append to `SchedulePlanTest.kt` (before the closing brace of the class):

```kotlin
    @Test
    fun `carries previous prayer time on prayer alarms`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Fajr", "Dhuhr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false
        )
        val fajr = alarms.first { it.prayerKey == "Fajr" }
        val dhuhr = alarms.first { it.prayerKey == "Dhuhr" }
        assertThat(fajr.previousPrayerTimeMillis).isNull()
        assertThat(dhuhr.previousPrayerTimeMillis).isEqualTo(fajr.triggerAtMillis)
    }

    @Test
    fun `last enabled prayer points to tomorrow's Fajr when provided`() {
        val plan = SchedulePlan()
        val nextDayFajr = Instant.parse("2026-08-23T01:30:00Z").toEpochMilli()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false,
            nextDayFajrTimeMillis = nextDayFajr
        )
        val isha = alarms.first { it.prayerKey == "Isha" }
        assertThat(isha.nextPrayerName).isEqualTo("Fajr")
        assertThat(isha.nextPrayerTimeMillis).isEqualTo(nextDayFajr)
    }

    @Test
    fun `last enabled prayer has no next when nextDayFajr not provided`() {
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

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*SchedulePlanTest"`
Expected: FAIL — `previousPrayerTimeMillis` and `nextDayFajrTimeMillis` are unresolved references.

- [ ] **Step 3: Add `previousPrayerTimeMillis` to `ScheduledAlarm`**

In `SchedulePlan.kt`, add the field to the `ScheduledAlarm` data class (after `nextPrayerName`):

```kotlin
data class ScheduledAlarm(
    val prayerKey: String,
    val triggerAtMillis: Long,
    val requestCode: Int,
    val type: AlarmType = AlarmType.PRAYER,
    val isJumuah: Boolean = false,
    val nextPrayerTimeMillis: Long? = null,
    val nextPrayerName: String? = null,
    val previousPrayerTimeMillis: Long? = null,
    val prePrayerMinutes: Int? = null,
    val dailySummary: String? = null,
    val specialDay: SpecialDay? = null
)
```

- [ ] **Step 4: Add `nextDayFajrTimeMillis` param and compute previous/overnight next in `buildDailyAlarms`**

In `SchedulePlan.kt`, add the parameter to `buildDailyAlarms` (after `jumuahEnabled`):

```kotlin
        jumuahEnabled: Boolean = true,
        nextDayFajrTimeMillis: Long? = null
    ): List<ScheduledAlarm> {
```

Then replace the `enabled.forEachIndexed { index, prayer ->` block's next/previous computation. Replace:

```kotlin
        val next = enabled.getOrNull(index + 1)
        val nextTime = next?.let {
            LocalTime.of(it.time.hour, it.time.minute)
                .atDate(nowZoned.toLocalDate())
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
        }
        if (trigger.isAfter(now)) {
            result += ScheduledAlarm(
                prayerKey = prayer.name,
                triggerAtMillis = trigger.toEpochMilli(),
                requestCode = requestCode++,
                type = AlarmType.PRAYER,
                isJumuah = jumuahEnabled &&
                    prayer.name == "Dhuhr" &&
                    nowZoned.dayOfWeek == DayOfWeek.FRIDAY,
                nextPrayerTimeMillis = nextTime,
                nextPrayerName = next?.name
            )
        }
```

with:

```kotlin
        val next = enabled.getOrNull(index + 1)
        val nextTime = next?.let {
            LocalTime.of(it.time.hour, it.time.minute)
                .atDate(nowZoned.toLocalDate())
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
        }
        val isLast = index == enabled.lastIndex
        val effectiveNextTime = if (isLast) nextDayFajrTimeMillis else nextTime
        val effectiveNextName = if (isLast && nextDayFajrTimeMillis != null) "Fajr" else next?.name
        val previous = enabled.getOrNull(index - 1)?.let {
            LocalTime.of(it.time.hour, it.time.minute)
                .atDate(nowZoned.toLocalDate())
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
        }
        if (trigger.isAfter(now)) {
            result += ScheduledAlarm(
                prayerKey = prayer.name,
                triggerAtMillis = trigger.toEpochMilli(),
                requestCode = requestCode++,
                type = AlarmType.PRAYER,
                isJumuah = jumuahEnabled &&
                    prayer.name == "Dhuhr" &&
                    nowZoned.dayOfWeek == DayOfWeek.FRIDAY,
                nextPrayerTimeMillis = effectiveNextTime,
                nextPrayerName = effectiveNextName,
                previousPrayerTimeMillis = previous
            )
        }
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*SchedulePlanTest"`
Expected: PASS (all existing + 3 new tests).

- [ ] **Step 6: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/SchedulePlan.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/domain/SchedulePlanTest.kt
git commit -m "feat(notifications): track previous prayer time and overnight Fajr in schedule plan"
```

---

## Task 3: `PrayerNotificationManager.showCountdownNotification` — title, body, progress bar

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManager.kt`
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt` (call site only)
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManagerTest.kt`

- [ ] **Step 1: Write the failing tests**

Append to `PrayerNotificationManagerTest.kt` (before the closing brace):

```kotlin
    @Test
    fun `showCountdownNotification shows prayer name and clock time in title`() {
        manager.createChannels()
        val target = LocalTime.of(18, 45).atDate(LocalDate.of(2026, 8, 22))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        manager.showCountdownNotification("Maghrib", target, null, 90 * 60_000L)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = shadowOf(nm).allNotifications.single()
        assertThat(notification.extras.getString("android.title")).isEqualTo("Maghrib · 18:45")
    }

    @Test
    fun `showCountdownNotification shows remaining time in body`() {
        manager.createChannels()
        val target = LocalTime.of(18, 45).atDate(LocalDate.of(2026, 8, 22))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        manager.showCountdownNotification("Maghrib", target, null, 90 * 60_000L)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = shadowOf(nm).allNotifications.single()
        assertThat(notification.extras.getString("android.text")).isEqualTo("1h 30m remaining")
    }

    @Test
    fun `showCountdownNotification sets progress bar between previous and next`() {
        manager.createChannels()
        val previous = LocalTime.of(16, 45).atDate(LocalDate.of(2026, 8, 22))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val target = LocalTime.of(19, 55).atDate(LocalDate.of(2026, 8, 22))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val gap = target - previous
        val now = previous + gap / 2
        manager.showCountdownNotification("Maghrib", target, previous, target - now)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = shadowOf(nm).allNotifications.single()
        assertThat(notification.maxProgress).isEqualTo(gap.toInt())
        assertThat(notification.progress).isEqualTo((gap / 2).toInt())
    }

    @Test
    fun `showCountdownNotification omits progress bar when previous is null`() {
        manager.createChannels()
        val target = LocalTime.of(18, 45).atDate(LocalDate.of(2026, 8, 22))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        manager.showCountdownNotification("Maghrib", target, null, 90 * 60_000L)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = shadowOf(nm).allNotifications.single()
        assertThat(notification.maxProgress).isEqualTo(0)
        assertThat(notification.progress).isEqualTo(0)
    }
```

Add the needed imports to the test file's import block:

```kotlin
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*PrayerNotificationManagerTest"`
Expected: FAIL — `showCountdownNotification` has the wrong arity (compile error).

- [ ] **Step 3: Rewrite `showCountdownNotification` in the manager**

In `PrayerNotificationManager.kt`, replace the existing `showCountdownNotification` (lines 98-121) with:

```kotlin
    fun showCountdownNotification(
        nextPrayerName: String,
        nextPrayerTimeMillis: Long,
        previousPrayerTimeMillis: Long?,
        remainingMillis: Long
    ) {
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, AlarmReceiver::class.java)
                .setAction(AlarmReceiver.ACTION_STOP_COUNTDOWN),
            PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_COUNTDOWN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                localizedString(
                    R.string.notification_countdown_title,
                    localizedPrayerName(nextPrayerName),
                    formatClockTime(nextPrayerTimeMillis)
                )
            )
            .setContentText(
                localizedString(R.string.notification_remaining, formatRemaining(remainingMillis))
            )
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, localizedString(R.string.notification_stop), stopIntent)
        previousPrayerTimeMillis?.let { previous ->
            if (previous < nextPrayerTimeMillis) {
                val now = nextPrayerTimeMillis - remainingMillis
                val max = (nextPrayerTimeMillis - previous).toInt()
                val progress = (now - previous).coerceIn(0, max.toLong()).toInt()
                builder.setProgress(max, progress, false)
            }
        }
        notificationManager.notify(NOTIFICATION_ID_COUNTDOWN, builder.build())
    }
```

- [ ] **Step 4: Add `formatClockTime` helper and imports to the manager**

In `PrayerNotificationManager.kt`, add `formatClockTime` next to `formatRemaining` (after line 201):

```kotlin
    private fun formatClockTime(epochMillis: Long): String {
        val time = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime()
        return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
    }
```

Add the imports to the manager's import block:

```kotlin
import java.time.Instant
import java.time.ZoneId
```

- [ ] **Step 5: Update the scheduler call site**

In `PrayerNotificationScheduler.kt`, `updateCountdown` currently calls `showCountdownNotification(prayerName, remaining)`. Change it to pass the target millis and null previous:

```kotlin
        notificationManager.showCountdownNotification(prayerName, targetMillis, null, remaining)
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*PrayerNotificationManagerTest"`
Expected: PASS (all existing + 4 new tests).

- [ ] **Step 7: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManager.kt prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManagerTest.kt
git commit -m "feat(notifications): show prayer time, remaining text, and progress bar in countdown"
```

---

## Task 4: `PrayerNotificationScheduler.updateCountdown` signature + tick intent carries previous time

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt`
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiver.kt` (call sites only)
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationSchedulerTest.kt`

- [ ] **Step 1: Update the failing test for the new `showCountdownNotification` arity**

In `PrayerNotificationSchedulerTest.kt`, the test `scheduleAll starts countdown when enabled` currently ends with:

```kotlin
        coVerify { notificationManager.showCountdownNotification("Fajr", any()) }
```

Change it to:

```kotlin
        coVerify { notificationManager.showCountdownNotification("Fajr", any(), any(), any()) }
```

- [ ] **Step 2: Write the failing test for the tick intent**

Append to `PrayerNotificationSchedulerTest.kt` (before the closing brace):

```kotlin
    @Test
    fun `updateCountdown schedules tick carrying previous time`() = runTest {
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        val target = System.currentTimeMillis() + 3_600_000
        val previous = System.currentTimeMillis() - 60_000
        scheduler.updateCountdown(target, "Maghrib", previous)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val tickAlarm = shadowOf(alarmManager).scheduledAlarms.first { alarm ->
            shadowOf(alarm.operation).requestCode == SchedulePlan.REQUEST_CODE_COUNTDOWN_TICK
        }
        val intent = shadowOf(tickAlarm.operation).savedIntent
        assertThat(intent.getLongExtra(AlarmReceiver.EXTRA_COUNTDOWN_PREVIOUS_TIME, 0L))
            .isEqualTo(previous)
    }
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*PrayerNotificationSchedulerTest"`
Expected: FAIL — `updateCountdown` takes 2 args (compile error) and `EXTRA_COUNTDOWN_PREVIOUS_TIME` is unresolved.

- [ ] **Step 4: Change `updateCountdown` and `scheduleCountdownTick` signatures**

In `PrayerNotificationScheduler.kt`, replace `updateCountdown` (lines 208-216) with:

```kotlin
    fun updateCountdown(targetMillis: Long, prayerName: String, previousTimeMillis: Long? = null) {
        val remaining = targetMillis - System.currentTimeMillis()
        if (remaining <= 0) {
            notificationManager.cancelCountdown()
            return
        }
        notificationManager.showCountdownNotification(prayerName, targetMillis, previousTimeMillis, remaining)
        scheduleCountdownTick(targetMillis, prayerName, previousTimeMillis)
    }
```

Replace `scheduleCountdownTick` (lines 229-239) with:

```kotlin
    private fun scheduleCountdownTick(targetMillis: Long, prayerName: String, previousTimeMillis: Long?) {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_COUNTDOWN_TICK)
            .putExtra(AlarmReceiver.EXTRA_COUNTDOWN_TARGET, targetMillis)
            .putExtra(AlarmReceiver.EXTRA_COUNTDOWN_PRAYER_NAME, prayerName)
        if (previousTimeMillis != null) {
            intent.putExtra(AlarmReceiver.EXTRA_COUNTDOWN_PREVIOUS_TIME, previousTimeMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, SchedulePlan.REQUEST_CODE_COUNTDOWN_TICK, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(System.currentTimeMillis() + 60_000, pendingIntent)
    }
```

- [ ] **Step 5: Add the `EXTRA_COUNTDOWN_PREVIOUS_TIME` constant to `AlarmReceiver`**

In `AlarmReceiver.kt`, add to the companion object (after `EXTRA_COUNTDOWN_PRAYER_NAME`):

```kotlin
        const val EXTRA_COUNTDOWN_PREVIOUS_TIME = "extra_countdown_previous_time"
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*PrayerNotificationSchedulerTest"`
Expected: PASS (existing tests + updated verify + new tick test).

- [ ] **Step 7: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiver.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationSchedulerTest.kt
git commit -m "feat(notifications): carry previous prayer time through countdown tick"
```

---

## Task 5: `scheduleAllSuspending` — overnight Fajr target and previous-time countdown start

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationSchedulerTest.kt`

- [ ] **Step 1: Write the failing test**

Append to `PrayerNotificationSchedulerTest.kt` (before the closing brace):

```kotlin
    @Test
    fun `scheduleAll after last prayer starts countdown to tomorrow's Fajr`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings(
            enabled = true,
            countdownEnabled = true
        )
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
                    time = LocalTime(0, 1),
                    date = LocalDate(2026, 8, 22)
                ),
                Prayer(
                    name = "Isha",
                    arabicName = "العشاء",
                    time = LocalTime(0, 2),
                    date = LocalDate(2026, 8, 22)
                )
            )
        )

        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleAll()

        coVerify { notificationManager.showCountdownNotification("Fajr", any(), any(), any()) }
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*PrayerNotificationSchedulerTest"`
Expected: FAIL — `showCountdownNotification("Fajr", ...)` is never invoked (no overnight branch yet).

- [ ] **Step 3: Compute `nextDayFajrTimeMillis` and pass it to `buildDailyAlarms`**

In `PrayerNotificationScheduler.kt`, inside `scheduleAllSuspending`, after the first `getPrayerTimesUseCase` call (which assigns `prayers`) and before `buildDailyAlarms`, insert:

```kotlin
        val nextDayFajrTimeMillis = if (settings.countdownEnabled) {
            val tomorrow = today.plusDays(1)
            getPrayerTimesUseCase(
                date = LocalDateTime(tomorrow.year, tomorrow.monthValue, tomorrow.dayOfMonth, 0, 0),
                latitude = location.latitude,
                longitude = location.longitude,
                zoneId = zoneId,
                calculationMethod = method,
                persistDailyCache = false
            ).getOrNull()
                ?.firstOrNull { it.name == "Fajr" }
                ?.let {
                    LocalTime.of(it.time.hour, it.time.minute)
                        .atDate(tomorrow)
                        .atZone(zoneId)
                        .toInstant()
                        .toEpochMilli()
                }
        } else {
            null
        }
```

Then add the argument to the `buildDailyAlarms(...)` call (after `jumuahEnabled = settings.jumuahEnabled`):

```kotlin
            nextDayFajrTimeMillis = nextDayFajrTimeMillis
```

- [ ] **Step 4: Update the countdown start to use previous time + overnight branch**

In `PrayerNotificationScheduler.kt`, replace the countdown start block (currently lines 125-130):

```kotlin
        if (settings.countdownEnabled) {
            val nextPrayer = alarms.firstOrNull { it.type == AlarmType.PRAYER }
            if (nextPrayer != null) {
                updateCountdown(nextPrayer.triggerAtMillis, nextPrayer.prayerKey)
            }
        }
```

with:

```kotlin
        if (settings.countdownEnabled) {
            val nextPrayer = alarms.firstOrNull { it.type == AlarmType.PRAYER }
            if (nextPrayer != null) {
                updateCountdown(
                    nextPrayer.triggerAtMillis,
                    nextPrayer.prayerKey,
                    nextPrayer.previousPrayerTimeMillis
                )
            } else if (nextDayFajrTimeMillis != null) {
                val lastEnabledTrigger = prayers
                    .filter { it.name in enabled }
                    .lastOrNull()
                    ?.let {
                        LocalTime.of(it.time.hour, it.time.minute)
                            .atDate(today)
                            .atZone(zoneId)
                            .toInstant()
                            .toEpochMilli()
                    }
                updateCountdown(nextDayFajrTimeMillis, "Fajr", lastEnabledTrigger)
            }
        }
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*PrayerNotificationSchedulerTest"`
Expected: PASS (existing tests + new overnight test).

- [ ] **Step 6: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationSchedulerTest.kt
git commit -m "feat(notifications): keep countdown alive overnight targeting tomorrow's Fajr"
```

---

## Task 6: `AlarmReceiver` — thread previous time through PRAYER and COUNTDOWN_TICK

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiver.kt`
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt` (`scheduleAlarm` — add trigger-time extra)
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiverTest.kt`

- [ ] **Step 1: Write the failing tests**

Update the existing `countdown tick updates countdown` test in `AlarmReceiverTest.kt` to include the previous-time extra and verify the 3-arg call:

```kotlin
    @Test
    fun `countdown tick updates countdown`() {
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_COUNTDOWN_TICK)
            .putExtra(AlarmReceiver.EXTRA_COUNTDOWN_TARGET, System.currentTimeMillis() + 60_000)
            .putExtra(AlarmReceiver.EXTRA_COUNTDOWN_PRAYER_NAME, "Dhuhr")
            .putExtra(AlarmReceiver.EXTRA_COUNTDOWN_PREVIOUS_TIME, System.currentTimeMillis())
        receiver.onReceive(context, intent)
        verify { scheduler.updateCountdown(any(), "Dhuhr", any()) }
    }
```

Append the new test (before the closing brace):

```kotlin
    @Test
    fun `prayer alarm transitions countdown with firing prayer trigger as previous`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings(countdownEnabled = true)
        val receiver = AlarmReceiver()
        val trigger = System.currentTimeMillis() + 60_000
        val nextTime = trigger + 3_600_000
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.PRAYER.name)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, "Asr")
            .putExtra(AlarmReceiver.EXTRA_NEXT_PRAYER_TIME, nextTime)
            .putExtra(AlarmReceiver.EXTRA_NEXT_PRAYER_NAME, "Maghrib")
            .putExtra(AlarmReceiver.EXTRA_ALARM_TRIGGER_TIME, trigger)
        receiver.handleAlarm(intent)
        verify { scheduler.updateCountdown(nextTime, "Maghrib", trigger) }
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*AlarmReceiverTest"`
Expected: FAIL — `EXTRA_ALARM_TRIGGER_TIME` unresolved; `updateCountdown(any(), "Dhuhr", any())` not matched (tick passes 2 args).

- [ ] **Step 3: Add the `EXTRA_ALARM_TRIGGER_TIME` constant**

In `AlarmReceiver.kt`, add to the companion object (after `EXTRA_COUNTDOWN_PREVIOUS_TIME`):

```kotlin
        const val EXTRA_ALARM_TRIGGER_TIME = "extra_alarm_trigger_time"
```

- [ ] **Step 4: Thread previous time through the receiver handlers**

In `AlarmReceiver.kt`, replace the `ACTION_COUNTDOWN_TICK` branch (lines 43-47):

```kotlin
            ACTION_COUNTDOWN_TICK -> {
                val target = intent.getLongExtra(EXTRA_COUNTDOWN_TARGET, 0L)
                val name = intent.getStringExtra(EXTRA_COUNTDOWN_PRAYER_NAME) ?: return
                val previous = if (intent.hasExtra(EXTRA_COUNTDOWN_PREVIOUS_TIME)) {
                    intent.getLongExtra(EXTRA_COUNTDOWN_PREVIOUS_TIME, 0L)
                } else {
                    null
                }
                scheduler.updateCountdown(target, name, previous)
            }
```

In `AlarmReceiver.kt`, replace the countdown transition inside the `AlarmType.PRAYER` branch (lines 77-83):

```kotlin
                if (settings.countdownEnabled) {
                    val nextTime = intent.getLongExtra(EXTRA_NEXT_PRAYER_TIME, 0L)
                    val nextName = intent.getStringExtra(EXTRA_NEXT_PRAYER_NAME)
                    val previous = intent.getLongExtra(EXTRA_ALARM_TRIGGER_TIME, 0L)
                    if (nextTime > 0L && nextName != null) {
                        scheduler.updateCountdown(nextTime, nextName, previous)
                    }
                }
```

- [ ] **Step 5: Add the trigger-time extra in `scheduleAlarm`**

In `PrayerNotificationScheduler.kt`, inside `scheduleAlarm`, add after the `EXTRA_NEXT_PRAYER_NAME` line:

```kotlin
            .putExtra(AlarmReceiver.EXTRA_ALARM_TRIGGER_TIME, alarm.triggerAtMillis)
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*AlarmReceiverTest"`
Expected: PASS (existing tests + updated tick test + new transition test).

- [ ] **Step 7: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiver.kt prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiverTest.kt
git commit -m "feat(notifications): thread previous prayer time through alarm receiver"
```

---

## Task 7: Full verification + docs

**Files:**
- Modify: `TODO.md`

- [ ] **Step 1: Run the full notification test suite**

Run: `./gradlew :prayer_notifications:testDebugUnitTest`
Expected: PASS (all tests in the module).

- [ ] **Step 2: Run the full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: PASS (no regressions in other modules).

- [ ] **Step 3: Update `TODO.md`**

Mark the countdown notification improvement as done (match the existing TODO format used for the delivery wiring).

- [ ] **Step 4: Commit**

```bash
git add TODO.md
git commit -m "docs: mark countdown notification improvement complete"
```
