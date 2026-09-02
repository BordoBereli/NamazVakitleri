package com.kutluoglu.prayer_notifications.scheduler

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import com.kutluoglu.prayer_notifications.domain.NotificationSettings
import com.kutluoglu.prayer_notifications.manager.AdhanPlayer
import com.kutluoglu.prayer_notifications.manager.NotificationDisplayer
import io.mockk.coEvery
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
    private val notificationDisplayer = mockk<NotificationDisplayer>(relaxed = true)
    private val dataStore = mockk<NotificationSettingsDataStore>(relaxed = true)

    @Before
    fun setUp() {
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(context)
                modules(module {
                    single { adhanPlayer }
                    single { notificationDisplayer }
                    single { dataStore }
                })
            }
        }
        every { notificationDisplayer.buildAdhanNotification(any()) } returns
            NotificationCompat.Builder(context, "adhan").build()
        coEvery { dataStore.getSettings() } returns NotificationSettings(adhanVolume = 50)
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
        verify { adhanPlayer.play("Fajr", 50, null) }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(nm).allNotifications).isNotEmpty()
    }

    @Test
    fun `volume decrease to non-zero does not stop the service`() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 5, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 5, 0)
        val controller = startService("Fajr")
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 4, 0)
        val uri = Settings.System.getUriFor("volume_alarm_sound")
        shadowOf(context.contentResolver).getContentObservers(uri).forEach { it.onChange(false) }
        assertThat(shadowOf(controller.get()).isStoppedBySelf()).isFalse()
        verify(exactly = 0) { adhanPlayer.stop() }
        controller.destroy()
    }

    @Test
    fun `volume to zero stops the service`() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 5, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 5, 0)
        val controller = startService("Fajr")
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
        val uri = Settings.System.getUriFor("volume_alarm_sound")
        shadowOf(context.contentResolver).getContentObservers(uri).forEach { it.onChange(false) }
        assertThat(shadowOf(controller.get()).isStoppedBySelf()).isTrue()
        controller.destroy()
        verify { adhanPlayer.stop() }
    }

    @Test
    fun `volume change on the other stream does not stop when one stream started at floor`() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 5, 0)
        val controller = startService("Fajr")
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 4, 0)
        val uri = Settings.System.getUriFor("volume_music_sound")
        shadowOf(context.contentResolver).getContentObservers(uri).forEach { it.onChange(false) }
        assertThat(shadowOf(controller.get()).isStoppedBySelf()).isFalse()
        controller.destroy()
    }

    @Test
    fun `volume down to floor on the other stream stops even when one stream started at floor`() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 5, 0)
        val controller = startService("Fajr")
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        val uri = Settings.System.getUriFor("volume_music_sound")
        shadowOf(context.contentResolver).getContentObservers(uri).forEach { it.onChange(false) }
        assertThat(shadowOf(controller.get()).isStoppedBySelf()).isTrue()
        controller.destroy()
    }

    @Test
    fun `alarm volume at minimum is at floor`() {
        val audioManager = mockk<AudioManager>(relaxed = true)
        every { audioManager.getStreamMinVolume(AudioManager.STREAM_ALARM) } returns 1
        every { audioManager.getStreamVolume(AudioManager.STREAM_ALARM) } returns 1
        assertThat(isStreamAtFloor(audioManager, AudioManager.STREAM_ALARM)).isTrue()
    }

    @Test
    fun `alarm volume above minimum is not at floor`() {
        val audioManager = mockk<AudioManager>(relaxed = true)
        every { audioManager.getStreamMinVolume(AudioManager.STREAM_ALARM) } returns 1
        every { audioManager.getStreamVolume(AudioManager.STREAM_ALARM) } returns 2
        assertThat(isStreamAtFloor(audioManager, AudioManager.STREAM_ALARM)).isFalse()
    }

    @Test
    fun `alarm volume at one is at floor when reported min is zero`() {
        val audioManager = mockk<AudioManager>(relaxed = true)
        every { audioManager.getStreamMinVolume(AudioManager.STREAM_ALARM) } returns 0
        every { audioManager.getStreamVolume(AudioManager.STREAM_ALARM) } returns 1
        assertThat(isStreamAtFloor(audioManager, AudioManager.STREAM_ALARM)).isTrue()
    }

    @Test
    fun `music volume at minimum is at floor`() {
        val audioManager = mockk<AudioManager>(relaxed = true)
        every { audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC) } returns 1
        every { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) } returns 1
        assertThat(isStreamAtFloor(audioManager, AudioManager.STREAM_MUSIC)).isTrue()
    }

    @Test
    fun `music volume above minimum is not at floor`() {
        val audioManager = mockk<AudioManager>(relaxed = true)
        every { audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC) } returns 1
        every { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) } returns 2
        assertThat(isStreamAtFloor(audioManager, AudioManager.STREAM_MUSIC)).isFalse()
    }

    @Test
    fun `music volume at minimum stops the service`() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 5, 0)
        val controller = startService("Fajr")
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        val uri = Settings.System.getUriFor("volume_music_sound")
        shadowOf(context.contentResolver).getContentObservers(uri).forEach { it.onChange(false) }
        assertThat(shadowOf(controller.get()).isStoppedBySelf()).isTrue()
        controller.destroy()
        verify { adhanPlayer.stop() }
    }

    @Test
    fun `service starting with volume at floor does not stop immediately`() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 5, 0)
        val controller = startService("Fajr")
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(100)
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(shadowOf(controller.get()).isStoppedBySelf()).isFalse()
        controller.destroy()
    }

    @Test
    fun `one stream transitioning to floor stops even when the other started at floor`() {
        assertThat(shouldStopForVolumeFloor(
            alarmWasAtFloor = true, alarmIsAtFloor = true,
            musicWasAtFloor = false, musicIsAtFloor = true,
        )).isTrue()
    }

    @Test
    fun `both streams at floor from start does not stop`() {
        assertThat(shouldStopForVolumeFloor(
            alarmWasAtFloor = true, alarmIsAtFloor = true,
            musicWasAtFloor = true, musicIsAtFloor = true,
        )).isFalse()
    }

    @Test
    fun `no stream at floor does not stop`() {
        assertThat(shouldStopForVolumeFloor(
            alarmWasAtFloor = false, alarmIsAtFloor = false,
            musicWasAtFloor = false, musicIsAtFloor = false,
        )).isFalse()
    }

    @Test
    fun `completion callback stops the service`() {
        val completionSlot = slot<() -> Unit>()
        every { adhanPlayer.setOnCompletionListener(capture(completionSlot)) } answers { }
        val controller = startService("Fajr")
        completionSlot.captured.invoke()
        assertThat(shadowOf(controller.get()).isStoppedBySelf()).isTrue()
        controller.destroy()
        verify { adhanPlayer.stop() }
    }

    @Test
    fun `focus loss callback stops the service`() {
        val focusLossSlot = slot<() -> Unit>()
        every { adhanPlayer.setOnFocusLossListener(capture(focusLossSlot)) } answers { }
        val controller = startService("Fajr")
        focusLossSlot.captured.invoke()
        assertThat(shadowOf(controller.get()).isStoppedBySelf()).isTrue()
        controller.destroy()
        verify { adhanPlayer.stop() }
    }
}
