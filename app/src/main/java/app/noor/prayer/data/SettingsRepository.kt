package app.noor.prayer.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import app.noor.prayer.domain.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore by preferencesDataStore("settings", corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() })
@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.settingsStore
    val settings: Flow<PrayerSettings> = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map(::decode)
    suspend fun read() = settings.first()
    suspend fun update(transform: (PrayerSettings) -> PrayerSettings) {
        store.edit { p -> encode(transform(decode(p)).copy(revision = UUID.randomUUID().toString()), p) }
    }
    suspend fun consume(event: PrayerAlarm): Boolean {
        var accepted = false
        store.edit { p ->
            val key = stringPreferencesKey("last_${event.id}")
            if (p[key] != event.eventKey && decode(p).revision == event.revision) { p[key] = event.eventKey; accepted = true }
        }
        return accepted
    }
    private inline fun <reified T: Enum<T>> enum(value: String?, fallback: T): T = enumValues<T>().firstOrNull { it.name == value } ?: fallback
    private fun decode(p: Preferences): PrayerSettings {
        fun s(k: String) = p[stringPreferencesKey(k)]
        fun b(k: String, default: Boolean = true) = p[booleanPreferencesKey(k)] ?: default
        fun i(k: String, default: Int = 0) = p[intPreferencesKey(k)] ?: default
        val location = runCatching { LocationSettings(requireNotNull(s("lat")).toDouble(), requireNotNull(s("lon")).toDouble(), s("city").orEmpty(), s("country").orEmpty(), b("automatic", false), s("zone").orEmpty()) }.getOrNull()
        return PrayerSettings(location,
            CalculationSettings(enum(s("method"), Method.MUSLIM_WORLD_LEAGUE), enum(s("madhhab"), Madhhab.STANDARD), PrayerName.entries.associateWith { i("offset_${it.name}").coerceIn(-60,60) }),
            AdhanSettings(b("adhan"), PrayerName.entries.filter { it.obligatory && b("enabled_${it.name}") }.toSet(), enum(s("voice"), Voice.STANDARD), Voice.entries.mapNotNull { v -> s("audio_${v.name}")?.let { v to it } }.toMap()),
            b("notifications"), i("reminder").takeIf { it in listOf(0,5,10,15,30) } ?: 0,
            enum(s("appearance"), Appearance.SYSTEM), s("language").orEmpty().takeIf { it in listOf("", "en", "ar", "de") } ?: "", i("hijri").coerceIn(-2,2), s("revision") ?: "initial")
    }
    private fun encode(s: PrayerSettings, p: MutablePreferences) {
        fun text(k: String, v: String) { p[stringPreferencesKey(k)] = v }
        fun bool(k: String, v: Boolean) { p[booleanPreferencesKey(k)] = v }
        fun num(k: String, v: Int) { p[intPreferencesKey(k)] = v }
        if (s.location == null) {
            listOf("lat", "lon", "city", "country", "zone").forEach { p.remove(stringPreferencesKey(it)) }
            p.remove(booleanPreferencesKey("automatic"))
        } else {
            val l = s.location
            text("lat", l.latitude.toString()); text("lon", l.longitude.toString()); text("city",l.city); text("country",l.country); text("zone",l.timeZone); bool("automatic",l.automatic)
        }
        text("method",s.calculation.method.name); text("madhhab",s.calculation.madhhab.name)
        PrayerName.entries.forEach { num("offset_${it.name}",s.calculation.offsets[it] ?: 0); bool("enabled_${it.name}",it in s.adhan.prayers) }
        bool("adhan",s.adhan.enabled); text("voice",s.adhan.voice.name)
        Voice.entries.forEach { voice ->
            val uri = s.adhan.customAudio[voice]
            if (uri == null) p.remove(stringPreferencesKey("audio_${voice.name}"))
            else text("audio_${voice.name}", uri)
        }
        bool("notifications",s.notifications); num("reminder",s.reminderMinutes)
        text("appearance",s.appearance.name); text("language",s.language); num("hijri",s.hijriOffset); text("revision",s.revision)
    }
}
