package com.kutluoglu.prayer.model.location

object LocationNameLocalizer {

    fun localized(entry: LocationEntry, languageCode: String): String = when (languageCode) {
        "tr" -> entry.displayNameTr ?: entry.displayName
        "ar" -> entry.displayNameAr ?: entry.displayName
        "fa" -> entry.displayNameFa ?: entry.displayName
        else -> entry.displayName
    }
}
