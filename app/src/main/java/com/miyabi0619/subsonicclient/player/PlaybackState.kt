package com.miyabi0619.subsonicclient.player

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentTitle: String? = null,
    val currentArtist: String? = null,
    val isReady: Boolean = false,
    val hasController: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)
