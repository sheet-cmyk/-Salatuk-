package app.noor.prayer.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import app.noor.prayer.R
import app.noor.prayer.core.media.RadioRepository
import app.noor.prayer.core.notifications.PrayerNotifications
import app.noor.prayer.domain.RadioStations
import app.noor.prayer.widget.PrayerWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Foreground Service + Media3 MediaSession for continuous Quran-radio streaming. Unlike the Adhan
 *  service this is regular media: it plays on the music stream and pauses (rather than overriding)
 *  when it genuinely loses focus, e.g. to a phone call. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@AndroidEntryPoint
class RadioPlaybackService : Service() {
    companion object { const val PLAY = "app.noor.RADIO_PLAY"; const val TOGGLE = "app.noor.RADIO_TOGGLE"; const val NEXT = "app.noor.RADIO_NEXT"; const val PREV = "app.noor.RADIO_PREV"; const val STOP = "app.noor.RADIO_STOP" }
    @Inject lateinit var notifications: PrayerNotifications
    @Inject lateinit var radio: RadioRepository
    private val scope = CoroutineScope(Dispatchers.Main)
    private var player: ExoPlayer? = null
    private var session: MediaSession? = null
    private var currentIndex = 0
    private val foregroundType get() = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() { super.onCreate(); notifications.channels() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            PLAY -> { currentIndex = RadioStations.indexOfFirst { it.id == intent.getStringExtra("stationId") }.coerceAtLeast(0); start() }
            TOGGLE -> if (player == null) start() else toggle()
            NEXT -> { currentIndex = (currentIndex + 1) % RadioStations.size; start() }
            PREV -> { currentIndex = (currentIndex - 1 + RadioStations.size) % RadioStations.size; start() }
            STOP -> finish()
            else -> finish()
        }
        return START_NOT_STICKY
    }

    private fun toggle() {
        val exo = player ?: return
        exo.playWhenReady = !exo.playWhenReady
        pushState(buffering = false)
    }

    private fun start() {
        val station = RadioStations.getOrNull(currentIndex) ?: return finish()
        releasePlayback()
        ServiceCompat.startForeground(this,PrayerNotifications.RADIO_ID,notifications.radio(station.name,true),foregroundType)
        try {
            val exo = ExoPlayer.Builder(this).build()
            player = exo
            exo.setAudioAttributes(androidx.media3.common.AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),true)
            exo.setWakeMode(C.WAKE_MODE_NETWORK)
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) { pushState(buffering = state == Player.STATE_BUFFERING); if (state == Player.STATE_IDLE) finish() }
                override fun onPlayerError(error: PlaybackException) { pushState(buffering = false); finish() }
                override fun onIsPlayingChanged(isPlaying: Boolean) { pushState() }
            })
            session = MediaSession.Builder(this,exo).build()
            exo.setMediaItem(MediaItem.Builder().setUri(station.url.toUri()).setMediaMetadata(MediaMetadata.Builder().setTitle(station.name).setArtist(getString(R.string.radio)).build()).build())
            exo.prepare(); exo.playWhenReady = true
        } catch (_: RuntimeException) { finish() }
    }

    private fun pushState(buffering: Boolean = false) {
        val exo = player; val station = RadioStations.getOrNull(currentIndex)
        val playing = exo?.playWhenReady == true
        radio.update { it.copy(stationId = station?.id, stationName = station?.name.orEmpty(), playing = playing, buffering = buffering) }
        if (station != null) ServiceCompat.startForeground(this,PrayerNotifications.RADIO_ID,notifications.radio(station.name,playing,session),foregroundType)
        scope.launch { PrayerWidget.refresh(this@RadioPlaybackService) }
    }

    private fun releasePlayback() {
        session?.release(); session = null
        player?.release(); player = null
    }
    private fun finish() {
        releasePlayback()
        radio.update { it.copy(playing = false, buffering = false) }
        scope.launch { PrayerWidget.refresh(this@RadioPlaybackService) }
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }
    override fun onDestroy() { releasePlayback(); stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy() }
}
