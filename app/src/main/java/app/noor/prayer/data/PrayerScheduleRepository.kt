package app.noor.prayer.data

import app.noor.prayer.core.alarm.PrayerAlarmScheduler
import app.noor.prayer.domain.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerScheduleRepository @Inject constructor(val settings: SettingsRepository, private val planner: AlarmPlanner, private val scheduler: PrayerAlarmScheduler) {
    private val mutex = Mutex()
    suspend fun update(transform: (PrayerSettings) -> PrayerSettings): Boolean = mutex.withLock {
        settings.update(transform)
        rebuild()
    }
    suspend fun reschedule(): Boolean = mutex.withLock { rebuild() }
    private suspend fun rebuild(): Boolean {
        val plan = runCatching { planner.plan(settings.read(),Instant.now()) }.getOrElse { scheduler.cancelAll(); return false }
        return scheduler.replace(plan)
    }
    suspend fun receive(alarm: PrayerAlarm, test: Boolean = false, deliver: (PrayerSettings) -> Unit) = mutex.withLock {
        val current = settings.read()
        val age = java.time.Duration.between(alarm.instant,Instant.now()).seconds
        if (current.revision == alarm.revision && age in 0..600 && alarm.prayer.obligatory) {
            val valid = test || runCatching { planner.plan(current,alarm.instant.minusSeconds(1)).any { it == alarm } }.getOrDefault(false)
            if (valid && settings.consume(alarm)) deliver(current)
        }
        rebuild()
    }
}
