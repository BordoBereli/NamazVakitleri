package com.kutluoglu.wear.di

import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.kutluoglu.wear.data.TileDataRepository
import com.kutluoglu.wear.data.TileDataStore
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
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
    fun provideTileDataRepository(
        dataClient: DataClient,
        dataStore: TileDataStore,
        messageClient: MessageClient,
        nodeClient: NodeClient
    ): TileDataRepository = TileDataRepository(dataClient, dataStore, messageClient, nodeClient)
}
