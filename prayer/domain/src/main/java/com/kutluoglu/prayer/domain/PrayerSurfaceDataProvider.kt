package com.kutluoglu.prayer.domain

import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.resolveZoneId
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.settings.SettingsProvider
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toKotlinLocalDateTime
import org.koin.core.annotation.Factory
import java.time.Clock
import java.time.ZoneId

/**
 * Port for resolving the currently selected location. Defined in the domain
 * (instead of depending on `prayer_location` directly) to keep this module a
 * pure JVM library; implemented by the location layer.
 */
interface SurfaceLocationSource {
    suspend fun resolveSelected(): LocationData?
}

/**
 * Shared orchestration for all prayer-time surfaces (widget, Wear tile,
 * Android Auto): resolve location, resolve zone, load settings, load daily
 * times, map current/next, compute epoch millis. Surfaces become thin
 * mappers over [SurfacePrayerData], guaranteeing they all agree on
 * current/next prayer. After Isha (next prayer on tomorrow's date) it also
 * loads tomorrow's Imsak time into [SurfacePrayerData.tomorrowImsakTime];
 * during the day that extra load is skipped.
 */
@Factory
class PrayerSurfaceDataProvider(
    private val dailyLoader: DailyPrayerTimesLoader,
    private val locationSource: SurfaceLocationSource,
    private val settingsProvider: SettingsProvider,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    suspend fun load(): SurfacePrayerData? {
        val location = locationSource.resolveSelected() ?: return null
        val zoneId = resolveZoneId(location)
        val settings = runCatching { settingsProvider.getSettings() }.getOrNull() ?: return null
        val method = CalculationMethod.fromSettingsId(settings.calculationMethod)
        val juristicMethod = JuristicMethod.fromSettingsId(settings.juristicMethod)
        val today = java.time.LocalDateTime.now(clock.withZone(zoneId)).toKotlinLocalDateTime()
        val result = dailyLoader.load(
            date = today,
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = zoneId,
            calculationMethod = method,
            juristicMethod = juristicMethod,
            persistDailyCache = false
        ).getOrNull() ?: return null
        val nextPrayer = result.nextPrayer ?: return null
        val isAfterIsha = nextPrayer.date > today.date
        val tomorrowImsakTime = if (isAfterIsha) {
            loadTomorrowImsakTime(
                location = location,
                zoneId = zoneId,
                calculationMethod = method,
                juristicMethod = juristicMethod,
                today = today
            )
        } else {
            null
        }
        // The engine can only date-shift today's Imsak as the after-Isha next
        // prayer; patch its time with tomorrow's exact Imsak so every surface
        // (widget countdown/ring, watch tile epoch, Android Auto rows) agrees
        // with the notification scheduler on tomorrow's real time. Falls back
        // to the date-shifted time when the tomorrow load fails.
        val effectiveNextPrayer = if (isAfterIsha && tomorrowImsakTime != null && nextPrayer.isImsak) {
            nextPrayer.copy(time = tomorrowImsakTime)
        } else {
            nextPrayer
        }
        return SurfacePrayerData(
            location = location,
            city = location.city ?: "",
            district = location.county,
            nextPrayerName = effectiveNextPrayer.name,
            nextPrayerTime = formatClockTime(effectiveNextPrayer.time),
            nextPrayerLocalTime = effectiveNextPrayer.time,
            currentPrayerLocalTime = result.currentPrayer?.time,
            nextPrayerEpochMillis = toEpochMillis(effectiveNextPrayer, zoneId),
            currentPrayerEpochMillis = result.currentPrayerEpochMillis,
            isJumuah = result.isJumuah,
            hijriAdjustment = settings.hijriAdjustment,
            prayers = result.prayers,
            nextPrayerDate = effectiveNextPrayer.date,
            tomorrowImsakTime = tomorrowImsakTime,
            currentPrayerName = result.currentPrayer?.name
        )
    }

    /**
     * Loads tomorrow's Imsak time for surfaces that need the exact pre-dawn
     * cutoff (e.g. Android Auto after Isha). Fails soft: any error or a
     * missing Imsak entry yields null instead of failing the whole load.
     */
    private suspend fun loadTomorrowImsakTime(
        location: LocationData,
        zoneId: ZoneId,
        calculationMethod: CalculationMethod,
        juristicMethod: JuristicMethod,
        today: LocalDateTime
    ): LocalTime? {
        val tomorrow = today.date.plus(1, DateTimeUnit.DAY)
            .atTime(today.hour, today.minute, today.second, today.nanosecond)
        return runCatching {
            dailyLoader.load(
                date = tomorrow,
                latitude = location.latitude,
                longitude = location.longitude,
                zoneId = zoneId,
                calculationMethod = calculationMethod,
                juristicMethod = juristicMethod,
                persistDailyCache = false
            ).getOrNull()?.prayers?.firstOrNull { it.isImsak }?.time
        }.getOrNull()
    }
}
