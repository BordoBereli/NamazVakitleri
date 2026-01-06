package com.kutluoglu.prayer_feature.home.feature

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CustomBottomSheet(
        isVisible: Boolean,
        onDismiss: () -> Unit,
        content: @Composable () -> Unit
) {
    // animate*AsState, görünürlük değişimini yumuşak bir animasyonla yapmamızı sağlar.
    val animatedVisibility by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "custom_bs_visibility"
    )

    if (animatedVisibility > 0f) {
        // Hem karartılmış arka planı hem de Card'ı içeren bir ana Box.
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Karartılmış arka plan
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f * animatedVisibility))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // Tıklama efektini kaldır
                    ) { onDismiss() }
            )

            // Card artık bu dış Box'ın içinde hizalanabilir.
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    // offset ile aşağıdan yukarıya kayma animasyonu
                    .offset(y = (300).dp * (1 - animatedVisibility)), // Kayma mesafesini dp ile çarp
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Tutma Çubuğu (Drag Handle / Grabber)
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .width(32.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                    )

                    Spacer(modifier = Modifier.height(16.dp)) // Tutma çubuğu ile içerik arasına boşluk
                    content()
                    Spacer(modifier = Modifier.height(8.dp)) // İçeriğin altında boşluk
                }
            }
        }
    }
}