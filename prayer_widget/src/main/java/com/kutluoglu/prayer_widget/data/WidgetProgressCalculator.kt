package com.kutluoglu.prayer_widget.data

import kotlinx.datetime.LocalTime

object WidgetProgressCalculator {

    fun computeRingProgress(current: LocalTime, next: LocalTime, now: LocalTime): Float {
        val currentMin = current.hour * 60 + current.minute
        val nextMin = next.hour * 60 + next.minute
        val nowMin = now.hour * 60 + now.minute
        val total = positiveMod(nextMin - currentMin, 24 * 60)
        if (total == 0) return 0f
        val elapsed = if (nextMin > currentMin) {
            (nowMin - currentMin).coerceIn(0, total)
        } else {
            positiveMod(nowMin - currentMin, 24 * 60)
        }
        return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    private fun positiveMod(value: Int, modulus: Int): Int =
        ((value % modulus) + modulus) % modulus
}
