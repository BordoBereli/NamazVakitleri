package com.kutluoglu.prayer_widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.kutluoglu.prayer_widget.data.WidgetData
import com.kutluoglu.prayer_widget.data.WidgetDataProvider
import com.kutluoglu.prayer_widget.data.WidgetResult
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.ZoneId

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
            val zoneId = ZoneId.systemDefault()
            when (val result = provider.load(zoneId)) {
                is WidgetResult.Success -> result.data
                WidgetResult.Error -> null
            }
        }.getOrNull()
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
    when {
        size.height >= PrayerWidgetSizes.LARGE.height -> LargeLayout(data)
        size.width >= PrayerWidgetSizes.MEDIUM.width -> MediumLayout(data)
        else -> SmallLayout(data)
    }
}

@Composable
private fun SmallLayout(data: WidgetData) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .clickable { openApp(context) }
            .padding(8.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            "${data.nextPrayerName} · ${data.nextPrayerTime}",
            style = TextStyle(fontWeight = FontWeight.Bold)
        )
        Text(data.timeRemaining)
    }
}

@Composable
private fun MediumLayout(data: WidgetData) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxSize()
            .clickable { openApp(context) }
            .padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(data.locationName, style = TextStyle(fontWeight = FontWeight.Bold))
            Text(data.gregorianDate)
            Text(data.hijriDate)
        }
        Column(
            modifier = GlanceModifier.defaultWeight(),
            horizontalAlignment = Alignment.Horizontal.End
        ) {
            Text("${data.nextPrayerName} · ${data.nextPrayerTime}")
            Text(data.timeRemaining)
        }
    }
}

@Composable
private fun LargeLayout(data: WidgetData) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .clickable { openApp(context) }
            .padding(12.dp)
    ) {
        Text(data.locationName, style = TextStyle(fontWeight = FontWeight.Bold))
        data.prayers.forEach { p ->
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(p.name, modifier = GlanceModifier.defaultWeight())
                Text(p.time)
            }
        }
    }
}

@Composable
private fun ErrorContent() {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Open app")
    }
}

private fun openApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
    context.startActivity(intent)
}
