package com.kutluoglu.prayer_qibla.di

import com.kutluoglu.core.ui.theme.di.CoreCommonModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

/**
 * Created by F.K. on 1.01.2026.
 *
 */

@Module(includes = [CoreCommonModule::class])
@ComponentScan("com.kutluoglu.prayer_qibla**")
@Configuration
object PrayerQiblaModule