package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        verseFormatter: QuranVerseFormatter,
        onLoadQuranVerse: () -> Unit = {}
) {
    if (uiState is HomeUiState.Success) {
        val quranVerse = uiState.data.quranVerse
        if (quranVerse != null) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                val context = LocalContext.current
                val localizedSurahName = verseFormatter.getLocalizedNameOf(
                    quranVerse = quranVerse,
                    context = context
                    )
                val verseInfo = "($localizedSurahName - ${quranVerse})"

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "\"${quranVerse.text}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondary,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                // The surah name and verse number
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = verseInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
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
