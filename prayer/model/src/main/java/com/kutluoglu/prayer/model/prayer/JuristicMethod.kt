package com.kutluoglu.prayer.model.prayer

enum class JuristicMethod {
    STANDARD, // Imam Shafi, Maliki, Hanbali
    HANAFI;

    companion object {
        fun fromSettingsId(id: String): JuristicMethod =
            entries.firstOrNull { it.name == id } ?: STANDARD
    }
}
