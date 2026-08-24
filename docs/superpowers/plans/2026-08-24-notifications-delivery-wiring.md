# Notifications Delivery Wiring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the end-to-end notification delivery: wire `AlarmReceiver` to post real prayer notifications + adhan, schedule daily-reminder / Jumu'ah / special-days (incl. a one-day-ahead pre-special-day alert), and run a self-re-arming countdown updated every minute.

**Architecture:** Extend the existing single-scheduler design. `SchedulePlan` emits typed alarms (`PRAYER`, `PRE_PRAYER`, `DAILY_REMINDER`, `SPECIAL_DAY`, `PRE_SPECIAL_DAY`); `PrayerNotificationScheduler` schedules them plus the countdown chain; `AlarmReceiver` dispatches on alarm type; `SpecialDaysCalculator` (Hijri) detects special days inside the existing daily `DailyRescheduleWorker`.

**Tech Stack:** Kotlin, AlarmManager exact alarms, WorkManager, DataStore, Robolectric, MockK, Truth.

**Spec:** `docs/superpowers/specs/2026-08-24-notifications-delivery-wiring-design.md`

---

## File Map

| File | Action |
|---|---|
| `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/AlarmType.kt` | Create — alarm type enum |
| `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/SpecialDay.kt` | Create — special day enum |
| `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/SpecialDaysCalculator.kt` | Create — Hijri special-day detection |
| `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/SchedulePlan.kt` | Modify — typed alarms + daily reminder + special days + jumu'ah + next-prayer |
| `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManager.kt` | Modify — signature changes + new builders |
| `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayer.kt` | Modify — `play(prayerKey)` + 5 audio resources |
| `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt` | Modify — extended scheduling + countdown + cancelAll |
| `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiver.kt` | Modify — dispatch on alarm type |
| `prayer_notifications/src/main/res/values/strings.xml` + 14 locale files | Modify — new notification strings |
| `prayer_notifications/src/main/res/raw/adhan_fajr.mp3` … `adhan_isha.mp3` | Create — downloaded audio |
| `prayer_notifications/src/main/res/raw/adhan.mp3` | Delete — placeholder |
| Tests: `SpecialDaysCalculatorTest.kt` (new), `SchedulePlanTest.kt`, `PrayerNotificationManagerTest.kt`, `AdhanPlayerTest.kt`, `PrayerNotificationSchedulerTest.kt`, `AlarmReceiverTest.kt` | Modify |
| `TODO.md` | Modify — mark delivery wiring done |

---

## Task 1: AlarmType enum + typed ScheduledAlarm

**Files:**
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/AlarmType.kt`
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/SchedulePlan.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/domain/SchedulePlanTest.kt`

- [ ] **Step 1: Create `AlarmType.kt`**

```kotlin
package com.kutluoglu.prayer_notifications.domain

enum class AlarmType {
    PRAYER,
    PRE_PRAYER,
    COUNTDOWN_TICK,
    DAILY_REMINDER,
    SPECIAL_DAY,
    PRE_SPECIAL_DAY
}
```

- [ ] **Step 2: Replace `isPrePrayer` with `type` in `ScheduledAlarm`**

In `SchedulePlan.kt`, change the `ScheduledAlarm` data class:

```kotlin
data class ScheduledAlarm(
    val prayerKey: String,
    val triggerAtMillis: Long,
    val requestCode: Int,
    val type: AlarmType = AlarmType.PRAYER
)
```

- [ ] **Step 3: Set the type in `buildDailyAlarms`**

In `SchedulePlan.kt`, the prayer alarm construction becomes:

```kotlin
            if (trigger.isAfter(now)) {
                result += ScheduledAlarm(
                    prayerKey = prayer.name,
                    triggerAtMillis = trigger.toEpochMilli(),
                    requestCode = requestCode++,
                    type = AlarmType.PRAYER
                )
            }
```

and the pre-prayer alarm becomes:

```kotlin
                if (preTrigger.isAfter(now)) {
                    result += ScheduledAlarm(
                        prayerKey = "${prayer.name}_pre",
                        triggerAtMillis = preTrigger.toEpochMilli(),
                        requestCode = requestCode++,
                        type = AlarmType.PRE_PRAYER
                    )
                }
```

