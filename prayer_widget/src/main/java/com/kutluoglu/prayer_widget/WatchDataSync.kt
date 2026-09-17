package com.kutluoglu.prayer_widget

import android.util.Log
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * KoinComponent accessor so non-DI contexts (WorkManager worker, broadcast
 * receiver, WidgetRefresher) can trigger a watch sync without wiring Koin
 * through their constructors. Mirrors the PrayerWidget KoinComponent pattern.
 */
object WatchDataSync : KoinComponent {

    private val syncer: WatchDataSyncer by inject()

    suspend fun sync() {
        runCatching { syncer.sync() }.onFailure {
            Log.e("WatchDataSync", "Failed to sync watch data -> ${it.message}")
        }
    }
}
