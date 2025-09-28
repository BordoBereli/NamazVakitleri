package com.kutluoglu.namazvakitleri

import android.app.Application
import com.kutluoglu.namazvakitleri.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class NamazVakitleriApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@NamazVakitleriApplication)
            modules(appModule)
        }
    }
}