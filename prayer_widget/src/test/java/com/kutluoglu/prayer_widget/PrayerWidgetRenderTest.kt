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
        countdownText = "2s 15d",
        ringProgress = 0.5f,
        locationName = "Istanbul",
        gregorianDate = "2026-09-02",
        hijriDate = "20 Safer 1448",
        prayers = listOf(
            WidgetPrayer("Asr", "16:00", false),
            WidgetPrayer("Dhuhr", "12:30", true)
        )
    )

    @Test
    fun `renders small size with next prayer and countdown`() {
        runGlanceAppWidgetUnitTest {
            setContext(ApplicationProvider.getApplicationContext())
            setAppWidgetSize(PrayerWidgetSizes.SMALL)
            provideComposable { WidgetContent(data) }
            awaitIdle()
            onNode(hasText("Dhuhr")).assertExists()
            onNode(hasText("12:30")).assertExists()
            onNode(hasText("2s 15d")).assertExists()
        }
    }

    @Test
    fun `renders medium size with location and hijri date`() {
        runGlanceAppWidgetUnitTest {
            setContext(ApplicationProvider.getApplicationContext())
            setAppWidgetSize(PrayerWidgetSizes.MEDIUM)
            provideComposable { WidgetContent(data) }
            awaitIdle()
            onNode(hasText("Istanbul")).assertExists()
            onNode(hasText("20 Safer 1448")).assertExists()
        }
    }

    @Test
    fun `renders large size with prayer list and next prayer bold`() {
        runGlanceAppWidgetUnitTest {
            setContext(ApplicationProvider.getApplicationContext())
            setAppWidgetSize(PrayerWidgetSizes.LARGE)
            provideComposable { WidgetContent(data) }
            awaitIdle()
            onAllNodes(hasText("Dhuhr")).assertCountEquals(2)
            onNode(hasTextWithFontWeight("Dhuhr", FontWeight.Bold)).assertExists()
            onNode(hasTextWithFontWeight("Asr", FontWeight.Normal)).assertExists()
        }
    }

    @Test
    fun `renders error content with set location and tap to open`() {
        runGlanceAppWidgetUnitTest {
            setContext(ApplicationProvider.getApplicationContext())
            setAppWidgetSize(PrayerWidgetSizes.SMALL)
            provideComposable { ErrorContent() }
            awaitIdle()
            onNode(hasText("Set location")).assertExists()
            onNode(hasText("Tap to open app")).assertExists()
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
