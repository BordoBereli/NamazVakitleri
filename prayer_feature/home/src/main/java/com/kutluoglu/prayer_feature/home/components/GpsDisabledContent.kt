package com.kutluoglu.prayer_feature.home.components

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.home.R

@Composable
fun GpsDisabledContent(
    resolution: PendingIntent?,
    onRetry: () -> Unit,
    onChooseLocation: () -> Unit
) {
    val context = LocalContext.current
    val resolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) onRetry()
    }
    val openLocationSettings = {
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.gps_disabled_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Button(onClick = {
                val sender = resolution?.intentSender
                if (sender != null) {
                    resolutionLauncher.launch(IntentSenderRequest.Builder(sender).build())
                } else {
                    openLocationSettings()
                }
            }) {
                Text(stringResource(R.string.enable_location))
            }
            OutlinedButton(onClick = openLocationSettings) {
                Text(stringResource(R.string.open_location_settings))
            }
            OutlinedButton(onClick = onChooseLocation) {
                Text(stringResource(R.string.choose_location))
            }
        }
    }
}
