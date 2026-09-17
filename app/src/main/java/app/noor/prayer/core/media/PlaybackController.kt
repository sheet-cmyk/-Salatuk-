package app.noor.prayer.core.media

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import app.noor.prayer.domain.*
import app.noor.prayer.service.AdhanPlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackController @Inject constructor(@ApplicationContext private val context: Context) {
    fun start(prayer: PrayerName, settings: PrayerSettings): Boolean = try {
        ContextCompat.startForegroundService(context,Intent(context,AdhanPlaybackService::class.java)
            .setAction(AdhanPlaybackService.PLAY).putExtra("prayer",prayer.name).putExtra("language",settings.language)
            .putExtra("audio",settings.adhan.customAudio[settings.adhan.voice]))
        true
    } catch (_: IllegalStateException) { false } catch (_: SecurityException) { false }
    fun stop() { context.stopService(Intent(context,AdhanPlaybackService::class.java)) }
}