- [ ] **Step 4: Run the existing tests**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*SchedulePlanTest*"`
Expected: PASS (existing assertions don't reference `isPrePrayer`).

- [ ] **Step 5: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/AlarmType.kt prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/SchedulePlan.kt
git commit -m "feat(notifications): add AlarmType and type alarms"
```

---

## Task 2: SpecialDaysCalculator

**Files:**
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/SpecialDay.kt`
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/SpecialDaysCalculator.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/domain/SpecialDaysCalculatorTest.kt`

- [ ] **Step 1: Write the failing test**

Create `SpecialDaysCalculatorTest.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.domain

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SpecialDaysCalculatorTest {

    private val calculator = SpecialDaysCalculator()

    @Test
    fun `detects Ramadan start`() {
        assertThat(calculator.specialDayFor(LocalDate.of(2026, 2, 18)))
            .isEqualTo(SpecialDay.RAMADAN_START)
    }

    @Test
    fun `detects Laylat al-Qadr`() {
        assertThat(calculator.specialDayFor(LocalDate.of(2026, 3, 16)))
            .isEqualTo(SpecialDay.LAYLAT_AL_QADIR)
    }

    @Test
    fun `detects Eid al-Fitr`() {
        assertThat(calculator.specialDayFor(LocalDate.of(2026, 3, 20)))
            .isEqualTo(SpecialDay.EID_AL_FITR)
    }

    @Test
    fun `detects Eid al-Adha`() {
        assertThat(calculator.specialDayFor(LocalDate.of(2026, 5, 27)))
            .isEqualTo(SpecialDay.EID_AL_ADHA)
    }

    @Test
    fun `returns null for ordinary days`() {
        assertThat(calculator.specialDayFor(LocalDate.of(2026, 6, 15))).isNull()
    }

    @Test
    fun `applies hijri adjustment`() {
        // 2026-02-18 is 1 Ramadan 1447. +1 day -> 2 Ramadan (not special).
        assertThat(calculator.specialDayFor(LocalDate.of(2026, 2, 18), hijriAdjustment = 1)).isNull()
        // 2026-02-17 is 29 Sha'ban 1447. +1 day -> 1 Ramadan 1447.
        assertThat(calculator.specialDayFor(LocalDate.of(2026, 2, 17), hijriAdjustment = 1))
            .isEqualTo(SpecialDay.RAMADAN_START)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*SpecialDaysCalculatorTest*"`
Expected: FAIL — `SpecialDaysCalculator` not defined.

- [ ] **Step 3: Create `SpecialDay.kt`**

```kotlin
package com.kutluoglu.prayer_notifications.domain

enum class SpecialDay {
    RAMADAN_START,
    EID_AL_FITR,
    EID_AL_ADHA,
    LAYLAT_AL_QADIR
}
```

- [ ] **Step 4: Create `SpecialDaysCalculator.kt`**

```kotlin
package com.kutluoglu.prayer_notifications.domain

import org.koin.core.annotation.Factory
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

@Factory
class SpecialDaysCalculator {

    fun specialDayFor(date: LocalDate, hijriAdjustment: Int = 0): SpecialDay? {
        val hijrah = HijrahDate.from(date).plus(hijriAdjustment.toLong(), ChronoUnit.DAYS)
        val month = hijrah.get(ChronoField.MONTH_OF_YEAR)
        val day = hijrah.get(ChronoField.DAY_OF_MONTH)
        return when {
            month == 9 && day == 1 -> SpecialDay.RAMADAN_START
            month == 9 && day == 27 -> SpecialDay.LAYLAT_AL_QADIR
            month == 10 && day == 1 -> SpecialDay.EID_AL_FITR
            month == 12 && day == 10 -> SpecialDay.EID_AL_ADHA
            else -> null
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*SpecialDaysCalculatorTest*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/SpecialDay.kt prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/SpecialDaysCalculator.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/domain/SpecialDaysCalculatorTest.kt
git commit -m "feat(notifications): add SpecialDaysCalculator for Hijri special days"
```

---

## Task 3: SchedulePlan extension

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/SchedulePlan.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/domain/SchedulePlanTest.kt`

- [ ] **Step 1: Write the failing tests**

Append these tests to `SchedulePlanTest.kt`:

```kotlin
    @Test
    fun `adds daily reminder alarm when enabled`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Fajr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false,
            dailyReminderEnabled = true,
            dailyReminderHour = 8,
            dailyReminderMinute = 0,
            dailySummary = "Fajr 04:30"
        )
        val reminder = alarms.first { it.type == AlarmType.DAILY_REMINDER }
        assertThat(reminder.dailySummary).isEqualTo("Fajr 04:30")
        assertThat(reminder.requestCode).isEqualTo(SchedulePlan.REQUEST_CODE_DAILY_REMINDER)
    }

    @Test
    fun `skips daily reminder when time already passed`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T15:00:00Z"), // 18:00 Istanbul
            enabledPrayers = setOf("Fajr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false,
            dailyReminderEnabled = true,
            dailyReminderHour = 8,
            dailyReminderMinute = 0
        )
        assertThat(alarms.none { it.type == AlarmType.DAILY_REMINDER }).isTrue()
    }

    @Test
    fun `adds special day and pre-special day alarms`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"),
            enabledPrayers = setOf("Fajr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false,
            specialDayToday = SpecialDay.EID_AL_FITR,
            specialDayTomorrow = SpecialDay.EID_AL_ADHA
        )
        assertThat(alarms.first { it.type == AlarmType.SPECIAL_DAY }.specialDay)
            .isEqualTo(SpecialDay.EID_AL_FITR)
        assertThat(alarms.first { it.type == AlarmType.PRE_SPECIAL_DAY }.specialDay)
            .isEqualTo(SpecialDay.EID_AL_ADHA)
    }

    @Test
    fun `marks Friday Dhuhr alarm as jumuah`() {
        // 2026-08-22 is a Saturday; use a Friday: 2026-08-21.
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-21T00:00:00Z"),
            enabledPrayers = setOf("Dhuhr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false,
            jumuahEnabled = true
        )
        assertThat(alarms.single().isJumuah).isTrue()
    }

    @Test
    fun `does not mark non-Friday Dhuhr as jumuah`() {
        val plan = SchedulePlan()
        val alarms = plan.buildDailyAlarms(
            prayers = prayers,
            zoneId = zone,
            now = Instant.parse("2026-08-22T00:00:00Z"), // Saturday
            enabledPrayers = setOf("Dhuhr"),
            prePrayerMinutes = 15,
            prePrayerEnabled = false,
            jumuahEnabled = true
        )
        assertThat(alarms.single().isJumuah).isFalse()
    }

    @Test
    fun `carries next prayer time and name on prayer alarms`() {
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
        assertThat(fajr.nextPrayerName).isEqualTo("Dhuhr")
        assertThat(fajr.nextPrayerTimeMillis).isNotNull()
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*SchedulePlanTest*"`
Expected: FAIL — new fields/methods not defined.

- [ ] **Step 3: Extend `ScheduledAlarm`**

In `SchedulePlan.kt`, replace the `ScheduledAlarm` data class with:

```kotlin
data class ScheduledAlarm(
    val prayerKey: String,
    val triggerAtMillis: Long,
    val requestCode: Int,
    val type: AlarmType = AlarmType.PRAYER,
    val isJumuah: Boolean = false,
    val nextPrayerTimeMillis: Long? = null,
    val nextPrayerName: String? = null,
    val prePrayerMinutes: Int? = null,
    val dailySummary: String? = null,
    val specialDay: SpecialDay? = null
)
```

- [ ] **Step 4: Add request-code constants to the `SchedulePlan` companion**

```kotlin
    companion object {
        const val REQUEST_CODE_COUNTDOWN_TICK = 2000
        const val REQUEST_CODE_DAILY_REMINDER = 2001
        const val REQUEST_CODE_SPECIAL_DAY = 2002
        const val REQUEST_CODE_PRE_SPECIAL_DAY = 2003
    }
```

- [ ] **Step 5: Rewrite `buildDailyAlarms`**

Replace the whole `buildDailyAlarms` function with:

```kotlin
    fun buildDailyAlarms(
        prayers: List<Prayer>,
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
        val nowZoned = now.atZone(zoneId)
        val result = mutableListOf<ScheduledAlarm>()
        var requestCode = 1000

        val enabled = prayers.filter { it.name in enabledPrayers }
        enabled.forEachIndexed { index, prayer ->
            val trigger = LocalTime.of(prayer.time.hour, prayer.time.minute)
                .atDate(nowZoned.toLocalDate())
                .atZone(zoneId)
                .toInstant()
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

        if (dailyReminderEnabled) {
            val reminderTrigger = LocalTime.of(dailyReminderHour, dailyReminderMinute)
                .atDate(nowZoned.toLocalDate())
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
                .atDate(nowZoned.toLocalDate())
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
                .atDate(nowZoned.toLocalDate())
                .atZone(zoneId)
                .toInstant()
            if (specialTrigger.isAfter(now)) {
                result += ScheduledAlarm(
                    prayerKey = "pre_special_day",
                    triggerAtMillis = specialTrigger.toEpochMilli(),
                    requestCode = REQUEST_CODE_PRE_SPECIAL_DAY,
                    type = AlarmType.PRE_SPECIAL_DAY,
                    specialDay = day
                )
            }
        }

        return result
    }
```

Add the import `java.time.DayOfWeek` to `SchedulePlan.kt`.

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*SchedulePlanTest*"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/domain/SchedulePlan.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/domain/SchedulePlanTest.kt
git commit -m "feat(notifications): extend SchedulePlan with reminder, special-day, jumuah alarms"
```

---

## Task 4: New notification strings (English)

**Files:**
- Modify: `prayer_notifications/src/main/res/values/strings.xml`

- [ ] **Step 1: Add the new strings**

Append to `prayer_notifications/src/main/res/values/strings.xml`:

```xml
    <string name="notification_jumuah_title">Jumu\'ah</string>
    <string name="notification_jumuah_body">Friday prayer time</string>
    <string name="notification_pre_prayer">in %1$d minutes</string>
    <string name="notification_daily_reminder_title">Today\'s prayer times</string>
    <string name="notification_special_day_title">Special day</string>
    <string name="notification_special_day_body">Today is %1$s</string>
    <string name="notification_pre_special_day_title">Tomorrow</string>
    <string name="notification_pre_special_day_body">Tomorrow is %1$s</string>
    <string name="special_day_ramadan_start">Ramadan start</string>
    <string name="special_day_eid_al_fitr">Eid al-Fitr</string>
    <string name="special_day_eid_al_adha">Eid al-Adha</string>
    <string name="special_day_laylat_al_qadr">Laylat al-Qadr</string>
```

- [ ] **Step 2: Verify build**

Run: `./gradlew :prayer_notifications:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add prayer_notifications/src/main/res/values/strings.xml
git commit -m "feat(notifications): add English notification strings"
```

---

## Task 5: PrayerNotificationManager — signatures + new builders

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManager.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManagerTest.kt`

- [ ] **Step 1: Write the failing tests**

Append to `PrayerNotificationManagerTest.kt`:

```kotlin
    @Test
    fun `showPrayerNotification takes a prayer name`() {
        manager.createChannels()
        manager.showPrayerNotification("Fajr", NotificationSettings())
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("prayer_alerts")
    }

    @Test
    fun `showJumuahNotification posts on reminders channel`() {
        manager.createChannels()
        manager.showJumuahNotification()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("reminders")
    }

    @Test
    fun `showPrePrayerNotification posts on reminders channel`() {
        manager.createChannels()
        manager.showPrePrayerNotification("Fajr", 15)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("reminders")
    }

    @Test
    fun `showDailyReminderNotification posts summary on reminders channel`() {
        manager.createChannels()
        manager.showDailyReminderNotification("Fajr 04:30 · Dhuhr 12:30")
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = shadowOf(nm).allNotifications.single()
        assertThat(notification.channelId).isEqualTo("reminders")
        assertThat(notification.extras.getString("android.text")).isEqualTo("Fajr 04:30 · Dhuhr 12:30")
    }

    @Test
    fun `showSpecialDayNotification posts on reminders channel`() {
        manager.createChannels()
        manager.showSpecialDayNotification(SpecialDay.EID_AL_FITR)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("reminders")
    }

    @Test
    fun `showPreSpecialDayNotification posts on reminders channel`() {
        manager.createChannels()
        manager.showPreSpecialDayNotification(SpecialDay.EID_AL_ADHA)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("reminders")
    }
```

Also update the existing `showPrayerNotification picks channel based on adhanEnabled` test — replace the `Prayer` construction with a name string:

```kotlin
    @Test
    fun `showPrayerNotification picks channel based on adhanEnabled`() {
        manager.createChannels()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.showPrayerNotification("Fajr", NotificationSettings(adhanEnabled = true))
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("adhan")

        manager.showPrayerNotification("Fajr", NotificationSettings(adhanEnabled = false))
        assertThat(shadowOf(nm).allNotifications.single().channelId).isEqualTo("prayer_alerts")
    }
```

Remove the now-unused `Prayer`, `LocalDate`, `LocalTime` imports from the test if no longer referenced.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*PrayerNotificationManagerTest*"`
Expected: FAIL — signature mismatch / methods not defined.

- [ ] **Step 3: Update `showPrayerNotification` and `showCountdownNotification`**

In `PrayerNotificationManager.kt`, change:

```kotlin
    fun showPrayerNotification(prayerName: String, settings: NotificationSettings) {
        val channel = if (settings.adhanEnabled) CHANNEL_ADHAN else CHANNEL_PRAYER_ALERTS
        val localizedName = localizedPrayerName(prayerName)
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localizedName)
            .setContentText(localizedString(R.string.notification_prayer_time, localizedName))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        notificationManager.notify(NOTIFICATION_ID_PRAYER, builder.build())
    }

    fun showCountdownNotification(nextPrayerName: String, remainingMillis: Long) {
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
        val notification = NotificationCompat.Builder(context, CHANNEL_COUNTDOWN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                localizedString(R.string.notification_next_prayer, localizedPrayerName(nextPrayerName))
            )
            .setContentText(formatRemaining(remainingMillis))
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, localizedString(R.string.notification_stop), stopIntent)
            .build()
        notificationManager.notify(NOTIFICATION_ID_COUNTDOWN, notification)
    }
```

- [ ] **Step 4: Add new notification IDs to the companion**

```kotlin
        const val NOTIFICATION_ID_JUMUAH = 1004
        const val NOTIFICATION_ID_PRE_PRAYER = 1005
        const val NOTIFICATION_ID_DAILY_REMINDER = 1006
        const val NOTIFICATION_ID_SPECIAL_DAY = 1007
        const val NOTIFICATION_ID_PRE_SPECIAL_DAY = 1008
```

- [ ] **Step 5: Add the new builder methods**

Add before `private fun formatRemaining`:

```kotlin
    fun showJumuahNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localizedString(R.string.notification_jumuah_title))
            .setContentText(localizedString(R.string.notification_jumuah_body))
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_JUMUAH, notification)
    }

    fun showPrePrayerNotification(prayerName: String, minutes: Int) {
        val localizedName = localizedPrayerName(prayerName)
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localizedName)
            .setContentText(localizedString(R.string.notification_pre_prayer, minutes))
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_PRE_PRAYER, notification)
    }

    fun showDailyReminderNotification(summary: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localizedString(R.string.notification_daily_reminder_title))
            .setContentText(summary)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_DAILY_REMINDER, notification)
    }

    fun showSpecialDayNotification(day: SpecialDay) {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localizedString(R.string.notification_special_day_title))
            .setContentText(
                localizedString(R.string.notification_special_day_body, localizedSpecialDay(day))
            )
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_SPECIAL_DAY, notification)
    }

    fun showPreSpecialDayNotification(day: SpecialDay) {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localizedString(R.string.notification_pre_special_day_title))
            .setContentText(
                localizedString(R.string.notification_pre_special_day_body, localizedSpecialDay(day))
            )
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_PRE_SPECIAL_DAY, notification)
    }
```

- [ ] **Step 6: Add `localizedSpecialDay` helper**

Add next to `localizedPrayerName`:

```kotlin
    private fun localizedSpecialDay(day: SpecialDay): String = when (day) {
        SpecialDay.RAMADAN_START -> localizedString(R.string.special_day_ramadan_start)
        SpecialDay.EID_AL_FITR -> localizedString(R.string.special_day_eid_al_fitr)
        SpecialDay.EID_AL_ADHA -> localizedString(R.string.special_day_eid_al_adha)
        SpecialDay.LAYLAT_AL_QADIR -> localizedString(R.string.special_day_laylat_al_qadr)
    }
```

Add the import `com.kutluoglu.prayer_notifications.domain.SpecialDay` and remove the now-unused `com.kutluoglu.prayer.model.prayer.Prayer` import.

- [ ] **Step 7: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*PrayerNotificationManagerTest*"`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManager.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManagerTest.kt
git commit -m "feat(notifications): add jumuah, pre-prayer, daily-reminder, special-day builders"
```

---

## Task 6: AdhanPlayer — per-prayer audio + 5 assets

**Files:**
- Create: `prayer_notifications/src/main/res/raw/adhan_fajr.mp3`, `adhan_dhuhr.mp3`, `adhan_asr.mp3`, `adhan_maghrib.mp3`, `adhan_isha.mp3`
- Delete: `prayer_notifications/src/main/res/raw/adhan.mp3`
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayer.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayerTest.kt`

- [ ] **Step 1: Download the 5 adhan clips (public domain, archive.org)**

```bash
curl -L -o prayer_notifications/src/main/res/raw/adhan_fajr.mp3 "https://archive.org/download/adhan.notifications/Mishary_Rashid_al_Afasy_Fajr_Adhan.mp3"
curl -L -o prayer_notifications/src/main/res/raw/adhan_dhuhr.mp3 "https://archive.org/download/adhan.notifications/Ahmed_al_Imadi_Adhan.mp3"
curl -L -o prayer_notifications/src/main/res/raw/adhan_asr.mp3 "https://archive.org/download/adhan.notifications/Majed_al_Hamathani_Adhan.mp3"
curl -L -o prayer_notifications/src/main/res/raw/adhan_maghrib.mp3 "https://archive.org/download/adhan.notifications/Mokhtar_Hadj_Slimane_Adhan.mp3"
curl -L -o prayer_notifications/src/main/res/raw/adhan_isha.mp3 "https://archive.org/download/adhan.notifications/Nasser_al_Qatami_Adhan.mp3"
rm prayer_notifications/src/main/res/raw/adhan.mp3
```

Verify each file is a valid MP3 (`file prayer_notifications/src/main/res/raw/adhan_*.mp3`).

- [ ] **Step 2: Write the failing test**

Update `AdhanPlayerTest.kt` — replace `player.play()` calls with `player.play("Fajr")`:

```kotlin
package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.kutluoglu.prayer_notifications.R
import java.io.IOException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AdhanPlayerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `play and stop do not throw`() {
        val player = AdhanPlayer(context)
        player.play("Fajr")
        player.stop()
    }

    @Test
    fun `play does not throw when playback fails`() {
        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_fajr}")
        ShadowMediaPlayer.addException(DataSource.toDataSource(context, uri), IOException("boom"))
        val player = AdhanPlayer(context)
        player.play("Fajr")
        player.stop()
    }

    @Test
    fun `stop is idempotent`() {
        val player = AdhanPlayer(context)
        player.play("Dhuhr")
        player.stop()
        player.stop()
    }

    @Test
    fun `repeated play calls do not throw`() {
        val player = AdhanPlayer(context)
        player.play("Fajr")
        player.play("Isha")
        player.stop()
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*AdhanPlayerTest*"`
Expected: FAIL — `play(String)` not defined.

- [ ] **Step 4: Update `AdhanPlayer`**

Replace `AdhanPlayer.kt` with:

```kotlin
package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.kutluoglu.prayer_notifications.R
import org.koin.core.annotation.Single

@Single
class AdhanPlayer(
    private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    fun play(prayerKey: String) {
        stop()
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
                prepare()
                start()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            runCatching { player.release() }
            Log.e("AdhanPlayer", "Failed to play adhan -> ${e.message}")
        }
    }

    fun stop() {
        mediaPlayer?.let {
            runCatching { it.stop() }
            it.release()
        }
        mediaPlayer = null
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*AdhanPlayerTest*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add prayer_notifications/src/main/res/raw/ prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayer.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayerTest.kt
git commit -m "feat(notifications): per-prayer adhan audio and play(prayerKey)"
```

---

## Task 7: PrayerNotificationScheduler — extended scheduling + countdown

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt`
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiver.kt` (constants only)
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationSchedulerTest.kt`

- [ ] **Step 0: Add the new intent-extra and action constants to `AlarmReceiver`**

The scheduler (this task) references these constants, so they must exist before the scheduler changes compile. In `AlarmReceiver.kt`, extend the companion object:

```kotlin
    companion object {
        const val EXTRA_PRAYER_KEY = "extra_prayer_key"
        const val EXTRA_ALARM_TYPE = "extra_alarm_type"
        const val EXTRA_IS_JUMUAH = "extra_is_jumuah"
        const val EXTRA_NEXT_PRAYER_TIME = "extra_next_prayer_time"
        const val EXTRA_NEXT_PRAYER_NAME = "extra_next_prayer_name"
        const val EXTRA_PRE_PRAYER_MINUTES = "extra_pre_prayer_minutes"
        const val EXTRA_DAILY_SUMMARY = "extra_daily_summary"
        const val EXTRA_SPECIAL_DAY = "extra_special_day"
        const val EXTRA_COUNTDOWN_TARGET = "extra_countdown_target"
        const val EXTRA_COUNTDOWN_PRAYER_NAME = "extra_countdown_prayer_name"
        const val ACTION_STOP_COUNTDOWN = "STOP_COUNTDOWN"
        const val ACTION_COUNTDOWN_TICK = "COUNTDOWN_TICK"
    }
```

- [ ] **Step 1: Write the failing tests**

Append to `PrayerNotificationSchedulerTest.kt`:

```kotlin
    @Test
    fun `cancelAll cancels countdown tick and reminder alarms`() = runTest {        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.DAILY_REMINDER.name)
        val pendingIntent = PendingIntent.getBroadcast(
            context, SchedulePlan.REQUEST_CODE_DAILY_REMINDER, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 60_000,
            pendingIntent
        )
        assertThat(shadowOf(alarmManager).scheduledAlarms).hasSize(1)

        scheduler.cancelAll()

        assertThat(shadowOf(alarmManager).scheduledAlarms).isEmpty()
    }

    @Test
    fun `scheduleAll starts countdown when enabled`() = runTest {
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
                    time = LocalTime(23, 59),
                    date = LocalDate(2026, 8, 22)
                )
            )
        )

        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleAll()

        coVerify { notificationManager.showCountdownNotification("Fajr", any()) }
    }
