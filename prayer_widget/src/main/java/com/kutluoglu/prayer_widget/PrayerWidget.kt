package com.kutluoglu.prayer_widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.BitmapImageProvider
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.background
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kutluoglu.prayer_widget.R
import com.kutluoglu.prayer_widget.data.WidgetData
import com.kutluoglu.prayer_widget.data.WidgetDataProvider
import com.kutluoglu.prayer_widget.data.WidgetResult
import com.kutluoglu.prayer_widget.data.toTurkishDative
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val Gold = Color(0xFFFFD700)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextDim = Color(0xFFA0A0A0)
private val NextPill = Color(0x29FFD700)
private val RingTrack = 0x1FFFFFFF
private val RingProgress = 0xFFFFD700.toInt()

class PrayerWidget(
    private val previewData: WidgetData? = null
) : GlanceAppWidget(), KoinComponent {

    private val provider: WidgetDataProvider by inject()

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            PrayerWidgetSizes.SMALL,
            PrayerWidgetSizes.MEDIUM,
            PrayerWidgetSizes.LARGE
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = previewData ?: runCatching {
            when (val result = provider.load()) {
                is WidgetResult.Success -> result.data
                WidgetResult.Error -> null
            }
        }.getOrElse {
            Log.e("PrayerWidget", "Failed to load widget data -> ${it.message}")
            null
        }
        provideContent {
            if (data == null) {
                ErrorContent()
            } else {
                WidgetContent(data)
            }
        }
    }
}

object PrayerWidgetSizes {
    val SMALL = DpSize(110.dp, 40.dp)
    val MEDIUM = DpSize(250.dp, 40.dp)
    val LARGE = DpSize(250.dp, 110.dp)
}

@Composable
internal fun WidgetContent(data: WidgetData) {
    val size = LocalSize.current
    Box(
        modifier = GlanceModifier.fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
    ) {
        when {
            size.height >= PrayerWidgetSizes.LARGE.height -> LargeLayout(data)
            size.width >= PrayerWidgetSizes.MEDIUM.width -> MediumLayout(data)
            else -> SmallLayout(data)
        }
    }
}

@Composable
private fun SmallLayout(data: WidgetData) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxSize()
            .clickable { openApp(context) }
            .padding(8.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        ProgressRing(
            progress = data.ringProgress,
            size = 34.dp,
            modifier = GlanceModifier.defaultWeight()
        ) {
            Text(data.countdownText, style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Gold)))
        }
        Column(
            modifier = GlanceModifier.defaultWeight(),
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            Text(data.nextPrayerName, style = TextStyle(fontWeight = FontWeight.Bold, color = ColorProvider(Gold)))
            Text(data.nextPrayerTime, style = TextStyle(color = ColorProvider(TextDim), fontSize = 11.sp))
        }
    }
}

@Composable
private fun MediumLayout(data: WidgetData) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxSize()
            .clickable { openApp(context) }
            .padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(data.locationName, style = TextStyle(fontWeight = FontWeight.Bold, color = ColorProvider(TextPrimary)))
            Text(data.hijriDate, style = TextStyle(color = ColorProvider(TextDim), fontSize = 11.sp))
        }
        Row(
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.End
        ) {
            Column(horizontalAlignment = Alignment.Horizontal.End) {
                Text(data.nextPrayerName, style = TextStyle(fontWeight = FontWeight.Bold, color = ColorProvider(Gold)))
                Text(data.nextPrayerTime, style = TextStyle(color = ColorProvider(TextDim), fontSize = 11.sp))
            }
            ProgressRing(
                progress = data.ringProgress,
                size = 38.dp,
                modifier = GlanceModifier.padding(start = 8.dp)
            ) {
                Text(data.countdownText, style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Gold)))
            }
        }
    }
}

@Composable
private fun LargeLayout(data: WidgetData) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .clickable { openApp(context) }
            .padding(8.dp)
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                data.locationName,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(fontWeight = FontWeight.Bold, color = ColorProvider(TextPrimary), fontSize = 12.sp)
            )
            Text(data.hijriDate, style = TextStyle(color = ColorProvider(Gold), fontSize = 10.sp))
        }
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            ProgressRing(progress = data.ringProgress, size = 28.dp) {
                Text(data.countdownText, style = TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Gold)))
            }
            Text(
                context.getString(R.string.widget_until_next, untilNextPrayerName(context, data.nextPrayerName)),
                modifier = GlanceModifier.padding(start = 8.dp),
                style = TextStyle(fontWeight = FontWeight.Bold, color = ColorProvider(Gold), fontSize = 12.sp)
            )
        }
        data.prayers.forEach { p ->
            val fontWeight = if (p.isNext) FontWeight.Bold else FontWeight.Normal
            val color = if (p.isNext) Gold else TextPrimary
            Row(
                modifier = GlanceModifier.fillMaxWidth()
                    .background(if (p.isNext) NextPill else Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    p.name,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(fontWeight = fontWeight, color = ColorProvider(color), fontSize = 11.sp)
                )
                Text(p.time, style = TextStyle(fontWeight = fontWeight, color = ColorProvider(color), fontSize = 11.sp))
            }
        }
    }
}

@Composable
private fun ProgressRing(
    progress: Float,
    size: Dp,
    modifier: GlanceModifier = GlanceModifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val sizePx = (size.value * density).toInt().coerceAtLeast(1)
    val bitmap = remember(progress, sizePx) {
        RingBitmapFactory.create(
            sizePx = sizePx,
            progress = progress,
            trackColor = RingTrack,
            progressColor = RingProgress
        )
    }
    Box(modifier = modifier.size(size)) {
        Image(
            provider = BitmapImageProvider(bitmap),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize()
        )
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { content() }
    }
}

@Composable
internal fun ErrorContent() {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .clickable { openApp(context) }
            .padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text(
            context.getString(R.string.prayer_widget_set_location),
            style = TextStyle(fontWeight = FontWeight.Bold, color = ColorProvider(TextPrimary))
        )
        Text(
            context.getString(R.string.prayer_widget_tap_to_open),
            style = TextStyle(color = ColorProvider(TextDim), fontSize = 11.sp)
        )
    }
}

private fun openApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
    context.startActivity(intent)
}

private fun untilNextPrayerName(context: Context, prayerName: String): String {
    val isTurkish = context.resources.configuration.locales[0].language == "tr"
    return if (isTurkish) prayerName.toTurkishDative() else prayerName
}
