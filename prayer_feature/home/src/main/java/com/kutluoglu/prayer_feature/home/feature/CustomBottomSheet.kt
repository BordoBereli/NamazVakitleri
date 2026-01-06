package com.kutluoglu.prayer_feature.home.feature

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CustomBottomSheet(
        isVisible: Boolean,
        onDismiss: () -> Unit,
        content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    // 1. Arka planın görünürlük animasyonu
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(100),
        label = "background_alpha"
    )

    // 2. Sheet'in dikey pozisyonunu (offset) tutan state
    var offsetY by remember { mutableFloatStateOf(0f) }

    // 3. Sheet'in tam yüksekliğini ölçmek için
    var sheetHeight by remember { mutableFloatStateOf(0f) }

    // 4. Offset'i yumuşak bir şekilde animasyona dönüştüren state
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = tween(durationMillis = 100),
        label = "offset_y_animation"
    )

    // 5. `isVisible` değiştiğinde animasyonu tetikle
    LaunchedEffect(isVisible, sheetHeight) {
        if (sheetHeight > 0) { // Sadece yükseklik bilindiğinde çalış
            offsetY = if (isVisible) 0f else sheetHeight
        }
    }

    if (animatedAlpha > 0f) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Karartılmış arka plan
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(animatedAlpha * 0.5f) // Alfa değerini yarıya indirerek daha şeffaf yap
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onDismiss() }
                    )
            )

            // İçeriği taşıyan Card
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { sheetHeight = it.height.toFloat() }
                    .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
                    // 6. Sürükleme mantığı
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    // Eğer sheet'in üçte birinden fazlası sürüklendiyse kapat
                                    if (offsetY > sheetHeight / 3) {
                                        onDismiss()
                                    } else {
                                        // Değilse, geri yerine oturt
                                        offsetY = 0f
                                    }
                                }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            // Sadece aşağı doğru sürüklemeye izin ver ve yeni ofseti hesapla
                            val newOffsetY = (offsetY + dragAmount).coerceAtLeast(0f)
                            offsetY = newOffsetY
                        }
                    },
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .width(32.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    content()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
