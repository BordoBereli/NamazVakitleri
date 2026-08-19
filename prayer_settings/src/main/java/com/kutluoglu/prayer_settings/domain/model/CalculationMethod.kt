package com.kutluoglu.prayer_settings.domain.model

data class CalculationMethod(
    val id: String,
    val name: String,
    val description: String
) {
    companion object {
        val methods = listOf(
            CalculationMethod("TURKEY_DIYANET", "Turkey (Diyanet)", "Used in Turkey"),
            CalculationMethod("MWL", "Muslim World League", "Muslim World League"),
            CalculationMethod("ISNA", "Islamic Society of North America", "ISNA"),
            CalculationMethod("EGYPT", "Egyptian General Authority", "Egyptian method"),
            CalculationMethod("MAKKAH", "Umm Al-Qura University", "Makkah method"),
            CalculationMethod("KARACHI", "University of Islamic Sciences, Karachi", "Karachi method")
        )

        fun fromId(id: String): CalculationMethod? = methods.find { it.id == id }
    }
}
