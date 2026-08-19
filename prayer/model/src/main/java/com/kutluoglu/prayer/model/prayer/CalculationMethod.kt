package com.kutluoglu.prayer.model.prayer

enum class CalculationMethod {
    TURKEY_DIYANET,
    MWL,
    ISNA,
    EGYPT,
    MAKKAH,
    KARACHI;

    companion object {
        fun fromSettingsId(id: String): CalculationMethod =
            entries.firstOrNull { it.name == id } ?: TURKEY_DIYANET
    }
}
