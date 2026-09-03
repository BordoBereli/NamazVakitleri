package com.kutluoglu.namazvakitleri

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kutluoglu.core.designsystem.utils.DisplayProvider
import com.kutluoglu.namazvakitleri.analytics.AnalyticsUserPropertiesManager
import com.kutluoglu.namazvakitleri.locale.LocaleManager
import com.kutluoglu.namazvakitleri.notifications.NotificationRescheduler
import com.kutluoglu.prayer_settings.data.local.SettingsDataStore
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import com.kutluoglu.prayer_widget.WidgetRefresher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinApplication
import org.koin.ksp.generated.*

@KoinApplication
class NamazVakitleriApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun attachBaseContext(base: Context) {
        val localeManager = LocaleManager()
        super.attachBaseContext(localeManager.applyPersistedLocale(base, SettingsDataStore.create(base)))
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        startKoin {
            androidLogger()
            androidContext(this@NamazVakitleriApplication)
            modules(configurationModules + appModule)
        }
        applyCrashlyticsConsent()
        setupActivityLifecycleCallbacks()
        startAnalyticsUserProperties()
        startNotificationRescheduler()
        startWidgetRefresher()
    }

    private fun applyCrashlyticsConsent() {
        applicationScope.launch {
            runCatching {
                val settings = get<SettingsRepository>().getSettings()
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(settings.crashlyticsEnabled)
            }.onFailure {
                android.util.Log.e("NamazVakitleriApp", "Failed to apply crashlytics consent -> ${it.message}")
            }
        }
    }

    private fun startAnalyticsUserProperties() {
        applicationScope.launch {
            runCatching {
                get<AnalyticsUserPropertiesManager>().start(applicationScope)
            }.onFailure {
                // Analytics must never crash the app.
                android.util.Log.e("NamazVakitleriApp", "Failed to start analytics user properties -> ${it.message}")
            }
        }
    }

    private fun startNotificationRescheduler() {
        applicationScope.launch {
            runCatching {
                get<NotificationRescheduler>().start(applicationScope)
            }.onFailure {
                // Rescheduling must never crash the app.
                android.util.Log.e("NamazVakitleriApp", "Failed to start notification rescheduler -> ${it.message}")
            }
        }
    }

    private fun startWidgetRefresher() {
        applicationScope.launch {
            runCatching {
                get<WidgetRefresher>().start(applicationScope)
            }.onFailure {
                // Widget refresh must never crash the app.
                android.util.Log.e("NamazVakitleriApp", "Failed to start widget refresher -> ${it.message}")
            }
        }
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