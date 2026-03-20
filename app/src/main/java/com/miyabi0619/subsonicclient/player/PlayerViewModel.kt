package com.miyabi0619.subsonicclient.player

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionUpdateJob: Job? = null

    init {
        connectController()
    }

    private fun connectController() {
        val context = getApplication<Application>().applicationContext
        val sessionToken = SessionToken(context, ComponentName(context, PlayerService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val ctrl = controllerFuture?.get()
                controller = ctrl
                ctrl?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) { updateStateFromPlayer(ctrl) }
                    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) { updateStateFromPlayer(ctrl) }
                    override fun onPlaybackStateChanged(playbackState: Int) { updateStateFromPlayer(ctrl) }
                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { updateStateFromPlayer(ctrl) }
                    override fun onRepeatModeChanged(repeatMode: Int) { updateStateFromPlayer(ctrl) }
                })
                updateStateFromPlayer(ctrl)
                _playbackState.value = _playbackState.value.copy(hasController = ctrl != null)
                startPositionUpdates(ctrl)
            } catch (_: Exception) { }
        }, MoreExecutors.directExecutor())
    }

    private fun updateStateFromPlayer(player: Player?) {
        if (player == null) return
        val mediaItem = player.currentMediaItem
        _playbackState.value = _playbackState.value.copy(
            isPlaying = player.isPlaying,
            isReady = player.playbackState == Player.STATE_READY,
            currentTitle = mediaItem?.mediaMetadata?.title?.toString(),
            currentArtist = mediaItem?.mediaMetadata?.artist?.toString(),
            currentSongId = mediaItem?.mediaId?.takeIf { it.isNotBlank() },
            hasController = true,
            positionMs = player.currentPosition,
            durationMs = player.duration.coerceAtLeast(0L),
            queueIndex = player.currentMediaItemIndex,
            queueSize = player.mediaItemCount,
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode
        )
    }

    fun toggleShuffle() {
        controller?.let { p -> p.shuffleModeEnabled = !p.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        controller?.let { p ->
            p.repeatMode = when (p.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun seekToMediaItem(index: Int) {
        controller?.seekToDefaultPosition(index)
    }

    private fun startPositionUpdates(player: Player?) {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive && controller != null) {
                controller?.let { updateStateFromPlayer(it) }
                delay(500)
            }
        }
    }

    fun play(songId: String, queueIds: List<String>, title: String? = null, artist: String? = null) {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PLAY
            putExtra(PlayerService.EXTRA_SONG_ID, songId)
            putExtra(PlayerService.EXTRA_QUEUE_IDS, ArrayList(queueIds))
            title?.let { putExtra(PlayerService.EXTRA_TITLE, it) }
            artist?.let { putExtra(PlayerService.EXTRA_ARTIST, it) }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            controller?.let { updateStateFromPlayer(it) }
        }
    }

    fun playPause() {
        controller?.let { p ->
            if (p.isPlaying) p.pause() else p.play()
        }
    }

    fun pause() {
        controller?.pause()
    }

    fun play() {
        controller?.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun seekToNext() {
        controller?.seekToNext()
    }

    fun seekToPrevious() {
        controller?.seekToPrevious()
    }

    override fun onCleared() {
        positionUpdateJob?.cancel()
        super.onCleared()
        MediaController.releaseFuture(controllerFuture ?: return)
        controller = null
    }
}
