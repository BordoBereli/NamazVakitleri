package com.kutluoglu.app_update.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kutluoglu.app_update.R
import com.kutluoglu.app_update.domain.model.UpdateInfo

@Composable
fun ForceUpdateDialog(
    info: UpdateInfo,
    urlOpenFailed: Boolean,
    onUpdateClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.update_required_title)) },
        text = {
            Column {
                Text(stringResource(R.string.update_required_message))
                if (info.releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(info.releaseNotes)
                }
                if (urlOpenFailed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.update_open_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdateClick) {
                Text(stringResource(R.string.update_now))
            }
        },
    )
}

@Composable
fun OptionalUpdateDialog(
    info: UpdateInfo,
    onUpdateClick: () -> Unit,
    onLaterClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLaterClick,
        title = { Text(stringResource(R.string.update_available_title)) },
        text = {
            Column {
                Text(stringResource(R.string.update_available_message))
                if (info.releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(info.releaseNotes)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdateClick) {
                Text(stringResource(R.string.update_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onLaterClick) {
                Text(stringResource(R.string.later))
            }
        },
    )
}
