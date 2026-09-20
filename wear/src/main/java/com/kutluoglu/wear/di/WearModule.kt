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
import com.kutluoglu.wear.data.TileDataRepository
import com.kutluoglu.wear.data.TileDataStore
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan("com.kutluoglu.wear")
class WearModule {

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
        nodeClient: NodeClient
    ): TileDataRepository = TileDataRepository(dataClient, dataStore, messageClient, nodeClient)
}
