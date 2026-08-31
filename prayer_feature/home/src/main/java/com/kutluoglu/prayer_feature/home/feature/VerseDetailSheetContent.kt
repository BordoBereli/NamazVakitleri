package com.kutluoglu.prayer_feature.home.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer_feature.home.R
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.common.shareVerse

@Composable
fun VerseDetailSheetContent(
        verse: AyahData,
        verseFormatter: QuranVerseFormatter,
        isSaved: Boolean = false,
        onToggleSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val localizedSurahName = verseFormatter.getLocalizedNameOf(
        quranVerse = verse,
        context = context
    )
    val verseInfo = "($localizedSurahName - $verse)"

    // Get screen height to calculate max height in Dp
    val screenHeight =
        LocalResources.current.displayMetrics.heightPixels.dp / LocalDensity.current.density

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Set a maximum height. The content will be scrollable if it exceeds this.
            // For short content, the Column will be smaller.
            .heightIn(min = 0.dp, max = screenHeight * 0.65f)
            // Make the entire column scrollable if content overflows
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = verse.text,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Start,
        )
        Spacer(modifier = Modifier.height(16.dp))
        // for the verse info and the action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, // Pushes items to the ends
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = verseInfo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleSaved
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = context.getString(
                            if (isSaved) R.string.unsave_verse else R.string.save_verse
                        )
                    )
                }
                IconButton(
                    onClick = { shareVerse(verse, verseFormatter, context) }
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = context.getString(R.string.share_verse)
                    )
                }
            }
        }
    }
}
