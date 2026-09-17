package app.noor.prayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.LocaleList
import android.view.View
import android.widget.RemoteViews
import app.noor.prayer.MainActivity
import app.noor.prayer.R
import app.noor.prayer.core.notifications.labelResource
import app.noor.prayer.di.AppEntryPoint
import app.noor.prayer.domain.PrayerName
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** RemoteViews keeps the widget light and fully native; the OS refreshes at most every 30 minutes,
 *  so the countdown reflects the remaining time as of the last refresh rather than ticking live. */
class PrayerWidget: AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch { try { refresh(context) } finally { pending.finish() } }
    }
    companion object {
        private val markIds = intArrayOf(R.id.widget_p1_mark,R.id.widget_p2_mark,R.id.widget_p3_mark,R.id.widget_p4_mark,R.id.widget_p5_mark)
        private val nameIds = intArrayOf(R.id.widget_p1_name,R.id.widget_p2_name,R.id.widget_p3_name,R.id.widget_p4_name,R.id.widget_p5_name)
        private val timeIds = intArrayOf(R.id.widget_p1_time,R.id.widget_p2_time,R.id.widget_p3_time,R.id.widget_p4_time,R.id.widget_p5_time)
        // visual order matches the reference layout: Isha first (start) through Fajr last (end), independent of locale mirroring.
        private val order = listOf(PrayerName.ISHA,PrayerName.MAGHRIB,PrayerName.ASR,PrayerName.DHUHR,PrayerName.FAJR)

        suspend fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context,PrayerWidget::class.java))
            if(ids.isEmpty()) return
            val entry = EntryPointAccessors.fromApplication(context,AppEntryPoint::class.java)
            val settings = entry.settings().read()
            val localized = if (settings.language.isBlank()) context else context.createConfigurationContext(
                Configuration(context.resources.configuration).apply {
                    setLocales(LocaleList(Locale.forLanguageTag(settings.language)))
                }
            )
            val view = RemoteViews(context.packageName,R.layout.prayer_widget)
            view.setInt(R.id.widget_root,"setLayoutDirection",localized.resources.configuration.layoutDirection)
            view.setInt(R.id.widget_row_prayers,"setLayoutDirection",View.LAYOUT_DIRECTION_LTR)
            view.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(context,20,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            val location = settings.location
            if (location == null) {
                view.setTextViewText(R.id.widget_label,localized.getString(R.string.app_name))
                view.setTextViewText(R.id.widget_countdown,"—")
                view.setTextViewText(R.id.widget_location,localized.getString(R.string.open_to_setup))
                view.setTextViewText(R.id.widget_hijri,"")
                view.setTextViewText(R.id.widget_sunrise,"")
                for (i in order.indices) { view.setTextViewText(nameIds[i],""); view.setTextViewText(timeIds[i],""); view.setViewVisibility(markIds[i],View.GONE) }
                manager.updateAppWidget(ids,view)
                return
            }

            val zone = location.zone()
            val now = Instant.now()
            val timeFormat = DateTimeFormatter.ofPattern("HH:mm",Locale.US)
            val today = entry.engine().calculate(now.atZone(zone).toLocalDate(),settings)
            val next = entry.engine().next(settings,now)

            view.setTextViewText(R.id.widget_label,next?.let { localized.getString(R.string.widget_next_in,localized.getString(it.name.labelResource())) } ?: localized.getString(R.string.app_name))
            view.setTextViewText(R.id.widget_countdown,next?.let { countdown(now,it.instant) } ?: "—")
            view.setTextViewText(R.id.widget_location,location.city.ifBlank { localized.getString(R.string.current_location) })

            val hijri = runCatching { HijrahDate.from(now.atZone(zone).toLocalDate()).plus(settings.hijriOffset.toLong(),ChronoUnit.DAYS).format(DateTimeFormatter.ofPattern("d MMMM yyyy",localized.resources.configuration.locales[0])) }.getOrDefault("")
            view.setTextViewText(R.id.widget_hijri,"$hijri ${localized.getString(R.string.hijri_suffix)}")

            val sunrise = today.times.firstOrNull { it.name == PrayerName.SUNRISE }
            view.setTextViewText(R.id.widget_sunrise,sunrise?.instant?.atZone(zone)?.format(timeFormat) ?: "—")

            val byName = today.times.associateBy { it.name }
            order.forEachIndexed { i, prayer ->
                val time = byName[prayer]
                view.setTextViewText(nameIds[i],localized.getString(prayer.labelResource()))
                view.setTextViewText(timeIds[i],time?.instant?.atZone(zone)?.format(timeFormat) ?: "—")
                view.setViewVisibility(markIds[i],if (next != null && prayer == next.name) View.VISIBLE else View.GONE)
            }

            manager.updateAppWidget(ids,view)
        }

        private fun countdown(now: Instant, target: Instant): String {
            val remaining = Duration.between(now,target).coerceAtLeast(Duration.ZERO)
            val h = remaining.toHours()
            val m = remaining.toMinutes() % 60
            val s = remaining.seconds % 60
            return if (h > 0) "%d:%02d".format(h,m) else "%d:%02d".format(m,s)
        }
    }
}
