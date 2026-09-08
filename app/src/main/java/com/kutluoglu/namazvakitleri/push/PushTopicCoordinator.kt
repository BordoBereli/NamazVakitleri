package com.kutluoglu.namazvakitleri.push

import android.util.Log
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.LocationsState
import com.kutluoglu.prayer_notifications.push.TopicSubscriptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Subscribes the device to the FCM topics matching the currently selected
 * location (global announcements, country, city) whenever it changes.
 */
@OptIn(FlowPreview::class)
class PushTopicCoordinator(
    private val topicSubscriptionManager: TopicSubscriptionManager,
    private val locationsCoordinator: LocationsCoordinator,
    private val debounceMillis: Long = 500
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            runCatching { topicSubscriptionManager.registerGlobal() }
                .onFailure { Log.e(TAG, "Failed to register global topic -> ${it.message}") }
        }
        scope.launch {
            locationsCoordinator.observeState()
                .distinctUntilChanged()
                .debounce(debounceMillis)
                .collect { state ->
                    val location = state.selectedLocation()
                    if (location != null) {
                        runCatching { topicSubscriptionManager.syncForLocation(location) }
                            .onFailure { Log.e(TAG, "Failed to sync topics -> ${it.message}") }
                    }
                }
        }
    }

    private companion object {
        const val TAG = "PushTopicCoordinator"

        fun LocationsState.selectedLocation(): LocationData? {
            val entry = entries.firstOrNull { it.id == selectedId } ?: entries.firstOrNull()
            return entry?.location
        }
    }
}