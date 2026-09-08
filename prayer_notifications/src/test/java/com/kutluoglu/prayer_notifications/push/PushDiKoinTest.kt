package com.kutluoglu.prayer_notifications.push

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.messaging.FirebaseMessaging
import com.kutluoglu.prayer_notifications.di.PrayerNotificationsModule
import com.kutluoglu.prayer_notifications.manager.NotificationDisplayer
import com.kutluoglu.prayer_notifications.manager.PrayerNotificationManager
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.ksp.generated.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PushDiKoinTest {

    @Test
    fun `push classes resolve from the prayer_notifications DI graph`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val koin = koinApplication {
            allowOverride(true)
            modules(
                PrayerNotificationsModule.module,
                module {
                    single<Context> { context }
                    single<FirebaseMessaging> { mockk(relaxed = true) }
                }
            )
        }.koin

        assertThat(koin.get<PushMessageHandler>()).isNotNull()
        assertThat(koin.get<TopicSubscriptionManager>()).isNotNull()
        assertThat(koin.get<FcmClient>())
            .isInstanceOf(FirebaseMessagingClient::class.java)
        assertThat(koin.get<NotificationDisplayer>())
            .isInstanceOf(PrayerNotificationManager::class.java)
    }
}
