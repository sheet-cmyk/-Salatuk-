package app.noor.prayer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import app.noor.prayer.BuildConfig
import app.noor.prayer.core.media.PlaybackController
import app.noor.prayer.core.notifications.PrayerNotifications
import app.noor.prayer.data.PrayerScheduleRepository
import app.noor.prayer.domain.*
import app.noor.prayer.widget.PrayerWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class PrayerAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var schedules: PrayerScheduleRepository
    @Inject lateinit var playback: PlaybackController
    @Inject lateinit var notifications: PrayerNotifications
    override fun onReceive(context: Context, intent: Intent) {
        val event = runCatching { PrayerAlarm(PrayerName.valueOf(intent.getStringExtra("prayer").orEmpty()),AlarmKind.valueOf(intent.getStringExtra("kind").orEmpty()),Instant.ofEpochMilli(intent.getLongExtra("at",0)),requireNotNull(intent.getStringExtra("revision"))) }.getOrNull() ?: return
        val pending = goAsync()
        val lock = context.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Noor:dispatch").apply { acquire(15_000) }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(9_000) {
                    notifications.channels()
                    schedules.receive(event,BuildConfig.DEBUG && intent.getBooleanExtra("test",false)) { settings ->
                        if(event.kind == AlarmKind.REMINDER) notifications.notify(event,settings)
                        else {
                            if(settings.notifications) notifications.notify(event,settings)
                            if(settings.adhan.enabled && event.prayer in settings.adhan.prayers && !playback.start(event.prayer,settings)) notifications.notify(event,settings,failed=true)
                        }
                    }
                    PrayerWidget.refresh(context)
                }
            } catch (error: Exception) {
                android.util.Log.w("NoorSchedule", "Alarm dispatch failed", error)
            } finally { if(lock.isHeld) lock.release(); pending.finish() }
        }
    }
}
