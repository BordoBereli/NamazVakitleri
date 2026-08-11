package com.kutluoglu.prayer.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
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

    @Single
    fun providePrayerTimesDataStore(context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile("prayer_times_cache") }
        )
}