package com.kutluoglu.namazvakitleri

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.google.android.gms.wearable.MessageClient
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kutluoglu.app_update.data.InstallSourceDetector
import com.kutluoglu.core.designsystem.utils.DisplayProvider
import com.kutluoglu.namazvakitleri.analytics.AnalyticsUserPropertiesManager
import com.kutluoglu.namazvakitleri.locale.LocaleManager
import com.kutluoglu.namazvakitleri.notifications.NotificationRescheduler
import com.kutluoglu.namazvakitleri.push.PushTopicCoordinator
import com.kutluoglu.prayer_settings.data.local.SettingsDataStore
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import com.kutluoglu.prayer_widget.WatchSyncRequestListener
import com.kutluoglu.prayer_widget.WidgetMinuteScheduler
import com.kutluoglu.prayer_widget.WidgetRefresher
import com.kutluoglu.prayer_widget.hasAnyWidget
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

    private var watchSyncRequestListener: WatchSyncRequestListener? = null

    /**
     * Applies the persisted locale before any activity is created.
     *
     * NOTE: This deliberately creates a LOCAL [SettingsDataStore] instance via
     * [SettingsDataStore.create] because `attachBaseContext` runs BEFORE Koin is
     * started in [onCreate]. The persisted locale must be applied to the base
     * context before the activity context exists, so we cannot resolve the Koin
     * singleton here.
     *
     * The Koin singleton registered by `AppModule.provideSettingsDataStore` is the
     * canonical instance used by the rest of the app (see [MainActivity]).
     *
     * This dual-instantiation is a deliberate, documented exception. Do NOT "fix"
     * it by removing the local instance — that would break locale application.
     */
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
            modules(configurationModules)
        }
        applyCrashlyticsConsent()
        setupActivityLifecycleCallbacks()
        startAnalyticsUserProperties()
        startNotificationRescheduler()
        startPushTopicCoordinator()
        startWidgetRefresher()
        startWidgetMinuteRefresh()
        startWatchSyncRequestListener()
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

    private fun startPushTopicCoordinator() {
        applicationScope.launch {
            runCatching {
                get<PushTopicCoordinator>().start(applicationScope)
            }.onFailure {
                // Topic subscription must never crash the app.
                android.util.Log.e("NamazVakitleriApp", "Failed to start push topic coordinator -> ${it.message}")
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
    private fun startWidgetMinuteRefresh() {
        applicationScope.launch {
            runCatching {
                if (hasAnyWidget(this@NamazVakitleriApplication)) {
                    WidgetMinuteScheduler(this@NamazVakitleriApplication).schedule()
                }
            }.onFailure {
                // Widget refresh must never crash the app.
                android.util.Log.e("NamazVakitleriApp", "Failed to start widget minute refresh -> ${it.message}")
            }
        }
    }

    /**
     * Registers a runtime [MessageClient.OnMessageReceivedListener] so the app
     * receives the watch's tile sync-request messages while its process is alive.
     * This bypasses GMS's manifest listener-service routing, which is only enabled
     * for Play Store / OEM apps and therefore never delivers messages to sideloaded
     * installs.
     *
     * Only registered for sideloaded installs: Play Store builds are served by the
     * manifest-declared [WatchDataSyncListenerService], so registering the runtime
     * listener there too would trigger [WatchDataSync.sync] twice per sync request.
     */
    private fun startWatchSyncRequestListener() {
        applicationScope.launch {
            runCatching {
                val installSourceDetector: InstallSourceDetector = get()
                if (installSourceDetector.isPlayStoreInstall()) {
                    android.util.Log.d("NamazVakitleriApp", "Skipping runtime watch sync listener (Play Store install)")
                } else {
                    val messageClient: MessageClient = get()
                    val listener: WatchSyncRequestListener = get()
                    watchSyncRequestListener = listener
                    messageClient.addListener(listener)
                }
            }.onFailure {
                // Wear sync listener must never crash the app.
                android.util.Log.e("NamazVakitleriApp", "Failed to register watch sync request listener -> ${it.message}")
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