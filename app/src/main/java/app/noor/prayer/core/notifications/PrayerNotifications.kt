package app.noor.prayer.core.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.LocaleList
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.noor.prayer.MainActivity
import app.noor.prayer.R
import app.noor.prayer.domain.*
import app.noor.prayer.service.AdhanPlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

fun PrayerName.labelResource() = when(this) {
    PrayerName.FAJR -> R.string.fajr; PrayerName.SUNRISE -> R.string.sunrise; PrayerName.DHUHR -> R.string.dhuhr
    PrayerName.ASR -> R.string.asr; PrayerName.MAGHRIB -> R.string.maghrib; PrayerName.ISHA -> R.string.isha
}
@Singleton
class PrayerNotifications @Inject constructor(@ApplicationContext private val context: Context) {
    companion object { const val PLAYBACK = "playback"; const val PRAYER = "prayer"; const val REMINDER = "reminder"; const val PLAYBACK_ID = 50 }
    fun channels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(
            NotificationChannel(PLAYBACK,context.getString(R.string.channel_playback),NotificationManager.IMPORTANCE_LOW).apply { setSound(null,null); lockscreenVisibility = Notification.VISIBILITY_PUBLIC },
            NotificationChannel(PRAYER,context.getString(R.string.channel_prayer),NotificationManager.IMPORTANCE_HIGH).apply { setSound(null,null); lockscreenVisibility = Notification.VISIBILITY_PUBLIC },
            NotificationChannel(REMINDER,context.getString(R.string.channel_reminder),NotificationManager.IMPORTANCE_DEFAULT)
        ))
    }
    private fun localized(language: String): Context = if(language.isEmpty()) context else context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocales(LocaleList(Locale.forLanguageTag(language))) })
    private fun open() = PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun playback(prayer: PrayerName, language: String, session: androidx.media3.session.MediaSession? = null): Notification {
        val c = localized(language)
        val stop = PendingIntent.getService(context,1,Intent(context,AdhanPlaybackService::class.java).setAction(AdhanPlaybackService.STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context,PLAYBACK).setSmallIcon(R.drawable.ic_moon)
            .setContentTitle(c.getString(R.string.prayer_title,c.getString(prayer.labelResource())))
            .setContentText(c.getString(R.string.prayer_now,c.getString(prayer.labelResource())))
            .setContentIntent(open()).setOngoing(true).setOnlyAlertOnce(true).setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .apply { session?.let { setStyle(androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(it).setShowActionsInCompactView(0)) } }.setVisibility(NotificationCompat.VISIBILITY_PUBLIC).addAction(R.drawable.ic_stop,c.getString(R.string.stop_adhan),stop).build()
    }
    fun notify(alarm: PrayerAlarm, settings: PrayerSettings, failed: Boolean = false) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val c = localized(settings.language)
        val title = c.getString(R.string.prayer_title,c.getString(alarm.prayer.labelResource()))
        val message = when { failed -> c.getString(R.string.playback_failed)
            alarm.kind == AlarmKind.REMINDER -> c.getString(R.string.reminder_message,settings.reminderMinutes,c.getString(alarm.prayer.labelResource()))
            else -> c.getString(R.string.prayer_now,c.getString(alarm.prayer.labelResource())) }
        val n = NotificationCompat.Builder(context,if(alarm.kind == AlarmKind.REMINDER) REMINDER else PRAYER)
            .setSmallIcon(R.drawable.ic_moon).setContentTitle(title).setContentText(message).setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(open()).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).build()
        try { NotificationManagerCompat.from(context).notify(alarm.id,n) } catch (_: SecurityException) { /* User can revoke permission between checking and posting. */ }
    }
}
