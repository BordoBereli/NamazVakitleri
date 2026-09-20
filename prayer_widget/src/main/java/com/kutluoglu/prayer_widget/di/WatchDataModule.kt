package com.kutluoglu.prayer_widget.di

import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@Configuration
class WatchDataModule {

    @Factory
    fun provideDataClient(context: Context): DataClient = Wearable.getDataClient(context)

    @Factory
    fun provideMessageClient(context: Context): MessageClient = Wearable.getMessageClient(context)
}
