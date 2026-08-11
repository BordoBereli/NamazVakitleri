package com.kutluoglu.prayer_remote.di

import okhttp3.OkHttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan("com.kutluoglu.prayer_remote**")
object PrayerRemoteModule {
    @Single
    fun provideOkHttp(): OkHttpClient = OkHttpClient()
}
