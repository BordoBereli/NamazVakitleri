package com.kutluoglu.prayer_feature.qibla.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.common.utils.AngleUtils
import com.kutluoglu.prayer_feature.qibla.QiblaUiState
import com.kutluoglu.prayer_feature.qibla.R
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun QiblaInfoSection(
    modifier: Modifier = Modifier,
    uiState: QiblaUiState,
    locationName: String?
) {
    val isAligned = abs(AngleUtils.normalizeDegrees(uiState.qiblaAngle)) <= QIBLA_ALIGNMENT_THRESHOLD
    val distanceLabel = qiblaDistanceLabel(uiState.qiblaAngle, QIBLA_ALIGNMENT_THRESHOLD)
    val accuracyText = when (accuracyLevel(uiState.sensorAccuracy)) {
        AccuracyLevel.HIGH -> stringResource(R.string.qibla_accuracy_high)
        AccuracyLevel.MEDIUM -> stringResource(R.string.qibla_accuracy_medium)
        AccuracyLevel.LOW -> stringResource(R.string.qibla_accuracy_low)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        if (isAligned) {
            AlignedBanner()
        }

        locationName?.let {
            InfoRow(title = stringResource(R.string.qibla_location), value = it)
        }

        InfoRow(
            title = stringResource(R.string.qibla_direction),
            value = stringResource(
                R.string.qibla_degrees_north,
                uiState.qiblaBearing.roundToInt()
            )
        )

        InfoRow(
            title = stringResource(R.string.qibla_distance),
            value = when (distanceLabel) {
                is QiblaDistanceLabel.Aligned -> "0°"
                is QiblaDistanceLabel.Turn -> when (distanceLabel.direction) {
                    TurnDirection.RIGHT -> stringResource(
                        R.string.qibla_turn_right,
                        distanceLabel.degrees
                    )
                    TurnDirection.LEFT -> stringResource(
                        R.string.qibla_turn_left,
                        distanceLabel.degrees
                    )
                }
            }
        )

        InfoRow(
            title = stringResource(R.string.qibla_measurement),
            value = accuracyText
        )
    }
}

@Composable
private fun AlignedBanner() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE6F4EA)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_kaaba),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
            Text(
                text = stringResource(R.string.qibla_aligned),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E7E34)
            )
        }
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
