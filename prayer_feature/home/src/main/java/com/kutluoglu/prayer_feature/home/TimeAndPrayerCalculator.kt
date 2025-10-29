package com.kutluoglu.prayer_feature.home// You can create a new file for this, e.g., TimeAndPrayerCalculator.kt

import com.kutluoglu.core.common.gregorianFormatter
import com.kutluoglu.core.common.hijriFormatter
import com.kutluoglu.core.common.timeFormatter
import com.kutluoglu.prayer.model.Prayer
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalTime
import org.koin.core.annotation.Factory
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.HijrahDate
import kotlin.time.toKotlinDuration

@Factory
class TimeAndPrayerCalculator {

}
