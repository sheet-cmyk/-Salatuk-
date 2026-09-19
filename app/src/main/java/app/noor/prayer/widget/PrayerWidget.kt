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
import app.noor.prayer.core.common.clockPattern
import app.noor.prayer.core.media.RadioState
import app.noor.prayer.core.notifications.labelResource
import app.noor.prayer.di.AppEntryPoint
import app.noor.prayer.domain.PrayerName
import app.noor.prayer.service.RadioPlaybackService
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/** RemoteViews keeps the widget light and fully native, so the OS refreshes it at most every 30 minutes.
 *  The countdown is formatted with Locale.US (not a Chronometer) so the digits stay Latin numerals even
 *  when the device's regional numeral setting is Eastern Arabic -- Android's Chronometer always renders
 *  with Locale.getDefault() and can't be overridden per-instance, so it would otherwise show ١٢٣ digits. */
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
            view.setInt(R.id.widget_radio_row,"setLayoutDirection",View.LAYOUT_DIRECTION_LTR)
            view.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(context,20,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            applyRadio(view,context,localized,entry.radio().state.value)

            val location = settings.location
            if (location == null) {
                view.setTextViewText(R.id.widget_caption,localized.getString(R.string.app_name))
                view.setTextViewText(R.id.widget_countdown,"—")
                view.setTextViewText(R.id.widget_date,"")
                view.setTextViewText(R.id.widget_p1_name,localized.getString(R.string.app_name))
                view.setTextViewText(R.id.widget_p1_time,localized.getString(R.string.open_to_setup))
                for (i in 1 until order.size) { view.setTextViewText(nameIds[i],""); view.setTextViewText(timeIds[i],"") }
                for (i in order.indices) view.setViewVisibility(markIds[i],View.GONE)
                manager.updateAppWidget(ids,view)
                return
            }

            val zone = location.zone()
            val now = Instant.now()
            val timeFormat = DateTimeFormatter.ofPattern(clockPattern(settings.clockFormat,context,includeMeridiem = false),Locale.US)
            val today = entry.engine().calculate(now.atZone(zone).toLocalDate(),settings)
            val next = entry.engine().next(settings,now)

            view.setTextViewText(R.id.widget_caption,next?.let { localized.getString(R.string.widget_remaining,localized.getString(it.name.labelResource())) } ?: localized.getString(R.string.app_name))
            view.setTextViewText(R.id.widget_countdown,next?.let { countdown(now,it.instant) } ?: "—")
            val dateFormat = DateTimeFormatter.ofPattern("d MMM",localized.resources.configuration.locales[0])
            view.setTextViewText(R.id.widget_date,now.atZone(zone).format(dateFormat))

            val byName = today.times.associateBy { it.name }
            order.forEachIndexed { i, prayer ->
                val time = byName[prayer]
                view.setTextViewText(nameIds[i],localized.getString(prayer.labelResource()))
                view.setTextViewText(timeIds[i],time?.instant?.atZone(zone)?.format(timeFormat) ?: "—")
                view.setViewVisibility(markIds[i],if (next != null && prayer == next.name) View.VISIBLE else View.GONE)
            }

            manager.updateAppWidget(ids,view)
        }

        private fun applyRadio(view: RemoteViews, context: Context, localized: Context, radio: RadioState) {
            view.setTextViewText(R.id.widget_radio_label,radio.stationName.ifBlank { localized.getString(R.string.widget_radio_default) })
            view.setImageViewResource(R.id.widget_radio_toggle,if (radio.playing) R.drawable.ic_widget_pause_dark else R.drawable.ic_widget_play_dark)
            fun action(name: String, code: Int) = PendingIntent.getForegroundService(context,code,Intent(context,RadioPlaybackService::class.java).setAction(name),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            view.setOnClickPendingIntent(R.id.widget_radio_prev,action(RadioPlaybackService.PREV,31))
            view.setOnClickPendingIntent(R.id.widget_radio_toggle,action(RadioPlaybackService.TOGGLE,32))
            view.setOnClickPendingIntent(R.id.widget_radio_next,action(RadioPlaybackService.NEXT,33))
        }

        private fun countdown(now: Instant, target: Instant): String {
            val remaining = Duration.between(now,target).coerceAtLeast(Duration.ZERO)
            val h = remaining.toHours()
            val m = remaining.toMinutes() % 60
            val s = remaining.seconds % 60
            return if (h > 0) String.format(Locale.US,"%d:%02d",h,m) else String.format(Locale.US,"%d:%02d",m,s)
        }
    }
}
