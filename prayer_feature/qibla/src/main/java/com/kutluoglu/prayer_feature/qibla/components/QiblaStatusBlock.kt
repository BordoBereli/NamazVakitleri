package com.kutluoglu.prayer_feature.qibla.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.qibla.R

@Composable
fun QiblaStatusBlock(
    qiblaAngle: Float,
    sensorAccuracy: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TurnPill(qiblaAngle = qiblaAngle)
        Spacer(modifier = Modifier.height(8.dp))
        AccuracyBadge(sensorAccuracy = sensorAccuracy)
        if (accuracyLevel(sensorAccuracy) == AccuracyLevel.LOW) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.qibla_calibrate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
