package com.kutluoglu.prayer.model.prayer

/**
 * A single day's prayer times together with its formatted Gregorian and Hijri dates.
 * Used to render one row in the monthly prayer-times list.
 */
data class DailyPrayer(
    val dayOfMonth: Int,
    val gregorianDate: String,
    val hijriDate: String,
    val prayers: List<Prayer>
)
