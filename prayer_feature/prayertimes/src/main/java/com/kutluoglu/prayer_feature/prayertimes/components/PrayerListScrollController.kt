package com.kutluoglu.prayer_feature.prayertimes.components

import kotlinx.datetime.YearMonth

class PrayerListScrollController(
    private val scrollToItem: suspend (Int) -> Unit
) {
    private var lastScrolledMonth: YearMonth? = null

    suspend fun onMonthChanged(
        month: YearMonth,
        isCurrentMonth: Boolean,
        todayIndex: Int,
        itemCount: Int
    ) {
        if (!isCurrentMonth) {
            lastScrolledMonth = null
            return
        }
        if (lastScrolledMonth == month) return
        if (todayIndex !in 0 until itemCount) return
        lastScrolledMonth = month
        scrollToItem(todayIndex)
    }
}
