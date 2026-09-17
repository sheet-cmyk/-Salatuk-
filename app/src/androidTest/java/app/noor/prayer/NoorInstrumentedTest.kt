package app.noor.prayer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.noor.prayer.di.AppEntryPoint
import app.noor.prayer.domain.*
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import android.app.NotificationManager
import app.noor.prayer.core.notifications.PrayerNotifications

@RunWith(AndroidJUnit4::class)
class NoorInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val entry get() = EntryPointAccessors.fromApplication(context,AppEntryPoint::class.java)
    @Before fun setup() {
        runBlocking { entry.schedules().update { PrayerSettings(location = LocationSettings(52.52,13.405,"Berlin","Germany",timeZone = "Europe/Berlin"),language = "en") } }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun navigationReachesCalendarCompassAndSettings() {
        compose.onNodeWithTag("nav_calendar").performClick()
        compose.onNodeWithText("Date").assertExists()
        compose.onNodeWithTag("nav_qibla").performClick()
        compose.onNodeWithContentDescription("Qibla direction indicator").assertExists()
        compose.onNodeWithTag("nav_settings").performClick()
        compose.onNodeWithText("Prayer calculation").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("nav_home").performClick()
        compose.onNodeWithText("Berlin").assertExists()
    }
    @Test fun atomicConsumptionRejectsDuplicateAndOldRevision() = runBlocking {
        val repository = entry.settings()
        val s = repository.read()
        val alarm = PrayerAlarm(PrayerName.FAJR,AlarmKind.PRAYER,Instant.now(),s.revision)
        assertTrue(repository.consume(alarm))
        assertFalse(repository.consume(alarm))
        entry.schedules().update { it.copy(hijriOffset = 1) }
        assertFalse(repository.consume(alarm.copy(prayer = PrayerName.ASR)))
    }
    @Test fun settingsSurviveActivityRecreation() {
        runBlocking { entry.schedules().update { it.copy(calculation = it.calculation.copy(madhhab = Madhhab.HANAFI,offsets = mapOf(PrayerName.FAJR to 7)),adhan = it.adhan.copy(prayers = setOf(PrayerName.FAJR))) } }
        compose.activityRule.scenario.recreate()
        val saved = runBlocking { entry.settings().read() }
        assertEquals(Madhhab.HANAFI,saved.calculation.madhhab)
        assertEquals(7,saved.calculation.offsets[PrayerName.FAJR])
        assertEquals(setOf(PrayerName.FAJR),saved.adhan.prayers)
    }
    @Test fun clearingLocationAndImportedVoiceDoesNotRestoreStaleValues() = runBlocking {
        entry.schedules().update { it.copy(adhan = it.adhan.copy(customAudio = mapOf(Voice.MAKKAH to "file:///test.ogg"))) }
        entry.schedules().update { it.copy(location = null, adhan = it.adhan.copy(customAudio = emptyMap())) }
        val saved = entry.settings().read()
        assertNull(saved.location)
        assertTrue(saved.adhan.customAudio.isEmpty())
    }
    @Test fun manualLocationFormSavesCoordinates() {
        runBlocking { entry.schedules().update { it.copy(location = null) } }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("City").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("City").performScrollTo().performTextReplacement("Berlin")
        compose.onNodeWithText("Country").performScrollTo().performTextReplacement("Germany")
        compose.onNodeWithText("Latitude").performScrollTo().performTextReplacement("52.52")
        compose.onNodeWithText("Longitude").performScrollTo().performTextReplacement("13.405")
        compose.onNodeWithText("Save location").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty() }
        val location = runBlocking { entry.settings().read().location }
        assertNotNull(location)
        assertEquals(52.52, location!!.latitude, 0.0001)
        assertFalse(location.automatic)
    }
    @Test fun previewNotificationStopReleasesPlayback() {
        val manager = context.getSystemService(NotificationManager::class.java)
        compose.onNodeWithTag("nav_settings").performClick()
        compose.onNodeWithText("Preview Adhan").performScrollTo().performClick()
        try {
            compose.waitUntil(10_000) { manager.activeNotifications.any { it.id == PrayerNotifications.PLAYBACK_ID } }
            val notification = manager.activeNotifications.first { it.id == PrayerNotifications.PLAYBACK_ID }.notification
            assertTrue(notification.flags and android.app.Notification.FLAG_FOREGROUND_SERVICE != 0)
            assertEquals("Stop Adhan", notification.actions.single().title.toString())
            notification.actions.single().actionIntent.send()
            compose.waitUntil(10_000) { manager.activeNotifications.none { it.id == PrayerNotifications.PLAYBACK_ID } }
        } finally {
            context.stopService(android.content.Intent(context, app.noor.prayer.service.AdhanPlaybackService::class.java))
        }
    }
}
