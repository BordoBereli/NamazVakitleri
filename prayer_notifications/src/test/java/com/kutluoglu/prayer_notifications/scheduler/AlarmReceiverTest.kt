package com.kutluoglu.prayer_notifications.scheduler

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.kutluoglu.prayer_notifications.manager.AdhanPlayer
import com.kutluoglu.prayer_notifications.manager.PrayerNotificationManager
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

    @Before
    fun setUp() {
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(context)
                modules(module {
                    single { PrayerNotificationManager(get()) }
                    single { AdhanPlayer(get()) }
                })
            }
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `onReceive with prayer key does not throw`() {
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, "Fajr")
        receiver.onReceive(context, intent)
    }

    @Test
    fun `onReceive with STOP_COUNTDOWN action does not throw`() {
        val receiver = AlarmReceiver()
        val intent = Intent(context, AlarmReceiver::class.java).setAction("STOP_COUNTDOWN")
        receiver.onReceive(context, intent)
    }
}
