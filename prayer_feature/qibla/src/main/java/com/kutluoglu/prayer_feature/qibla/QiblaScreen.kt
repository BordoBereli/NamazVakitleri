package com.kutluoglu.prayer_feature.qibla

import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kutluoglu.prayer_feature.common.components.TopContainer
import com.kutluoglu.prayer_feature.qibla.components.QiblaCompass
import com.kutluoglu.prayer_feature.qibla.components.QiblaInfoSection
import kotlinx.coroutines.delay

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
        TopContainer(
            modifier = Modifier.weight(0.4f),
            painter = painterResource(id = R.drawable.ic_kaaba),
            stringResourceId = R.string.qibla_page_title
        ) {
            QiblaInfoSection(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                uiState = uiState,
                locationName = locationName
            )
        }
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (uiState.error != null) {
                Text("Hata: Konum alınamadı. Lütfen konum servislerinizi kontrol edin...${uiState.error}")
            } else if (!uiState.isLocationAvailable) {
                Text("Konum bekleniyor...")
            } else {
                if (uiState.sensorAccuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        text = "Lütfen telefonunuzu 8 şeklinde sallayarak pusulayı kalibre edin."
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                QiblaCompass(
                    deviceAzimuth = uiState.deviceAzimuth,
                    qiblaAngle = uiState.qiblaAngle
                )
            }
        }
    }
}
