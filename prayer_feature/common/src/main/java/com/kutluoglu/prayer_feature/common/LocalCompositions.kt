package com.kutluoglu.prayer_feature.common

import androidx.compose.runtime.compositionLocalOf

/**
 * Created by F.K. on 29.12.2025.
 *
 */

/**
 * CompositionLocal to provide information about the screen orientation.
 * true if the screen is in landscape mode (width > height), false otherwise.
 */
val LocalIsLandscape = compositionLocalOf { false }
