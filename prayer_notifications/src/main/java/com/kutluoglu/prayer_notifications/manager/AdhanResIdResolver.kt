package com.kutluoglu.prayer_notifications.manager

import com.kutluoglu.prayer_notifications.R
import com.kutluoglu.prayer_notifications.domain.AdhanStyle
import org.koin.core.annotation.Factory

@Factory
class AdhanResIdResolver {

    fun resolve(prayerKey: String, styleId: String?): Int {
        val style = AdhanStyle.fromId(styleId)
        return if (style == AdhanStyle.DEFAULT) {
            defaultResId(prayerKey)
        } else {
            styledResId(prayerKey, style) ?: defaultResId(prayerKey)
        }
    }

    private fun defaultResId(prayerKey: String): Int = when (prayerKey) {
        "Fajr" -> R.raw.adhan_fajr
        "Dhuhr" -> R.raw.adhan_dhuhr
        "Asr" -> R.raw.adhan_asr
        "Maghrib" -> R.raw.adhan_maghrib
        "Isha" -> R.raw.adhan_isha
        else -> R.raw.adhan_fajr
    }

    private fun styledResId(prayerKey: String, style: AdhanStyle): Int? = when (style) {
        // Future styles map to adhan_<prayer>_<style> resources, e.g.:
        // "Fajr" -> R.raw.adhan_fajr_makkah
        else -> null
    }
}
