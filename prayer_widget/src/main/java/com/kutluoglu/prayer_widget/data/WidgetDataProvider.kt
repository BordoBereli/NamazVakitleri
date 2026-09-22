package com.kutluoglu.prayer_widget.data

import com.kutluoglu.core.common.now
import com.kutluoglu.prayer.domain.DailyPrayerTimesLoader
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.domain.formatClockTime
import com.kutluoglu.prayer.domain.isJumuahPrayer
import com.kutluoglu.prayer.model.location.resolveZoneId
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalTime
import org.koin.core.annotation.Factory
import java.time.Clock
import java.time.ZoneId
import kotlin.time.toKotlinDuration

@Factory
class WidgetDataProvider(
    private val dailyLoader: DailyPrayerTimesLoader,
    private val locationsCoordinator: LocationsCoordinator,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val calculator: PrayerLogicEngine,
    private val formatter: PrayerFormatter,
    private val countdownFormatter: WidgetCountdownFormatter,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    suspend fun load(): WidgetResult {
        val location = locationsCoordinator.resolveSelected() ?: return WidgetResult.Error
        val zoneId = resolveZoneId(location)
        val settings = runCatching { getSettingsUseCase() }.getOrNull() ?: return WidgetResult.Error
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
        ).getOrNull() ?: return WidgetResult.Error
        val localizedPrayers = formatter.withLocalizedNames(result.prayers)
        val nextPrayer = result.nextPrayer?.let { raw ->
            localizedPrayers.firstOrNull { it.time == raw.time }?.copy(date = raw.date)
        } ?: return WidgetResult.Error
        val currentPrayer = result.currentPrayer?.let { raw ->
            localizedPrayers.firstOrNull { it.time == raw.time }?.copy(date = raw.date)
        }
        val duration = calculator.calculateTimeRemaining(nextPrayer.time, zoneId)
        val countdownText = duration.toKotlinDuration().toComponents { _, hours, minutes, _, _ ->
            countdownFormatter.format(hours, minutes)
        }
        val ringProgress = currentPrayer?.let {
            WidgetProgressCalculator.computeRingProgress(
                current = it.time,
                next = nextPrayer.time,
                now = java.time.LocalTime.now(clock.withZone(zoneId)).toKotlinLocalTime()
            )
        } ?: 0f
        val timeInfo = formatter.getInitialTimeInfo(zoneId, hijriAdjustment = settings.hijriAdjustment)
        return WidgetResult.Success(
            WidgetData(
                nextPrayerName = nextPrayer.name,
                nextPrayerTime = formatClockTime(nextPrayer.time),
                countdownText = countdownText,
                ringProgress = ringProgress,
                locationName = location.city ?: "",
                gregorianDate = timeInfo.gregorianFullDate,
                hijriDate = timeInfo.hijriDate,
                prayers = localizedPrayers.map { p ->
                    WidgetPrayer(
                        name = p.name,
                        time = formatClockTime(p.time),
                        isNext = p.name == nextPrayer.name,
                        isJumuah = isJumuahPrayer(p)
                    )
                },
                isJumuah = result.isJumuah,
                currentPrayerEpochMillis = result.currentPrayerEpochMillis,
                nextPrayerEpochMillis = result.nextPrayerEpochMillis
            )
        )
    }
}
