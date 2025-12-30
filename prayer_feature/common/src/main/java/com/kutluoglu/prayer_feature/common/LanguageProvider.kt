package com.kutluoglu.prayer_feature.common

import android.content.Context
import androidx.core.os.LocaleListCompat
import org.koin.core.annotation.Single
import java.util.Locale

// Bu sınıf, mevcut cihaz dilini sağlamaktan sorumludur.
@Single
class LanguageProvider(private val context: Context) {
    /**
     * Cihazın geçerli dil kodunu (örn: "en", "tr") döndürür.
     */
    fun getLanguageCode(): String {
        return LocaleListCompat.getAdjustedDefault()[0]?.language ?: Locale.getDefault().language
    }
}