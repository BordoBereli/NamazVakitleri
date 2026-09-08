package com.kutluoglu.prayer_notifications.push

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.kutluoglu.prayer_notifications.di.PrayerNotificationsModule
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ksp.generated.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PushMessagingServiceTest {

    private val topicSubscriptionManager = mockk<TopicSubscriptionManager>(relaxed = true)

    @Before
    fun setUp() {
        startKoin {
            allowOverride(true)
            modules(
                PrayerNotificationsModule.module,
                module {
                    single<Context> { ApplicationProvider.getApplicationContext() }
                    single<FirebaseMessaging> { mockk(relaxed = true) }
                    single<TopicSubscriptionManager> { topicSubscriptionManager }
                }
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `onMessageReceived shows push notification from data payload`() {
        val service = PushMessagingService()
        val message = RemoteMessage.Builder("from")
            .setData(mapOf("title" to "Eid message", "body" to "Ramadan Kareem"))
            .build()

        service.onMessageReceived(message)

        val nm = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = shadowOf(nm).allNotifications.single()
        assertThat(notification.channelId).isEqualTo("announcements")
        assertThat(notification.extras.getString("android.title")).isEqualTo("Eid message")
        assertThat(notification.extras.getString("android.text")).isEqualTo("Ramadan Kareem")
    }

    @Test
    fun `onMessageReceived ignores invalid payload`() {
        val service = PushMessagingService()
        val message = RemoteMessage.Builder("from")
            .setData(mapOf("body" to "missing title"))
            .build()

        service.onMessageReceived(message)

        val nm = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(nm).allNotifications).isEmpty()
    }

    @Test
    fun `onNewToken re-registers the global topic`() {
        val service = PushMessagingService()

        service.onNewToken("new-token")

        coVerify(timeout = 1_000) { topicSubscriptionManager.registerGlobal() }
    }
}
