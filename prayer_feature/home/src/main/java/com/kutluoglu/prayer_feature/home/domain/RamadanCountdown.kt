package com.kutluoglu.prayer_feature.home.domain

import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalTime
import java.time.Duration

sealed interface RamadanCountdownState {
    data class SahurEndsIn(val duration: Duration) : RamadanCountdownState
    data class IftarIn(val duration: Duration) : RamadanCountdownState
}

fun computeRamadanCountdown(
    now: LocalTime,
    imsak: LocalTime,
    maghrib: LocalTime,
    nextImsak: LocalTime
): RamadanCountdownState {
    val nowJava = now.toJavaLocalTime()
    val imsakJava = imsak.toJavaLocalTime()
    val maghribJava = maghrib.toJavaLocalTime()
    val nextImsakJava = nextImsak.toJavaLocalTime()

    return when {
        nowJava.isBefore(imsakJava) ->
            RamadanCountdownState.SahurEndsIn(Duration.between(nowJava, imsakJava))
        nowJava.isBefore(maghribJava) ->
            RamadanCountdownState.IftarIn(Duration.between(nowJava, maghribJava))
        else -> {
            val untilNextImsak = Duration.between(nowJava, nextImsakJava)
            val adjusted = if (untilNextImsak.isNegative) untilNextImsak.plusHours(24) else untilNextImsak
            RamadanCountdownState.SahurEndsIn(adjusted)
        }
    }
}
