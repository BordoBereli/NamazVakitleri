package com.kutluoglu.prayer_feature.prayertimes.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.kutluoglu.core.common.timeFormatter
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.ZoneId

private const val MILLIS_PER_MINUTE = 60_000L

/**
 * Returns the device time formatted as HH:mm for [zoneId], refreshed aligned to every
 * minute boundary so the clock reflects real time.
 *
 * Starts from [initial] when it is non-blank, keeping the caller's snapshot visible until
 * the first tick; otherwise computes the time immediately.
 */
@Composable
fun rememberCurrentTime(zoneId: ZoneId?, initial: String? = null): State<String> {
    val zone = zoneId ?: ZoneId.systemDefault()
    return produceState(
        initialValue = initial?.takeIf { it.isNotBlank() }
            ?: LocalDateTime.now(zone).format(timeFormatter),
        key1 = zone
    ) {
        while (true) {
            val now = LocalDateTime.now(zone)
            delay(MILLIS_PER_MINUTE - (now.second * 1000L) - now.nano / 1_000_000L)
            value = LocalDateTime.now(zone).format(timeFormatter)
        }
    }
}
