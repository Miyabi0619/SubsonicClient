package com.miyabi0619.subsonicclient.player

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentTitle: String? = null,
    val currentArtist: String? = null,
    val isReady: Boolean = false,
    val hasController: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queueIndex: Int = 0,
    val queueSize: Int = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = 0  // Player.REPEAT_MODE_OFF=0, ONE=1, ALL=2
)
