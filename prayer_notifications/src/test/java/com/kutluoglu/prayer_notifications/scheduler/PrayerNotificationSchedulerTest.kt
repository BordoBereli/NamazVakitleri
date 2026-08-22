package com.kutluoglu.prayer_notifications.scheduler

import android.app.AlarmManager
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
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.SchedulePlan
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

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

    private fun scheduler(scope: CoroutineScope) = PrayerNotificationScheduler(
        context = context,
        dataStore = dataStore,
        schedulePlan = schedulePlan,
        getPrayerTimesUseCase = getPrayerTimesUseCase,
        locationsCoordinator = locationsCoordinator,
        getSettingsUseCase = getSettingsUseCase,
        scope = scope
    )

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
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, "Fajr")
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
        coEvery { getPrayerTimesUseCase(any(), any(), any(), any(), any(), any()) } returns Result.success(
            listOf(
                Prayer(
                    name = "Fajr",
                    arabicName = "الفجر",
                    time = LocalTime(23, 59),
                    date = LocalDate(2026, 8, 22)
                ),
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

        val exceptions = mutableListOf<Throwable>()
        val scope = CoroutineScope(
            UnconfinedTestDispatcher(testScheduler) +
                CoroutineExceptionHandler { _, throwable -> exceptions += throwable }
        )
        val scheduler = scheduler(scope)
        scheduler.scheduleAll()

        assertThat(exceptions).isEmpty()
    }
}
