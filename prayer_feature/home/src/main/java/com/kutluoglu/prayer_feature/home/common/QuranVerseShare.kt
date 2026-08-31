package com.kutluoglu.prayer_feature.home.common

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer_feature.home.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun shareVerse(verse: AyahData, verseFormatter: QuranVerseFormatter, context: Context) {
    val localizedSurahName = verseFormatter.getLocalizedNameOf(verse, context)
    val verseInfo = "($localizedSurahName - $verse)"
    val appName = context.getString(R.string.app_name)
    val sharedApp = "\n\n${context.getString(R.string.shared_from_app, appName)}"
    val fullTextToShare = "\"${verse.text}\" - $verseInfo $sharedApp"

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_TEXT, fullTextToShare)
        val iconUri = getIconUri(context)
        iconUri?.let {
            putExtra(Intent.EXTRA_STREAM, it)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.share_verse))
    )
}

private fun getIconUri(context: Context): Uri? {
    try {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val originalBitmap = if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
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
        val imagesDir = File(context.cacheDir, "images")
        imagesDir.mkdirs()
        val imageFile = File(imagesDir, "app_icon.png")
        FileOutputStream(imageFile).use {
            originalBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}
