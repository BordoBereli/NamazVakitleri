package com.kutluoglu.prayer_auto.session

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.test.core.app.ApplicationProvider
import com.kutluoglu.prayer.domain.PrayerSurfaceDataProvider
import com.kutluoglu.prayer_auto.screen.NextPrayerScreen
import com.kutluoglu.prayer_auto.service.PrayerCarAppService
import io.mockk.mockk
import org.junit.Assert.assertTrue
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
        val intent = Intent(ApplicationProvider.getApplicationContext(), PrayerCarAppService::class.java)
        val screen: Screen = session.onCreateScreen(intent)

        assertTrue(screen is NextPrayerScreen)
    }
}
