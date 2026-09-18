package app.noor.prayer.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

 enum class PrayerName { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA;
    val obligatory get() = this != SUNRISE
 }
enum class Method { MUSLIM_WORLD_LEAGUE, UMM_AL_QURA, EGYPTIAN, KARACHI, NORTH_AMERICA, DUBAI, KUWAIT, QATAR, SINGAPORE, TURKEY, TEHRAN, MOON_SIGHTING_COMMITTEE }
enum class Madhhab { STANDARD, HANAFI }
enum class Appearance { SYSTEM, LIGHT, DARK }
enum class ClockFormat { SYSTEM, H24, H12 }
enum class Voice { STANDARD, MAKKAH, MADINAH }
data class LocationSettings(val latitude: Double, val longitude: Double, val city: String, val country: String, val automatic: Boolean = false, val timeZone: String = "") {
    init { require(latitude.isFinite() && latitude in -90.0..90.0); require(longitude.isFinite() && longitude in -180.0..180.0) }
    fun zone(): ZoneId = if (timeZone.isBlank()) ZoneId.systemDefault() else runCatching { ZoneId.of(timeZone) }.getOrDefault(ZoneId.systemDefault())
}
data class CalculationSettings(val method: Method = Method.MUSLIM_WORLD_LEAGUE, val madhhab: Madhhab = Madhhab.STANDARD, val offsets: Map<PrayerName, Int> = emptyMap())
data class AdhanSettings(val enabled: Boolean = true, val prayers: Set<PrayerName> = PrayerName.entries.filter { it.obligatory }.toSet(), val voice: Voice = Voice.STANDARD, val customAudio: Map<Voice, String> = emptyMap())
data class PrayerSettings(val location: LocationSettings? = null, val calculation: CalculationSettings = CalculationSettings(), val adhan: AdhanSettings = AdhanSettings(), val notifications: Boolean = true, val reminderMinutes: Int = 0, val appearance: Appearance = Appearance.DARK, val clockFormat: ClockFormat = ClockFormat.SYSTEM, val language: String = "", val hijriOffset: Int = 0, val revision: String = "initial")
data class PrayerTime(val name: PrayerName, val instant: Instant)
data class DailyPrayerTimes(val date: LocalDate, val zone: ZoneId, val times: List<PrayerTime>)
enum class AlarmKind { PRAYER, REMINDER }
data class PrayerAlarm(val prayer: PrayerName, val kind: AlarmKind, val instant: Instant, val revision: String) {
    val id get() = 100 + kind.ordinal * 10 + prayer.ordinal
    val eventKey get() = "${prayer.name}:${kind.name}:${instant.toEpochMilli()}:$revision"
}
