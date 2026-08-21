package com.kutluoglu.prayer_feature.prayertimes.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * Created by F.K. on 24.12.2025.
 */

@Module
@ComponentScan("com.kutluoglu.prayer_feature.prayertimes**")
@Configuration
object PrayerFeaturePrayerTimesModule {
    @Single
    @Named("prayerSaveScope")
    fun providePrayerSaveScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
