package com.kutluoglu.core.common.utils

fun countryCodeFromTimeZone(timeZone: String): String? {
    return when {
        timeZone.contains("Istanbul", ignoreCase = true) ||
            timeZone.contains("Europe/Istanbul", ignoreCase = true) -> "TR"
        timeZone.contains("Europe/Berlin", ignoreCase = true) -> "DE"
        timeZone.contains("Europe/London", ignoreCase = true) -> "GB"
        timeZone.contains("Europe/Paris", ignoreCase = true) -> "FR"
        timeZone.contains("Asia/Jakarta", ignoreCase = true) -> "ID"
        timeZone.contains("Asia/Riyadh", ignoreCase = true) -> "SA"
        else -> null
    }
}
