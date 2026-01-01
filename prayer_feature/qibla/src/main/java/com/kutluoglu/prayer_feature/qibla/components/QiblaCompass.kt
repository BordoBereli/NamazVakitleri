package com.kutluoglu.prayer_feature.qibla.components

import android.R.attr.textColor
import android.graphics.Paint
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kutluoglu.prayer_feature.qibla.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaCompass(
    deviceAzimuth: Float,
    qiblaAngle: Float,
    modifier: Modifier = Modifier
) {
    val compassRotation by animateFloatAsState(
        targetValue = -deviceAzimuth,
        animationSpec = tween(durationMillis = 300),
        label = "compass_rotation"
    )

    // Cihazın kıbleye göre ne kadar döndüğünü hesaplar.
    // Bu değer 0'a yaklaştığında, cihaz kıbleye dönük demektir.
    val qiblaIndicatorRotation by animateFloatAsState(
        targetValue = qiblaAngle,
        animationSpec = tween(durationMillis = 300),
        label = "qibla_indicator_rotation"
    )

    // --- YENİ DEĞİŞİKLİK ---
    // Cihazın yönü ile kıblenin yönü arasındaki farkı hesapla.
    // abs() ile mutlak değerini alıyoruz, böylece her iki yönden de ne kadar yakın olduğunu anlarız.
    val angleDifference = abs(qiblaAngle)
    val isAligned = angleDifference < 5.0f // 5 derecelik bir tolerans payı bırakalım.

    // Yön bulunduğunda okun rengini değiştir.
    val arrowColor by animateColorAsState(
        targetValue = if (isAligned) Color.Green else MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 500),
        label = "arrow_color"
    )

    // Yön bulunduğunda oku hafifçe büyüt.
    val arrowScale by animateFloatAsState(
        targetValue = if (isAligned) 1.1f else 1.0f,
        animationSpec = tween(durationMillis = 500),
        label = "arrow_scale"
    )
    // --- DEĞİŞİKLİK SONU ---

    Box(
        modifier = modifier
            .size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        val textColor = MaterialTheme.colorScheme.surface
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(compassRotation) // Kadran, cihazın dönüşüne göre döner
        ) {
            drawCompassDial(textColor)
        }

        // Kıble Yönünü Gösteren İbre
        Icon(
            painter = painterResource(id = R.drawable.ic_qibla_arrow),
            contentDescription = "Qibla Direction Arrow",
            modifier = Modifier
                .fillMaxSize(0.7f)
                // Cihazın kendisi değil, pusula kadranı üzerindeki yönü göstermesi için
                // 'qiblaIndicatorRotation' kullanılır.
                .rotate(qiblaIndicatorRotation)
                .scale(arrowScale), // Yön bulunduğunda büyütme efekti uygula
            tint = arrowColor // Rengi dinamik olarak değiştir
        )

        // Ortadaki Kabe ikonu (Sabit)
        Icon(
            painter = painterResource(id = R.drawable.ic_kaaba),
            contentDescription = "Kaaba",
            modifier = Modifier.size(48.dp),
            tint = Color.Unspecified
        )
    }
}


private fun DrawScope.drawCompassDial(textColor: Color) {
    val radius = size.minDimension / 2
    val center = this.center

    // Metin çizimi için Paint nesnesi oluştur
    val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 16.sp.toPx()
        color = textColor.hashCode()
        textAlign = Paint.Align.CENTER
    }

    // Derece çizgilerini çiz
    (0 until 360 step 10).forEach { angle ->
        val isMajorLine = angle % 90 == 0 // Ana yönler (N, E, S, W)
        val isMediumLine = angle % 30 == 0 // Her 30 derece

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
        val color = if (isMajorLine) textColor else Color.Gray

        val angleInRad = Math.toRadians(angle.toDouble())
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

        // Ana Yön Harflerini Çiz (N, E, S, W)
        if (isMajorLine) {
            val text = when (angle) {
                0 -> "E"   // 0 derece Doğu'yu gösterir
                90 -> "S"  // 90 derece Güney'i
                180 -> "W" // 180 derece Batı'yı
                270 -> "N" // 270 derece Kuzey'i
                else -> ""
            }
            // Metni çizgilerin biraz içine yerleştir
            val textRadius = radius - lineLength - 12.dp.toPx()
            val textX = center.x + textRadius * cos(angleInRad).toFloat()
            val textY = center.y + textRadius * sin(angleInRad).toFloat() + 8.dp.toPx() // Dikey hizalama için ayar

            drawContext.canvas.nativeCanvas.drawText(
                text,
                textX,
                textY,
                textPaint
            )
        }
    }

    // Kuzeyi belirten kırmızı üçgen
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
