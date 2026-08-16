package com.kutluoglu.prayer_feature.qibla

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.kutluoglu.prayer_feature.qibla.components.AccuracyBadge
import com.kutluoglu.prayer_feature.qibla.components.BearingBadge
import com.kutluoglu.prayer_feature.qibla.components.LocationChip
import com.kutluoglu.prayer_feature.qibla.components.QIBLA_ALIGNMENT_THRESHOLD
import com.kutluoglu.prayer_feature.qibla.components.QiblaCompass
import com.kutluoglu.prayer_feature.qibla.components.QiblaDistanceLabel
import com.kutluoglu.prayer_feature.qibla.components.QiblaStatusBlock
import com.kutluoglu.prayer_feature.qibla.components.TurnDirection
import com.kutluoglu.prayer_feature.qibla.components.TurnPill
import com.kutluoglu.prayer_feature.qibla.components.qiblaDistanceLabel
import kotlin.math.roundToInt

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
        val isLandscape = maxWidth > maxHeight
        when {
            uiState.error != null -> {
                Text(stringResource(R.string.qibla_location_error))
            }
            !uiState.isLocationAvailable -> {
                Text(stringResource(R.string.qibla_waiting_location))
            }
            else -> {
                if (isLandscape) {
                    LandscapeLayout(
                        uiState = uiState,
                        locationName = locationName
                    )
                } else {
                    PortraitLayout(
                        uiState = uiState,
                        locationName = locationName
                    )
                }
            }
        }
    }
}

@Composable
private fun PortraitLayout(
    uiState: QiblaUiState,
    locationName: String?
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        locationName?.let {
            LocationChip(locationName = it)
            Spacer(modifier = Modifier.height(8.dp))
        }
        BearingBadge(bearing = uiState.qiblaBearing)
        Spacer(modifier = Modifier.height(16.dp))
        QiblaCompass(
            deviceAzimuth = uiState.deviceAzimuth,
            qiblaAngle = uiState.qiblaAngle,
            sensorAccuracy = uiState.sensorAccuracy
        )
        Spacer(modifier = Modifier.height(16.dp))
        QiblaStatusBlock(
            qiblaAngle = uiState.qiblaAngle,
            sensorAccuracy = uiState.sensorAccuracy
        )
    }
}

@Composable
private fun LandscapeLayout(
    uiState: QiblaUiState,
    locationName: String?
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            locationName?.let {
                LocationChip(locationName = it)
                Spacer(modifier = Modifier.height(8.dp))
            }
            BearingBadge(bearing = uiState.qiblaBearing)
        }

        QiblaCompass(
            deviceAzimuth = uiState.deviceAzimuth,
            qiblaAngle = uiState.qiblaAngle,
            sensorAccuracy = uiState.sensorAccuracy
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            QiblaStatusBlock(
                qiblaAngle = uiState.qiblaAngle,
                sensorAccuracy = uiState.sensorAccuracy
            )
        }
    }
}
