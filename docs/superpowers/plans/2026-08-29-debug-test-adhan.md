# Debug "Test Adhan" Trigger Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a debug-only (release-excluded) "Test Adhan" control in Settings → Notifications that schedules a real prayer alarm at a chosen delay (0–15 min) so the Ezan fires through the exact production `AlarmReceiver → AdhanService → Media3 AdhanPlayer` path — surviving process death and device idle/Doze — for on-device verification of the Media3 migration.

**Architecture:** Extend the `AlarmScheduler` interface with `scheduleTestAdhan(delayMinutes)`, implemented in `PrayerNotificationScheduler` as a one-off `AlarmType.PRAYER` alarm for `prayerKey = "Fajr"` at `now + delay` using `setExactAndAllowWhileIdle`, with a dedicated request code (`900`) outside the `1000..1020` daily range so `cancelAll()` never wipes it. The settings-UI section is gated by `BuildConfig.DEBUG` (needs `buildConfig = true` in `prayer_feature/settings`) and reuses the existing exact-alarm permission dialog flow.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Koin, Robolectric + Truth + MockK, kotlinx.coroutines-test. See the approved spec: `docs/superpowers/specs/2026-08-29-debug-test-adhan-design.md`.

**Key invariants (from spec):**
- Faithful to the real prayer path: the alarm is handled by unchanged `AlarmReceiver.handleAlarm`, which reads the data store — if the Adhan toggle is OFF it shows a static prayer notification (no audio) instead. The debug UI warns when the toggle is off.
- Delay clamped to 0..15; a new scheduling cancels the previous test alarm first; 0 = fires immediately.
- Debug-only: `prayer_feature/settings` currently only has `buildFeatures { compose = true }` — must add `buildConfig = true` (package/namespace is `com.kutluoglu.prayer_feature.settings`).

---

## File Structure

| File | Responsibility |
|------|----------------|
| `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmScheduler.kt` | Interface: add `scheduleTestAdhan(delayMinutes: Int)` |
| `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt` | Impl: `REQUEST_CODE_TEST_ADHAN = 900`, cancel-prior-then-schedule, reuse private `scheduleAlarm` |
| `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationSchedulerTest.kt` | Robolectric scheduler tests (exact alarm, delay, clamp, replace, cancelAll isolation) |
| `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsContract.kt` | Add `ScheduleTestAdhan(delayMinutes: Int)` event |
| `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsViewModel.kt` | Inject `AlarmScheduler`; handle the new event |
| `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsViewModelTest.kt` | VM test for the new event + ctor arg |
| `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreen.kt` | Debug-only "Test Adhan" section (chips + button + off-warning) |
| `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreenTest.kt` | Screen tests: render, warning, scheduling, permission gate |
| `prayer_feature/settings/build.gradle.kts` | `buildConfig = true` |
| `prayer_feature/settings/src/main/res/values/strings.xml` | 4 new default-locale debug strings |

No DI-module change: `PrayerNotificationScheduler` is `@Single(binds = [AlarmScheduler::class])` in `prayer_notifications` and already part of the app graph (same module that provides the `NotificationDisplayer` the ViewModel already injects).

---

## Task 1: Scheduler — `scheduleTestAdhan`

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmScheduler.kt`
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationSchedulerTest.kt`

- [ ] **Step 1: Write the failing tests**

