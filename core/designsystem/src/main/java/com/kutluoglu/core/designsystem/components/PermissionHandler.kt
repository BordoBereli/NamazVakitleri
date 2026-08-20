package com.kutluoglu.core.designsystem.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kutluoglu.core.designsystem.extensions.REQUIRED_LOCATION_PERMISSIONS
import com.kutluoglu.core.designsystem.R

/**
 * A container composable that handles permission logic.
 * It shows the content only when all permissions are granted.
 * Otherwise, it displays the appropriate UI for requesting permissions,
 * showing a rationale, or guiding the user to settings.
 *
 * @param onPermissionsGranted A lambda that is invoked when permissions are granted.
 * @param onChooseLocation An optional action to let the user pick a location manually
 *   instead of granting the permission (e.g. navigate to the location selection screen).
 * @param canProceedWithoutPermission When true, the content is shown even if the
 *   permission is not granted (e.g. the user already has a manually selected location).
 * @param content The composable content to display when all permissions are granted.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    onPermissionsGranted: () -> Unit,
    onChooseLocation: (() -> Unit)? = null,
    canProceedWithoutPermission: Boolean = false,
    content: @Composable () -> Unit
) {
    // --- START OF THE FIX ---
    // A simple counter state that we will increment to force recomposition.
    var permissionCheckTrigger by remember { mutableIntStateOf(0) }

    // We now pass the trigger as a key to rememberMultiplePermissionsState.
    // When the key changes, the state will be re-created and re-evaluated.
    val permissionState = rememberMultiplePermissionsState(
        permissions = REQUIRED_LOCATION_PERMISSIONS,
        onPermissionsResult = {
            // This is another way to force a check after a result.
            permissionCheckTrigger++
        }
    )
    // --- END OF THE FIX ---

    val lifecycleOwner = LocalLifecycleOwner.current
    // --- FIX STARTS HERE ---
    // This effect observes lifecycle events. When the app resumes, it triggers a
    // re-evaluation of the permissions. This is crucial for when the user comes
    // back from the settings screen.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // When the app resumes, increment the trigger.
                // This forces the permissionState to be re-evaluated.
                permissionCheckTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Effect to notify the caller when permissions are granted.
    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            onPermissionsGranted()
        }
    }

    ShowOf(permissionState, onChooseLocation, canProceedWithoutPermission, content)
}

@Composable
@OptIn(ExperimentalPermissionsApi::class)
private fun ShowOf(
        permissionState: MultiplePermissionsState,
        onChooseLocation: (() -> Unit)?,
        canProceedWithoutPermission: Boolean,
        content: @Composable (() -> Unit)
) {
    when {
        // If all permissions are granted, or the user can proceed without them
        // (e.g. already has a manually selected location), display the main content.
        permissionState.allPermissionsGranted || canProceedWithoutPermission -> {
            content()
        }

        // If rationale should be shown, display the rationale UI.
        permissionState.shouldShowRationale   -> {
            PermissionRationale(
                onRequestPermission = { permissionState.launchMultiplePermissionRequest() },
                onChooseLocation = onChooseLocation
            )
        }

        // Otherwise, it's the first launch or permissions are permanently denied.
        else                                  -> {
            PermissionFirstLaunchOrDenied(permissionState, onChooseLocation)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionFirstLaunchOrDenied(
    permissionState: MultiplePermissionsState,
    onChooseLocation: (() -> Unit)?
) {
    val context = LocalContext.current
    var hasLaunched by remember { mutableStateOf(false) }

    // This effect runs on composition.
    LaunchedEffect(Unit) {
        if (!hasLaunched) {
            permissionState.launchMultiplePermissionRequest()
            hasLaunched = true
        }
    }

    // This UI is shown if the user permanently denies the permission.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.permission_required),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_dialog_info),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                this.data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }) {
            Text(stringResource(R.string.open_settings))
        }
        if (onChooseLocation != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onChooseLocation) {
                Text(stringResource(R.string.choose_location))
            }
        }
    }
}

@Composable
private fun PermissionRationale(
    onRequestPermission: () -> Unit,
    onChooseLocation: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.location_permission_needed),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.permission_grant_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text(stringResource(R.string.grant_permission))
        }
        if (onChooseLocation != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onChooseLocation) {
                Text(stringResource(R.string.choose_location))
            }
        }
    }
}
