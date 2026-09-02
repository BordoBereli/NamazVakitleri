package com.kutluoglu.prayer_notifications.scheduler

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.AlarmType
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.domain.SpecialDay
import com.kutluoglu.prayer_notifications.manager.AdhanPlayer
import com.kutluoglu.prayer_notifications.manager.NotificationDisplayer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlarmReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val notificationDisplayer = mockk<NotificationDisplayer>(relaxed = true)
    private val adhanPlayer = mockk<AdhanPlayer>(relaxed = true)
    private val scheduler = mockk<AlarmScheduler>(relaxed = true)
    private val dataStore = mockk<NotificationSettingsDataStore>(relaxed = true)

    @Before
    fun setUp() {
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(context)
                modules(module {
                    single { notificationDisplayer }
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
    fun `prayer alarm with adhan enabled starts adhan service`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings(adhanEnabled = true)
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.PRAYER.name)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, "Dhuhr")
        receiver.handleAlarm(context, intent)
        val started = shadowOf(context as Application).getNextStartedService()
        assertThat(started?.component?.className).isEqualTo(AdhanService::class.java.name)
        verify(exactly = 0) { notificationDisplayer.showPrayerNotification(any(), any()) }
        verify(exactly = 0) { adhanPlayer.play(any(), any()) }
    }

    @Test
    fun `prayer alarm with adhan enabled starts adhan service as foreground`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings(adhanEnabled = true)
        val receiver = AlarmReceiver()
        val spyContext = spyk(context)
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.PRAYER.name)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, "Dhuhr")
        receiver.handleAlarm(spyContext, intent)
        verify { spyContext.startForegroundService(any()) }
    }

    @Test
    fun `prayer alarm with adhan disabled posts prayer notification`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings(adhanEnabled = false)
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.PRAYER.name)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, "Dhuhr")
        receiver.handleAlarm(context, intent)
        verify { notificationDisplayer.showPrayerNotification("Dhuhr", any()) }
        verify(exactly = 0) { adhanPlayer.play(any(), any()) }
    }

    @Test
    fun `jumuah prayer alarm posts jumuah notification`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings(jumuahEnabled = true, adhanEnabled = false)
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.PRAYER.name)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, "Dhuhr")
            .putExtra(AlarmReceiver.EXTRA_IS_JUMUAH, true)
        receiver.handleAlarm(context, intent)
        verify { notificationDisplayer.showJumuahNotification() }
        verify(exactly = 0) { notificationDisplayer.showPrayerNotification(any(), any()) }
    }

    @Test
    fun `pre-prayer alarm posts pre-prayer notification`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings()
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.PRE_PRAYER.name)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, "Dhuhr_pre")
            .putExtra(AlarmReceiver.EXTRA_PRE_PRAYER_MINUTES, 15)
        receiver.handleAlarm(context, intent)
        verify { notificationDisplayer.showPrePrayerNotification("Dhuhr", 15) }
    }

    @Test
    fun `daily reminder alarm posts summary and re-arms`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings()
        coEvery { scheduler.scheduleDailyReminder() } returns Unit
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.DAILY_REMINDER.name)
            .putExtra(AlarmReceiver.EXTRA_DAILY_SUMMARY, "Dhuhr 12:30")
        receiver.handleAlarm(context, intent)
        verify { notificationDisplayer.showDailyReminderNotification("Dhuhr 12:30") }
        coVerify { scheduler.scheduleDailyReminder() }
    }

    @Test
    fun `special day alarm posts special day notification`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings()
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.SPECIAL_DAY.name)
            .putExtra(AlarmReceiver.EXTRA_SPECIAL_DAY, SpecialDay.EID_AL_FITR.name)
        receiver.handleAlarm(context, intent)
        verify { notificationDisplayer.showSpecialDayNotification(SpecialDay.EID_AL_FITR) }
    }

    @Test
    fun `pre-special day alarm posts pre-special day notification`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings()
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.PRE_SPECIAL_DAY.name)
            .putExtra(AlarmReceiver.EXTRA_SPECIAL_DAY, SpecialDay.RAMADAN_START.name)
        receiver.handleAlarm(context, intent)
        verify { notificationDisplayer.showPreSpecialDayNotification(SpecialDay.RAMADAN_START) }
    }

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

    @Test
    fun `countdown tick without previous time passes null`() {
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_COUNTDOWN_TICK)
            .putExtra(AlarmReceiver.EXTRA_COUNTDOWN_TARGET, System.currentTimeMillis() + 60_000)
            .putExtra(AlarmReceiver.EXTRA_COUNTDOWN_PRAYER_NAME, "Dhuhr")
        receiver.onReceive(context, intent)
        verify { scheduler.updateCountdown(any(), "Dhuhr", null) }
    }

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
        receiver.handleAlarm(context, intent)
        verify { scheduler.updateCountdown(nextTime, "Maghrib", trigger) }
    }

    @Test
    fun `prayer alarm without trigger time passes null previous`() = runTest {
        coEvery { dataStore.getSettings() } returns NotificationSettings(countdownEnabled = true)
        val receiver = AlarmReceiver()
        val nextTime = System.currentTimeMillis() + 3_600_000
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.PRAYER.name)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, "Asr")
            .putExtra(AlarmReceiver.EXTRA_NEXT_PRAYER_TIME, nextTime)
            .putExtra(AlarmReceiver.EXTRA_NEXT_PRAYER_NAME, "Maghrib")
        receiver.handleAlarm(context, intent)
        verify { scheduler.updateCountdown(nextTime, "Maghrib", null) }
    }

    @Test
    fun `STOP_COUNTDOWN cancels countdown`() {
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_STOP_COUNTDOWN)
        receiver.onReceive(context, intent)
        verify { scheduler.cancelCountdown() }
    }

    @Test
    fun `STOP_ADHAN stops the adhan service`() {
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_STOP_ADHAN)
        receiver.onReceive(context, intent)
        val stopped = shadowOf(context as Application).getNextStoppedService()
        assertThat(stopped?.component?.className).isEqualTo(AdhanService::class.java.name)
    }
}
