package com.kutluoglu.prayer_auto.manifest

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

private fun Element.children(tag: String): List<Element> =
    (0 until childNodes.length).map { childNodes.item(it) }.filterIsInstance<Element>().filter { it.tagName == tag }

private fun Element.androidName(): String? = getAttributeNS(ANDROID_NS, "name")

class CarAppManifestTest {

    private val manifest: Element = File("src/main/AndroidManifest.xml").let { file ->
        assertThat(file.exists()).isTrue() // fail loudly on a wrong working directory
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(file).documentElement
    }

    private val application: Element = manifest.children("application").single()
    private val service: Element = application.children("service")
        .single { it.androidName() == "com.kutluoglu.prayer_auto.service.PrayerCarAppService" }

    @Test
    fun `main manifest declares the CarAppService the car host discovers`() {
        assertThat(service.getAttributeNS(ANDROID_NS, "exported")).isEqualTo("true")
        assertThat(service.children("intent-filter").flatMap { it.children("action") }.map { it.androidName() })
            .contains("androidx.car.app.CarAppService")
        assertThat(service.children("intent-filter").flatMap { it.children("category") }.map { it.androidName() })
            .contains("androidx.car.app.category.POI")
    }

    @Test
    fun `main manifest declares the car metadata the host reads`() {
        val metadata = application.children("meta-data").associate { it.androidName() to it }
        assertThat(metadata.keys).containsAtLeast("androidx.car.app.minCarApiLevel", "com.google.android.gms.car.application")
        assertThat(metadata.getValue("com.google.android.gms.car.application").getAttributeNS(ANDROID_NS, "resource"))
            .isEqualTo("@xml/automotive_app_desc")
        assertThat(File("src/main/res/xml/automotive_app_desc.xml").exists()).isTrue()
    }

    @Test
    fun `main manifest declares the map-templates permission used by the POI screens`() {
        val permissions = manifest.children("uses-permission").map { it.androidName() }
        assertThat(permissions).contains("androidx.car.app.MAP_TEMPLATES")
    }

    @Test
    fun `car declarations are no longer debug-only`() {
        assertThat(File("src/debug/AndroidManifest.xml").exists()).isFalse()
        assertThat(File("src/debug/res/xml/automotive_app_desc.xml").exists()).isFalse()
    }
}
