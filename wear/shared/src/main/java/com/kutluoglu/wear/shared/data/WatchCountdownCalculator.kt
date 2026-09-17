package com.kutluoglu.wear.shared.data

object WatchCountdownCalculator {

    fun countdownText(
        nextPrayerEpochMillis: Long,
        nowEpochMillis: Long,
        hourShort: String,
        minuteShort: String
    ): String {
        val remainingMillis = (nextPrayerEpochMillis - nowEpochMillis).coerceAtLeast(0L)
        val totalMinutes = remainingMillis / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0 && minutes > 0 -> "$hours$hourShort $minutes$minuteShort"
            hours > 0 -> "$hours$hourShort"
            else -> "$minutes$minuteShort"
        }
    }

    fun ringProgress(
        currentPrayerEpochMillis: Long,
        nextPrayerEpochMillis: Long,
        nowEpochMillis: Long
    ): Float {
        val total = nextPrayerEpochMillis - currentPrayerEpochMillis
        if (total <= 0L) return 0f
        val elapsed = (nowEpochMillis - currentPrayerEpochMillis).coerceIn(0L, total)
        return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }
}
