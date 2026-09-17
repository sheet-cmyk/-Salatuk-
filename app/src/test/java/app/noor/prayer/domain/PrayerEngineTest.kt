package app.noor.prayer.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class PrayerEngineTest {
    private val engine = PrayerEngine()
    private val settings = PrayerSettings(location = LocationSettings(21.4225,39.8262,"Makkah","Saudi Arabia",timeZone = "Asia/Riyadh"))
    private val date = LocalDate.of(2026,9,16)
    @Test fun allSixTimesAreValidAndOrdered() {
        val day = engine.calculate(date,settings)
        assertEquals(6,day.times.size)
        assertEquals(PrayerName.entries.toList(),day.times.map { it.name })
        assertTrue(day.times.zipWithNext().all { (a,b) -> b.instant > a.instant })
        assertTrue(day.times.all { it.instant.atZone(day.zone).toLocalDate() == date })
    }
    @Test fun wrapperMatchesAstronomyLibraryWithoutOffsets() {
        val direct = com.batoulapps.adhan2.PrayerTimes(com.batoulapps.adhan2.Coordinates(21.4225,39.8262),com.batoulapps.adhan2.data.DateComponents(2026,9,16),com.batoulapps.adhan2.CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.copy(highLatitudeRule = com.batoulapps.adhan2.HighLatitudeRule.TWILIGHT_ANGLE))
        assertEquals(direct.fajr.toEpochMilliseconds(),engine.calculate(date,settings).times.first().instant.toEpochMilli())
    }
    @Test fun nextPrayerNeverSelectsSunrise() {
        val fajr = engine.calculate(date,settings).times.first { it.name == PrayerName.FAJR }
        assertEquals(PrayerName.DHUHR,engine.next(settings,fajr.instant.plusSeconds(1))!!.name)
    }
    @Test fun exactlyAtPrayerMovesToFollowingPrayer() {
        val day = engine.calculate(date,settings)
        val dhuhr = day.times.first { it.name == PrayerName.DHUHR }
        assertEquals(PrayerName.ASR,engine.next(settings,dhuhr.instant)!!.name)
    }
    @Test fun afterIshaSelectsTomorrowsFajr() {
        val isha = engine.calculate(date,settings).times.last()
        val next = engine.next(settings,isha.instant.plusSeconds(1))!!
        assertEquals(PrayerName.FAJR,next.name)
        assertEquals(engine.calculate(date.plusDays(1),settings).times.first().instant,next.instant)
    }
    @Test fun offsetsApplyToEachObligatoryPrayerButNotSunrise() {
        val modified = settings.copy(calculation = settings.calculation.copy(offsets = PrayerName.entries.associateWith { -7 }))
        val base = engine.calculate(date,settings).times.associateBy { it.name }
        engine.calculate(date,modified).times.forEach { assertEquals(if(it.name.obligatory) -420L else 0L,Duration.between(base.getValue(it.name).instant,it.instant).seconds) }
    }
    @Test fun timeZoneConversionRetainsInstantAndHandlesDst() {
        val berlin = settings.copy(location = LocationSettings(52.52,13.405,"Berlin","Germany",timeZone = "Europe/Berlin"))
        val before = engine.calculate(LocalDate.of(2026,10,24),berlin)
        val after = engine.calculate(LocalDate.of(2026,10,25),berlin)
        assertEquals(ZoneOffset.ofHours(2),before.times[2].instant.atZone(before.zone).offset)
        assertEquals(ZoneOffset.ofHours(1),after.times[2].instant.atZone(after.zone).offset)
        assertEquals(before.times[2].instant,before.times[2].instant.atZone(before.zone).toInstant())
    }
    @Test fun hanafiAsrIsLater() {
        val standard = engine.calculate(date,settings).times.first { it.name == PrayerName.ASR }
        val hanafi = engine.calculate(date,settings.copy(calculation = settings.calculation.copy(madhhab = Madhhab.HANAFI))).times.first { it.name == PrayerName.ASR }
        assertTrue(hanafi.instant > standard.instant)
    }
    @Test fun allMethodsCalculateLocally() { Method.entries.forEach { assertEquals(6,engine.calculate(date,settings.copy(calculation = settings.calculation.copy(method = it))).times.size) } }
    @Test fun tehranMaghribOccursAfterSunsetAndBeforeIsha() {
        val local = settings.copy(location = LocationSettings(35.6892,51.3890,"Tehran","Iran",timeZone = "Asia/Tehran"))
        val sunset = engine.calculate(date,local).times.first { it.name == PrayerName.MAGHRIB }.instant
        val day = engine.calculate(date,local.copy(calculation = local.calculation.copy(method = Method.TEHRAN)))
        val maghrib = day.times.first { it.name == PrayerName.MAGHRIB }.instant
        assertTrue(Duration.between(sunset,maghrib).toMinutes() in 10..40)
        assertTrue(maghrib < day.times.first { it.name == PrayerName.ISHA }.instant)
    }
    @Test fun qiblaFromBerlinIsSoutheast() { assertTrue(engine.qibla(LocationSettings(52.52,13.405,"Berlin","Germany")) in 135.0..140.0) }
    @Test(expected = IllegalArgumentException::class) fun invalidCoordinatesAreRejected() { LocationSettings(Double.NaN,13.0,"","") }
    @Test fun polarFailureIsExplicitNotEpochAlarm() { assertTrue(runCatching { engine.calculate(LocalDate.of(2026,6,21),settings.copy(location = LocationSettings(89.0,0.0,"",""))) }.isFailure) }
}
