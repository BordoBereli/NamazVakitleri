package com.kutluoglu.core.designsystem.utils

import android.content.Context
import org.koin.core.annotation.Single

@Single
class StringResourcesProvider(private val context: Context) {
    fun getStringArray(arrayResId: Int): Array<String> {
        return context.resources.getStringArray(arrayResId)
    }
}
