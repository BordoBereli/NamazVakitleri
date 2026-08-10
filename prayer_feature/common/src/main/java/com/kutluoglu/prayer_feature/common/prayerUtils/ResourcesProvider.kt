package com.kutluoglu.prayer_feature.common.prayerUtils

import android.content.Context
import org.koin.core.annotation.Factory

@Factory
class ResourcesProvider(private val context: Context) {
    fun getStringArray(arrayResId: Int): Array<String> {
        return context.resources.getStringArray(arrayResId)
    }
}
