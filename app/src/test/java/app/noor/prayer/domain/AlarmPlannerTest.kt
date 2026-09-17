package app.noor.prayer.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class AlarmPlannerTest {
    private val planner = AlarmPlanner(PrayerEngine())
    private val s = PrayerSettings(location = LocationSettings(52.52,13.405,"Berlin","Germany",timeZone = "Europe/Berlin"),revision = "version-one")
    private val now = Instant.parse("2026-09-16T00:00:00Z")
    @Test fun fiveUniquePrayerIdsNoSunrise() {
        val plan = planner.plan(s,now)
        assertEquals(5,plan.size); assertEquals(5,plan.map { it.id }.distinct().size)
        assertTrue(plan.none { it.prayer == PrayerName.SUNRISE }); assertTrue(plan.all { it.instant > now })
    }
    @Test fun perPrayerDisableSuppressesAudioOnlyAlarm() {
        val plan = planner.plan(s.copy(notifications = false,adhan = s.adhan.copy(prayers = setOf(PrayerName.FAJR))),now)
        assertEquals(listOf(PrayerName.FAJR),plan.map { it.prayer })
    }
    @Test fun masterDisableStillAllowsRequestedNotifications() { assertEquals(5,planner.plan(s.copy(adhan = s.adhan.copy(enabled = false)),now).size) }
    @Test fun allDisabledProducesNoAlarms() { assertTrue(planner.plan(s.copy(notifications = false,adhan = s.adhan.copy(enabled = false)),now).isEmpty()) }
    @Test fun remindersHaveSeparateIdentities() {
        val plan = planner.plan(s.copy(reminderMinutes = 15),now)
        assertEquals(10,plan.size); assertEquals(10,plan.map { it.id }.distinct().size)
        plan.filter { it.kind == AlarmKind.PRAYER }.forEach { actual -> assertEquals(actual.instant.minusSeconds(900),plan.first { it.prayer == actual.prayer && it.kind == AlarmKind.REMINDER }.instant) }
    }
    @Test fun stableIdsAcrossSettingsRecalculationAndDayRollover() { assertEquals(planner.plan(s,now).map { it.id },planner.plan(s.copy(revision = "changed"),now.plusSeconds(86400)).map { it.id }) }
    @Test fun revisionInvalidatesEventIdentity() { assertNotEquals(planner.plan(s,now).first().eventKey,planner.plan(s.copy(revision = "changed"),now).first().eventKey) }
    @Test fun firedPrayerIsReplacedWithTomorrowOccurrence() {
        val fajr = planner.plan(s,now).first { it.prayer == PrayerName.FAJR }
        val after = planner.plan(s,fajr.instant.plusSeconds(1)).first { it.prayer == PrayerName.FAJR }
        assertTrue(after.instant > fajr.instant.plusSeconds(23*3600))
    }
    @Test fun noLocationMeansNoAlarms() { assertTrue(planner.plan(s.copy(location = null),now).isEmpty()) }
}
