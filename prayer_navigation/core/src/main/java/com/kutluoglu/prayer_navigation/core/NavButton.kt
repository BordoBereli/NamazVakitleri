package com.kutluoglu.prayer_navigation.core

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp

@Composable
fun RowScope.NavButton(
    modifier: Modifier = Modifier,
    destination: Destination,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Determine colors based on selection state
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent

    // Define the border
    val border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null

    // Use a Surface for background, border, and shape control
    Surface(
        modifier = modifier
            .weight(1f) // Each button will take equal width
            .fillMaxHeight()
            .clickable(
                onClick = onClick,
                // Remove the ripple effect for a cleaner look, optional
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp), // Rounded corners for the "button"
        border = border // Apply the border here
    ) {
        Column(
            modifier = Modifier.fillMaxSize(), // Center content within the Surface
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(destination.iconDrawable),
                contentDescription = destination.contentDescription,
                tint = contentColor // Use the dynamic color
            )
            Text(
                text = destination.label,
                color = contentColor, // Use the dynamic color
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}
