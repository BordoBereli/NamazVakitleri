package com.kutluoglu.prayer_feature.qibla

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.qibla.components.AccuracyLevel
import com.kutluoglu.prayer_feature.qibla.components.QiblaCompass
import com.kutluoglu.prayer_feature.qibla.components.QiblaInfoSection
import com.kutluoglu.prayer_feature.qibla.components.accuracyLevel

@Composable
fun QiblaScreen(
    uiState: QiblaUiState,
    locationName: String? = "Istanbul, TR",
    onEvent: (QiblaEvent) -> Unit
) {
    LaunchedEffect(Unit) {
        onEvent(QiblaEvent.OnStart)
    }

    DisposableEffect(Unit) {
        onDispose {
            onEvent(QiblaEvent.OnStop)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(0.78f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                uiState.error != null -> {
                    Text(stringResource(R.string.qibla_location_error))
                }
                !uiState.isLocationAvailable -> {
                    Text(stringResource(R.string.qibla_waiting_location))
                }
                else -> {
                    QiblaCompass(
                        deviceAzimuth = uiState.deviceAzimuth,
                        qiblaAngle = uiState.qiblaAngle,
                        sensorAccuracy = uiState.sensorAccuracy
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AccuracyBadge(sensorAccuracy = uiState.sensorAccuracy)
                    if (accuracyLevel(uiState.sensorAccuracy) == AccuracyLevel.LOW) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.qibla_calibrate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    locationName?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .weight(0.22f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            QiblaInfoSection(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                uiState = uiState,
                locationName = locationName
            )
        }
    }
}

@Composable
private fun AccuracyBadge(sensorAccuracy: Int, modifier: Modifier = Modifier) {
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
