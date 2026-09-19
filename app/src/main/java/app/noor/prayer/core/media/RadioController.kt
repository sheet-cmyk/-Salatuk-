package app.noor.prayer.core.media

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import app.noor.prayer.service.RadioPlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioController @Inject constructor(@ApplicationContext private val context: Context) {
    private fun send(action: String, stationId: String? = null) = try {
        ContextCompat.startForegroundService(context,Intent(context,RadioPlaybackService::class.java).setAction(action).putExtra("stationId",stationId))
        true
    } catch (_: IllegalStateException) { false } catch (_: SecurityException) { false }
    fun play(stationId: String) = send(RadioPlaybackService.PLAY,stationId)
    fun toggle() = send(RadioPlaybackService.TOGGLE)
    fun next() = send(RadioPlaybackService.NEXT)
    fun previous() = send(RadioPlaybackService.PREV)
    fun stop() { context.stopService(Intent(context,RadioPlaybackService::class.java)) }
}