```

Add the imports `com.kutluoglu.prayer_notifications.domain.AlarmType`, `com.kutluoglu.prayer_notifications.domain.SchedulePlan`, and `io.mockk.coVerify` to the test.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*PrayerNotificationSchedulerTest*"`
Expected: FAIL — `updateCountdown`/countdown behavior not present.

- [ ] **Step 3: Add `SpecialDaysCalculator` to the constructor**

In `PrayerNotificationScheduler.kt`, add a constructor param with a default (keeps existing callers compiling):

```kotlin
    private val specialDaysCalculator: SpecialDaysCalculator = SpecialDaysCalculator(),
```

Add the import `com.kutluoglu.prayer_notifications.domain.SpecialDaysCalculator`.

- [ ] **Step 4: Restructure `scheduleAllSuspending`**

Replace the body of `scheduleAllSuspending` (from `val alarms = runCatching {` through the `alarms.forEach { scheduleAlarm(...) }` line) with:

```kotlin
        val appSettings = runCatching { getSettingsUseCase() }.getOrElse {
            cancelAll()
            cancelDailyReschedule()
            return
        }
        val zoneId = runCatching { ZoneId.of(appSettings.location.timeZone) }
            .getOrDefault(ZoneId.systemDefault())
        val today = LocalDate.now(zoneId)
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

        val enabled = settings.prayerToggles.filterValues { it }.keys
        val summary = buildDailySummary(prayers)
        val specialDayToday = specialDaysCalculator.specialDayFor(today, appSettings.hijriAdjustment)
        val specialDayTomorrow = specialDaysCalculator.specialDayFor(today.plusDays(1), appSettings.hijriAdjustment)
        val alarms = schedulePlan.buildDailyAlarms(
            prayers = prayers,
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
        cancelAll()
        alarms.forEach { scheduleAlarm(it) }
        if (settings.countdownEnabled) {
            val nextPrayer = alarms.firstOrNull { it.type == AlarmType.PRAYER }
            if (nextPrayer != null) {
                updateCountdown(nextPrayer.triggerAtMillis, nextPrayer.prayerKey)
            }
        }
```

