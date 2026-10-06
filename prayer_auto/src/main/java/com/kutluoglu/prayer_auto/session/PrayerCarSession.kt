package com.kutluoglu.prayer_auto.session

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import com.kutluoglu.prayer.domain.PrayerSurfaceDataProvider
import com.kutluoglu.prayer_auto.screen.NextPrayerScreen

/**
 * Car app session; hosts the [NextPrayerScreen] as the car home screen.
 */
class PrayerCarSession(
    private val surfaceProvider: PrayerSurfaceDataProvider,
) : Session() {

    override fun onCreateScreen(intent: Intent): Screen =
        NextPrayerScreen(carContext, surfaceProvider)
}
