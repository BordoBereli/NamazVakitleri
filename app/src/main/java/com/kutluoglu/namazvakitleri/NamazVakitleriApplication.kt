package com.kutluoglu.namazvakitleri

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.kutluoglu.core.designsystem.utils.DisplayProvider
import com.kutluoglu.namazvakitleri.locale.LocaleManager
import com.kutluoglu.prayer_settings.data.local.SettingsDataStore
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinApplication
import org.koin.ksp.generated.*

@KoinApplication
class NamazVakitleriApplication : Application() {

    override fun attachBaseContext(base: Context) {
        val localeManager = LocaleManager()
        super.attachBaseContext(localeManager.applyPersistedLocale(base, SettingsDataStore.create(base)))
    }

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@NamazVakitleriApplication)
            modules(configurationModules + appModule)
        }
        setupActivityLifecycleCallbacks()
    }
    private fun setupActivityLifecycleCallbacks() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                val displayProvider: DisplayProvider = get()
                displayProvider.setCurrentActivity(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                val displayProvider: DisplayProvider = get()
                displayProvider.setCurrentActivity(activity)
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}