Add the import `java.time.LocalDate`.

- [ ] **Step 5: Replace `scheduleAlarm` and add the new helpers**

Replace the existing `scheduleAlarm` with:

```kotlin
    private fun scheduleAlarm(alarm: ScheduledAlarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, alarm.type.name)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, alarm.prayerKey)
            .putExtra(AlarmReceiver.EXTRA_IS_JUMUAH, alarm.isJumuah)
            .putExtra(AlarmReceiver.EXTRA_NEXT_PRAYER_TIME, alarm.nextPrayerTimeMillis ?: 0L)
            .putExtra(AlarmReceiver.EXTRA_NEXT_PRAYER_NAME, alarm.nextPrayerName ?: "")
            .putExtra(AlarmReceiver.EXTRA_PRE_PRAYER_MINUTES, alarm.prePrayerMinutes ?: 0)
            .putExtra(AlarmReceiver.EXTRA_DAILY_SUMMARY, alarm.dailySummary ?: "")
            .putExtra(AlarmReceiver.EXTRA_SPECIAL_DAY, alarm.specialDay?.name ?: "")
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarm.requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(alarm.triggerAtMillis, pendingIntent)
    }

    private fun setExactAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun updateCountdown(targetMillis: Long, prayerName: String) {
        val remaining = targetMillis - System.currentTimeMillis()
        if (remaining <= 0) {
            notificationManager.cancelCountdown()
            return
        }
        notificationManager.showCountdownNotification(prayerName, remaining)
        scheduleCountdownTick(targetMillis, prayerName)
    }

    fun cancelCountdown() {
        notificationManager.cancelCountdown()
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, SchedulePlan.REQUEST_CODE_COUNTDOWN_TICK, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    private fun scheduleCountdownTick(targetMillis: Long, prayerName: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_COUNTDOWN_TICK)
            .putExtra(AlarmReceiver.EXTRA_COUNTDOWN_TARGET, targetMillis)
            .putExtra(AlarmReceiver.EXTRA_COUNTDOWN_PRAYER_NAME, prayerName)
        val pendingIntent = PendingIntent.getBroadcast(
            context, SchedulePlan.REQUEST_CODE_COUNTDOWN_TICK, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(System.currentTimeMillis() + 60_000, pendingIntent)
    }

    suspend fun scheduleDailyReminder() {
        val settings = dataStore.getSettings()
        if (!settings.dailyReminderEnabled) return
        val location = locationsCoordinator.resolveSelected() ?: return
        val appSettings = runCatching { getSettingsUseCase() }.getOrNull() ?: return
        val zoneId = runCatching { ZoneId.of(appSettings.location.timeZone) }
            .getOrDefault(ZoneId.systemDefault())
        val tomorrow = LocalDate.now(zoneId).plusDays(1)
        val method = CalculationMethod.fromSettingsId(appSettings.calculationMethod)
        val prayers = getPrayerTimesUseCase(
            date = LocalDateTime(tomorrow.year, tomorrow.monthValue, tomorrow.dayOfMonth, 0, 0),
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = zoneId,
            calculationMethod = method
        ).getOrNull() ?: return
        val trigger = LocalTime.of(settings.dailyReminderHour, settings.dailyReminderMinute)
            .atDate(tomorrow)
            .atZone(zoneId)
            .toInstant()
        scheduleAlarm(
            ScheduledAlarm(
                prayerKey = "daily_reminder",
                triggerAtMillis = trigger.toEpochMilli(),
                requestCode = SchedulePlan.REQUEST_CODE_DAILY_REMINDER,
                type = AlarmType.DAILY_REMINDER,
                dailySummary = buildDailySummary(prayers)
            )
        )
    }

    private fun buildDailySummary(prayers: List<Prayer>): String =
        prayers.joinToString(" · ") { prayer ->
            val time = "${prayer.time.hour.toString().padStart(2, '0')}:" +
                prayer.time.minute.toString().padStart(2, '0')
            "${prayer.name} $time"
        }
```

