package com.kutluoglu.prayer_notifications.scheduler

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.AlarmType
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.SchedulePlan
import com.kutluoglu.prayer_notifications.domain.SpecialDay
import com.kutluoglu.prayer_notifications.domain.SpecialDaysCalculator
import com.kutluoglu.prayer_notifications.manager.NotificationDisplayer
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrayerNotificationSchedulerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dataStore = mockk<NotificationSettingsDataStore>(relaxed = true)
    private val schedulePlan = SchedulePlan()
    private val getPrayerTimesUseCase = mockk<GetPrayerTimesUseCase>(relaxed = true)
    private val locationsCoordinator = mockk<LocationsCoordinator>(relaxed = true)
    private val getSettingsUseCase = mockk<GetSettingsUseCase>(relaxed = true)
    private val notificationDisplayer = mockk<NotificationDisplayer>(relaxed = true)

    private fun scheduler(
        scope: CoroutineScope,
        specialDaysCalculator: SpecialDaysCalculator = SpecialDaysCalculator()
    ) = PrayerNotificationScheduler(
        context = context,
        dataStore = dataStore,
        schedulePlan = schedulePlan,
        getPrayerTimesUseCase = getPrayerTimesUseCase,
        locationsCoordinator = locationsCoordinator,
        getSettingsUseCase = getSettingsUseCase,
        notificationDisplayer = notificationDisplayer,
        specialDaysCalculator = specialDaysCalculator,
        scope = scope
    )

    @Before
    fun grantExactAlarmPermission() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    @Test
    fun `scheduleAll with disabled settings schedules nothing`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings(enabled = false)
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleAll()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        assertThat(shadowOf(alarmManager).scheduledAlarms).isEmpty()
    }

    @Test
    fun `cancelAll cancels scheduled alarms`() = runTest {
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, "Dhuhr")
        val pendingIntent = PendingIntent.getBroadcast(
            context, PrayerNotificationScheduler.REQUEST_CODE_START, intent,
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
    fun `scheduleAll schedules alarms for enabled prayers`() = runTest {
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
        coEvery { getPrayerTimesUseCase(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(
            listOf(
                Prayer(
                    name = "Dhuhr",
                    arabicName = "الظهر",
                    time = LocalTime(23, 59),
                    date = LocalDate(2026, 8, 22)
                )
            )
        )

        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleAll()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        assertThat(shadowOf(alarmManager).scheduledAlarms).isNotEmpty()
    }

    @Test
    fun `scheduleAll with invalid timezone does not crash`() = runTest {
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
            location = LocationSettings(timeZone = "Not/AZone"),
            calculationMethod = "TURKEY_DIYANET"
        )
        coEvery { getPrayerTimesUseCase(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(
            listOf(
                Prayer(
                    name = "Dhuhr",
                    arabicName = "الظهر",
                    time = LocalTime(23, 59),
                    date = LocalDate(2026, 8, 22)
                )
            )
        )

        val exceptions = mutableListOf<Throwable>()
        val scope = CoroutineScope(
            UnconfinedTestDispatcher(testScheduler) +
                CoroutineExceptionHandler { _, throwable -> exceptions += throwable }
        )
        val scheduler = scheduler(scope)
        scheduler.scheduleAll()

        assertThat(exceptions).isEmpty()
    }

    @Test
    fun `cancelAll cancels countdown tick and reminder alarms`() = runTest {
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminderIntent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.DAILY_REMINDER.name)
        val reminderPendingIntent = PendingIntent.getBroadcast(
            context, SchedulePlan.REQUEST_CODE_DAILY_REMINDER, reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 60_000,
            reminderPendingIntent
        )
        val tickIntent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_COUNTDOWN_TICK)
        val tickPendingIntent = PendingIntent.getBroadcast(
            context, SchedulePlan.REQUEST_CODE_COUNTDOWN_TICK, tickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 60_000,
            tickPendingIntent
        )
        assertThat(shadowOf(alarmManager).scheduledAlarms).hasSize(2)

        scheduler.cancelAll()

        assertThat(shadowOf(alarmManager).scheduledAlarms).isEmpty()
    }

    @Test
    fun `cancelAll cancels countdown notification`() = runTest {
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.cancelAll()
        verify { notificationDisplayer.cancelCountdown() }
    }

    @Test
    fun `cancelCountdown cancels scheduled countdown tick alarm`() = runTest {
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val tickIntent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_COUNTDOWN_TICK)
        val tickPendingIntent = PendingIntent.getBroadcast(
            context, SchedulePlan.REQUEST_CODE_COUNTDOWN_TICK, tickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 60_000,
            tickPendingIntent
        )
        assertThat(shadowOf(alarmManager).scheduledAlarms).hasSize(1)

        scheduler.cancelCountdown()

        assertThat(shadowOf(alarmManager).scheduledAlarms).isEmpty()
    }

    @Test
    fun `scheduleAll with specialDaysEnabled false does not schedule special day alarms`() = runTest {
        val specialDaysCalculator = mockk<SpecialDaysCalculator>(relaxed = true)
        every { specialDaysCalculator.specialDayFor(any(), any()) } returns SpecialDay.EID_AL_FITR
        coEvery { dataStore.getSettings() } returns NotificationSettings(
            enabled = true,
            specialDaysEnabled = false
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
        coEvery { getPrayerTimesUseCase(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(
            listOf(
                Prayer(
                    name = "Dhuhr",
                    arabicName = "الظهر",
                    time = LocalTime(23, 59),
                    date = LocalDate(2026, 8, 22)
                )
            )
        )

        val scheduler = scheduler(
            CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            specialDaysCalculator
        )
        scheduler.scheduleAll()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val specialDayAlarms = shadowOf(alarmManager).scheduledAlarms.filter { alarm ->
            val requestCode = shadowOf(alarm.operation).requestCode
            requestCode == SchedulePlan.REQUEST_CODE_SPECIAL_DAY ||
                requestCode == SchedulePlan.REQUEST_CODE_PRE_SPECIAL_DAY
        }
        assertThat(specialDayAlarms).isEmpty()
        verify(exactly = 0) { specialDaysCalculator.specialDayFor(any(), any()) }
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
        coEvery { getPrayerTimesUseCase(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(
            listOf(
                Prayer(
                    name = "Dhuhr",
                    arabicName = "الظهر",
                    time = LocalTime(23, 59),
                    date = LocalDate(2026, 8, 22)
                )
            )
        )

        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleAll()

        coVerify { notificationDisplayer.showCountdownNotification("Dhuhr", any(), any(), any()) }
    }

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

    @Test
    fun `scheduleAll after last prayer starts countdown to tomorrow's Dhuhr`() = runTest {
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
        coEvery { getPrayerTimesUseCase(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(
            listOf(
                Prayer(
                    name = "Dhuhr",
                    arabicName = "الظهر",
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

        val zoneId = java.time.ZoneId.of("Europe/Istanbul")
        val today = java.time.LocalDate.now(zoneId)
        val tomorrow = today.plusDays(1)
        val expectedDhuhr = java.time.LocalTime.of(0, 1)
            .atDate(tomorrow).atZone(zoneId).toInstant().toEpochMilli()
        val expectedLastTrigger = java.time.LocalTime.of(0, 2)
            .atDate(today).atZone(zoneId).toInstant().toEpochMilli()
        coVerify {
            notificationDisplayer.showCountdownNotification("Dhuhr", expectedDhuhr, expectedLastTrigger, any())
        }
    }

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
        coEvery { getPrayerTimesUseCase(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(
            listOf(
                Prayer(
                    name = "Dhuhr",
                    arabicName = "الظهر",
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
        coEvery { getPrayerTimesUseCase(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(
            listOf(
                Prayer(
                    name = "Dhuhr",
                    arabicName = "الظهر",
                    time = LocalTime(23, 59),
                    date = LocalDate(2026, 8, 22)
                )
            )
        )

        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleAll()

        val zoneId = java.time.ZoneId.of("Europe/Istanbul")
        val tomorrow = java.time.LocalDate.now(zoneId).plusDays(1)
        val tomorrowDhuhr = java.time.LocalTime.of(23, 59)
            .atDate(tomorrow)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val tomorrowAlarms = shadowOf(alarmManager).scheduledAlarms.filter { alarm ->
            shadowOf(alarm.operation).savedIntent
                .getLongExtra(AlarmReceiver.EXTRA_ALARM_TRIGGER_TIME, 0L) == tomorrowDhuhr
        }
        assertThat(tomorrowAlarms).isNotEmpty()
    }

    @Test
    fun `cancelAll stops the adhan service`() = runTest {
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.cancelAll()
        val stopped = shadowOf(context as Application).getNextStoppedService()
        assertThat(stopped?.component?.className).isEqualTo(AdhanService::class.java.name)
    }

    @Test
    fun `scheduleAll does not stop adhan service on reschedule`() = runTest {
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
        coEvery { getPrayerTimesUseCase(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(
            listOf(
                Prayer(
                    name = "Dhuhr",
                    arabicName = "الظهر",
                    time = LocalTime(23, 59),
                    date = LocalDate(2026, 8, 22)
                )
            )
        )

        val intent = Intent(context, AdhanService::class.java)
        context.startService(intent)

        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleAll()

        val stopped = shadowOf(context as Application).getNextStoppedService()
        assertThat(stopped).isNull()
    }

    @Test
    fun `scheduleTestAdhan schedules a single exact alarm at now plus delay`() = runTest {
        val before = System.currentTimeMillis()
        coEvery { dataStore.getSettings() } returns NotificationSettings(enabled = true)
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleTestAdhan(5)
        val after = System.currentTimeMillis()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarm = shadowOf(alarmManager).scheduledAlarms.single()
        assertThat(shadowOf(alarm.operation).requestCode)
            .isEqualTo(PrayerNotificationScheduler.REQUEST_CODE_TEST_ADHAN)
        assertThat(alarm.type).isEqualTo(AlarmManager.RTC_WAKEUP)
        assertThat(alarm.triggerAtTime).isAtLeast(before + 300_000L)
        assertThat(alarm.triggerAtTime).isAtMost(after + 300_000L)
        val testIntent = shadowOf(alarm.operation).savedIntent
        assertThat(testIntent.getStringExtra(AlarmReceiver.EXTRA_PRAYER_KEY)).isEqualTo("Dhuhr")
        assertThat(testIntent.getStringExtra(AlarmReceiver.EXTRA_ALARM_TYPE))
            .isEqualTo(AlarmType.PRAYER.name)
    }

    @Test
    fun `scheduleTestAdhan with zero delay fires at once`() = runTest {
        val before = System.currentTimeMillis()
        coEvery { dataStore.getSettings() } returns NotificationSettings(enabled = true)
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
        coEvery { dataStore.getSettings() } returns NotificationSettings(enabled = true)
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
        coEvery { dataStore.getSettings() } returns NotificationSettings(enabled = true)
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
        coEvery { dataStore.getSettings() } returns NotificationSettings(enabled = true)
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleTestAdhan(5)
        scheduler.cancelAll()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarm = shadowOf(alarmManager).scheduledAlarms.single()
        assertThat(shadowOf(alarm.operation).requestCode)
            .isEqualTo(PrayerNotificationScheduler.REQUEST_CODE_TEST_ADHAN)
    }

    @Test
    fun `scheduleTestAdhan with notifications disabled schedules nothing`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings(enabled = false)
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleTestAdhan(5)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        assertThat(shadowOf(alarmManager).scheduledAlarms).isEmpty()
    }

    @Test
    fun `scheduleTestAdhan with notifications disabled cancels a previously scheduled test alarm`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings(enabled = true)
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        scheduler.scheduleTestAdhan(5)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        assertThat(shadowOf(alarmManager).scheduledAlarms).hasSize(1)

        coEvery { dataStore.getSettings() } returns NotificationSettings(enabled = false)
        scheduler.scheduleTestAdhan(5)

        assertThat(shadowOf(alarmManager).scheduledAlarms).isEmpty()
    }

    @Test
    fun `buildDailySummary excludes imsak`() {
        val scheduler = scheduler(CoroutineScope(UnconfinedTestDispatcher()))
        val prayers = listOf(
            Prayer("Imsak", "الإمساك", LocalTime(4, 50), LocalDate(2026, 9, 2), isImsak = true),
            Prayer("Asr", "العصر", LocalTime(5, 0), LocalDate(2026, 9, 2)),
            Prayer("Dhuhr", "الظهر", LocalTime(12, 30), LocalDate(2026, 9, 2))
        )

        val summary = scheduler.buildDailySummary(prayers)

        assertThat(summary).doesNotContain("Imsak")
        assertThat(summary).contains("Asr 05:00")
        assertThat(summary).contains("Dhuhr 12:30")
    }
}
