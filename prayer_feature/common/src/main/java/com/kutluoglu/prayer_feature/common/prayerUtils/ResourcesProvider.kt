package com.kutluoglu.prayer_feature.common.prayerUtils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import org.koin.core.annotation.Factory

@Factory
class ResourcesProvider(private val context: Context) {
    fun getStringArray(arrayResId: Int): Array<String> {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.getDefault())
        return context.createConfigurationContext(config).resources.getStringArray(arrayResId)
    }
}
