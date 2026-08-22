package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AdhanPlayerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `play and stop do not throw`() {
        val player = AdhanPlayer(context)
        player.play()
        player.stop()
    }
}
