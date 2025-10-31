package com.kutluoglu.core.ui.theme.common

import android.content.Context
import androidx.annotation.StringRes
import org.koin.core.annotation.Single

@Single
class StringResourcesProvider(
    private val context: Context
) {
    fun getString(@StringRes stringResId: Int): String {
        return context.getString(stringResId)
    }

    fun getStringArray(stringResId: Int): List<String> {
        return context.resources.getStringArray(stringResId).toList()
    }
}