Add imports: `com.kutluoglu.prayer_notifications.domain.AlarmType`, `com.kutluoglu.prayer_notifications.domain.SchedulePlan`, `com.kutluoglu.prayer_notifications.domain.ScheduledAlarm`, `java.time.LocalTime`.

- [ ] **Step 6: Extend `cancelAll`**

Replace `cancelAll` with:

```kotlin
    fun cancelAll() {
        // Cancel all pending alarms by re-issuing the same PendingIntents with FLAG_NO_CREATE.
        for (code in REQUEST_CODE_START until REQUEST_CODE_END) {
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, code, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
        listOf(
            SchedulePlan.REQUEST_CODE_COUNTDOWN_TICK,
            SchedulePlan.REQUEST_CODE_DAILY_REMINDER,
            SchedulePlan.REQUEST_CODE_SPECIAL_DAY,
            SchedulePlan.REQUEST_CODE_PRE_SPECIAL_DAY
        ).forEach { code ->
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, code, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiver.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationSchedulerTest.kt
git commit -m "feat(notifications): schedule reminders, special days, and countdown chain"
```

---

## Task 8: AlarmReceiver — dispatch on alarm type

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiver.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiverTest.kt`

- [ ] **Step 1: Write the failing tests**

Replace `AlarmReceiverTest.kt` with:

```kotlin
package com.kutluoglu.prayer_notifications.scheduler

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.AlarmType
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.SpecialDay
import com.kutluoglu.prayer_notifications.manager.AdhanPlayer
import com.kutluoglu.prayer_notifications.manager.PrayerNotificationManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlarmReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val notificationManager = mockk<PrayerNotificationManager>(relaxed = true)
    private val adhanPlayer = mockk<AdhanPlayer>(relaxed = true)
    private val scheduler = mockk<PrayerNotificationScheduler>(relaxed = true)
    private val dataStore = mockk<NotificationSettingsDataStore>(relaxed = true)

    @Before
    fun setUp() {
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(context)
                modules(module {
                    single { notificationManager }
                    single { adhanPlayer }
                    single { scheduler }
                    single { dataStore }
                })
            }
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `prayer alarm posts prayer notification and plays adhan`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings(adhanEnabled = true)
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.PRAYER.name)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, "Fajr")
        receiver.handleAlarm(intent)
        verify { notificationManager.showPrayerNotification("Fajr", any()) }
        verify { adhanPlayer.play("Fajr") }
    }

    @Test
    fun `jumuah prayer alarm posts jumuah notification`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings(jumuahEnabled = true)
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.PRAYER.name)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, "Dhuhr")
            .putExtra(AlarmReceiver.EXTRA_IS_JUMUAH, true)
        receiver.handleAlarm(intent)
        verify { notificationManager.showJumuahNotification() }
        verify(exactly = 0) { notificationManager.showPrayerNotification(any(), any()) }
    }

    @Test
    fun `pre-prayer alarm posts pre-prayer notification`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings()
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.PRE_PRAYER.name)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, "Fajr_pre")
            .putExtra(AlarmReceiver.EXTRA_PRE_PRAYER_MINUTES, 15)
        receiver.handleAlarm(intent)
        verify { notificationManager.showPrePrayerNotification("Fajr", 15) }
    }

    @Test
    fun `daily reminder alarm posts summary and re-arms`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings()
        coEvery { scheduler.scheduleDailyReminder() } returns Unit
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.DAILY_REMINDER.name)
            .putExtra(AlarmReceiver.EXTRA_DAILY_SUMMARY, "Fajr 04:30")
        receiver.handleAlarm(intent)
        verify { notificationManager.showDailyReminderNotification("Fajr 04:30") }
        coVerify { scheduler.scheduleDailyReminder() }
    }

    @Test
    fun `special day alarm posts special day notification`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings()
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.SPECIAL_DAY.name)
            .putExtra(AlarmReceiver.EXTRA_SPECIAL_DAY, SpecialDay.EID_AL_FITR.name)
        receiver.handleAlarm(intent)
        verify { notificationManager.showSpecialDayNotification(SpecialDay.EID_AL_FITR) }
    }

    @Test
    fun `pre-special day alarm posts pre-special day notification`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings()
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.PRE_SPECIAL_DAY.name)
            .putExtra(AlarmReceiver.EXTRA_SPECIAL_DAY, SpecialDay.RAMADAN_START.name)
        receiver.handleAlarm(intent)
        verify { notificationManager.showPreSpecialDayNotification(SpecialDay.RAMADAN_START) }
    }

    @Test
    fun `countdown tick updates countdown`() {
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_COUNTDOWN_TICK)
            .putExtra(AlarmReceiver.EXTRA_COUNTDOWN_TARGET, System.currentTimeMillis() + 60_000)
            .putExtra(AlarmReceiver.EXTRA_COUNTDOWN_PRAYER_NAME, "Dhuhr")
        receiver.onReceive(context, intent)
        verify { scheduler.updateCountdown(any(), "Dhuhr") }
    }

    @Test
    fun `STOP_COUNTDOWN cancels countdown`() {
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_STOP_COUNTDOWN)
        receiver.onReceive(context, intent)
        verify { scheduler.cancelCountdown() }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*AlarmReceiverTest*"`
