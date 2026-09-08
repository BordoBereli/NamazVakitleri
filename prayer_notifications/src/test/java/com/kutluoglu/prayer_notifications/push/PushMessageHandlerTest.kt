package com.kutluoglu.prayer_notifications.push

import com.kutluoglu.prayer_notifications.manager.NotificationDisplayer
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PushMessageHandlerTest {

    private val displayer = mockk<NotificationDisplayer>(relaxed = true)
    private lateinit var handler: PushMessageHandler

    @BeforeEach
    fun setUp() {
        handler = PushMessageHandler(displayer)
    }

    @Test
    fun `valid payload shows notification with trimmed title and body`() {
        handler.handle(
            mapOf(
                "title" to "  Eid message  ",
                "body" to "  Ramadan Kareem  "
            )
        )

        verify { displayer.showPushNotification("Eid message", "Ramadan Kareem") }
    }

    @Test
    fun `extra payload keys do not break delivery`() {
        handler.handle(
            mapOf(
                "title" to "Hello",
                "body" to "World",
                "type" to "announcement",
                "channel" to "announcements",
                "screen" to "home"
            )
        )

        verify { displayer.showPushNotification("Hello", "World") }
    }

    @Test
    fun `missing title is ignored`() {
        handler.handle(mapOf("body" to "Hello"))

        verify(exactly = 0) { displayer.showPushNotification(any(), any()) }
    }

    @Test
    fun `blank title is ignored`() {
        handler.handle(mapOf("title" to "   ", "body" to "Hello"))

        verify(exactly = 0) { displayer.showPushNotification(any(), any()) }
    }

    @Test
    fun `missing body is ignored`() {
        handler.handle(mapOf("title" to "Hello"))

        verify(exactly = 0) { displayer.showPushNotification(any(), any()) }
    }

    @Test
    fun `blank body is ignored`() {
        handler.handle(mapOf("title" to "Hello", "body" to " "))

        verify(exactly = 0) { displayer.showPushNotification(any(), any()) }
    }

    @Test
    fun `empty payload is ignored`() {
        handler.handle(emptyMap())

        verify(exactly = 0) { displayer.showPushNotification(any(), any()) }
    }
}