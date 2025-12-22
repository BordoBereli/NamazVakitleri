package com.kutluoglu.prayer_feature.home.di

import com.kutluoglu.prayer_feature.common.di.PrayerFeatureCommonModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

/**
 * Created by F.K. on 22.10.2025.
 *
 */

@Module(includes = [PrayerFeatureCommonModule::class])
@Configuration
@ComponentScan("com.kutluoglu.prayer_feature.home**")
object PrayerFeatureHomeModule