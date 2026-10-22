package com.kutluoglu.prayer_location.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

/**
 * Stores baked-crop background images in app-private storage.
 *
 * Files live in `filesDir/backgrounds/<uuid>.jpg` with unique names (not keyed
 * by location id) so they survive location removal and can be reused via the
 * saved-backgrounds gallery.
 */
@Single
class BackgroundImageStore(
    private val context: Context
) {

    private val backgroundsDir: File
        get() = File(context.filesDir, BACKGROUNDS_DIR).apply { mkdirs() }

    /**
     * Decodes [sourceUri] (sampled), applies EXIF rotation, crops [cropRect]
     * (normalized 0..1 in the displayed orientation), downscales to at most
     * [MAX_OUTPUT_EDGE_PX] on the long edge, and writes a JPEG to private
     * storage. Returns the absolute path, or null on failure (never throws).
     */
    suspend fun saveBakedCrop(
        sourceUri: Uri,
        cropRect: RectF,
        locationId: String
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val source = decodeSampled(sourceUri) ?: return@runCatching null
            val rotated = applyExifRotation(sourceUri, source)
            val cropped = cropNormalized(rotated, cropRect) ?: return@runCatching null
            val output = downscale(cropped, MAX_OUTPUT_EDGE_PX)
            val file = File(backgroundsDir, "${UUID.randomUUID()}.jpg")
            file.outputStream().use { stream ->
                output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            }
            file.absolutePath
        }.onFailure {
            Log.e(TAG, "saveBakedCrop failed -> ${it.message}")
        }.getOrNull()
    }

    /** All stored background files (the gallery source). */
    fun listBackgrounds(): List<File> =
        backgroundsDir.listFiles()?.filter { it.isFile && it.extension == "jpg" }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

    /** Removes the background file at [path] if present. */
    fun deleteBackground(path: String) {
        runCatching { File(path).delete() }
            .onFailure { Log.e(TAG, "deleteBackground failed -> ${it.message}") }
    }

    private fun decodeSampled(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Sample so the long edge is around 2x the output cap: the crop keeps
        // full resolution while the decode stays bounded.
        var sampleSize = 1
        val longEdge = maxOf(bounds.outWidth, bounds.outHeight)
        while (longEdge / (sampleSize * 2) >= MAX_OUTPUT_EDGE_PX * 2) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
            ExifInterface.ORIENTATION_FLIP_VERTICAL,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE -> 90f
            else -> 0f
        }
        val mirrored = orientation in intArrayOf(
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
            ExifInterface.ORIENTATION_FLIP_VERTICAL,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE
        )
        if (degrees == 0f && !mirrored) return bitmap

        val matrix = Matrix().apply {
            if (mirrored) postScale(-1f, 1f)
            postRotate(degrees)
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun cropNormalized(bitmap: Bitmap, cropRect: RectF): Bitmap? {
        val left = (cropRect.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (cropRect.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val width = ((cropRect.width()) * bitmap.width).toInt()
            .coerceIn(1, bitmap.width - left)
        val height = ((cropRect.height()) * bitmap.height).toInt()
            .coerceIn(1, bitmap.height - top)
        if (width <= 0 || height <= 0) return null
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    private fun downscale(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longEdge = maxOf(bitmap.width, bitmap.height)
        if (longEdge <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / longEdge
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private companion object {
        const val TAG = "BackgroundImageStore"
        const val BACKGROUNDS_DIR = "backgrounds"
        const val MAX_OUTPUT_EDGE_PX = 1080
        const val JPEG_QUALITY = 90
    }
}
