package app.noor.prayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.noor.prayer.core.alarm.PrayerAlarmScheduler
import app.noor.prayer.data.SettingsRepository
import app.noor.prayer.domain.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.time.Instant
import javax.inject.Inject

/** Debug APK only. Exercises the production AlarmManager -> receiver -> service path. */
@AndroidEntryPoint
class DebugAlarmReceiver: BroadcastReceiver() {
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var scheduler: PrayerAlarmScheduler
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if(intent.getBooleanExtra("setup",false)) settings.update { it.copy(location = LocationSettings(52.52,13.405,"Berlin","Germany",timeZone = "Europe/Berlin")) }
                intent.getStringExtra("language")?.let { language -> settings.update { it.copy(language = language) } }
                intent.getStringExtra("appearance")?.let { appearance -> settings.update { it.copy(appearance = Appearance.valueOf(appearance)) } }
                if(!intent.getBooleanExtra("schedule",true)) return@launch
                val current = settings.read()
                val at = Instant.now().plusSeconds(intent.getIntExtra("seconds",20).coerceIn(10,120).toLong())
                val prayer = runCatching { PrayerName.valueOf(intent.getStringExtra("prayer") ?: "FAJR") }.getOrDefault(PrayerName.FAJR)
                scheduler.scheduleTest(PrayerAlarm(prayer,AlarmKind.PRAYER,at,current.revision))
                android.util.Log.i("NoorTest","Scheduled ${prayer.name} at $at exact=${scheduler.allowed()}")
            } finally { pending.finish() }
        }
    }
}