Insert the following five tests at the end of `PrayerNotificationSchedulerTest` (after the last existing `@Test`, before the class's final closing `}`):

```kotlin
    @Test
    fun `scheduleTestAdhan schedules a single exact alarm at now plus delay`() = runTest {
        val before = System.currentTimeMillis()
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleTestAdhan(5)
        val after = System.currentTimeMillis()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarm = shadowOf(alarmManager).scheduledAlarms.single()
        assertThat(alarm.operation.requestCode)
            .isEqualTo(PrayerNotificationScheduler.REQUEST_CODE_TEST_ADHAN)
        assertThat(alarm.type).isEqualTo(AlarmManager.RTC_WAKEUP)
        assertThat(alarm.triggerAtTime).isAtLeast(before + 300_000L)
        assertThat(alarm.triggerAtTime).isAtMost(after + 300_000L)
        val testIntent = shadowOf(alarm.operation).savedIntent
        assertThat(testIntent.getStringExtra(AlarmReceiver.EXTRA_PRAYER_KEY)).isEqualTo("Fajr")
        assertThat(testIntent.getStringExtra(AlarmReceiver.EXTRA_ALARM_TYPE))
            .isEqualTo(AlarmType.PRAYER.name)
    }

    @Test
    fun `scheduleTestAdhan with zero delay fires at once`() = runTest {
        val before = System.currentTimeMillis()
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleTestAdhan(0)
        val after = System.currentTimeMillis()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarm = shadowOf(alarmManager).scheduledAlarms.single()
        assertThat(alarm.triggerAtTime).isAtLeast(before)
        assertThat(alarm.triggerAtTime).isAtMost(after)
    }

    @Test
    fun `scheduleTestAdhan clamps delay to fifteen minutes`() = runTest {
        val before = System.currentTimeMillis()
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleTestAdhan(20)
        val after = System.currentTimeMillis()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarm = shadowOf(alarmManager).scheduledAlarms.single()
        assertThat(alarm.triggerAtTime).isAtLeast(before + 900_000L)
        assertThat(alarm.triggerAtTime).isAtMost(after + 900_000L)
    }

    @Test
    fun `scheduleTestAdhan replaces a previously scheduled test alarm`() = runTest {
        val before = System.currentTimeMillis()
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleTestAdhan(15)
        scheduler.scheduleTestAdhan(0)
        val after = System.currentTimeMillis()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarm = shadowOf(alarmManager).scheduledAlarms.single()
        assertThat(alarm.triggerAtTime).isAtLeast(before)
        assertThat(alarm.triggerAtTime).isAtMost(after)
    }

    @Test
    fun `cancelAll leaves the test adhan alarm scheduled`() = runTest {
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleTestAdhan(5)
        scheduler.cancelAll()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarm = shadowOf(alarmManager).scheduledAlarms.single()
        assertThat(alarm.operation.requestCode)
            .isEqualTo(PrayerNotificationScheduler.REQUEST_CODE_TEST_ADHAN)
    }
```

Note: `shadowOf(alarm.operation)` resolves to `ShadowPendingIntent` (imported via `Shadows.shadowOf`, already in the file); `savedIntent` returns the underlying `Intent` with its extras. Reference `AlarmType` and `AlarmReceiver` — both already in scope.

- [ ] **Step 2: Run the tests to verify they fail (compile error)**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*PrayerNotificationSchedulerTest"`
Expected: FAIL — compilation error `Cannot find class REQUEST_CODE_TEST_ADHAN` / `Unresolved reference: scheduleTestAdhan`.

- [ ] **Step 3: Implement the interface method and scheduler**

In `AlarmScheduler.kt`, after the `suspend fun scheduleDailyReminder()` declaration (before the closing `}`):

```kotlin

    /**
     * Schedules a single debug "Test Adhan" prayer alarm to fire [delayMinutes] from
     * now (clamped to 0..15). Scheduling again cancels any previously scheduled test
     * alarm. Uses the exact production PRAYER alarm path, so playback runs through
     * [com.kutluoglu.prayer_notifications.scheduler.AlarmReceiver] and respects the
     * current adhan-enabled setting.
     */
    fun scheduleTestAdhan(delayMinutes: Int)
```

In `PrayerNotificationScheduler.kt`:

a) Add the request-code constant in the `companion object` (keep the existing comment; add the new line after `REQUEST_CODE_END`):

```kotlin
        const val REQUEST_CODE_END = 1020
        // Debug "Test Adhan" alarm; outside the 1000..1020 daily range so cancelAll() ignores it.
        const val REQUEST_CODE_TEST_ADHAN = 900
        const val DAILY_RESCHEDULE_WORK_NAME = "daily_prayer_reschedule"
```

b) Add the override immediately before `private fun scheduleAlarm(alarm: ScheduledAlarm)`:

```kotlin
    override fun scheduleTestAdhan(delayMinutes: Int) {
        val delayMillis = delayMinutes.coerceIn(0, 15) * 60_000L
        val testIntent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_TEST_ADHAN, testIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
        scheduleAlarm(
            ScheduledAlarm(
                prayerKey = "Fajr",
                triggerAtMillis = System.currentTimeMillis() + delayMillis,
                requestCode = REQUEST_CODE_TEST_ADHAN
            )
        )
    }
```

All referenced symbols (`Intent`, `PendingIntent`, `alarmManager`, `ScheduledAlarm`, `AlarmReceiver`, `scheduleAlarm`) are already imported/declared in this file.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*PrayerNotificationSchedulerTest"`
Expected: PASS — 5 new tests green, existing tests still green.

- [ ] **Step 5: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmScheduler.kt \
        prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationScheduler.kt \
        prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/PrayerNotificationSchedulerTest.kt
