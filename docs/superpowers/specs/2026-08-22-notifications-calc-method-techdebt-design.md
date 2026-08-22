# Notifications, Calculation Method Fix & Technical Debt

Date: 2026-08-22

## Overview

Three workstreams for the NamazVakitleri app:

1. **Prayer-time notifications** — a full notification system (alerts, adhan audio, countdown, reminders, special days).
2. **Calculation method fix** — correct the broken method-name mapping, localize names, unify duplicate definitions.
3. **Technical debt** — dedupe `getCountryCode`, consolidate legacy DataStores, rewrite the aspirational doc, add Compose UI tests, add a CI pipeline.

---

# Workstream 1: Prayer Time Notifications

## Problem

The app has no notification system. `Prayer.notificationEnabled` (`prayer/model/.../Prayer.kt:13`) exists but is never used. There is no `POST_NOTIFICATIONS` permission, no notification channels, no scheduler, and no adhan audio. For a prayer-times app, notifications are the defining feature.

## Goal

- Notify the user at each of the 5 daily prayer times for the **active location only**.
- Play bundled adhan audio when a prayer time arrives (optional per user).
- Show an always-on persistent countdown to the next prayer (with a Stop action).
- Let users configure per-prayer toggles, a daily reminder, a pre-prayer reminder (5/10/15/30/60 min before), Jumu'ah, and special Islamic days.
- Sound & vibration settings, plus a "Send test notification" button.
- Reliable exact scheduling (AlarmManager exact + WorkManager for daily reschedule), rescheduling after reboot.

## Approach

Dedicated **`prayer_notifications`** module (mirrors the existing `prayer_location` / `prayer_qibla` pattern) containing all notification logic. The Settings feature adds a new **Notifications sub-screen** on top. Scheduling uses **AlarmManager exact alarms** for prayer moments + **WorkManager** for daily rescheduling and special-day checks, with a **BootReceiver** to reschedule after reboot.

## Design

### New module `prayer_notifications`

```
prayer_notifications/
├── di/PrayerNotificationsModule.kt          # Koin @Module
├── scheduler/
│   ├── PrayerNotificationScheduler.kt       # Orchestrates AlarmManager + WorkManager
│   ├── AlarmReceiver.kt                     # BroadcastReceiver — fires at prayer time
│   └── BootReceiver.kt                      # Reschedules after device reboot
├── manager/
│   ├── PrayerNotificationManager.kt         # Creates channels, posts notifications
│   └── AdhanPlayer.kt                       # MediaPlayer for bundled adhan audio
├── data/
│   └── NotificationSettingsDataStore.kt     # Persists all notification prefs (typed DataStore)
├── domain/
│   ├── NotificationSettings.kt              # Data model + defaults
│   ├── SchedulePlan.kt                      # Pure calculator: times -> alarm intents (unit-testable)
│   └── usecases/                            # ScheduleAllUseCase, CancelAllUseCase, etc.
└── res/raw/adhan.mp3                        # Bundled adhan audio
```

### Data flow

```
Settings UI (prayer_feature:settings)
   │  toggles change
   ▼
NotificationSettingsDataStore (typed DataStore)
   │  observed
   ▼
PrayerNotificationScheduler
   ├── AlarmManager.setExactAndAllowWhileIdle(...)  → AlarmReceiver (fires at prayer time)
   ├── WorkManager (daily reschedule + special-days check)
   └── BootReceiver (reschedule on reboot)
        ▼
PrayerNotificationManager → posts notification (+ AdhanPlayer if enabled)
```

### Notification channels

| Channel | Purpose | Importance |
|---|---|---|
| `prayer_alerts` | Prayer time notifications | High (sound/vibration per settings) |
| `adhan` | Adhan audio playback | High |
| `countdown` | Persistent next-prayer countdown | Low (ongoing) |
| `reminders` | Daily reminder, pre-prayer, Jumu'ah, special days | Default |

### Permissions

- `POST_NOTIFICATIONS` (Android 13+) — runtime request, gated in the Notifications settings screen.
- `SCHEDULE_EXACT_ALARM` (Android 12+) — declared in manifest; graceful fallback to inexact if revoked.
- `RECEIVE_BOOT_COMPLETED` — for `BootReceiver`.

### Behavior details

- **Active-location only**: the scheduler reads the active location from `LocationsCoordinator`; only that location's times are scheduled. Re-schedules when the active location changes.
- **Per-prayer toggles**: wired to the existing `Prayer.notificationEnabled` field.
- **Countdown**: ongoing notification updated every minute; "Stop" action dismisses it (a toggle re-enables it).
- **Daily reminder**: user-picked time (TimePicker).
- **Pre-prayer reminder**: configurable minutes-before — **5, 10, 15, 30, 60**.
- **Jumu'ah**: notification at Friday Dhuhr time.
- **Special days**: computed from the Hijri calendar (Ramadan start, Eid al-Fitr, Eid al-Adha, Laylat al-Qadr).
- **Sound & vibration**: per-channel sound selection (adhan / silent / system) + vibration toggle.
- **Test button**: fires a sample notification immediately.

### Settings UI

New **Notifications sub-screen** in Settings (`prayer_feature:settings`), reachable from the Settings list. Contains all toggles: master enable, per-prayer, adhan, countdown, daily reminder (time picker), pre-prayer minutes, Jumu'ah, special days, sound, vibration, and the test button. Requests `POST_NOTIFICATIONS` when the master toggle is enabled.

