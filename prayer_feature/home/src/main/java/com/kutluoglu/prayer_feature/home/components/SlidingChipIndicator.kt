package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
internal fun BoxScope.SlidingChipIndicator(
    offsetX: Float,
    width: Float,
    height: Float
) {
    Box(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .offset(x = offsetX.dp + 4.dp)
            .width((width - 8f).dp)
            .height((height - 8f).dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    )
}
