package com.kutluoglu.namazvakitleri.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test

class FirebaseAnalyticsTrackerTest {

    private val firebaseAnalytics = mockk<FirebaseAnalytics>(relaxed = true)
    private val tracker = FirebaseAnalyticsTracker(firebaseAnalytics)

    @Test
    fun `logEvent forwards event name and a bundle to firebase`() {
        val bundleSlot = slot<Bundle>()
        every { firebaseAnalytics.logEvent(any(), capture(bundleSlot)) } returns Unit

        tracker.logEvent("test_event", mapOf("key" to "value"))

        verify { firebaseAnalytics.logEvent(eq("test_event"), any()) }
        assertThat(bundleSlot.captured).isNotNull()
    }

    @Test
    fun `logEvent with empty params still forwards the event`() {
        tracker.logEvent("test_event")

        verify { firebaseAnalytics.logEvent(eq("test_event"), any()) }
    }

    @Test
    fun `setUserProperty forwards name and value to firebase`() {
        tracker.setUserProperty("language", "tr")

        verify { firebaseAnalytics.setUserProperty("language", "tr") }
    }

    @Test
    fun `setUserProperty with null clears the property`() {
        tracker.setUserProperty("language", null)

        verify { firebaseAnalytics.setUserProperty("language", null) }
    }

    @Test
    fun `toFirebaseParams maps supported types to firebase compatible values`() {
        val params = mapOf(
            "string" to "value",
            "int" to 42,
            "long" to 42L,
            "double" to 1.5,
            "float" to 2.5f,
            "bool" to true,
            "other" to 3.14
        )

        val result = params.toFirebaseParams()

        assertThat(result).containsExactly(
            FirebaseParam("string", "value"),
            FirebaseParam("int", 42L),
            FirebaseParam("long", 42L),
            FirebaseParam("double", 1.5),
            FirebaseParam("float", 2.5),
            FirebaseParam("bool", 1L),
            FirebaseParam("other", 3.14)
        )
    }

    @Test
    fun `toFirebaseParams stringifies unsupported types`() {
        val result = mapOf("custom" to listOf(1, 2)).toFirebaseParams()

        assertThat(result).containsExactly(FirebaseParam("custom", "[1, 2]"))
    }

    @Test
    fun `toFirebaseParams drops null values`() {
        val result = mapOf("null" to null, "key" to "value").toFirebaseParams()

        assertThat(result).containsExactly(FirebaseParam("key", "value"))
    }

    @Test
    fun `toFirebaseParams returns empty list for empty map`() {
        assertThat(emptyMap<String, Any?>().toFirebaseParams()).isEmpty()
    }
}
