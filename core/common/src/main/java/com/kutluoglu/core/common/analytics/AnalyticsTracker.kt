package com.kutluoglu.core.common.analytics

/**
 * Abstraction over the analytics provider (e.g. Firebase Analytics).
 *
 * Keeps ViewModels and features decoupled from the concrete SDK so they stay
 * unit-testable (mock this interface) and the provider can be swapped.
 */
interface AnalyticsTracker {

    /**
     * Logs a single analytics event with optional parameters.
     *
     * @param event  event name (see [AnalyticsEvents])
     * @param params key/value parameters (see [AnalyticsParams])
     */
    fun logEvent(event: String, params: Map<String, Any?> = emptyMap())

    /**
     * Sets a user property that is attached to all subsequent events.
     *
     * @param name  property name (see [AnalyticsUserProperties])
     * @param value property value, or null to clear it
     */
    fun setUserProperty(name: String, value: String?)
}
