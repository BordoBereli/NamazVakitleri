package com.kutluoglu.prayer_feature.prayertimes.components

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.YearMonth
import org.junit.jupiter.api.Test

class PrayerListScrollControllerTest {

    @Test
    fun `scrolls to today when entering the current month for the first time`() = runTest {
        val scrolled = mutableListOf<Int>()
        val controller = PrayerListScrollController { scrolled.add(it) }
        controller.onMonthChanged(YearMonth(2026, 8), isCurrentMonth = true, todayIndex = 16, itemCount = 31)
        assertThat(scrolled).containsExactly(16)
    }

    @Test
    fun `does not scroll again when the same month is re-emitted`() = runTest {
        val scrolled = mutableListOf<Int>()
        val controller = PrayerListScrollController { scrolled.add(it) }
        controller.onMonthChanged(YearMonth(2026, 8), true, 16, 31)
        controller.onMonthChanged(YearMonth(2026, 8), true, 16, 31)
        assertThat(scrolled).containsExactly(16)
    }

    @Test
    fun `does not scroll when the month is not the current month`() = runTest {
        val scrolled = mutableListOf<Int>()
        val controller = PrayerListScrollController { scrolled.add(it) }
        controller.onMonthChanged(YearMonth(2026, 7), isCurrentMonth = false, todayIndex = 16, itemCount = 31)
        assertThat(scrolled).isEmpty()
    }

    @Test
    fun `does not scroll when the today index is out of range`() = runTest {
        val scrolled = mutableListOf<Int>()
        val controller = PrayerListScrollController { scrolled.add(it) }
        controller.onMonthChanged(YearMonth(2026, 8), true, todayIndex = 40, itemCount = 31)
        assertThat(scrolled).isEmpty()
    }

    @Test
    fun `scrolls again when returning to the current month after leaving it`() = runTest {
        val scrolled = mutableListOf<Int>()
        val controller = PrayerListScrollController { scrolled.add(it) }
        controller.onMonthChanged(YearMonth(2026, 8), true, 16, 31)
        controller.onMonthChanged(YearMonth(2026, 7), false, 16, 31)
        controller.onMonthChanged(YearMonth(2026, 8), true, 16, 31)
        assertThat(scrolled).containsExactly(16, 16)
    }
}
