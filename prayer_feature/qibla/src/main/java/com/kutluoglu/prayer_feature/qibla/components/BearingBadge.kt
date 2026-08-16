package com.kutluoglu.prayer_feature.qibla.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.qibla.R
import kotlin.math.roundToInt

@Composable
fun BearingBadge(bearing: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFB8860B).copy(alpha = 0.35f))
    ) {
        Text(
            text = stringResource(R.string.qibla_degrees_north, bearing.roundToInt()),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB8860B)
        )
    }
}