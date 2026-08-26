package com.kutluoglu.core.designsystem.utils

import org.koin.core.annotation.Single
import java.util.Locale

// Bu sınıf, uygulama içi dil tercihini yansıtan dili sağlamaktan sorumludur.
@Single
class LanguageProvider {
    /**
     * Uygulamanın geçerli dil kodunu (örn: "en", "tr") döndürür.
     * LocaleManager, kalıcı Settings.language tercihini Locale.setDefault ile senkronize ettiği
     * için bu değer cihaz dilini değil uygulama içi dil tercihini yansıtır.
     */
    fun getLanguageCode(): String = Locale.getDefault().language
}
