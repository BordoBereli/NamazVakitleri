package com.kutluoglu.wear

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class WearPackagingTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `watch app shares the phone package so Play attaches it to the existing listing`() {
        assertThat(context.packageName).isEqualTo("com.kutluoglu.namazvakitleri")
    }

    @Test
    fun `watch artifact version code is unique across form factors`() {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        assertThat(info.longVersionCode).isEqualTo(3620200L)
    }

    @Test
    fun `watch app declares standalone so it is usable without the phone`() {
        val appInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        assertThat(appInfo.metaData.getBoolean("com.google.android.wearable.standalone")).isTrue()
    }

    @Test
    fun `watch form factor stays required`() {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_CONFIGURATIONS)
        assertThat(info.reqFeatures.orEmpty().map { it.name }).contains("android.hardware.type.watch")
    }

    @Test
    fun `location features are optional so watches without GPS stay discoverable`() {
        val manifest = File("src/main/AndroidManifest.xml")
        assertThat(manifest.exists()).isTrue() // fail loudly on a wrong working directory
        // Namespace-aware so getAttributeNS can resolve the "android:" prefixed attributes;
        // a plain parser treats them as literal names and returns "".
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(manifest)
        val features = document.getElementsByTagName("uses-feature")
        val optional = (0 until features.length)
            .map { features.item(it) as org.w3c.dom.Element }
            .filter { it.getAttributeNS("http://schemas.android.com/apk/res/android", "required") == "false" }
            .map { it.getAttributeNS("http://schemas.android.com/apk/res/android", "name") }
        assertThat(optional).containsAtLeast("android.hardware.location.gps", "android.hardware.location.network")
    }
}
