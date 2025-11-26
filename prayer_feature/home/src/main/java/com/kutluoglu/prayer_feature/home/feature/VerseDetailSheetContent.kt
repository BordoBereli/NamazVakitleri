package com.kutluoglu.prayer_feature.home.feature

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer_feature.home.R
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter

@Composable
fun VerseDetailSheetContent(
        verse: AyahData,
        verseFormatter: QuranVerseFormatter,
) {
    val context = LocalContext.current
    val localizedSurahName = verseFormatter.getLocalizedNameOf(
        quranVerse = verse,
        context = context
    )
    val verseInfo = "($localizedSurahName - $verse)"
    val fullTextToShare = "\"${verse.text}\" - $verseInfo"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Main verse text at the top
        Box(
            modifier = Modifier
                .heightIn(max = 200.dp) // Set a maximum height for the scrollable area
                .verticalScroll(rememberScrollState()) // Make the Box scrollable
        ) {
            // 2. Put the Text inside the Box without maxLines.
            //    The Text will now expand fully within the scrollable Box.
            Text(
                text = verse.text,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                // Removed maxLines and overflow from here
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // A new Row for the verse info and the share button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center, // Pushes items to the ends
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Verse Info on the left
            Text(
                text = verseInfo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Share Button on the right
            IconButton(
                onClick = { shareVerse(fullTextToShare, context) }
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = context.getString(R.string.share_verse)
                )
            }
        }
    }
}

private fun shareVerse(fullTextToShare: String, context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, fullTextToShare)
    }
    context.startActivity(
        Intent.createChooser(
            intent,
            context.getString(R.string.share_verse)
        )
    )
}