git commit -m "feat(prayer_notifications): add debug scheduleTestAdhan alarm to AlarmScheduler"
```

---

## Task 2: Contract + ViewModel wiring

**Files:**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsContract.kt`
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsViewModel.kt`
- Modify: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsViewModelTest.kt`
- Modify: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreenTest.kt` (only the constructor call in `launchScreen`)

- [ ] **Step 1: Add the contract event**

In `NotificationsContract.kt`, append inside `sealed class NotificationsEvent` (after `data object SendTest : NotificationsEvent()`):

```kotlin
    data class ScheduleTestAdhan(val delayMinutes: Int) : NotificationsEvent()
```

- [ ] **Step 2: Write the failing ViewModel test**

In `NotificationsViewModelTest.kt`:
a) Add the mock field after `private val notificationManager = mockk<NotificationDisplayer>(relaxed = true)`:

```kotlin
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
```

b) Add the import (in the imports, after the `NotificationDisplayer` import):

```kotlin
import com.kutluoglu.prayer_notifications.scheduler.AlarmScheduler
```

c) Replace the constructor call in every existing test (`NotificationsViewModel(getUseCase, updateUseCase, notificationManager)`) with `NotificationsViewModel(getUseCase, updateUseCase, notificationManager, alarmScheduler)` — apply to all four occurrences (`loads settings on init`, `load failure surfaces error state`, `toggling master enabled persists`, `toggling a prayer persists the updated settings`, and any later tests that construct the ViewModel; the file has ~10 construction sites — update ALL of them).

d) Add the new test at the end of the class (before the closing `}`):

```kotlin
    @Test
    fun `scheduling a test adhan invokes the alarm scheduler`() = runTest {
        coEvery { getUseCase() } returns NotificationSettings()

        val viewModel = NotificationsViewModel(getUseCase, updateUseCase, notificationManager, alarmScheduler)
        viewModel.onEvent(NotificationsEvent.ScheduleTestAdhan(5))

        verify { alarmScheduler.scheduleTestAdhan(5) }
    }
```

`verify` is already imported (`io.mockk.verify`).

- [ ] **Step 3: Run the ViewModel tests to verify they fail (compile error)**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*NotificationsViewModelTest"`
Expected: FAIL — compilation error: too few arguments for `NotificationsViewModel` constructor.

- [ ] **Step 4: Implement the ViewModel wiring**

In `NotificationsViewModel.kt`:
a) Add the import:

```kotlin
import com.kutluoglu.prayer_notifications.scheduler.AlarmScheduler
```

b) Add the constructor parameter (after `notificationDisplayer`):

```kotlin
@KoinViewModel
class NotificationsViewModel(
    private val getSettingsUseCase: GetNotificationSettingsUseCase,
    private val updateSettingsUseCase: UpdateNotificationSettingsUseCase,
    private val notificationDisplayer: NotificationDisplayer,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {
```

c) Add the `when` branch (after the `NotificationsEvent.SendTest -> sendTestNotification()` line):

```kotlin
            is NotificationsEvent.ScheduleTestAdhan ->
                alarmScheduler.scheduleTestAdhan(event.delayMinutes)
```

- [ ] **Step 5: Fix the screen test constructor and run both suites**

In `NotificationsScreenTest.kt`:
- Add the import `import com.kutluoglu.prayer_notifications.scheduler.AlarmScheduler`.
- Add field `private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)`.
- In `launchScreen`, change `val viewModel = NotificationsViewModel(getUseCase, updateUseCase, notificationManager)` to `val viewModel = NotificationsViewModel(getUseCase, updateUseCase, notificationManager, alarmScheduler)`.

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*NotificationsViewModelTest" --tests="*NotificationsScreenTest"`
Expected: PASS — new VM test green; existing VM and screen tests green.

- [ ] **Step 6: Commit**

```bash
git add prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsContract.kt \
        prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsViewModel.kt \
        prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsViewModelTest.kt \
        prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreenTest.kt
git commit -m "feat(settings): wire ScheduleTestAdhan event through NotificationsViewModel"
```

---

## Task 3: Enable BuildConfig + add debug strings

**Files:**
- Modify: `prayer_feature/settings/build.gradle.kts`
- Modify: `prayer_feature/settings/src/main/res/values/strings.xml`

- [ ] **Step 1: Enable buildConfig**

In `prayer_feature/settings/build.gradle.kts`, the `buildFeatures` block currently is:

```kotlin
    buildFeatures {
        compose = true
    }
```

