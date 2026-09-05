package com.kutluoglu.prayer_feature.qibla

import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.qibla.components.QiblaLayout
import com.kutluoglu.prayer_feature.qibla.components.qiblaLayoutStrategy
import com.kutluoglu.prayer_feature.qibla.util.findActivity

@Composable
fun QiblaScreen(
    uiState: QiblaUiState,
    onEvent: (QiblaEvent) -> Unit
) {
    val activity = LocalContext.current.findActivity()

    LaunchedEffect(Unit) {
        onEvent(QiblaEvent.OnStart)
    }

    DisposableEffect(Unit) {
        onDispose {
            onEvent(QiblaEvent.OnStop)
        }
    }

    SideEffect {
        activity?.requestedOrientation = if (uiState.lockPortrait) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(uiState.lockPortrait) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
                    compassAutoRotate = uiState.compassAutoRotate,
                    sensorAccuracy = uiState.sensorAccuracy,
                    locationName = uiState.locationName
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val buttonColors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            )
            FilledTonalIconButton(
                onClick = { onEvent(QiblaEvent.ToggleLockPortrait) },
                colors = buttonColors
            ) {
                Icon(
                    imageVector = Icons.Default.ScreenRotation,
                    contentDescription = stringResource(R.string.qibla_lock_portrait),
                    tint = if (uiState.lockPortrait)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalIconButton(
                onClick = { onEvent(QiblaEvent.ToggleCompassAutoRotate) },
                colors = buttonColors
            ) {
                Icon(
                    imageVector = Icons.Filled.CompassCalibration,
                    contentDescription = stringResource(R.string.qibla_auto_rotate_compass),
                    tint = if (uiState.compassAutoRotate)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
