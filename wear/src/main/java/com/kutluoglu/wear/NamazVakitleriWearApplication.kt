package com.kutluoglu.wear

import android.app.Application
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable
import com.kutluoglu.wear.data.TileDataStore
import com.kutluoglu.wear.data.WatchDataListener
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinApplication
import org.koin.ksp.generated.*

@KoinApplication
class NamazVakitleriWearApplication : Application() {

    private var dataListener: WatchDataListener? = null

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@NamazVakitleriWearApplication)
            modules(configurationModules)
        }
        registerWatchDataListener()
    }

    private fun registerWatchDataListener() {
        runCatching {
            val dataClient: DataClient = Wearable.getDataClient(this)
            val dataStore: TileDataStore = get()
            dataListener = WatchDataListener(dataStore)
            dataClient.addListener(dataListener!!)
        }.onFailure {
            Log.e("NamazVakitleriWearApp", "Failed to register watch data listener -> ${it.message}")
        }
    }
}
