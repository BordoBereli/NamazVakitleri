package com.kutluoglu.prayer_auto.screen

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Template
import com.kutluoglu.prayer.domain.PrayerSurfaceDataProvider

/**
 * Car home screen (placeholder — full PaneTemplate implementation lands in Task 7).
 */
class NextPrayerScreen(
    carContext: CarContext,
    private val surfaceProvider: PrayerSurfaceDataProvider,
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        return PaneTemplate.Builder(Pane.Builder().build())
            .setTitle("")
            .build()
    }
}
