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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.home.HomeUiState
import com.kutluoglu.prayer_feature.home.R
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter

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
                val context = LocalContext.current
                val verseFormatter = QuranVerseFormatter()
                val localizedSurahName = verseFormatter.getLocalizedNameOf(
                    quranVerse = quranVerse,
                    context = context
                    )
                val verseInfo = "($localizedSurahName - ${quranVerse})"

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
                    text = verseInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // A slightly less prominent color
                )
            }
        }
        else { onLoadQuranVerse() }
    }
    else {
        Text(
            text = stringResource(R.string.no_verse_available),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, // A slightly less prominent color
        )
    }
}
