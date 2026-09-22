package com.kutluoglu.namazvakitleri.notifications

import com.kutluoglu.prayer_notifications.domain.PrayerCalculationSettings
import com.kutluoglu.prayer_notifications.domain.PrayerCalculationSettingsProvider
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase

/**
 * Composition-root implementation of [PrayerCalculationSettingsProvider] that
 * delegates to [prayer_settings]'s [GetSettingsUseCase]. This is what lets
 * `prayer_notifications` consume app settings without depending on
 * `prayer_settings`.
 */
class PrayerCalculationSettingsProviderImpl(
    private val getSettingsUseCase: GetSettingsUseCase
) : PrayerCalculationSettingsProvider {

    override suspend fun getSettings(): PrayerCalculationSettings {
        val settings = getSettingsUseCase()
        return PrayerCalculationSettings(
            calculationMethod = settings.calculationMethod,
            juristicMethod = settings.juristicMethod,
            hijriAdjustment = settings.hijriAdjustment
        )
    }
}
