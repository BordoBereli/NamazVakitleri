package com.kutluoglu.prayer_widget

import androidx.glance.EmittableWithText
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.GlanceNodeMatcher
import androidx.glance.testing.unit.MappedNode
import androidx.glance.testing.unit.hasClickAction
import androidx.glance.testing.unit.hasText
import androidx.glance.text.FontWeight
import androidx.test.core.app.ApplicationProvider
import com.kutluoglu.prayer_widget.data.WidgetData
import com.kutluoglu.prayer_widget.data.WidgetPrayer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrayerWidgetRenderTest {

    private val data = WidgetData(
        nextPrayerName = "Dhuhr",
        nextPrayerTime = "12:30",
        timeRemaining = "02:15:00",
        locationName = "Istanbul",
        gregorianDate = "2026-09-02",
        hijriDate = "20 Safer 1448",
        prayers = listOf(
            WidgetPrayer("Fajr", "05:00", false),
            WidgetPrayer("Dhuhr", "12:30", true)
        )
    )

    @Test
    fun `renders small size without throwing`() {
        runGlanceAppWidgetUnitTest {
            setContext(ApplicationProvider.getApplicationContext())
            setAppWidgetSize(PrayerWidgetSizes.SMALL)
            provideComposable { WidgetContent(data) }
            awaitIdle()
            onNode(hasText("Dhuhr · 12:30")).assertExists()
        }
    }

    @Test
    fun `renders medium size without throwing`() {
        runGlanceAppWidgetUnitTest {
            setContext(ApplicationProvider.getApplicationContext())
            setAppWidgetSize(PrayerWidgetSizes.MEDIUM)
            provideComposable { WidgetContent(data) }
            awaitIdle()
            onNode(hasText("Istanbul")).assertExists()
        }
    }

    @Test
    fun `renders medium layout for medium size`() {
        runGlanceAppWidgetUnitTest {
            setContext(ApplicationProvider.getApplicationContext())
            setAppWidgetSize(PrayerWidgetSizes.MEDIUM)
            provideComposable { WidgetContent(data) }
            awaitIdle()
            onNode(hasText("2026-09-02")).assertExists()
        }
    }

    @Test
    fun `renders large size without throwing`() {
        runGlanceAppWidgetUnitTest {
            setContext(ApplicationProvider.getApplicationContext())
            setAppWidgetSize(PrayerWidgetSizes.LARGE)
            provideComposable { WidgetContent(data) }
            awaitIdle()
            onNode(hasText("Fajr")).assertExists()
        }
    }

    @Test
    fun `renders next prayer in large layout with bold weight`() {
        runGlanceAppWidgetUnitTest {
            setContext(ApplicationProvider.getApplicationContext())
            setAppWidgetSize(PrayerWidgetSizes.LARGE)
            provideComposable { WidgetContent(data) }
            awaitIdle()
            onNode(hasTextWithFontWeight("Dhuhr", FontWeight.Bold)).assertExists()
            onNode(hasTextWithFontWeight("Fajr", FontWeight.Normal)).assertExists()
        }
    }

    @Test
    fun `renders error content with open app text and click action`() {
        runGlanceAppWidgetUnitTest {
            setContext(ApplicationProvider.getApplicationContext())
            setAppWidgetSize(PrayerWidgetSizes.SMALL)
            provideComposable { ErrorContent() }
            awaitIdle()
            onNode(hasText("Open app")).assertExists()
            onAllNodes(hasClickAction()).assertCountEquals(1)
        }
    }

    private fun hasTextWithFontWeight(
        text: String,
        fontWeight: FontWeight
    ): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
        "has text '$text' with fontWeight $fontWeight"
    ) { node ->
        val emittable = node.value.emittable
        emittable is EmittableWithText &&
            emittable.text == text &&
            emittable.style?.fontWeight == fontWeight
    }
}
