package com.kutluoglu.prayer_feature.qibla.components

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

@Composable
fun AccuracyBadge(sensorAccuracy: Int, modifier: Modifier = Modifier) {
    val level = accuracyLevel(sensorAccuracy)
    val (text, container, content) = when (level) {
        AccuracyLevel.HIGH -> Triple(
            stringResource(R.string.qibla_accuracy_high_badge),
            Color(0xFFE6F4EA),
            Color(0xFF1E7E34)
        )
        AccuracyLevel.MEDIUM -> Triple(
            stringResource(R.string.qibla_accuracy_medium_badge),
            Color(0xFFFFF4E0),
            Color(0xFFB26A00)
        )
        AccuracyLevel.LOW -> Triple(
            stringResource(R.string.qibla_calibration_required),
            Color(0xFFFDE8E8),
            Color(0xFFB3261E)
        )
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = container
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = content
        )
    }
}
