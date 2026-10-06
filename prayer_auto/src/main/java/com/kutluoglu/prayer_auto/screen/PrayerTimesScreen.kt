package com.kutluoglu.prayer_auto.screen

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.kutluoglu.prayer.domain.PrayerSurfaceDataProvider
import com.kutluoglu.prayer.domain.SurfacePrayerData
import com.kutluoglu.prayer.domain.formatClockTime
import com.kutluoglu.prayer_auto.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinLocalDate
import java.time.Clock
import java.time.LocalDate

/**
 * Daily prayer times list. After Isha: today's full list stays visible and
 * tomorrow's Imsak is appended with a next-event cue (spec Option A).
 * Note: shows today's Imsak time as an approximation for tomorrow's;
 * tomorrow's exact time requires a provider extension (see TODO below).
 * Refreshes every 60s via invalidate() while resumed.
 */
class PrayerTimesScreen(
    carContext: CarContext,
    private val surfaceProvider: PrayerSurfaceDataProvider,
    private val clock: Clock = Clock.systemDefaultZone()
) : Screen(carContext) {

    private var surface: SurfacePrayerData? = null
    private var refreshJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            startRefreshLoop()
        }

        override fun onPause(owner: LifecycleOwner) {
            refreshJob?.cancel()
            refreshJob = null
        }

        override fun onDestroy(owner: LifecycleOwner) {
            scope.cancel()
        }
    }

    init {
        lifecycle.addObserver(lifecycleObserver)
    }

    private fun startRefreshLoop() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            while (isActive) {
                surface = runCatching { surfaceProvider.load() }.getOrNull()
                invalidate()
                delay(60_000)
            }
        }
    }

    override fun onGetTemplate(): Template {
        val data = surface
        val city = data?.city?.takeIf { it.isNotBlank() }
        val headerTitle = if (city != null) {
            carContext.getString(R.string.auto_today_prefix) + " — " + city
        } else {
            carContext.getString(R.string.auto_today_prefix)
        }
        val header = Header.Builder()
            .setTitle(headerTitle)
            .setStartHeaderAction(Action.BACK)
            .build()

        if (data == null) {
            return ListTemplate.Builder()
                .setHeader(header)
                .setSingleList(
                    ItemList.Builder()
                        .addItem(Row.Builder().setTitle(carContext.getString(R.string.auto_error)).build())
                        .build()
                )
                .build()
        }

        val today = LocalDate.now(clock).toKotlinLocalDate()
        val isAfterIsha = data.nextPrayerDate > today
        val list = ItemList.Builder()
        data.prayers.forEach { p ->
            val isNext = p.name == data.nextPrayerName && p.date == data.nextPrayerDate
            val title = if (isNext) "▶ ${p.name}" else p.name
            list.addItem(
                Row.Builder()
                    .setTitle(title)
                    .addText(formatClockTime(p.time))
                    .build()
            )
        }
        if (isAfterIsha) {
            // TODO: SurfacePrayerData should expose tomorrow's Imsak time for exact display
            val imsak = data.prayers.firstOrNull { it.isImsak }
            if (imsak != null) {
                list.addItem(
                    Row.Builder()
                        .setTitle("▶ ${imsak.name} (${carContext.getString(R.string.auto_tomorrow)})")
                        .addText(formatClockTime(imsak.time))
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setHeader(header)
            .setSingleList(list.build())
            .build()
    }
}
