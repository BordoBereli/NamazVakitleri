package com.kutluoglu.prayer_feature.qibla

import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.qibla.components.QiblaCompass

@Composable
fun QiblaScreen(
        uiState: QiblaUiState
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (uiState.error != null) {
            Text("Hata: Konum alınamadı. Lütfen konum servislerinizi kontrol edin.")
        } else if (!uiState.isLocationAvailable) {
            Text("Konum bekleniyor...")
        } else {
            // Sensör doğruluğu düşükse kullanıcıyı uyar
            if (uiState.sensorAccuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
                Text("Lütfen telefonunuzu 8 şeklinde sallayarak pusulayı kalibre edin.")
                Spacer(modifier = Modifier.height(16.dp))
            }
            QiblaCompass(
                deviceAzimuth = uiState.deviceAzimuth,
                qiblaAngle = uiState.qiblaAngle
            )
        }
    }
}
