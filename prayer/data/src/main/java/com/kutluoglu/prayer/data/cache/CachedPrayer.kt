package com.kutluoglu.prayer.data.cache

import com.kutluoglu.prayer.model.prayer.Prayer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/**
 * Serializable snapshot of a [Prayer] used to persist the prayer-times cache.
 * `time` and `date` are stored as ISO strings to avoid custom serializers.
 */
@Serializable
data class CachedPrayer(
    val name: String,
    val arabicName: String,
    val time: String,
    val date: String
) {
    fun toPrayer(): Prayer = Prayer(
        name = name,
        arabicName = arabicName,
        time = LocalTime.parse(time),
        date = LocalDate.parse(date)
    )
}

fun Prayer.toCached(): CachedPrayer = CachedPrayer(
    name = name,
    arabicName = arabicName,
    time = time.toString(),
    date = date.toString()
)
