package com.kutluoglu.prayer_feature.settings.background

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.kutluoglu.core.designsystem.components.BackNavigationIcon
import com.kutluoglu.core.designsystem.components.RoundedPageTitleBar
import com.kutluoglu.prayer_feature.common.background.BackgroundBannerMetrics
import com.kutluoglu.prayer_feature.settings.R
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundAdjustRoute(
    locationId: String,
    imageUri: String,
    onNavigateBack: () -> Unit,
    viewModel: BackgroundAdjustViewModel = koinViewModel {
        parametersOf(
            locationId,
            runCatching { android.net.Uri.parse(imageUri) }.getOrNull()
        )
    }
) {
    val state by viewModel.uiState.collectAsState()
    val navigateBack by viewModel.navigateBack.collectAsState()

    LaunchedEffect(navigateBack) {
        if (navigateBack) onNavigateBack()
    }

    Scaffold(
        topBar = {
            RoundedPageTitleBar(
                title = stringResource(R.string.background_adjust),
                navigationIcon = { BackNavigationIcon(onClick = onNavigateBack) }
            )
        }
    ) { padding ->
        BackgroundAdjustContent(
            state = state,
            onEvent = viewModel::onEvent,
            onFrameSizeChanged = { size ->
                viewModel.onEvent(BackgroundAdjustEvent.FrameMeasured(size.toSize()))
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}

@Composable
private fun BackgroundAdjustContent(
    state: BackgroundAdjustUiState,
    onEvent: (BackgroundAdjustEvent) -> Unit,
    onFrameSizeChanged: (androidx.compose.ui.unit.IntSize) -> Unit,
    modifier: Modifier = Modifier
) {
    // Read the newest state inside the long-lived pointerInput coroutine; the
    // composable parameter captured by pointerInput(Unit) never changes.
    val latestState by rememberUpdatedState(state)

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    // Same aspect ratio as the home banner, so the baked image
                    // is shown on home without any additional cropping.
                    .aspectRatio(BackgroundBannerMetrics.aspectRatio)
                    .clip(RoundedCornerShape(16.dp))
                    .onSizeChanged(onFrameSizeChanged)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val current = latestState
                            onEvent(
                                BackgroundAdjustEvent.OnTransform(
                                    scale = current.scale * zoom,
                                    offset = current.offset + pan
                                )
                            )
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { onEvent(BackgroundAdjustEvent.OnDoubleTap) }
                        )
                    },
                // TopStart so the image's top-left corner sits exactly at
                // (offset.x, offset.y), matching CropMath's offset convention.
                contentAlignment = Alignment.TopStart
            ) {
                if (state.uri != null) {
                    // Draw the decoded bitmap directly with an explicit dstSize
                    // equal to the Canvas node's pixel size (frame width x
                    // width / aspect). No image-loader painter in between, so
                    // the drawn geometry is deterministic and always matches
                    // CropMath's displayedImageSize convention.
                    val imageAspectRatio = state.imageAspectRatio
                        .takeIf { it > 0f }
                        ?: BackgroundBannerMetrics.aspectRatio
                    val bitmap = state.previewBitmap
                    if (bitmap != null && !bitmap.isRecycled) {
                        BoxWithConstraints {
                            Canvas(
                                modifier = Modifier
                                    .requiredSize(maxWidth, maxWidth / imageAspectRatio)
                                    .graphicsLayer {
                                        // Scale around the top-left corner so the
                                        // top-left corner stays at (offset.x,
                                        // offset.y) at any zoom.
                                        transformOrigin = TransformOrigin(0f, 0f)
                                        scaleX = state.scale
                                        scaleY = state.scale
                                        translationX = state.offset.x
                                        translationY = state.offset.y
                                    }
                            ) {
                                drawImage(
                                    image = bitmap.asImageBitmap(),
                                    // DrawScope size == the node's pixel size
                                    // (requiredSize above); using it keeps the
                                    // drawn image exactly node-sized.
                                    dstSize = IntSize(
                                        size.width.roundToInt(),
                                        size.height.roundToInt()
                                    ),
                                    dstOffset = IntOffset.Zero,
                                    filterQuality = FilterQuality.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.background_adjust_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = { onEvent(BackgroundAdjustEvent.Confirm) },
            enabled = !state.isSaving && state.uri != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    strokeWidth = 2.dp
                )
            }
            Text(text = stringResource(R.string.confirm))
        }
    }
}
