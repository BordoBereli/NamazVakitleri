package com.kutluoglu.prayer_feature.common.di

import com.kutluoglu.prayer.domain.DailyPrayerTimesSource
import com.kutluoglu.prayer.domain.GetPrayerTimesSource
import com.kutluoglu.prayer.usecases.prayer.GetPrayerTimesUseCase
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

/**
 * Created by F.K. on 21.12.2025.
 *
 */

@Module
@ComponentScan("com.kutluoglu.prayer_feature.common**")
@Configuration
object PrayerFeatureCommonModule {

    @Factory
    fun provideDailyPrayerTimesSource(getPrayerTimesUseCase: GetPrayerTimesUseCase): DailyPrayerTimesSource =
        GetPrayerTimesSource(getPrayerTimesUseCase)
}