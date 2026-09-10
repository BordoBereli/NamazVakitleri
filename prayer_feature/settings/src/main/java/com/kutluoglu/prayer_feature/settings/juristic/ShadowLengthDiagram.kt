package com.kutluoglu.prayer_feature.settings.juristic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp

private val SunColor = Color(0xFFF2B134)
private val StickColor = Color(0xFF6B7280)
private val GroundColor = Color(0xFF6B7280)
private val ShadowColor = Color(0xB32F9E44)

@Composable
fun ShadowLengthDiagram(
    shadowFactor: Float,
    angleLabel: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            val stickHeight = size.height * 0.62f
            val groundY = size.height * 0.88f
            val stickWidth = (size.width * 0.02f).coerceIn(3f, 6f)
            val stickLeft = size.width * 0.45f
            val stickRight = stickLeft + stickWidth

            drawLine(
                color = GroundColor,
                start = Offset(0f, groundY),
                end = Offset(size.width, groundY),
                strokeWidth = 2f
            )

            drawRoundRect(
                color = StickColor,
                topLeft = Offset(stickLeft, groundY - stickHeight),
                size = Size(stickWidth, stickHeight),
                cornerRadius = CornerRadius(stickWidth / 2f, stickWidth / 2f)
            )

            val shadowHeight = 4f
            drawRoundRect(
                color = ShadowColor,
                topLeft = Offset(stickRight, groundY - shadowHeight),
                size = Size(stickHeight * shadowFactor, shadowHeight),
                cornerRadius = CornerRadius(shadowHeight / 2f, shadowHeight / 2f)
            )

            val sunRadius = 7.dp.toPx()
            val sunCenter = if (shadowFactor <= 1f) {
                Offset(size.width * 0.08f, size.height * 0.10f)
            } else {
                Offset(size.width * 0.06f, size.height * 0.30f)
            }
            drawCircle(
                color = SunColor,
                radius = sunRadius,
                center = sunCenter
            )

            val stickTop = Offset(stickLeft + stickWidth / 2f, groundY - stickHeight)
            drawLine(
                color = SunColor,
                start = Offset(sunCenter.x + sunRadius, sunCenter.y + sunRadius),
                end = stickTop,
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
        }

        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = angleLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
                )
            }
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
