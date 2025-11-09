package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.home.HomeEvent
import com.kutluoglu.prayer_feature.home.HomeUiState

@Composable
fun BottomContainer(
        uiState: HomeUiState,
        onLoadQuranVerse: () -> Unit = {}
) {
    if (uiState is HomeUiState.Success) {
        val quranVerse = uiState.data.quranVerse
        // 2. Display the verse if it's not null
        if (quranVerse != null) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // The verse text
                Text(
                    text = "\"${quranVerse.text}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondary, // Use onSecondary for text on the container
                    textAlign = TextAlign.Start,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                // The surah name and verse number
                Text(
                    text = "(${quranVerse.surah.englishName} - ${quranVerse.surah.number}:${quranVerse.numberInSurah})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // A slightly less prominent color
                )
            }
        }
        else { onLoadQuranVerse() }
    }
    else {
        Text(
            text = "(No verse available.)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, // A slightly less prominent color
        )
    }
}
