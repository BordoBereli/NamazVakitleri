package com.kutluoglu.prayer_location.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackgroundImageStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = BackgroundImageStore(context)

    private fun writeTestJpeg(width: Int, height: Int): Uri {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFF336699.toInt())
        val file = File(context.cacheDir, "source-${width}x${height}-${System.nanoTime()}.jpg")
        file.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
        bitmap.recycle()
        return Uri.fromFile(file)
    }

    @Test
    fun `saveBakedCrop caps output to 1080px long edge`() = runTest {
        val source = writeTestJpeg(2000, 1200)
        val path = store.saveBakedCrop(source, RectF(0f, 0f, 1f, 1f), "loc1")

        assertThat(path).isNotNull()
        val saved = BitmapFactory.decodeFile(path)
        assertThat(maxOf(saved.width, saved.height)).isAtMost(1080)
        assertThat(saved.width).isGreaterThan(1000) // crop kept resolution
    }

    @Test
    fun `saveBakedCrop crops the normalized rect`() = runTest {
        val source = writeTestJpeg(1000, 1000)
        // Crop the top-left quarter
        val path = store.saveBakedCrop(source, RectF(0f, 0f, 0.5f, 0.5f), "loc1")

        assertThat(path).isNotNull()
        val saved = BitmapFactory.decodeFile(path)
        assertThat(saved.width).isEqualTo(saved.height) // square from a square crop
    }

    @Test
    fun `listBackgrounds returns saved files newest first`() = runTest {
        val source = writeTestJpeg(500, 500)
        val first = store.saveBakedCrop(source, RectF(0f, 0f, 1f, 1f), "loc1")
        val second = store.saveBakedCrop(source, RectF(0f, 0f, 1f, 1f), "loc2")

        val list = store.listBackgrounds()
        assertThat(list.map { it.absolutePath }).containsAtLeast(first, second)
        assertThat(list.first().absolutePath).isEqualTo(second) // newest first
    }

    @Test
    fun `deleteBackground removes the file`() = runTest {
        val source = writeTestJpeg(500, 500)
        val path = store.saveBakedCrop(source, RectF(0f, 0f, 1f, 1f), "loc1")
        assertThat(File(path!!).exists()).isTrue()

        store.deleteBackground(path)
        assertThat(File(path).exists()).isFalse()
        assertThat(store.listBackgrounds().map { it.absolutePath }).doesNotContain(path)
    }

    @Test
    fun `saveBakedCrop returns null for unreadable uri and does not throw`() = runTest {
        val missing = Uri.fromFile(File(context.cacheDir, "does-not-exist-${System.nanoTime()}.jpg"))
        val path = store.saveBakedCrop(missing, RectF(0f, 0f, 1f, 1f), "loc1")
        assertThat(path).isNull()
    }
}
