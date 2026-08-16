package com.kutluoglu.prayer_feature.qibla

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kutluoglu.prayer_feature.qibla.components.QiblaLayout
import com.kutluoglu.prayer_feature.qibla.components.qiblaLayoutStrategy

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

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.error != null -> {
                Text(stringResource(R.string.qibla_location_error))
            }
            !uiState.isLocationAvailable -> {
                Text(stringResource(R.string.qibla_waiting_location))
            }
            else -> {
                QiblaLayout(
                    strategy = qiblaLayoutStrategy(maxWidth = maxWidth, maxHeight = maxHeight),
                    qiblaBearing = uiState.qiblaBearing,
                    deviceAzimuth = uiState.deviceAzimuth,
                    qiblaAngle = uiState.qiblaAngle,
                    sensorAccuracy = uiState.sensorAccuracy,
                    locationName = locationName
                )
            }
        }
    }
}
