package app.noor.prayer.domain

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.HighLatitudeRule
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerEngine @Inject constructor() {
    fun calculate(date: LocalDate, settings: PrayerSettings): DailyPrayerTimes {
        val location = requireNotNull(settings.location)
        val c = settings.calculation
        val base = if (c.method == Method.TEHRAN) CalculationParameters(fajrAngle = 17.7, ishaAngle = 14.0)
            else CalculationMethod.valueOf(c.method.name).parameters
        val parameters = base.copy(madhab = if (c.madhhab == Madhhab.HANAFI) Madhab.HANAFI else Madhab.SHAFI, highLatitudeRule = HighLatitudeRule.TWILIGHT_ANGLE)
        val times = PrayerTimes(Coordinates(location.latitude, location.longitude), DateComponents(date.year, date.monthValue, date.dayOfMonth), parameters)
        val raw = listOf(times.fajr, times.sunrise, times.dhuhr, times.asr, times.maghrib, times.isha)
        val result = PrayerName.entries.zip(raw).map { (name, time) ->
            val rawInstant = Instant.ofEpochMilli(time.toEpochMilliseconds())
            val instant = if(name == PrayerName.MAGHRIB && c.method == Method.TEHRAN) SolarDepression.afterSunset(rawInstant, location, 4.5) else rawInstant
            // The library uses distant sentinel instants when the sun does not rise/set.
            require(kotlin.math.abs(instant.atZone(location.zone()).toLocalDate().toEpochDay() - date.toEpochDay()) <= 2) { "No solar solution for this location and date" }
            PrayerTime(name, instant.plusSeconds(if (name.obligatory) (c.offsets[name] ?: 0).coerceIn(-60, 60) * 60L else 0))
        }.sortedBy { it.instant }
        return DailyPrayerTimes(date, location.zone(), result)
    }
    fun upcoming(settings: PrayerSettings, now: Instant): List<PrayerTime> {
        val date = now.atZone(requireNotNull(settings.location).zone()).toLocalDate()
        // Include yesterday for adjustments crossing midnight.
        return (-1L..2L).flatMap { calculate(date.plusDays(it), settings).times }
            .filter { it.name.obligatory && it.instant > now }.sortedBy { it.instant }
    }
    fun next(settings: PrayerSettings, now: Instant): PrayerTime? = upcoming(settings, now).firstOrNull()
}

/** Pure planner: one next occurrence per prayer and type; IDs remain stable across all reschedules. */
class AlarmPlanner @Inject constructor(private val engine: PrayerEngine) {
    fun plan(settings: PrayerSettings, now: Instant): List<PrayerAlarm> {
        if (settings.location == null) return emptyList()
        val future = engine.upcoming(settings, now)
        return PrayerName.entries.filter { it.obligatory }.flatMap { prayer ->
            buildList {
                if (settings.notifications || (settings.adhan.enabled && prayer in settings.adhan.prayers)) {
                    future.firstOrNull { it.name == prayer }?.let { add(PrayerAlarm(prayer, AlarmKind.PRAYER, it.instant, settings.revision)) }
                }
                if (settings.reminderMinutes > 0) future.firstOrNull { it.name == prayer && it.instant.minusSeconds(settings.reminderMinutes * 60L) > now }?.let {
                    add(PrayerAlarm(prayer, AlarmKind.REMINDER, it.instant.minusSeconds(settings.reminderMinutes * 60L), settings.revision))
                }
            }
        }
    }
}
