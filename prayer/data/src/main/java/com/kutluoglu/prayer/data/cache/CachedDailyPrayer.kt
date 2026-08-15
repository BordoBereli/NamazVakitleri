package com.kutluoglu.prayer.data.cache

import com.kutluoglu.prayer.model.prayer.DailyPrayer
import kotlinx.serialization.Serializable

/**
 * Serializable snapshot of a [DailyPrayer] used to persist the whole-month
 * prayer-times cache. `gregorianDate` and `hijriDate` are already formatted
 * strings, so only the per-prayer data needs conversion via [CachedPrayer].
 */
@Serializable
data class CachedDailyPrayer(
    val dayOfMonth: Int,
    val gregorianDate: String,
    val hijriDate: String,
    val prayers: List<CachedPrayer>
) {
    fun toDailyPrayer(): DailyPrayer = DailyPrayer(
        dayOfMonth = dayOfMonth,
        gregorianDate = gregorianDate,
        hijriDate = hijriDate,
        prayers = prayers.map { it.toPrayer() }
    )
}

fun DailyPrayer.toCached(): CachedDailyPrayer = CachedDailyPrayer(
    dayOfMonth = dayOfMonth,
    gregorianDate = gregorianDate,
    hijriDate = hijriDate,
    prayers = prayers.map { it.toCached() }
)
