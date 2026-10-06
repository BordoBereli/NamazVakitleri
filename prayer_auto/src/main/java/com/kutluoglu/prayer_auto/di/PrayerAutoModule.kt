package com.kutluoglu.prayer_auto.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

/**
 * Koin module for the Android Auto screens and data sources.
 * KSP generates the configuration module; NamazVakitleriApplication loads it
 * via `modules(configurationModules)`.
 */
@Module
@Configuration
@ComponentScan("com.kutluoglu.prayer_auto**")
object PrayerAutoModule
