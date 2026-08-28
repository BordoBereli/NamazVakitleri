# Cuma Namazı Countdown Label (Home Screen + Notification)

Date: 2026-08-28

## Overview

On Fridays, the home screen's "`xxx namazına kalan süre`" section and the persistent countdown notification currently show the noon prayer ("Öğle" / "Dhuhr") as the next prayer. Since Friday prayer (Cuma namazı) is performed at the noon prayer time, this spec changes the **label** to "Cuma" whenever the countdown targets the noon prayer on a Friday. The countdown **value stays identical** — only the displayed prayer name changes.

## Problem

- On Fridays, when the next prayer is Öğle (Dhuhr), the home screen shows "Öğle namazına kalan süre" and the notification shows "Öğle · 12:30". Users expect "Cuma namazına kalan süre" / "Cuma · 12:30" on that day.
- The app already has a Jumu'ah concept (`ScheduledAlarm.isJumuah`, `notification_jumuah_title`, `showJumuahNotification`), but it is only used for the Jumu'ah *reminder* notification, not for the countdown label.

## Goal

- On Friday, while the countdown targets the noon prayer (between Sunrise and Dhuhr), show "Cuma" instead of the localized Dhuhr name — on both the home screen and the countdown notification.
- The countdown value and all other behavior stay unchanged.
- After Dhuhr passes (or on any other day), behavior reverts to normal.

## Detection rule

A countdown is a **Jumu'ah countdown** when both hold:

1. The target prayer is the noon prayer (Dhuhr).
   - Home screen: `nextPrayer.arabicName == "الظهر"` (stable identifier, preserved through localization).
   - Notification: raw key `nextPrayerName == "Dhuhr"`.
2. The target prayer's date is a Friday.
   - Home screen: `nextPrayer.date.dayOfWeek == DayOfWeek.FRIDAY`.
   - Notification: the date of `nextPrayerTimeMillis` is a Friday.

Rationale for the notification date check: the countdown to Dhuhr only ever happens on the same day as that Dhuhr (the transition chain is Fajr → Sunrise → Dhuhr → … → Isha → next-day Fajr, and `scheduleAllSuspending` targets the first upcoming prayer). Therefore "target is Dhuhr" + "target date is Friday" is equivalent to "Friday's Dhuhr".

## Design

### 1. Home screen (`prayer_feature:home`)

**`state/HomeUiStates.kt`** — add a testable helper:

```kotlin
private const val DHUHR_ARABIC_NAME = "الظهر"

fun PrayerUiState.isJumuahCountdown(): Boolean =
    nextPrayer?.let { it.arabicName == DHUHR_ARABIC_NAME && it.date.dayOfWeek == DayOfWeek.FRIDAY } ?: false
```

**`components/HomeTopContainer.kt`** — in `NextPrayerInfo`, when `prayerState.isJumuahCountdown()` is true, use the new localized `prayer_jumuah` string as the display name instead of the localized Dhuhr name:

```kotlin
val nextPrayerDisplayName = when {
    prayerState.isJumuahCountdown() -> stringResource(id = R.string.prayer_jumuah)
    nextPrayerNameRaw == "İmsak" -> "Sabah"
    else -> nextPrayerNameRaw
}
```

The existing `time_until_prayer_format` (`%1$s namazına kalan süre: `) is reused; only the `%1$s` argument changes.

**Strings** — add `prayer_jumuah` to all 15 locale files in `prayer_feature/home/src/main/res/` (values, ar, ta, fa, es, ur, th, tr, fr, de, ru, bn, hi, id, ms). Values mirror the existing `notification_jumuah_title` translations (e.g. tr: `Cuma`, en: `Jumu'ah`).

### 2. Notifications (`prayer_notifications`)

**`manager/PrayerNotificationManager.kt`** — in `showCountdownNotification`, derive the display name:

```kotlin
private fun countdownDisplayName(nextPrayerName: String, nextPrayerTimeMillis: Long): String =
    if (nextPrayerName == "Dhuhr" && isFriday(nextPrayerTimeMillis)) {
        localizedString(R.string.notification_jumuah_title)
    } else {
        localizedPrayerName(nextPrayerName)
    }

private fun isFriday(epochMillis: Long): Boolean =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .dayOfWeek == DayOfWeek.FRIDAY
```

Use `countdownDisplayName(...)` in the title builder. The existing `notification_jumuah_title` string ("Cuma" / "Jumu'ah", already present in all 15 locales) is reused — no new notification strings.

### 3. No changes to

- `CountdownEngine` / `CountdownUiState` (countdown value logic).
- `SchedulePlan` / `ScheduledAlarm` / `AlarmReceiver` / `PrayerNotificationScheduler` (no flag threading).
- The Jumu'ah reminder notification (`showJumuahNotification`) and its `jumuahEnabled` setting.

## Edge cases

- **Friday before Sunrise / after Dhuhr:** next prayer is not Dhuhr → normal label.
- **Non-Friday Dhuhr countdown:** date check fails → normal "Öğle"/"Dhuhr" label.
- **`nextPrayer` null:** helper returns false → fallback "İmsak" label unchanged.
- **Multi-timezone location:** the notification date check uses `ZoneId.systemDefault()`, consistent with the existing `formatClockTime`. In the rare case the selected location's timezone differs from the device by >12h, the label may stay "Öğle" — cosmetic only, no crash.

## Testing

### Unit tests (pure JVM)

- New `PrayerUiStateTest` (or extend an existing home state test) for `isJumuahCountdown`:
  - Dhuhr on Friday → `true`
  - Dhuhr on Monday → `false`
  - Asr on Friday → `false`
  - `nextPrayer == null` → `false`

### Robolectric tests

- `PrayerNotificationManagerTest` — `showCountdownNotification`:
  - Dhuhr target on a Friday → title shows localized "Cuma" (e.g. `Cuma · 12:30` under `Locale("tr")`).
  - Dhuhr target on a Monday → title shows localized Dhuhr name (e.g. `Öğle · 12:30`).
  - Non-Dhuhr target on Friday (e.g. Maghrib) → unchanged.

## Out of scope

- Changing the countdown value or cadence.
- Changing the Jumu'ah reminder notification behavior or its setting.
- Other screens (prayer times list, etc.) — only the home screen countdown label and the countdown notification label change.
