package com.kutluoglu.prayer_feature.home.feature

import android.R.attr.bitmap
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log.e
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
import androidx.core.content.FileProvider
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer_feature.home.R
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

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
    val appName = context.getString(R.string.app_name)
    val sharedApp = "\n\n${context.getString(R.string.shared_from_app, appName)}"
    val fullTextToShare = "\"${verse.text}\" - $verseInfo $sharedApp"

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
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = verse.text,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Start,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // A new Row for the verse info and the share button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End, // Pushes items to the ends
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Verse Info on the left
            Text(
                text = verseInfo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        type = "image/png"
        putExtra(Intent.EXTRA_TEXT, fullTextToShare)

        // Get the icon URI
        val iconUri = getIconUri(context)
        iconUri?.let {
            putExtra(Intent.EXTRA_STREAM, it)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    context.startActivity(
        Intent.createChooser(
            intent,
            context.getString(R.string.share_verse)
        )
    )
}

private fun getIconUri(context: Context): Uri? {
    try {
        // Get the launcher icon drawable
        val drawable = context.packageManager.getApplicationIcon(context.packageName)

        val originalBitmap = if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            // Create a bitmap from the drawable
            createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            ).also {
                val canvas = android.graphics.Canvas(it)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        }

        // Save the bitmap to the cache directory
        val imagesDir = File(context.cacheDir, "images")
        imagesDir.mkdirs()
        val imageFile = File(imagesDir, "app_icon.png")

        FileOutputStream(imageFile).use {
            originalBitmap.compress(
                Bitmap.CompressFormat.PNG, 100, it
            )
        }

        // Get the content URI using FileProvider
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)

    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}
