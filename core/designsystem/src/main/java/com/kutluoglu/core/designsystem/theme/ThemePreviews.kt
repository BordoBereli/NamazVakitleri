package com.kutluoglu.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
private fun ThemeSampleContent() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Namaz Vakitleri",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Theme spot-check preview",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ColorSwatch(label = "Primary", color = MaterialTheme.colorScheme.primary)
                ColorSwatch(label = "Secondary", color = MaterialTheme.colorScheme.secondary)
                ColorSwatch(label = "Background", color = MaterialTheme.colorScheme.background)
            }
        }
    }
}

@Composable
private fun ColorSwatch(label: String, color: Color) {
    Column {
        Spacer(
            modifier = Modifier
                .size(48.dp)
                .background(color = color, shape = RoundedCornerShape(8.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, name = "Theme - Light")
@Composable
private fun NamazVakitleriThemeLightPreview() {
    NamazVakitleriTheme(darkTheme = false) {
        ThemeSampleContent()
    }
}

@Preview(showBackground = true, name = "Theme - Dark")
@Composable
private fun NamazVakitleriThemeDarkPreview() {
    NamazVakitleriTheme(darkTheme = true) {
        ThemeSampleContent()
    }
}
