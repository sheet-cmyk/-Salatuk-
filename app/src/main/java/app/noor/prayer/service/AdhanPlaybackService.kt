package app.noor.prayer.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.IBinder
import android.os.Build
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
import app.noor.prayer.core.notifications.PrayerNotifications
import app.noor.prayer.core.notifications.labelResource
import app.noor.prayer.domain.PrayerName
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Foreground Service + Media3 MediaSession: no service remains alive between prayers. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@AndroidEntryPoint
class AdhanPlaybackService : Service() {
    companion object { const val PLAY = "app.noor.PLAY"; const val STOP = "app.noor.STOP" }
    @Inject lateinit var notifications: PrayerNotifications
    @Inject lateinit var feedback: app.noor.prayer.core.media.PlaybackFeedback
    private var player: ExoPlayer? = null
    private var session: MediaSession? = null
    private lateinit var audio: AudioManager
    private var focus: AudioFocusRequest? = null
    private val foregroundType get() = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() { super.onCreate(); audio = getSystemService(AudioManager::class.java); notifications.channels() }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.action != PLAY) { finish(); return START_NOT_STICKY }
        val prayer = runCatching { PrayerName.valueOf(intent.getStringExtra("prayer").orEmpty()) }.getOrDefault(PrayerName.FAJR)
        // Foreground promotion precedes focus request (required for target 35+).
        ServiceCompat.startForeground(this,PrayerNotifications.PLAYBACK_ID,notifications.playback(prayer,intent.getStringExtra("language").orEmpty()),foregroundType)
        releasePlayback()
        // Adhan must never share airtime with the Quran radio -- stop it outright rather than
        // relying on audio-focus ducking, since focus ducking would leave it silently paused
        // and liable to resume mid-Adhan once focus is abandoned.
        startService(Intent(this,RadioPlaybackService::class.java).setAction(RadioPlaybackService.STOP))
        // Request focus as a courtesy so well-behaved apps duck -- but the Adhan plays on the ALARM
        // stream either way and must never be skipped just because focus was denied (e.g. an active
        // call, another alarm, or a media app holding it). A missed prayer beats a polite silence.
        focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ALARM).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .build()
        audio.requestAudioFocus(requireNotNull(focus))
        try {
            val uri = intent.getStringExtra("audio")?.toUri() ?: "android.resource://$packageName/${R.raw.adhan_standard}".toUri()
            val exo = ExoPlayer.Builder(this).build()
            player = exo
            exo.setAudioAttributes(androidx.media3.common.AudioAttributes.Builder().setUsage(C.USAGE_ALARM).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),false)
            exo.setHandleAudioBecomingNoisy(true)
            exo.setWakeMode(C.WAKE_MODE_LOCAL)
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) { if(state == Player.STATE_ENDED || state == Player.STATE_IDLE) finish() }
                override fun onPlayerError(error: PlaybackException) { feedback.errors.tryEmit(R.string.playback_failed); finish() }
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    if(!playWhenReady && (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY || reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)) finish()
                }
            })
            session = MediaSession.Builder(this,exo).build()
            ServiceCompat.startForeground(this,PrayerNotifications.PLAYBACK_ID,notifications.playback(prayer,intent.getStringExtra("language").orEmpty(),session),foregroundType)
            exo.setMediaItem(MediaItem.Builder().setUri(uri).setMediaMetadata(MediaMetadata.Builder().setTitle(getString(prayer.labelResource())).setArtist(getString(R.string.app_name)).build()).build())
            exo.prepare(); exo.play()
        } catch (_: RuntimeException) { feedback.errors.tryEmit(R.string.playback_failed); finish() }
        return START_NOT_STICKY
    }
    private fun releasePlayback() {
        session?.release(); session = null
        player?.release(); player = null
        focus?.let { audio.abandonAudioFocusRequest(it) }; focus = null
    }
    private fun finish() { releasePlayback(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    override fun onDestroy() { releasePlayback(); stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy() }
}