Replace it with:

```kotlin
    buildFeatures {
        compose = true
        buildConfig = true
    }
```

- [ ] **Step 2: Add the strings**

In `prayer_feature/settings/src/main/res/values/strings.xml`, after the `send_test_notification` string (line 88), add:

```xml
    <string name="test_adhan">Test Adhan</string>
    <string name="test_adhan_instant">Instant</string>
    <string name="schedule_adhan_test">Schedule Adhan test</string>
    <string name="test_adhan_adhan_off_warning">Adhan toggle is OFF — the test will show a notification but will NOT play sound.</string>
```

(Other locale files fall back to the default; no `values-*` edits.)

- [ ] **Step 3: Verify the module compiles**

Run: `./gradlew :prayer_feature:settings:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/settings/build.gradle.kts prayer_feature/settings/src/main/res/values/strings.xml
git commit -m "chore(settings): enable buildConfig and add debug test-adhan strings"
```

---

## Task 4: Debug-only "Test Adhan" section (screen)

**Files:**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreen.kt`
- Modify: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreenTest.kt`

Robolectric screen tests run against the debug variant, so `BuildConfig.DEBUG == true` and the section is visible/testable. `FilterChip`, `FlowRow`, `HorizontalDivider`, and `Arrangement` are already imported and used elsewhere in the file.

- [ ] **Step 1: Write the failing screen tests**

In `NotificationsScreenTest.kt`, add imports:

```kotlin
import io.mockk.any
import io.mockk.verify
```

Add the following tests at the end of the class (before the closing `}`):

```kotlin
    @Test
    fun `renders test adhan section in debug build`() {
        launchScreen(
            NotificationSettings(
                enabled = true,
                adhanEnabled = true
            )
        )

        composeTestRule.onNodeWithText("Test Adhan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Schedule Adhan test").assertIsDisplayed()
    }

    @Test
    fun `shows adhan-off warning when adhan is disabled`() {
        launchScreen(
            NotificationSettings(
                enabled = true,
                adhanEnabled = false
            )
        )

        composeTestRule
            .onNodeWithText(
                "Adhan toggle is OFF — the test will show a notification but will NOT play sound."
            )
            .assertIsDisplayed()
    }

    @Test
    fun `hides adhan-off warning when adhan is enabled`() {
        launchScreen(
            NotificationSettings(
                enabled = true,
                adhanEnabled = true
            )
        )

        composeTestRule
            .onNodeWithText(
                "Adhan toggle is OFF — the test will show a notification but will NOT play sound."
            )
            .assertDoesNotExist()
    }

    @Test
    fun `scheduling a test adhan schedules an alarm with the selected delay`() {
        launchScreen(
            NotificationSettings(
                enabled = true,
                adhanEnabled = true
            )
        )

        composeTestRule.onNodeWithText("10m").performClick()
        composeTestRule.onNodeWithText("Schedule Adhan test").performClick()

        verify { alarmScheduler.scheduleTestAdhan(10) }
    }

    @Test
    fun `does not schedule a test adhan without exact alarm permission`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        launchScreen(
            NotificationSettings(
                enabled = true,
                adhanEnabled = true
            )
        )

        composeTestRule.onNodeWithText("Schedule Adhan test").performClick()

        verify(exactly = 0) { alarmScheduler.scheduleTestAdhan(any()) }
    }
```

(Note: chip labels `1m/5m/10m/15m` are unique on screen — the pre-prayer chips show bare numbers `5/15/60`.)

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*NotificationsScreenTest"`
Expected: FAIL — `renders test adhan section in debug build` fails with "could not find node" (the section does not exist yet). The other new tests fail similarly or trivially.

- [ ] **Step 3: Implement the debug section**

In `NotificationsScreen.kt`, add the import (in the `com.kutluoglu` import group):

```kotlin
import com.kutluoglu.prayer_feature.settings.BuildConfig
```

In `NotificationsContent`, immediately AFTER the "Send test notification" `Button` block (the one whose body is `{ Text(stringResource(R.string.send_test_notification)) }`) and still inside the `Column { ... }`, insert:

```kotlin
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
```

`checkExactAlarmPermission()`, `pendingExactAlarmAction`, and `showExactAlarmDialog` are local declarations already present in `NotificationsContent`; the existing `ON_RESUME` observer invokes `pendingExactAlarmAction` once the permission is granted.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*NotificationsScreenTest"`
Expected: PASS — all 5 new tests green, existing screen tests green.

- [ ] **Step 5: Confirm the release build excludes the section**

