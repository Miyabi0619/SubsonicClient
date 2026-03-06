package com.miyabi0619.subsonicclient.eq

import android.media.audiofx.Equalizer

/**
 * 端末の Equalizer に EqState を適用する.
 * 再生のオーディオセッションに紐づけて使用する.
 */
class EqApplier(private val audioSessionId: Int) {

    private var equalizer: Equalizer? = null

    fun attach(): Boolean {
        return try {
            release()
            equalizer = Equalizer(0, audioSessionId)
            equalizer?.enabled = true
            true
        } catch (e: Exception) {
            false
        }
    }

    fun apply(state: EqState) {
        val eq = equalizer ?: return
        eq.enabled = state.enabled
        if (!state.enabled) return
        val numBands = eq.numberOfBands
        val range = eq.bandLevelRange
        val minLevel = range[0].toInt()
        val maxLevel = range[1].toInt()
        val gains = state.bandGains()
        for (i in 0 until numBands) {
            val gainIndex = (i * gains.size) / numBands
            val millibels = ((gains.getOrNull(gainIndex) ?: 0f) * 100).toInt().coerceIn(minLevel, maxLevel)
            eq.setBandLevel(i.toShort(), millibels.toShort())
        }
    }

    fun release() {
        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (_: Exception) { }
        equalizer = null
    }
}