## Error Handling

- Exact-alarm permission revoked → fall back to inexact alarms and surface a hint in settings.
- Notification permission denied → toggles stay OFF with a rationale; "Open Settings" action when permanently denied.
- Missing/empty location → scheduler no-ops; re-schedules when a location becomes available.
- Adhan playback failure → fall back to the channel's default sound.

## Testing

- **Unit tests** (pure JVM): `SchedulePlanTest` (times → alarm intents), `NotificationSettingsDataStoreTest`, use-case tests.
- **Robolectric tests**: `PrayerNotificationSchedulerTest` (AlarmManager interactions), `AdhanPlayerTest`, `AlarmReceiverTest`, `BootReceiverTest`.
- **Compose UI tests**: Notifications sub-screen toggles render and persist.
- Koin graph verified (module registered in `app`).

---

# Workstream 2: Calculation Method Fix

## Problem

`SettingsScreen.kt:319` `getCalculationMethodName` maps wrong IDs (`MUSLIM_WORLD_LEAGUE`, `EGYPT_SURVEY`, `TEHRAN`) that don't exist in the project's enum. The real enum (`prayer/model/.../CalculationMethod.kt`) has exactly 6 methods: `TURKEY_DIYANET`, `MWL`, `ISNA`, `EGYPT`, `MAKKAH`, `KARACHI` — each mapped to the adhan2 library in `PrayerTimeEngine.kt:81-87`. As a result, `MWL`, `ISNA`, and `EGYPT` show raw IDs in Settings instead of friendly names. Additionally, `getLanguageName` (`SettingsScreen.kt:332`) maps only 6 of the 13 supported languages.

## Goal

- Fix the name mapping so all **6 real methods** show friendly names. The bogus entries (`MUSLIM_WORLD_LEAGUE`, `EGYPT_SURVEY`, `TEHRAN`) are **deleted** — no new methods are added to project scope.
- Localize method names and language names across all 13 locales.
- Unify the duplicate `CalculationMethod` definitions into one source of truth.

## Design

### 1. Unify duplicate definitions

- Keep `prayer/model/.../CalculationMethod.kt` (enum) as the single source of truth.
- Delete `prayer_settings/.../CalculationMethod.kt` (data class with `methods` list); route its consumers through the enum.
- Add localized display-name resolution in `prayer_feature:common` (where `PrayerFormatter` lives) mapping enum → string resource.

### 2. Fix the name mapping bug

- Replace `getCalculationMethodName` (`SettingsScreen.kt:319`) with enum-driven lookup covering **only the 6 real methods**.
- Delete the bogus `MUSLIM_WORLD_LEAGUE` / `EGYPT_SURVEY` / `TEHRAN` branches.

### 3. Localize method names (13 languages)

- Add `calculation_method_*` string resources for the 6 methods to all 13 locale folders in `prayer_feature:settings` (and `core:designsystem` where shared).
- Settings subtitle + `CalculationMethodScreen` list both use localized resources.

### 4. Fix language name list

- Add native names for the missing languages (bn, fa, hi, id, ms, ru, ta, th, ur) and localize display names.

## Testing

- Enum→resource mapping test: all 6 methods resolve; unknown IDs fall back safely.
- Update `SettingsScreenTest` / `CalculationMethodScreenTest` for the corrected names.
- Assert all 13 locale folders contain the new string keys.

---

# Workstream 3: Technical Debt

## 3.1 Dedupe `getCountryCode`

- Extract the duplicated mapping (`HomeViewModel.kt:135`, `SettingsRepositoryImpl.kt:39`) into a shared util in `core:common` (e.g., alongside `ZoneIdUtils`).
- Update both call sites. Unit test the util.

## 3.2 Consolidate legacy DataStores

- `prayer_cache/SettingsDataStoreImp.kt` + `LocationDataStoreImp.kt` are `@Single` implementations of `prayer:data` interfaces (`SettingsDataStore`, `LocationDataStore`).
- Trace consumers of those `prayer:data` interfaces:
  - If unused → remove legacy impls + `prayer_cache` module (and its `app` / `prayer_settings` deps).
  - If used → migrate callers to the typed `prayer_settings` DataStore, then remove legacy.
- Update Koin modules accordingly.

## 3.3 Rewrite aspirational doc

- `NamazVakitleriTechnicalAnalysis` describes Room/WorkManager/widgets/notifications that don't exist.
- **Rewrite** it to reflect the actual architecture (DataStore cache, no Room, no widgets yet, notifications now being added).
- Audit and rewrite any other stale docs (README module list, TODO.md statuses) as needed.

## 3.4 Compose UI tests (4 main screens)

- Robolectric-based Compose UI tests for Home, Prayer Times, Qibla, Settings — happy-path rendering + key interactions (tab navigation, location switch, month navigation, settings navigation).
- Follows the existing `MyLocationsScreenTest` pattern.
- Comprehensive coverage of sub-screens / error states deferred to a follow-up.

## 3.5 CI pipeline (GitHub Actions)

- `.github/workflows/ci.yml`:
  - Trigger: push + PR to `main`.
  - Jobs: `./gradlew allTests` → `./gradlew assembleDebug` → `./gradlew lint` → upload debug APK artifact.
  - JDK 21, Gradle caching.

## Testing

- Each item carries its own tests as described above.
- Full suite (`./gradlew allTests`) must stay green after each item.
