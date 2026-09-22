package com.kutluoglu.wear.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.kutluoglu.prayer.domain.DailyPrayerTimesSource
import com.kutluoglu.prayer.domain.PrayerTimeEngine
import com.kutluoglu.prayer.domain.PrayerTimeEngineSource
import com.kutluoglu.wear.data.TileDataRepository
import com.kutluoglu.wear.data.TileDataStore
import com.kutluoglu.wear.data.WatchLocationProvider
import com.kutluoglu.wear.data.WatchSettingsProvider
import com.kutluoglu.wear.data.WatchTileDataBuilder
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan("com.kutluoglu.wear")
class WearModule {

    @Factory
    fun provideDailyPrayerTimesSource(prayerTimeEngine: PrayerTimeEngine): DailyPrayerTimesSource =
        PrayerTimeEngineSource(prayerTimeEngine)

    @Single
    fun provideDataClient(context: Context): DataClient = Wearable.getDataClient(context)

    @Single
    fun provideMessageClient(context: Context): MessageClient = Wearable.getMessageClient(context)

    @Single
    fun provideNodeClient(context: Context): NodeClient = Wearable.getNodeClient(context)

    @Single
    fun provideTileDataStore(context: Context): TileDataStore = TileDataStore.create(context)

    @Single
    fun provideWatchSettingsDataStore(context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile("watch_settings") }
        )

    @Single
    fun provideTileDataRepository(
        dataClient: DataClient,
        dataStore: TileDataStore,
        messageClient: MessageClient,
        nodeClient: NodeClient,
        tileDataBuilder: WatchTileDataBuilder,
        locationProvider: WatchLocationProvider,
        settingsProvider: WatchSettingsProvider,
        context: Context
    ): TileDataRepository = TileDataRepository(
        dataClient,
        dataStore,
        messageClient,
        nodeClient,
        tileDataBuilder,
        locationProvider,
        settingsProvider,
        context
    )
}