Expected: FAIL — `handleAlarm` not defined / new extras missing.

- [ ] **Step 3: Rewrite `AlarmReceiver`**

Replace `AlarmReceiver.kt` with:

```kotlin
package com.kutluoglu.prayer_notifications.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.AlarmType
import com.kutluoglu.prayer_notifications.domain.SpecialDay
import com.kutluoglu.prayer_notifications.manager.AdhanPlayer
import com.kutluoglu.prayer_notifications.manager.PrayerNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AlarmReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        const val EXTRA_PRAYER_KEY = "extra_prayer_key"
        const val EXTRA_ALARM_TYPE = "extra_alarm_type"
        const val EXTRA_IS_JUMUAH = "extra_is_jumuah"
        const val EXTRA_NEXT_PRAYER_TIME = "extra_next_prayer_time"
        const val EXTRA_NEXT_PRAYER_NAME = "extra_next_prayer_name"
        const val EXTRA_PRE_PRAYER_MINUTES = "extra_pre_prayer_minutes"
        const val EXTRA_DAILY_SUMMARY = "extra_daily_summary"
        const val EXTRA_SPECIAL_DAY = "extra_special_day"
        const val EXTRA_COUNTDOWN_TARGET = "extra_countdown_target"
        const val EXTRA_COUNTDOWN_PRAYER_NAME = "extra_countdown_prayer_name"
        const val ACTION_STOP_COUNTDOWN = "STOP_COUNTDOWN"
        const val ACTION_COUNTDOWN_TICK = "COUNTDOWN_TICK"
    }

    private val notificationManager: PrayerNotificationManager by inject()
    private val adhanPlayer: AdhanPlayer by inject()
    private val scheduler: PrayerNotificationScheduler by inject()
    private val dataStore: NotificationSettingsDataStore by inject()
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_STOP_COUNTDOWN -> scheduler.cancelCountdown()
            ACTION_COUNTDOWN_TICK -> {
                val target = intent.getLongExtra(EXTRA_COUNTDOWN_TARGET, 0L)
                val name = intent.getStringExtra(EXTRA_COUNTDOWN_PRAYER_NAME) ?: return
                scheduler.updateCountdown(target, name)
            }
            else -> {
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        handleAlarm(intent)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    internal suspend fun handleAlarm(intent: Intent) {
        val type = intent.getStringExtra(EXTRA_ALARM_TYPE)
            ?.let { runCatching { AlarmType.valueOf(it) }.getOrNull() }
            ?: return
        val settings = dataStore.getSettings()
        when (type) {
            AlarmType.PRAYER -> {
                val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY) ?: return
                if (intent.getBooleanExtra(EXTRA_IS_JUMUAH, false) && settings.jumuahEnabled) {
                    notificationManager.showJumuahNotification()
                } else {
                    notificationManager.showPrayerNotification(prayerKey, settings)
                }
                if (settings.adhanEnabled) {
                    adhanPlayer.play(prayerKey)
                }
                if (settings.countdownEnabled) {
                    val nextTime = intent.getLongExtra(EXTRA_NEXT_PRAYER_TIME, 0L)
                    val nextName = intent.getStringExtra(EXTRA_NEXT_PRAYER_NAME)
                    if (nextTime > 0L && nextName != null) {
                        scheduler.updateCountdown(nextTime, nextName)
                    }
                }
            }
            AlarmType.PRE_PRAYER -> {
                val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY)?.removeSuffix("_pre") ?: return
                val minutes = intent.getIntExtra(EXTRA_PRE_PRAYER_MINUTES, 15)
                notificationManager.showPrePrayerNotification(prayerKey, minutes)
            }
            AlarmType.DAILY_REMINDER -> {
                val summary = intent.getStringExtra(EXTRA_DAILY_SUMMARY) ?: return
                notificationManager.showDailyReminderNotification(summary)
                scheduler.scheduleDailyReminder()
            }
            AlarmType.SPECIAL_DAY -> {
                val day = intent.getStringExtra(EXTRA_SPECIAL_DAY)
                    ?.let { runCatching { SpecialDay.valueOf(it) }.getOrNull() }
                    ?: return
                notificationManager.showSpecialDayNotification(day)
            }
            AlarmType.PRE_SPECIAL_DAY -> {
                val day = intent.getStringExtra(EXTRA_SPECIAL_DAY)
                    ?.let { runCatching { SpecialDay.valueOf(it) }.getOrNull() }
                    ?: return
                notificationManager.showPreSpecialDayNotification(day)
            }
            AlarmType.COUNTDOWN_TICK -> Unit
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiver.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiverTest.kt
git commit -m "feat(notifications): dispatch alarm types in AlarmReceiver"
```

---

## Task 9: Localize the new strings (14 locales)

