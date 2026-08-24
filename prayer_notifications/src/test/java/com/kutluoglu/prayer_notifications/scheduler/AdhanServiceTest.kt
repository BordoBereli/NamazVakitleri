package com.kutluoglu.prayer_notifications.scheduler

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_notifications.manager.AdhanPlayer
import com.kutluoglu.prayer_notifications.manager.PrayerNotificationManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AdhanServiceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val adhanPlayer = mockk<AdhanPlayer>(relaxed = true)
    private val notificationManager = mockk<PrayerNotificationManager>(relaxed = true)

    @Before
    fun setUp() {
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(context)
                modules(module {
                    single { adhanPlayer }
                    single { notificationManager }
                })
            }
        }
        every { notificationManager.buildAdhanNotification(any()) } returns
            NotificationCompat.Builder(context, "adhan").build()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun startService(prayerKey: String = "Fajr"): ServiceController<AdhanService> {
        val intent = Intent(context, AdhanService::class.java)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, prayerKey)
        return Robolectric.buildService(AdhanService::class.java)
            .create()
            .also { it.get().onStartCommand(intent, 0, 1) }
    }

    @Test
    fun `onStartCommand plays adhan and shows foreground notification`() {
        startService("Fajr")
        verify { adhanPlayer.play("Fajr") }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(nm).allNotifications).isNotEmpty()
    }

    @Test
    fun `volume decrease stops the service`() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 5, 0)
        val controller = startService("Fajr")
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 4, 0)
        val uri = Settings.System.getUriFor("volume_alarm_sound")
        shadowOf(context.contentResolver).getContentObservers(uri).forEach { it.onChange(false) }
        controller.destroy()
        verify { adhanPlayer.stop() }
    }

    @Test
    fun `completion callback stops the service`() {
        val completionSlot = slot<() -> Unit>()
        every { adhanPlayer.setOnCompletionListener(capture(completionSlot)) } answers { }
        val controller = startService("Fajr")
        completionSlot.captured.invoke()
        controller.destroy()
        verify { adhanPlayer.stop() }
    }
}
