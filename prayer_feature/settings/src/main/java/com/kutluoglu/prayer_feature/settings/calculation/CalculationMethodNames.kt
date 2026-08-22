package com.kutluoglu.prayer_feature.settings.calculation

import androidx.annotation.StringRes
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer_feature.settings.R

@StringRes
fun CalculationMethod.displayNameRes(): Int = when (this) {
    CalculationMethod.TURKEY_DIYANET -> R.string.calculation_method_turkey_diyanet
    CalculationMethod.MWL -> R.string.calculation_method_mwl
    CalculationMethod.ISNA -> R.string.calculation_method_isna
    CalculationMethod.EGYPT -> R.string.calculation_method_egypt
    CalculationMethod.MAKKAH -> R.string.calculation_method_makkah
    CalculationMethod.KARACHI -> R.string.calculation_method_karachi
}