**Files:**
- Modify: `prayer_notifications/src/main/res/values-{ar,bn,de,es,fa,fr,hi,id,ms,ru,ta,th,tr,ur}/strings.xml`

- [ ] **Step 1: Add translations to each locale file**

Append the following block to each locale's `strings.xml` (translations below are per-locale):

**values-ar:**
```xml
    <string name="notification_jumuah_title">جمعة</string>
    <string name="notification_jumuah_body">وقت صلاة الجمعة</string>
    <string name="notification_pre_prayer">بعد %1$d دقيقة</string>
    <string name="notification_daily_reminder_title">أوقات صلاة اليوم</string>
    <string name="notification_special_day_title">يوم خاص</string>
    <string name="notification_special_day_body">اليوم هو %1$s</string>
    <string name="notification_pre_special_day_title">غدًا</string>
    <string name="notification_pre_special_day_body">غدًا هو %1$s</string>
    <string name="special_day_ramadan_start">بداية رمضان</string>
    <string name="special_day_eid_al_fitr">عيد الفطر</string>
    <string name="special_day_eid_al_adha">عيد الأضحى</string>
    <string name="special_day_laylat_al_qadr">ليلة القدر</string>
```

**values-bn:**
```xml
    <string name="notification_jumuah_title">জুমু\'আহ</string>
    <string name="notification_jumuah_body">জুমার নামাজের সময়</string>
    <string name="notification_pre_prayer">%1$d মিনিটের মধ্যে</string>
    <string name="notification_daily_reminder_title">আজকের নামাজের সময়</string>
    <string name="notification_special_day_title">বিশেষ দিন</string>
    <string name="notification_special_day_body">আজ %1$s</string>
    <string name="notification_pre_special_day_title">আগামীকাল</string>
    <string name="notification_pre_special_day_body">আগামীকাল %1$s</string>
    <string name="special_day_ramadan_start">রমজান শুরু</string>
    <string name="special_day_eid_al_fitr">ঈদুল ফিতর</string>
    <string name="special_day_eid_al_adha">ঈদুল আজহা</string>
    <string name="special_day_laylat_al_qadr">লাইলাতুল কদর</string>
```

**values-de:**
```xml
    <string name="notification_jumuah_title">Jumu\'ah</string>
    <string name="notification_jumuah_body">Freitagsgebetszeit</string>
    <string name="notification_pre_prayer">in %1$d Minuten</string>
    <string name="notification_daily_reminder_title">Heutige Gebetszeiten</string>
    <string name="notification_special_day_title">Besonderer Tag</string>
    <string name="notification_special_day_body">Heute ist %1$s</string>
    <string name="notification_pre_special_day_title">Morgen</string>
    <string name="notification_pre_special_day_body">Morgen ist %1$s</string>
    <string name="special_day_ramadan_start">Beginn des Ramadan</string>
    <string name="special_day_eid_al_fitr">Eid al-Fitr</string>
    <string name="special_day_eid_al_adha">Eid al-Adha</string>
    <string name="special_day_laylat_al_qadr">Laylat al-Qadr</string>
```

**values-es:**
```xml
    <string name="notification_jumuah_title">Yumu\'ah</string>
    <string name="notification_jumuah_body">Hora de la oración del viernes</string>
    <string name="notification_pre_prayer">en %1$d minutos</string>
    <string name="notification_daily_reminder_title">Horarios de oración de hoy</string>
    <string name="notification_special_day_title">Día especial</string>
    <string name="notification_special_day_body">Hoy es %1$s</string>
    <string name="notification_pre_special_day_title">Mañana</string>
    <string name="notification_pre_special_day_body">Mañana es %1$s</string>
    <string name="special_day_ramadan_start">Inicio del Ramadán</string>
    <string name="special_day_eid_al_fitr">Eid al-Fitr</string>
    <string name="special_day_eid_al_adha">Eid al-Adha</string>
    <string name="special_day_laylat_al_qadr">Laylat al-Qadr</string>
```

**values-fa:**
```xml
    <string name="notification_jumuah_title">جمعه</string>
    <string name="notification_jumuah_body">وقت نماز جمعه</string>
    <string name="notification_pre_prayer">تا %1$d دقیقه دیگر</string>
    <string name="notification_daily_reminder_title">اوقات نماز امروز</string>
    <string name="notification_special_day_title">روز ویژه</string>
    <string name="notification_special_day_body">امروز %1$s است</string>
    <string name="notification_pre_special_day_title">فردا</string>
    <string name="notification_pre_special_day_body">فردا %1$s است</string>
    <string name="special_day_ramadan_start">آغاز رمضان</string>
    <string name="special_day_eid_al_fitr">عید فطر</string>
    <string name="special_day_eid_al_adha">عید قربان</string>
    <string name="special_day_laylat_al_qadr">شب قدر</string>
```

**values-fr:**
```xml
    <string name="notification_jumuah_title">Jumu\'ah</string>
    <string name="notification_jumuah_body">Heure de la prière du vendredi</string>
    <string name="notification_pre_prayer">dans %1$d minutes</string>
    <string name="notification_daily_reminder_title">Heures de prière d\'aujourd\'hui</string>
    <string name="notification_special_day_title">Jour spécial</string>
    <string name="notification_special_day_body">Aujourd\'hui c\'est %1$s</string>
    <string name="notification_pre_special_day_title">Demain</string>
    <string name="notification_pre_special_day_body">Demain c\'est %1$s</string>
    <string name="special_day_ramadan_start">Début du Ramadan</string>
    <string name="special_day_eid_al_fitr">Aïd al-Fitr</string>
    <string name="special_day_eid_al_adha">Aïd al-Adha</string>
    <string name="special_day_laylat_al_qadr">Laylat al-Qadr</string>
```

**values-hi:**
```xml
    <string name="notification_jumuah_title">जुमुआ</string>
    <string name="notification_jumuah_body">जुम्मा की नमाज़ का समय</string>
    <string name="notification_pre_prayer">%1$d मिनट में</string>
    <string name="notification_daily_reminder_title">आज के नमाज़ के समय</string>
    <string name="notification_special_day_title">विशेष दिन</string>
    <string name="notification_special_day_body">आज %1$s है</string>
    <string name="notification_pre_special_day_title">कल</string>
    <string name="notification_pre_special_day_body">कल %1$s है</string>
    <string name="special_day_ramadan_start">रमज़ान की शुरुआत</string>
    <string name="special_day_eid_al_fitr">ईद-उल-फितर</string>
    <string name="special_day_eid_al_adha">ईद-उल-अज़हा</string>
    <string name="special_day_laylat_al_qadr">शब-ए-क़द्र</string>
```

**values-id:**
```xml
    <string name="notification_jumuah_title">Jumu\'ah</string>
    <string name="notification_jumuah_body">Waktu salat Jumat</string>
    <string name="notification_pre_prayer">dalam %1$d menit</string>
    <string name="notification_daily_reminder_title">Waktu salat hari ini</string>
    <string name="notification_special_day_title">Hari istimewa</string>
    <string name="notification_special_day_body">Hari ini %1$s</string>
    <string name="notification_pre_special_day_title">Besok</string>
    <string name="notification_pre_special_day_body">Besok %1$s</string>
    <string name="special_day_ramadan_start">Awal Ramadan</string>
    <string name="special_day_eid_al_fitr">Idulfitri</string>
    <string name="special_day_eid_al_adha">Iduladha</string>
    <string name="special_day_laylat_al_qadr">Lailatulqadar</string>
```

