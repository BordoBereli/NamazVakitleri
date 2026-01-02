package com.kutluoglu.prayer_feature.qibla.components

import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kutluoglu.prayer_feature.qibla.QiblaUiState
import kotlin.math.roundToInt

@Composable
fun QiblaInfoSection(
    modifier: Modifier = Modifier,
    uiState: QiblaUiState, // QiblaViewModel'den gelen state
    locationName: String? // Konum bilgisini de alalım
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        // 1. Location
        locationName?.let {
            InfoRow(title = "Konum", value = it)
        }

        // 2. Direction
        InfoRow(title = "Yön", value = "${uiState.qiblaBearing.roundToInt()}° Kuzey")

        // 3. Measurement
        val accuracyText = when(uiState.sensorAccuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "Yüksek"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Orta"
            else -> "Düşük"
        }
        InfoRow(title = "Ölçüm", value = accuracyText)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
