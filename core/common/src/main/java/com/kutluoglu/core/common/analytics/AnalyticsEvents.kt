package com.kutluoglu.core.common.analytics

/**
 * Analytics event names. Keep in sync with the Play Console / BigQuery event list.
 */
object AnalyticsEvents {

    // Screen views
    const val SCREEN_VIEW = "screen_view"

    // Home
    const val HOME_LOADED = "home_loaded"
    const val LOCATION_SWITCHED = "location_switched"
    const val PULL_TO_REFRESH = "pull_to_refresh"
    const val QURAN_VERSE_LOADED = "quran_verse_loaded"
    const val QURAN_VERSE_OPENED = "quran_verse_opened"
    const val QURAN_VERSE_DISMISSED = "quran_verse_dismissed"

    // Prayer times
    const val MONTH_NAVIGATED = "month_navigated"
    const val TODAY_PRESSED = "today_pressed"
    const val PRAYER_TIMES_ERROR = "prayer_times_error"

    // Qibla
    const val QIBLA_OPENED = "qibla_opened"
    const val QIBLA_COMPASS_STARTED = "qibla_compass_started"
    const val QIBLA_COMPASS_STOPPED = "qibla_compass_stopped"
    const val QIBLA_ALIGNED = "qibla_aligned"

    // Settings & configuration
    const val CALCULATION_METHOD_CHANGED = "calculation_method_changed"
    const val LANGUAGE_CHANGED = "language_changed"
    const val HIJRI_ADJUSTMENT_CHANGED = "hijri_adjustment_changed"
    const val CACHE_CLEARED = "cache_cleared"

    // Location management
    const val LOCATION_SELECTION_OPENED = "location_selection_opened"
    const val LOCATION_SEARCH = "location_search"
    const val LOCATION_SELECTED = "location_selected"
    const val LOCATION_ADDED = "location_added"
    const val LOCATION_REMOVED = "location_removed"
    const val LOCATION_REORDERED = "location_reordered"
    const val GPS_TOGGLED = "gps_toggled"
    const val MAP_LOCATION_CONFIRMED = "map_location_confirmed"
    const val USE_MY_LOCATION = "use_my_location"

    // Permission funnel
    const val PERMISSION_REQUESTED = "permission_requested"
    const val PERMISSION_GRANTED = "permission_granted"
    const val PERMISSION_DENIED = "permission_denied"

    // Errors
    const val PRAYER_TIMES_LOAD_ERROR = "prayer_times_load_error"
    const val QURAN_VERSE_LOAD_ERROR = "quran_verse_load_error"
    const val LOCATION_SEARCH_ERROR = "location_search_error"
    const val NETWORK_ERROR = "network_error"
}