**values-ms:**
```xml
    <string name="notification_jumuah_title">Jumaat</string>
    <string name="notification_jumuah_body">Waktu solat Jumaat</string>
    <string name="notification_pre_prayer">dalam %1$d minit</string>
    <string name="notification_daily_reminder_title">Waktu solat hari ini</string>
    <string name="notification_special_day_title">Hari istimewa</string>
    <string name="notification_special_day_body">Hari ini %1$s</string>
    <string name="notification_pre_special_day_title">Esok</string>
    <string name="notification_pre_special_day_body">Esok %1$s</string>
    <string name="special_day_ramadan_start">Permulaan Ramadan</string>
    <string name="special_day_eid_al_fitr">Aidilfitri</string>
    <string name="special_day_eid_al_adha">Aidiladha</string>
    <string name="special_day_laylat_al_qadr">Lailatulqadar</string>
```

**values-ru:**
```xml
    <string name="notification_jumuah_title">Джума</string>
    <string name="notification_jumuah_body">Время пятничной молитвы</string>
    <string name="notification_pre_prayer">через %1$d мин</string>
    <string name="notification_daily_reminder_title">Время намаза на сегодня</string>
    <string name="notification_special_day_title">Особый день</string>
    <string name="notification_special_day_body">Сегодня %1$s</string>
    <string name="notification_pre_special_day_title">Завтра</string>
    <string name="notification_pre_special_day_body">Завтра %1$s</string>
    <string name="special_day_ramadan_start">Начало Рамадана</string>
    <string name="special_day_eid_al_fitr">Ид аль-Фитр</string>
    <string name="special_day_eid_al_adha">Ид аль-Адха</string>
    <string name="special_day_laylat_al_qadr">Ночь Предопределения</string>
```

**values-ta:**
```xml
    <string name="notification_jumuah_title">ஜுமுஆ</string>
    <string name="notification_jumuah_body">வெள்ளிக்கிழமை தொழுகை நேரம்</string>
    <string name="notification_pre_prayer">%1$d நிமிடத்தில்</string>
    <string name="notification_daily_reminder_title">இன்றைய தொழுகை நேரங்கள்</string>
    <string name="notification_special_day_title">சிறப்பு நாள்</string>
    <string name="notification_special_day_body">இன்று %1$s</string>
    <string name="notification_pre_special_day_title">நாளை</string>
    <string name="notification_pre_special_day_body">நாளை %1$s</string>
    <string name="special_day_ramadan_start">ரமலான் தொடக்கம்</string>
    <string name="special_day_eid_al_fitr">ஈத் அல்-பித்ர்</string>
    <string name="special_day_eid_al_adha">ஈத் அல்-அத்ஹா</string>
    <string name="special_day_laylat_al_qadr">லைலத்துல் கத்ர்</string>
```

**values-th:**
```xml
    <string name="notification_jumuah_title">ญุมุอะฮ์</string>
    <string name="notification_jumuah_body">เวลาละหมาดวันศุกร์</string>
    <string name="notification_pre_prayer">ใน %1$d นาที</string>
    <string name="notification_daily_reminder_title">เวลาละหมาดวันนี้</string>
    <string name="notification_special_day_title">วันพิเศษ</string>
    <string name="notification_special_day_body">วันนี้คือ %1$s</string>
    <string name="notification_pre_special_day_title">พรุ่งนี้</string>
    <string name="notification_pre_special_day_body">พรุ่งนี้คือ %1$s</string>
    <string name="special_day_ramadan_start">เริ่มต้นเดือนรอมฎอน</string>
    <string name="special_day_eid_al_fitr">อีดิลฟิฏรี</string>
    <string name="special_day_eid_al_adha">อีดิลอัฎฮา</string>
    <string name="special_day_laylat_al_qadr">ลัยละตุลก็อดร์</string>
```

**values-tr:**
```xml
    <string name="notification_jumuah_title">Cuma</string>
    <string name="notification_jumuah_body">Cuma namazı vakti</string>
    <string name="notification_pre_prayer">%1$d dakika sonra</string>
    <string name="notification_daily_reminder_title">Bugünün namaz vakitleri</string>
    <string name="notification_special_day_title">Özel gün</string>
    <string name="notification_special_day_body">Bugün %1$s</string>
    <string name="notification_pre_special_day_title">Yarın</string>
    <string name="notification_pre_special_day_body">Yarın %1$s</string>
    <string name="special_day_ramadan_start">Ramazan başlangıcı</string>
    <string name="special_day_eid_al_fitr">Ramazan Bayramı</string>
    <string name="special_day_eid_al_adha">Kurban Bayramı</string>
    <string name="special_day_laylat_al_qadr">Kadir Gecesi</string>
```

**values-ur:**
```xml
    <string name="notification_jumuah_title">جمعہ</string>
    <string name="notification_jumuah_body">جمعہ کی نماز کا وقت</string>
    <string name="notification_pre_prayer">%1$d منٹ میں</string>
    <string name="notification_daily_reminder_title">آج کے نماز کے اوقات</string>
    <string name="notification_special_day_title">خاص دن</string>
    <string name="notification_special_day_body">آج %1$s ہے</string>
    <string name="notification_pre_special_day_title">کل</string>
    <string name="notification_pre_special_day_body">کل %1$s ہے</string>
    <string name="special_day_ramadan_start">رمضان کا آغاز</string>
    <string name="special_day_eid_al_fitr">عید الفطر</string>
    <string name="special_day_eid_al_adha">عید الاضحی</string>
    <string name="special_day_laylat_al_qadr">شب قدر</string>
```

- [ ] **Step 2: Verify all XML files are valid and the build passes**

Run: `./gradlew :prayer_notifications:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add prayer_notifications/src/main/res/
git commit -m "feat(notifications): localize new notification strings across 14 locales"
```

---

## Task 10: Docs + full verification

**Files:**
- Modify: `TODO.md`

- [ ] **Step 1: Update `TODO.md`**

In the "Prayer-time notifications system" entry, append a line noting the delivery wiring is complete:

```markdown
  - Delivery wiring complete (2026-08-24): AlarmReceiver dispatches real prayer notifications + per-prayer adhan, daily reminder (with prayer-times summary), Jumu'ah, special days + one-day-ahead pre-special-day alerts, and a self-re-arming per-minute countdown.
```

- [ ] **Step 2: Run the full test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL (all tests pass).

- [ ] **Step 3: Run the full build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add TODO.md
git commit -m "docs: mark notifications delivery wiring complete"
```

---

## Self-Review Notes

- **Spec coverage:** typed alarms (T1), SpecialDaysCalculator (T2), SchedulePlan extension incl. daily-reminder summary + special/pre-special days + Jumu'ah flag + next-prayer extras (T3), strings (T4/T9), manager builders + signature change (T5), per-prayer adhan + assets (T6), scheduler scheduling + countdown chain + cancelAll (T7), receiver dispatch (T8), docs (T10). All spec sections map to a task.
- **Known limitation:** the daily-reminder summary body uses English prayer keys (e.g. "Fajr 05:12") — localizing the summary itself is a follow-up; all static notification strings are localized.
