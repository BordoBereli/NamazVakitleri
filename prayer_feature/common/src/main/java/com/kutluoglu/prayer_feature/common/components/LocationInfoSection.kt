package com.kutluoglu.prayer_feature.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.R

@Composable
fun LocationInfoSection(
        modifier: Modifier = Modifier,
        locationState: LocationUiState,
        textColor: Color = MaterialTheme.colorScheme.primary,
        textSize: TextUnit = 16.sp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.konum),
            contentDescription = "",//stringResource(id = R.string.home_page_fallback),
            tint = Color.Unspecified
        )
        Text(
            text = locationState.locationInfoText,
            fontSize = textSize,
            color = textColor
        )
    }
}
