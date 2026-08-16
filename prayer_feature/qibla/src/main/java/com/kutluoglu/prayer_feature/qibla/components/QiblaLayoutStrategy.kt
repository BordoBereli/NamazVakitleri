package com.kutluoglu.prayer_feature.qibla.components

import androidx.compose.ui.unit.Dp

enum class QiblaLayoutStrategy { PORTRAIT, LANDSCAPE }

fun qiblaLayoutStrategy(maxWidth: Dp, maxHeight: Dp): QiblaLayoutStrategy =
    if (maxWidth > maxHeight) QiblaLayoutStrategy.LANDSCAPE else QiblaLayoutStrategy.PORTRAIT