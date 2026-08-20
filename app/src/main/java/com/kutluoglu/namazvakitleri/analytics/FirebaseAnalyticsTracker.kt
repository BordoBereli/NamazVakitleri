package com.kutluoglu.namazvakitleri.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.kutluoglu.core.common.analytics.AnalyticsTracker

/**
 * Firebase-backed [AnalyticsTracker] implementation.
 *
 * Registered via [com.kutluoglu.namazvakitleri.di.AppAnalyticsModule] as a singleton
 * bound to the [AnalyticsTracker] interface so feature ViewModels can inject the abstraction.
 */
class FirebaseAnalyticsTracker(
    private val firebaseAnalytics: FirebaseAnalytics
) : AnalyticsTracker {

    override fun logEvent(event: String, params: Map<String, Any?>) {
        val bundle = Bundle()
        params.toFirebaseParams().forEach { param ->
            when (val value = param.value) {
                is String -> bundle.putString(param.key, value)
                is Long -> bundle.putLong(param.key, value)
                is Double -> bundle.putDouble(param.key, value)
                else -> bundle.putString(param.key, value.toString())
            }
        }
        firebaseAnalytics.logEvent(event, bundle)
    }

    override fun setUserProperty(name: String, value: String?) {
        firebaseAnalytics.setUserProperty(name, value)
    }
}

/**
 * A normalized analytics parameter: a key and a Firebase-compatible value
 * (String, Long or Double).
 */
internal data class FirebaseParam(val key: String, val value: Any)

/**
 * Maps raw event parameters to Firebase-compatible types, dropping null values.
 *
 * - Int/Float/Boolean are widened to Long/Double as Firebase expects.
 * - Any other type is stringified.
 */
internal fun Map<String, Any?>.toFirebaseParams(): List<FirebaseParam> =
    mapNotNull { (key, value) ->
        when (value) {
            is String -> FirebaseParam(key, value)
            is Int -> FirebaseParam(key, value.toLong())
            is Long -> FirebaseParam(key, value)
            is Double -> FirebaseParam(key, value)
            is Float -> FirebaseParam(key, value.toDouble())
            is Boolean -> FirebaseParam(key, if (value) 1L else 0L)
            null -> null
            else -> FirebaseParam(key, value.toString())
        }
    }
