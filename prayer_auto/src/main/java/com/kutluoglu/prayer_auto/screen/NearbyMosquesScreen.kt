package com.kutluoglu.prayer_auto.screen

import android.content.Intent
import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarLocation
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
 * Maps navigation. Search failures show an empty list with a message; the
 * map half stays visible. One-shot load on first resume (no refresh loop —
 * mosque locations don't change).
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
                    loadJob = scope.launch {
                        val surface = runCatching { surfaceProvider.load() }.getOrNull()
                        mosques = surface?.let {
                            searcher.search(it.location.latitude, it.location.longitude)
                        } ?: emptyList()
                        loaded = true
                        invalidate()
                    }
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

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
        if (mosques.isEmpty()) {
            list.addItem(
                Row.Builder()
                    .setTitle(carContext.getString(R.string.auto_no_mosques))
                    .build()
            )
        } else {
            mosques.forEach { mosque ->
                list.addItem(
                    Row.Builder()
                        .setTitle(mosque.name)
                        .addText("%.1f km".format(mosque.distanceKm))
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
                            carContext.startActivity(
                                Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps")
                            )
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
