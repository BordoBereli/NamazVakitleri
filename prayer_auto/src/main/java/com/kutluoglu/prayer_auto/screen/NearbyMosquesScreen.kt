package com.kutluoglu.prayer_auto.screen

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.kutluoglu.prayer.domain.PrayerSurfaceDataProvider
import com.kutluoglu.prayer_auto.R

/**
 * Nearby mosques list (placeholder — full PlaceListMapTemplate implementation lands in Task 9).
 */
class NearbyMosquesScreen(
    carContext: CarContext,
    private val surfaceProvider: PrayerSurfaceDataProvider
) : Screen(carContext) {

    override fun onGetTemplate(): Template =
        PaneTemplate.Builder(
            Pane.Builder()
                .addRow(Row.Builder().setTitle("...").build())
                .build()
        ).setTitle(carContext.getString(R.string.auto_mosques_action)).build()
}
