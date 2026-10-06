package com.kutluoglu.prayer_auto.screen

import android.content.Intent
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarLocation
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.Metadata
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.kutluoglu.prayer.domain.PrayerSurfaceDataProvider
import com.kutluoglu.prayer_auto.R
import com.kutluoglu.prayer_auto.data.Mosque
import com.kutluoglu.prayer_auto.data.MosqueSearcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Nearby mosques on a half-screen map. Tapping a mosque hands off to Google
 * Maps navigation. Search failures show a message row that retries the search on
 * tap; the map half stays visible. One load on first resume, plus on-demand
 * retry (no refresh loop — mosque locations don't change).
 *
 * Instantiated by the car library (not Koin), hence KoinComponent.
 */
class NearbyMosquesScreen(
    carContext: CarContext,
    private val surfaceProvider: PrayerSurfaceDataProvider
) : Screen(carContext), KoinComponent {

    private val searcher: MosqueSearcher by inject()
    private var mosques: List<Mosque> = emptyList()
    private var loaded = false
    private var loadJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                if (!loaded) {
                    load()
                }
            }

            override fun onPause(owner: LifecycleOwner) {
                loadJob?.cancel()
                loadJob = null
            }

            override fun onDestroy(owner: LifecycleOwner) {
                scope.cancel()
            }
        })
    }

    private fun load() {
        if (loadJob?.isActive == true) {
            return
        }
        loadJob = scope.launch {
            val surface = runCatching { surfaceProvider.load() }.getOrNull()
            mosques = surface?.let {
                searcher.search(it.location.latitude, it.location.longitude)
            } ?: emptyList()
            loaded = true
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
        if (mosques.isEmpty()) {
            list.addItem(
                Row.Builder()
                    .setTitle(carContext.getString(R.string.auto_no_mosques))
                    .setBrowsable(true)
                    .setOnClickListener { load() }
                    .build()
            )
        } else {
            mosques.forEach { mosque ->
                val distanceText = SpannableStringBuilder(
                    "%.1f km".format(mosque.distanceKm)
                )
                distanceText.setSpan(
                    DistanceSpan.create(
                        Distance.create(mosque.distanceKm.toDouble(), Distance.UNIT_KILOMETERS_P1)
                    ),
                    0,
                    distanceText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                list.addItem(
                    Row.Builder()
                        .setTitle(mosque.name)
                        .addText(distanceText)
                        .setMetadata(
                            Metadata.Builder()
                                .setPlace(
                                    Place.Builder(CarLocation.create(mosque.latitude, mosque.longitude))
                                        .build()
                                )
                                .build()
                        )
                        .setOnClickListener {
                            val uri = Uri.parse("google.navigation:q=${mosque.latitude},${mosque.longitude}")
                            runCatching {
                                carContext.startActivity(
                                    Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps")
                                )
                            }
                        }
                        .build()
                )
            }
        }

        return PlaceListMapTemplate.Builder()
            .setTitle(carContext.getString(R.string.auto_mosques_action))
            .setHeaderAction(Action.BACK)
            .setItemList(list.build())
            .build()
    }
}
