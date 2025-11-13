package com.kutluoglu.prayer.data.di

import okhttp3.OkHttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * Created by F.K. on 22.10.2025.
 *
 */

@Module
@Configuration
@ComponentScan("com.kutluoglu.prayer.data**", "com.kutluoglu.prayer.data.**")
object PrayerDataModule {
    @Single
    fun provideOkHttp(): OkHttpClient = OkHttpClient()
}