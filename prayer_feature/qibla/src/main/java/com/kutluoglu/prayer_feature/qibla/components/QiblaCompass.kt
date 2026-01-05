package com.kutluoglu.prayer_feature.qibla.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.kutluoglu.prayer_feature.qibla.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("ObsoleteSdkInt")
@Composable
fun QiblaCompass(
        deviceAzimuth: Float,
        qiblaAngle: Float,
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

    val acceptableDifference by remember { mutableFloatStateOf(10.0f) }

    val compassRotation by animateFloatAsState(
        targetValue = -deviceAzimuth,
        animationSpec = tween(durationMillis = 300),
        label = "compass_rotation"
    )

    val qiblaIndicatorRotation by animateFloatAsState(
        targetValue = qiblaAngle,
        animationSpec = tween(durationMillis = 300),
        label = "qibla_indicator_rotation"
    )

    val angleDifference = abs(qiblaAngle)
    val isAligned = angleDifference < acceptableDifference

    val arrowColor by animateColorAsState(
        targetValue = if (isAligned) Color.Green else MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 500),
        label = "arrow_color"
    )

    val arrowScale by animateFloatAsState(
        targetValue = if (isAligned) {
            0.8f + (1 - (angleDifference / acceptableDifference)) * 0.5f
        } else 0.8f,
        animationSpec = tween(durationMillis = 500),
        label = "arrow_scale"
    )

    // Yön hizalandığında titreşim efekti uygula
    LaunchedEffect(isAligned) {
        if (isAligned && !hasVibrated) {
            vibrator?.let { v ->
                val vibrationDuration = 200L
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(vibrationDuration, VibrationEffect.DEFAULT_AMPLITUDE))
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
        modifier = modifier
            .size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        val textColor = MaterialTheme.colorScheme.surface
        val majorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(compassRotation)
        ) {

            drawCompassDial(textColor, majorColor)
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_qibla_arrow),
            contentDescription = "Qibla Direction Arrow",
            modifier = Modifier
                .fillMaxSize(0.75f)
                .rotate(qiblaIndicatorRotation)
                .scale(arrowScale),
            tint = arrowColor
        )

        Icon(
            painter = painterResource(id = R.drawable.ic_kaaba),
            contentDescription = "Kaaba",
            modifier = Modifier.size(40.dp),
            tint = Color.Unspecified
        )
    }
}


private fun DrawScope.drawCompassDial(textColor: Color, majorColor: Color) {

    val radius = size.minDimension / 2
    val center = this.center

    val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 18.sp.toPx() // Harfleri biraz daha belirgin yapalım
        color = majorColor.toArgb()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    (0 until 360 step 10).forEach { angle ->
        val isMajorLine = angle % 90 == 0
        val isMediumLine = angle % 30 == 0

        val lineLength = when {
            isMajorLine -> 30.dp.toPx()
            isMediumLine -> 20.dp.toPx()
            else -> 15.dp.toPx()
        }
        val strokeWidth = when {
            isMajorLine -> 3.dp.toPx()
            isMediumLine -> 2.dp.toPx()
            else -> 1.dp.toPx()
        }

        // Ana yön çizgilerini daha belirgin yapalım
        val color = if (isMajorLine) majorColor else Color.Gray

        // --- Açıyı -90 derece kaydırıyoruz ---
        // Canvas'ta 0 derece sağ tarafı (Doğu), 270 derece üst tarafı (Kuzey) gösterir.
        // Bu yüzden tüm hesaplamaları 90 derece sola kaydırarak 0'ın üste gelmesini sağlıyoruz.
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

        // --- Ana Yön Harflerini Çiz (N, E, S, W) ---
        if (isMajorLine) {
            val text = when (angle) {
                0 -> "K"   // Kuzey (North)
                90 -> "D"  // Doğu (East)
                180 -> "G" // Güney (South)
                270 -> "B" // Batı (West)
                else -> ""
            }

            // Metni çizgilerin biraz içine yerleştir
            val textRadius = radius - lineLength - 15.dp.toPx()
            val textX = center.x + textRadius * cos(angleInRad).toFloat()
            // Metnin dikey olarak merkezlenmesi için boyutu ve pozisyonu ayarla
            val textBounds = android.graphics.Rect()
            textPaint.getTextBounds(text, 0, text.length, textBounds)
            val textY = center.y + textRadius * sin(angleInRad).toFloat() + textBounds.height() / 2f

            drawContext.canvas.nativeCanvas.drawText(
                text,
                textX,
                textY,
                textPaint
            )
        }
    }

    // Kuzeyi belirten kırmızı üçgen (Artık bunu kullanmaya gerek yok, "K" harfi yeterli)
    // Dilerseniz bu bölümü silebilir veya yorum satırı yapabilirsiniz.
    val northAngleRad = Math.toRadians(270.0) // Kuzey 270 derecede
    val triangleBaseRadius = radius - 32.dp.toPx()
    val triangleTipRadius = radius - 20.dp.toPx()

    val p1 = Offset(
        x = center.x + triangleBaseRadius * cos(northAngleRad + 0.05).toFloat(),
        y = center.y + triangleBaseRadius * sin(northAngleRad + 0.05).toFloat()
    )
    val p2 = Offset(
        x = center.x + triangleBaseRadius * cos(northAngleRad - 0.05).toFloat(),
        y = center.y + triangleBaseRadius * sin(northAngleRad - 0.05).toFloat()
    )
    val p3 = Offset(
        x = center.x + triangleTipRadius * cos(northAngleRad).toFloat(),
        y = center.y + triangleTipRadius * sin(northAngleRad).toFloat()
    )

    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        close()
    }
    drawPath(path, color = Color.Red)
}
