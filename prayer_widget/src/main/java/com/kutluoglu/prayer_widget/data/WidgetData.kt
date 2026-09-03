package com.kutluoglu.prayer_widget.data

data class WidgetData(
    val nextPrayerName: String,
    val nextPrayerTime: String,
    val countdownText: String,
    val ringProgress: Float,
    val locationName: String,
    val gregorianDate: String,
    val hijriDate: String,
    val prayers: List<WidgetPrayer>
)

data class WidgetPrayer(
    val name: String,
    val time: String,
    val isNext: Boolean
)

sealed interface WidgetResult {
    data class Success(val data: WidgetData) : WidgetResult
    data object Error : WidgetResult
}