Run: `./gradlew :prayer_feature:settings:compileReleaseKotlin`
Expected: BUILD SUCCESSFUL (dead-branch code with `BuildConfig.DEBUG` constant `false` is compiled out; `BuildConfig` still resolves).

- [ ] **Step 6: Commit**

```bash
git add prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreen.kt \
        prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreenTest.kt
git commit -m "feat(settings): add debug-only Test Adhan section with delay presets"
```

---

## Task 5: Full regression, lint, and scope check

**Files:** none (verification only — no commit)

- [x] **Step 1: Full build + test suite**

Run: `./gradlew assembleDebug testDebugUnitTest`
Result: BUILD SUCCESSFUL (2m 6s, 570 tasks) — all modules compile; all unit tests pass.

- [x] **Step 2: Settings module lint**

Run: `./gradlew :prayer_feature:settings:lintDebug`
Result: the 4 new debug strings were marked `translatable="false"` (commit `418e9de`) so this feature adds no lint errors. The build still fails on PRE-EXISTING `main` errors (out of scope): `location_permission_required` + `app_name` MissingTranslation, and `LocationSelectionViewModel.kt:374` NewApi `removeLast`.

- [x] **Step 3: GitNexus scope check**

Run `gitnexus_detect_changes()` (MCP) with `repo: "NamazVakitleri"`, scope `compare`, base `main`. Changed symbols are exactly the expected feature scope: `NotificationsContent`, `NotificationsViewModel` (+ctor), `AppModule.appModule` (Koin registration), `PrayerNotificationScheduler` (new method/constant; `scheduleAlarm`/`setExactAlarm` only newly called, behavior unchanged), plus the test files. Affected processes are all within the NotificationsRoute flow and the alarm-scheduling flow — no existing behavior altered.

- [ ] **Step 4: Manual on-device checklist (user)**

Debug build → Settings → Notifications → "Test Adhan":
1. Leave Adhan toggle ON, pick `Instant`, tap "Schedule Adhan test" → the Ezan plays immediately while app is open.
2. Kill the app, pick `5m`, tap "Schedule Adhan test" → Ezan fires ~5 min later with the app dead and the phone idle/Doze, through the production path (validates the Media3 migration Task 3 device checklist from `2026-08-29-ezan-media3-migration.md`).
3. Flip Adhan toggle OFF, pick `Instant` → a static prayer notification appears; no audio; the in-UI warning had already told you this is expected.
4. Release build (`./gradlew assembleRelease`) → the "Test Adhan" section is absent.

---

## Self-Review (completed)

- **Spec coverage:** Decision 1 (faithful path + off-toggle warning) → Task 4 warning + Task 5 step 4.3. Decision 2 (exact alarm, 0 fires now) → Task 1 tests. Decision 3 (code 900, cancelAll isolation, replace-prior) → Task 1 tests. Decision 4 (presets 0/1/5/10/15 chips) → Task 4. Decision 5 (`buildConfig = true`, debug gate, release excluded) → Tasks 3 & 4 step 5. Decision 6 (permission dialog reuse) → Task 4 button branch. Decision 7 (`AlarmScheduler` seam) → Task 2. Acceptance criteria → Tasks 4/5. All spec items traced.
- **Placeholder scan:** no TBD/TODO; every code step has full code.
- **Type consistency:** `scheduleTestAdhan(Int)` on interface/impl/VM/event `ScheduleTestAdhan(delayMinutes: Int)`; `REQUEST_CODE_TEST_ADHAN` constant name matches across Task 1 test/impl; `AlarmScheduler` param name `alarmScheduler` consistent in VM, both tests; chip delay `Int` matches event/VM/scheduler args.

## Execution Deviations (recorded during implementation)

1. **Task 1:** test assertions use `shadowOf(alarm.operation).requestCode` (not `alarm.operation.requestCode`) — `PendingIntent.getRequestCode()` is `@hide`; matches existing file style.
2. **Task 2:** `AppModule.kt:68` manual Koin DSL registration needed a 4th `get()` (`a406ff7`) — the plan's "no DI change" note missed the manual `viewModel { ... }` registration alongside the `@KoinViewModel` annotation.
3. **Task 4:** `import io.mockk.any` is not importable in MockK 1.14.5 (`any()` resolves inside the `verify` lambda); only `import io.mockk.verify` was added.
4. **Task 5:** the 4 new debug strings marked `translatable="false"` (`418e9de`) to avoid new MissingTranslation lint errors; settings-module lint was already red on `main` for unrelated pre-existing errors.