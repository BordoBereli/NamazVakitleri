package com.kutluoglu.prayer_feature.qibla.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.qibla.R

@Composable
fun TurnPill(qiblaAngle: Float, modifier: Modifier = Modifier) {
    val label = qiblaDistanceLabel(qiblaAngle, QIBLA_ALIGNMENT_THRESHOLD)
    val isAligned = label is QiblaDistanceLabel.Aligned
    val container = if (isAligned) Color(0xFF1E7E34) else Color(0xFFB8860B)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = container
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (label) {
                is QiblaDistanceLabel.Aligned -> {
                    Text(
                        text = stringResource(R.string.qibla_aligned_pill),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                is QiblaDistanceLabel.Turn -> {
                    Text(
                        text = "${label.degrees}°",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(
                            if (label.direction == TurnDirection.RIGHT) {
                                R.string.qibla_turn_right_pill
                            } else {
                                R.string.qibla_turn_left_pill
                            }
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
