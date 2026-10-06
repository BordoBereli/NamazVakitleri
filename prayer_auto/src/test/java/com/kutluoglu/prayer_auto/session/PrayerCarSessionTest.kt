package com.kutluoglu.prayer_auto.session

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.domain.PrayerSurfaceDataProvider
import com.kutluoglu.prayer_auto.screen.NextPrayerScreen
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class PrayerCarSessionTest {

    @Test
    fun `onCreateScreen returns NextPrayerScreen`() {
        val carContext = mockk<CarContext>(relaxed = true)
        val provider = mockk<PrayerSurfaceDataProvider>(relaxed = true)

        val session = PrayerCarSession(provider)
        val intent = Intent()
        val screen: Screen = session.onCreateScreen(intent)

        assertThat(screen).isInstanceOf(NextPrayerScreen::class.java)
    }
}
