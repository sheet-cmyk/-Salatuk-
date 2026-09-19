package app.noor.prayer.core.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class RadioState(val stationId: String? = null, val stationName: String = "", val playing: Boolean = false, val buffering: Boolean = false)

/** In-process shared playback state: written by RadioPlaybackService, read by the UI (as a Flow)
 *  and synchronously by the widget (same process, so a direct .value read is safe and instant). */
@Singleton
class RadioRepository @Inject constructor() {
    private val _state = MutableStateFlow(RadioState())
    val state: StateFlow<RadioState> = _state.asStateFlow()
    fun update(transform: (RadioState) -> RadioState) { _state.value = transform(_state.value) }
}
