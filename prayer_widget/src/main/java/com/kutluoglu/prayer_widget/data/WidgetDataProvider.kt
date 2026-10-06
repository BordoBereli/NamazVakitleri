package com.kutluoglu.prayer_widget.data

import com.kutluoglu.core.designsystem.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.domain.PrayerSurfaceDataProvider
import com.kutluoglu.prayer.domain.formatClockTime
import com.kutluoglu.prayer.domain.isJumuahPrayer
import com.kutluoglu.prayer.model.location.resolveZoneId
import kotlinx.datetime.toKotlinLocalTime
import org.koin.core.annotation.Factory
import java.time.Clock
import java.time.LocalTime
import kotlin.time.toKotlinDuration

/**
 * Maps `SurfacePrayerData` to `WidgetData`; orchestration lives in `PrayerSurfaceDataProvider`.
 */
@Factory
class WidgetDataProvider(
    private val surfaceProvider: PrayerSurfaceDataProvider,
    private val formatter: PrayerFormatter,
    private val countdownFormatter: WidgetCountdownFormatter,
    private val calculator: PrayerLogicEngine,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    suspend fun load(): WidgetResult {
        val surface = surfaceProvider.load() ?: return WidgetResult.Error
        val zoneId = resolveZoneId(surface.location)
        val localizedPrayers = formatter.withLocalizedNames(surface.prayers)
        // The raw current/next names are elements of surface.prayers, and
        // withLocalizedNames preserves size/order, so map by index into the
        // localized list.
        val nextPrayerName = localizedPrayers.getOrNull(
            surface.prayers.indexOfFirst { it.time == surface.nextPrayerLocalTime }
        )?.name ?: surface.nextPrayerName
        val duration = calculator.calculateTimeRemaining(surface.nextPrayerLocalTime, zoneId)
        val countdownText = duration.toKotlinDuration().toComponents { _, hours, minutes, _, _ ->
            countdownFormatter.format(hours, minutes)
        }
        val ringProgress = surface.currentPrayerLocalTime?.let {
            WidgetProgressCalculator.computeRingProgress(
                current = it,
                next = surface.nextPrayerLocalTime,
                now = LocalTime.now(clock.withZone(zoneId)).toKotlinLocalTime()
            )
        } ?: 0f
        val timeInfo = formatter.getInitialTimeInfo(zoneId, hijriAdjustment = surface.hijriAdjustment)
        return WidgetResult.Success(
            WidgetData(
                nextPrayerName = nextPrayerName,
                nextPrayerTime = surface.nextPrayerTime,
                countdownText = countdownText,
                ringProgress = ringProgress,
                locationName = surface.city,
                gregorianDate = timeInfo.gregorianFullDate,
                hijriDate = timeInfo.hijriDate,
                prayers = localizedPrayers.map { p ->
                    WidgetPrayer(
                        name = p.name,
                        time = formatClockTime(p.time),
                        isNext = p.name == nextPrayerName,
                        isJumuah = isJumuahPrayer(p)
                    )
                },
                isJumuah = surface.isJumuah,
                currentPrayerEpochMillis = surface.currentPrayerEpochMillis,
                nextPrayerEpochMillis = surface.nextPrayerEpochMillis
            )
        )
    }
}
