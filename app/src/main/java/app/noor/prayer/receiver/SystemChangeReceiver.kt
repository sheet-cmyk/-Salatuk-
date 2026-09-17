package app.noor.prayer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.noor.prayer.data.PrayerScheduleRepository
import app.noor.prayer.widget.PrayerWidget
import app.noor.prayer.core.alarm.MaintenanceWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class SystemChangeReceiver : BroadcastReceiver() {
    @Inject lateinit var schedules: PrayerScheduleRepository
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(
                Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_LOCALE_CHANGED,
                android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
            )) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { withTimeout(9_000) { schedules.reschedule(); PrayerWidget.refresh(context); MaintenanceWorker.enqueue(context) } }
            catch (error: Exception) { android.util.Log.w("NoorSchedule", "Could not restore alarms; maintenance or next resume will retry", error) }
            finally { pending.finish() }
        }
    }
}
