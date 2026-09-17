package app.noor.prayer.feature

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.noor.prayer.R
import app.noor.prayer.core.common.*
import app.noor.prayer.core.location.*
import app.noor.prayer.core.media.*
import app.noor.prayer.data.*
import app.noor.prayer.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.*
import javax.inject.Inject

data class PrayerUiState(val loaded: Boolean = false, val settings: PrayerSettings = PrayerSettings(), val daily: DailyPrayerTimes? = null, val next: PrayerTime? = null, val now: Instant = Instant.now(), val calculationError: Boolean = false)
@HiltViewModel
class PrayerViewModel @Inject constructor(private val repository: SettingsRepository, private val schedules: PrayerScheduleRepository, private val location: LocationRepository, private val capabilitiesRepository: CapabilityRepository, private val playback: PlaybackController, private val audio: AudioRepository, private val feedback: PlaybackFeedback, val engine: PrayerEngine): ViewModel() {
    private val ticker = flow { while(currentCoroutineContext().isActive) { emit(Instant.now()); delay(1000) } }
    private var cachedKey = ""
    private var daily: DailyPrayerTimes? = null
    private var upcoming = emptyList<PrayerTime>()
    private var failed = false
    val state = combine(repository.settings,ticker) { s, now ->
        val date = now.atZone(s.location?.zone() ?: ZoneId.systemDefault()).toLocalDate()
        val key = "${s.revision}:$date:${s.location?.zone()}"
        if(key != cachedKey) {
            cachedKey = key; failed = false; daily = null; upcoming = emptyList()
            if(s.location != null) runCatching { daily = engine.calculate(date,s); upcoming = engine.upcoming(s,now.minusSeconds(1)) }.onFailure { failed = true }
        }
        PrayerUiState(true,s,daily,upcoming.firstOrNull { it.instant > now },now,failed)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),PrayerUiState())
    val capabilities = MutableStateFlow(Capabilities())
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<Int?>(null)
    init { viewModelScope.launch { feedback.errors.collect { message.value = it } } }
    private var lastLocation = Instant.EPOCH
    fun clearMessage() { message.value = null }
    fun resume() {
        capabilities.value = capabilitiesRepository.read()
        viewModelScope.launch {
            runCatching { schedules.reschedule() }.onFailure { message.value = R.string.schedule_failed }
            val s = repository.read()
            if(s.location?.automatic == true && capabilities.value.location && Duration.between(lastLocation,Instant.now()).toHours() >= 6) locate()
        }
    }
    fun update(transform: (PrayerSettings) -> PrayerSettings) {
        viewModelScope.launch { runCatching { schedules.update(transform) }.onFailure { message.value = R.string.save_failed } }
    }
    fun locate() {
        if(busy.value) return
        busy.value = true
        viewModelScope.launch {
            try { val value = location.current(); schedules.update { it.copy(location = value) }; lastLocation = Instant.now() }
            catch(e: CancellationException) { if(e !is TimeoutCancellationException) throw e; message.value = R.string.location_failed }
            catch(_: Exception) { message.value = R.string.location_failed }
            finally { busy.value = false; capabilities.value = capabilitiesRepository.read() }
        }
    }
    fun preview() { viewModelScope.launch { if(!playback.start(PrayerName.FAJR,repository.read())) message.value = R.string.playback_failed } }
    fun stop() = playback.stop()
    fun importAudio(uri: Uri, voice: Voice) { viewModelScope.launch {
        try { val path = audio.import(uri,voice); schedules.update { it.copy(adhan = it.adhan.copy(voice = voice,customAudio = it.adhan.customAudio + (voice to path))) } }
        catch(_: Exception) { message.value = R.string.audio_failed }
    } }
    suspend fun calendar(month: YearMonth, settings: PrayerSettings): List<DailyPrayerTimes> = withContext(Dispatchers.Default) { (1..month.lengthOfMonth()).map { engine.calculate(month.atDay(it),settings) } }
}
