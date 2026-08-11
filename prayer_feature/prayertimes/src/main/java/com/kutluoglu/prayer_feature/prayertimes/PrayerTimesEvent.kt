package com.kutluoglu.prayer_feature.prayertimes

/**
 * User-initiated events on the Prayer Times screen.
 */
sealed class PrayerTimesEvent {
    data object OnPreviousMonth : PrayerTimesEvent()
    data object OnNextMonth : PrayerTimesEvent()
    data object OnToday : PrayerTimesEvent()
}
