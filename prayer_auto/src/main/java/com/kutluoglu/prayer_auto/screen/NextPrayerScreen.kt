package com.kutluoglu.prayer_auto.screen

import android.text.SpannableString
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.ForegroundCarColorSpan
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.kutluoglu.core.common.PrayerCountdownCalculator
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
import java.time.ZoneId

/**
 * Car home screen: location line, next prayer + time as the pane title,
 * remaining time (minute granularity) as the subtitle. Refreshes every 60s
 * via invalidate() while resumed; no per-second ticking (distraction policy).
 */
class NextPrayerScreen(
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
        if (data == null) {
            return PaneTemplate.Builder(
                Pane.Builder()
                    .addRow(Row.Builder().setTitle(carContext.getString(R.string.auto_error)).build())
                    .build()
            ).setTitle(carContext.getString(R.string.auto_app_name)).build()
        }
        val zoneId = ZoneId.systemDefault()
        val isTomorrow = data.nextPrayerDate > LocalDate.now(zoneId).toKotlinLocalDate()
        val remaining = PrayerCountdownCalculator.countdownText(
            nextPrayerEpochMillis = data.nextPrayerEpochMillis,
            nowEpochMillis = clock.millis(),
            hourShort = carContext.getString(R.string.auto_countdown_hour_short),
            minuteShort = carContext.getString(R.string.auto_countdown_minute_short)
        )
        val title = if (data.isJumuah) {
            "${data.nextPrayerName} (${carContext.getString(R.string.auto_jumuah)}) — ${data.nextPrayerTime}"
        } else {
            "${data.nextPrayerName} — ${data.nextPrayerTime}"
        }
        val locationLine = listOfNotNull(data.city, data.district)
            .filter { it.isNotBlank() }
            .joinToString(" — ")
        val pane = Pane.Builder()
        data.currentPrayerName?.let { currentName ->
            data.currentPrayerLocalTime?.let { currentTime ->
                val spannable = SpannableString(formatClockTime(currentTime))
                spannable.setSpan(
                    ForegroundCarColorSpan.create(CarColor.PRIMARY),
                    0,
                    spannable.length,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                pane.addRow(
                    Row.Builder()
                        .setTitle("● $currentName")
                        .addText(spannable)
                        .build()
                )
            }
        }
        val row = Row.Builder()
            .setTitle("$title --> $remaining ${carContext.getString(R.string.auto_remaining_suffix)}")
        if (isTomorrow) {
            row.addText(carContext.getString(R.string.auto_tomorrow_morning))
        }
        pane.addRow(row.build())
        return PaneTemplate.Builder(
            pane
                .addAction(
                    Action.Builder()
                        .setTitle(carContext.getString(R.string.auto_prayer_times_action))
                        .setOnClickListener { screenManager.push(PrayerTimesScreen(carContext, surfaceProvider)) }
                        .build()
                )
                .addAction(
                    Action.Builder()
                        .setTitle(carContext.getString(R.string.auto_mosques_action))
                        .setOnClickListener { screenManager.push(NearbyMosquesScreen(carContext, surfaceProvider)) }
                        .build()
                )
                .build()
        ).setTitle("📍 $locationLine").build()
    }
}
