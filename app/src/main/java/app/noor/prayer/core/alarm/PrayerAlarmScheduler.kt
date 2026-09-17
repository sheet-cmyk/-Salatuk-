package app.noor.prayer.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.noor.prayer.domain.*
import app.noor.prayer.receiver.PrayerAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerAlarmScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager = context.getSystemService(AlarmManager::class.java)
    fun allowed() = Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()
    private fun pending(prayer: PrayerName, kind: AlarmKind, flags: Int, alarm: PrayerAlarm? = null): PendingIntent? {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).setAction("app.noor.${kind.name}.${prayer.name}")
        alarm?.let { intent.putExtra("prayer", it.prayer.name).putExtra("kind", it.kind.name).putExtra("at", it.instant.toEpochMilli()).putExtra("revision", it.revision) }
        return PendingIntent.getBroadcast(context, 100 + kind.ordinal * 10 + prayer.ordinal, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }
    fun scheduleTest(alarm: PrayerAlarm) {
        check(app.noor.prayer.BuildConfig.DEBUG)
        if (!allowed()) return
        val intent = Intent(context, PrayerAlarmReceiver::class.java).setAction("app.noor.TEST")
            .putExtra("prayer",alarm.prayer.name).putExtra("kind",alarm.kind.name).putExtra("at",alarm.instant.toEpochMilli()).putExtra("revision",alarm.revision).putExtra("test",true)
        val pi = PendingIntent.getBroadcast(context,999,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,alarm.instant.toEpochMilli(),pi)
    }
    fun cancelAll() {
        PrayerName.entries.forEach { prayer -> AlarmKind.entries.forEach { kind ->
            pending(prayer,kind,PendingIntent.FLAG_NO_CREATE)?.let { manager.cancel(it); it.cancel() }
        } }
    }
    fun replace(alarms: List<PrayerAlarm>): Boolean {
        cancelAll()
        if (!allowed()) return false
        try {
            alarms.forEach { alarm ->
                val pi = requireNotNull(pending(alarm.prayer, alarm.kind, PendingIntent.FLAG_UPDATE_CURRENT, alarm))
                if (alarm.kind == AlarmKind.PRAYER) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,alarm.instant.toEpochMilli(),pi)
                // Reminders may be deferred in Doze. They must not consume the idle exact-alarm quota ahead of Adhan.
                else manager.setWindow(AlarmManager.RTC_WAKEUP,alarm.instant.toEpochMilli(),600_000L,pi)
            }
            return true
        } catch (_: SecurityException) { cancelAll(); return false }
    }
}
