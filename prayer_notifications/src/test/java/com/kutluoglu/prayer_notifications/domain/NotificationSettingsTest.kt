package com.kutluoglu.prayer_notifications.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationSettingsTest {

    @Test
    fun `adhan defaults to off`() {
        assertThat(NotificationSettings().adhanEnabled).isFalse()
    }
}
