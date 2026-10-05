package com.kutluoglu.prayer.domain

import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.resolveZoneId
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.settings.SettingsProvider
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Factory
import java.time.Clock

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
 * current/next prayer.
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
        val result = dailyLoader.load(
            date = LocalDateTime.now(zoneId),
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = zoneId,
            calculationMethod = method,
            juristicMethod = juristicMethod,
            persistDailyCache = false
        ).getOrNull() ?: return null
        val nextPrayer = result.nextPrayer ?: return null
        return SurfacePrayerData(
            location = location,
            city = location.city ?: "",
            district = location.county,
            nextPrayerName = nextPrayer.name,
            nextPrayerTime = formatClockTime(nextPrayer.time),
            nextPrayerLocalTime = nextPrayer.time,
            currentPrayerLocalTime = result.currentPrayer?.time,
            nextPrayerEpochMillis = result.nextPrayerEpochMillis,
            currentPrayerEpochMillis = result.currentPrayerEpochMillis,
            isJumuah = result.isJumuah,
            hijriAdjustment = settings.hijriAdjustment,
            prayers = result.prayers,
            nextPrayerDate = nextPrayer.date
        )
    }
}
