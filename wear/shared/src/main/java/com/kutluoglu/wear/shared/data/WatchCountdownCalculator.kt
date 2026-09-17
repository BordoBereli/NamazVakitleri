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
        val dayMillis = 24 * 60 * 60 * 1000L
        val total = nextPrayerEpochMillis - currentPrayerEpochMillis
        val wrapped = total <= 0L
        val effectiveTotal = if (wrapped) total + dayMillis else total
        if (effectiveTotal <= 0L) return 0f
        val elapsed = if (wrapped) {
            positiveMod(nowEpochMillis - currentPrayerEpochMillis, dayMillis)
        } else {
            (nowEpochMillis - currentPrayerEpochMillis).coerceIn(0L, effectiveTotal)
        }
        return (elapsed.toFloat() / effectiveTotal.toFloat()).coerceIn(0f, 1f)
    }

    private fun positiveMod(value: Long, modulus: Long): Long =
        ((value % modulus) + modulus) % modulus
}
