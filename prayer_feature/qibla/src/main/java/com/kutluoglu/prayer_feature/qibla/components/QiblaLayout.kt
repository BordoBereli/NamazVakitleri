package com.kutluoglu.prayer_feature.qibla.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun QiblaLayout(
    strategy: QiblaLayoutStrategy,
    qiblaBearing: Double,
    deviceAzimuth: Float,
    qiblaAngle: Float,
    sensorAccuracy: Int,
    locationName: String?,
    modifier: Modifier = Modifier
) {
    when (strategy) {
        QiblaLayoutStrategy.PORTRAIT -> PortraitColumn(
            qiblaBearing = qiblaBearing,
            deviceAzimuth = deviceAzimuth,
            qiblaAngle = qiblaAngle,
            sensorAccuracy = sensorAccuracy,
            locationName = locationName,
            modifier = modifier
        )
        QiblaLayoutStrategy.LANDSCAPE -> LandscapeRow(
            qiblaBearing = qiblaBearing,
            deviceAzimuth = deviceAzimuth,
            qiblaAngle = qiblaAngle,
            sensorAccuracy = sensorAccuracy,
            locationName = locationName,
            modifier = modifier
        )
    }
}

@Composable
private fun PortraitColumn(
    qiblaBearing: Double,
    deviceAzimuth: Float,
    qiblaAngle: Float,
    sensorAccuracy: Int,
    locationName: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        locationName?.let {
            LocationChip(locationName = it)
            Spacer(modifier = Modifier.height(8.dp))
        }
        BearingBadge(bearing = qiblaBearing)
        Spacer(modifier = Modifier.height(16.dp))
        QiblaCompass(
            deviceAzimuth = deviceAzimuth,
            qiblaAngle = qiblaAngle,
            sensorAccuracy = sensorAccuracy
        )
        Spacer(modifier = Modifier.height(16.dp))
        QiblaStatusBlock(
            qiblaAngle = qiblaAngle,
            sensorAccuracy = sensorAccuracy
        )
    }
}

@Composable
private fun LandscapeRow(
    qiblaBearing: Double,
    deviceAzimuth: Float,
    qiblaAngle: Float,
    sensorAccuracy: Int,
    locationName: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
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
            BearingBadge(bearing = qiblaBearing)
        }

        QiblaCompass(
            deviceAzimuth = deviceAzimuth,
            qiblaAngle = qiblaAngle,
            sensorAccuracy = sensorAccuracy
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
                qiblaAngle = qiblaAngle,
                sensorAccuracy = sensorAccuracy
            )
        }
    }
}
