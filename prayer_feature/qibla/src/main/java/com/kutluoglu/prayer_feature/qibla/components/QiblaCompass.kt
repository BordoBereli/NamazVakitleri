package com.kutluoglu.prayer_feature.qibla.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.kutluoglu.core.common.utils.AngleUtils
import com.kutluoglu.prayer_feature.qibla.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

const val QIBLA_ALIGNMENT_THRESHOLD = 10f

@Composable
fun QiblaCompass(
    deviceAzimuth: Float,
    qiblaAngle: Float,
    qiblaBearing: Double,
    compassAutoRotate: Boolean,
    sensorAccuracy: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    var hasVibrated by remember { mutableStateOf(false) }

    val angleDifference = abs(AngleUtils.normalizeDegrees(qiblaAngle))
    val isAligned = angleDifference <= QIBLA_ALIGNMENT_THRESHOLD

    val arrowColor by animateColorAsState(
        targetValue = if (isAligned) Color(0xFF1E7E34) else Color(0xFFB8860B),
        animationSpec = tween(durationMillis = 500),
        label = "arrow_color"
    )

    val arrowScale by animateFloatAsState(
        targetValue = if (isAligned) 1.1f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "arrow_scale"
    )

    // Yön hizalandığında titreşim efekti uygula
    LaunchedEffect(isAligned) {
        if (isAligned && !hasVibrated) {
            vibrator?.let { v ->
                val vibrationDuration = 200L
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(
                        VibrationEffect.createOneShot(
                            vibrationDuration,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(vibrationDuration)
                }
                hasVibrated = true
            }
        } else if (!isAligned) {
            hasVibrated = false
        }
    }

    Box(
        modifier = modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        AccuracyRing(sensorAccuracy, Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFDF6E3),
                            Color(0xFFF0E6C8),
                            Color(0xFFE4D5A8)
                        )
                    )
                )
        )

        // Dial drawn once, rotated on the GPU
        val dialRotation = if (compassAutoRotate) -deviceAzimuth else 0f
        Canvas(
            modifier = Modifier
                .fillMaxSize(0.88f)
                .graphicsLayer { rotationZ = dialRotation }
        ) {
            drawCompassDial()
        }

        val arrowRotation = if (compassAutoRotate) qiblaAngle else qiblaBearing.toFloat()
        Icon(
            painter = painterResource(id = R.drawable.ic_qibla_arrow),
            contentDescription = stringResource(R.string.qibla_compass_arrow),
            modifier = Modifier
                .fillMaxSize(0.75f)
                .graphicsLayer { rotationZ = arrowRotation }
                .scale(arrowScale),
            tint = arrowColor
        )

        Icon(
            painter = painterResource(id = R.drawable.ic_kaaba),
            contentDescription = stringResource(R.string.qibla_kaaba),
            modifier = Modifier.size(44.dp),
            tint = Color.Unspecified
        )
    }
}

@Composable
private fun AccuracyRing(sensorAccuracy: Int, modifier: Modifier = Modifier) {
    val level = accuracyLevel(sensorAccuracy)
    val color = when (level) {
        AccuracyLevel.HIGH -> Color(0xFF1E7E34)
        AccuracyLevel.MEDIUM -> Color(0xFFB26A00)
        AccuracyLevel.LOW -> Color(0xFFB3261E)
    }
    val isLow = level == AccuracyLevel.LOW
    val rotation: Float = if (isLow) {
        val transition = rememberInfiniteTransition(label = "ring_rotation")
        val animatedRotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ring_rotation_value"
        )
        animatedRotation
    } else {
        0f
    }

    Canvas(
        modifier = modifier.graphicsLayer { rotationZ = rotation }
    ) {
        val strokeWidth = 6.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val style = if (isLow) {
            Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20.dp.toPx(), 16.dp.toPx()))
            )
        } else {
            Stroke(width = strokeWidth)
        }
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = style
        )
    }
}

private fun DrawScope.drawCompassDial() {
    val radius = size.minDimension / 2
    val center = this.center

    val majorColor = Color(0xFFB8860B)
    val minorColor = Color(0xFFC9A227).copy(alpha = 0.6f)
    val northColor = Color(0xFFB3261E)

    val textPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        textSize = 16.sp.toPx()
        color = majorColor.toArgb()
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD
        )
    }

    (0 until 360 step 10).forEach { angle ->
        val isMajorLine = angle % 90 == 0
        val isMediumLine = angle % 30 == 0

        val lineLength = when {
            isMajorLine -> 28.dp.toPx()
            isMediumLine -> 18.dp.toPx()
            else -> 12.dp.toPx()
        }
        val strokeWidth = when {
            isMajorLine -> 3.dp.toPx()
            isMediumLine -> 2.dp.toPx()
            else -> 1.dp.toPx()
        }
        val color = if (isMajorLine) majorColor else minorColor

        val angleInRad = Math.toRadians(angle.toDouble() - 90)
        val lineStart = Offset(
            x = center.x + (radius - lineLength) * cos(angleInRad).toFloat(),
            y = center.y + (radius - lineLength) * sin(angleInRad).toFloat()
        )
        val lineEnd = Offset(
            x = center.x + radius * cos(angleInRad).toFloat(),
            y = center.y + radius * sin(angleInRad).toFloat()
        )
        drawLine(
            color = color,
            start = lineStart,
            end = lineEnd,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        if (isMajorLine) {
            val text = when (angle) {
                0 -> "K"   // Kuzey (North)
                90 -> "D"  // Doğu (East)
                180 -> "G" // Güney (South)
                270 -> "B" // Batı (West)
                else -> ""
            }
            val textRadius = radius - lineLength - 14.dp.toPx()
            val textX = center.x + textRadius * cos(angleInRad).toFloat()
            val textBounds = android.graphics.Rect()
            textPaint.getTextBounds(text, 0, text.length, textBounds)
            val textY = center.y + textRadius * sin(angleInRad).toFloat() + textBounds.height() / 2f

            if (angle == 0) {
                val northPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 18.sp.toPx()
                    this.color = northColor.toArgb()
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD
                    )
                }
                drawContext.canvas.nativeCanvas.drawText(text, textX, textY, northPaint)
            } else {
                drawContext.canvas.nativeCanvas.drawText(text, textX, textY, textPaint)
            }
        }
    }
}
