package com.kutluoglu.prayer_feature.qibla.components

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.qibla.R // Bu R dosyasını qibla modülünüzde oluşturmanız gerekebilir
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaCompassOld(
    deviceAzimuth: Float,
    qiblaAngle: Float,
    modifier: Modifier = Modifier
) {
    // Cihazın gerçek kuzeyi göstermesi için pusula kadranını döndür.
    // Animasyon, dönüşlerin daha yumuşak olmasını sağlar.
    val compassRotation by animateFloatAsState(
        targetValue = -deviceAzimuth,
        animationSpec = tween(durationMillis = 300),
        label = "compass_rotation"
    )

    // Kıble ibresinin, pusula kadranı üzerindeki doğru yönü göstermesi için
    // cihazın kuzeye göre dönüşünü de hesaba katarak döndür.
    val qiblaIndicatorRotation by animateFloatAsState(
        targetValue = qiblaAngle - deviceAzimuth,
        animationSpec = tween(durationMillis = 300),
        label = "qibla_indicator_rotation"
    )

    Box(
        modifier = modifier
            .size(300.dp), // Pusula için sabit bir boyut belirleyelim
        contentAlignment = Alignment.Center
    ) {
        // 1. Pusula Kadranı (Arka Plan)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(compassRotation) // Cihaz döndükçe pusula kadranı döner
        ) {
            drawCompassDial()
        }

        // 2. Kıble Yönünü Gösteren İbre
        // Bu ikon, doğru kıble açısını göstermek için dönecektir.
        Icon(
            // Projenize `ic_qibla_arrow.xml` adında bir vektör ikonu eklemeniz gerekecek.
            painter = painterResource(id = R.drawable.ic_qibla_arrow),
            contentDescription = "Qibla Direction Arrow",
            modifier = Modifier
                .fillMaxSize(0.7f) // İbreyi kadranın içine sığdır
                .rotate(qiblaIndicatorRotation),
            tint = MaterialTheme.colorScheme.primary // Temanızın ana rengini kullan
        )

        // 3. Ortadaki Kabe ikonu (Sabit)
        // Bu ikon, pusulanın merkezinde sabit durur.
        Icon(
            // Projenize `ic_kaaba.xml` adında bir vektör ikonu eklemeniz gerekecek.
            painter = painterResource(id = R.drawable.ic_kaaba),
            contentDescription = "Kaaba",
            modifier = Modifier.size(48.dp),
            tint = Color.Unspecified // İkonun kendi renklerini kullanması için
        )
    }
}

private fun DrawScope.drawCompassDial() {
    val radius = size.minDimension / 2
    val center = this.center

    // Ana yönler (Kuzey, Doğu, Güney, Batı)
    (0..360 step 90).forEach { angle ->
        val lineLength = 30.dp.toPx()
        val lineStart = Offset(
            x = center.x + (radius - lineLength) * cos(Math.toRadians(angle.toDouble())).toFloat(),
            y = center.y + (radius - lineLength) * sin(Math.toRadians(angle.toDouble())).toFloat()
        )
        val lineEnd = Offset(
            x = center.x + radius * cos(Math.toRadians(angle.toDouble())).toFloat(),
            y = center.y + radius * sin(Math.toRadians(angle.toDouble())).toFloat()
        )
        drawLine(
            color = Color.Gray,
            start = lineStart,
            end = lineEnd,
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Ara yönler (Her 10 derecede bir)
    (0..360 step 10).forEach { angle ->
        val lineLength = if (angle % 30 == 0) 20.dp.toPx() else 15.dp.toPx()
        val strokeWidth = if (angle % 30 == 0) 2.dp.toPx() else 1.dp.toPx()

        val lineStart = Offset(
            x = center.x + (radius - lineLength) * cos(Math.toRadians(angle.toDouble())).toFloat(),
            y = center.y + (radius - lineLength) * sin(Math.toRadians(angle.toDouble())).toFloat()
        )
        val lineEnd = Offset(
            x = center.x + radius * cos(Math.toRadians(angle.toDouble())).toFloat(),
            y = center.y + radius * sin(Math.toRadians(angle.toDouble())).toFloat()
        )
        drawLine(
            color = Color.LightGray,
            start = lineStart,
            end = lineEnd,
            strokeWidth = strokeWidth
        )
    }

    // Kuzeyi belirtmek için kırmızı bir çizgi
    val northLineStart = Offset(
        x = center.x + (radius - 35.dp.toPx()) * cos(Math.toRadians(270.0)).toFloat(),
        y = center.y + (radius - 35.dp.toPx()) * sin(Math.toRadians(270.0)).toFloat()
    )
    val northLineEnd = Offset(
        x = center.x + radius * cos(Math.toRadians(270.0)).toFloat(),
        y = center.y + radius * sin(Math.toRadians(270.0)).toFloat()
    )
    drawLine(
        color = Color.Red,
        start = northLineStart,
        end = northLineEnd,
        strokeWidth = 8.dp.toPx(),
        cap = StrokeCap.Round
    )
}
