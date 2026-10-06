package com.kutluoglu.prayer_auto.screen

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.kutluoglu.prayer.domain.PrayerSurfaceDataProvider

/**
 * Daily prayer times list (placeholder — full ListTemplate implementation lands in Task 8).
 */
class PrayerTimesScreen(
    carContext: CarContext,
    private val surfaceProvider: PrayerSurfaceDataProvider
) : Screen(carContext) {

    override fun onGetTemplate(): Template =
        PaneTemplate.Builder(
            Pane.Builder()
                .addRow(Row.Builder().setTitle("...").build())
                .build()
        ).setTitle("Namaz Vakitleri").build()
}
