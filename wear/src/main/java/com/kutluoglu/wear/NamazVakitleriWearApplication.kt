package com.kutluoglu.wear

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinApplication
import org.koin.ksp.generated.*

@KoinApplication
class NamazVakitleriWearApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@NamazVakitleriWearApplication)
            modules(configurationModules)
        }
    }
}
