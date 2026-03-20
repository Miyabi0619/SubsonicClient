package com.miyabi0619.subsonicclient.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.miyabi0619.subsonicclient.R
import com.miyabi0619.subsonicclient.data.api.SubsonicStreamUrlBuilder
import com.miyabi0619.subsonicclient.data.prefs.CredentialsStore
import com.miyabi0619.subsonicclient.data.prefs.AppSettingsStore
import com.miyabi0619.subsonicclient.eq.EqApplier
import com.miyabi0619.subsonicclient.eq.EqStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PlayerService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private lateinit var credentialsStore: CredentialsStore
    private lateinit var eqStore: EqStore
    private lateinit var appSettingsStore: AppSettingsStore
    private var eqApplier: EqApplier? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        credentialsStore = CredentialsStore(applicationContext)
        eqStore = EqStore(applicationContext)
        appSettingsStore = AppSettingsStore(applicationContext)
        val exoPlayer = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
        player = exoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer).build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_playback),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val songId = intent.getStringExtra(EXTRA_SONG_ID)
                val queueIds = intent.getStringArrayListExtra(EXTRA_QUEUE_IDS)
                val title = intent.getStringExtra(EXTRA_TITLE)
                val artist = intent.getStringExtra(EXTRA_ARTIST)
                if (!songId.isNullOrBlank()) {
                    startForeground(NOTIFICATION_ID, createPlaceholderNotification())
                    serviceScope.launch {
                        playSong(songId, queueIds ?: arrayListOf(songId), title, artist)
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createPlaceholderNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("再生中")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()

    private suspend fun playSong(
        songId: String,
        queueIds: List<String>,
        title: String? = null,
        artist: String? = null
    ) {
        val creds = credentialsStore.credentials.first() ?: return
        val maxBitRate = appSettingsStore.maxBitRate.first().let { if (it > 0) it else null }
        val startIndex = queueIds.indexOf(songId).coerceAtLeast(0)
        val mediaItems = queueIds.mapIndexed { index, id ->
            val url = SubsonicStreamUrlBuilder.build(
                baseUrl = creds.serverUrl,
                username = creds.username,
                password = creds.password,
                songId = id,
                maxBitRate = maxBitRate
            )
            if (index == startIndex && (title != null || artist != null)) {
                MediaItem.Builder()
                    .setUri(url)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title ?: "")
                            .setArtist(artist ?: "")
                            .build()
                    )
                    .build()
            } else {
                MediaItem.fromUri(url)
            }
        }
        player?.let { p ->
            p.setMediaItems(mediaItems, startIndex, 0L)
            p.prepare()
            p.play()
            attachEqIfNeeded(p.audioSessionId)
        }
    }

    private fun attachEqIfNeeded(audioSessionId: Int) {
        if (audioSessionId == 0) return
        eqApplier?.release()
        val applier = EqApplier(audioSessionId)
        val attached = applier.attach()
        serviceScope.launch { eqStore.setHardwareAvailable(attached) }
        if (attached) {
            serviceScope.launch {
                eqStore.eqState.first().let { applier.apply(it) }
            }
            eqStore.eqState.onEach { applier.apply(it) }.launchIn(serviceScope)
            eqApplier = applier
        }
    }

    override fun onDestroy() {
        eqApplier?.release()
        eqApplier = null
        mediaSession?.run {
            player?.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.miyabi0619.subsonicclient.PLAY"
        const val EXTRA_SONG_ID = "song_id"
        const val EXTRA_QUEUE_IDS = "queue_ids"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        private const val CHANNEL_ID = "playback"
        private const val NOTIFICATION_ID = 1
    }
}
