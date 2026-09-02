package com.kutluoglu.prayer_feature.common.states

import java.time.ZoneId

data class TimeUiState(
        val hijriDate: String = "",
        val gregorianFullDate: String = "",
        val gregorianShortDate: String = "",
        val gregorianDayAndName: String = "",
        val currentTime: String = "",
        val hijriAdjustment: Int = 0,
        val zoneId: ZoneId? = null
)