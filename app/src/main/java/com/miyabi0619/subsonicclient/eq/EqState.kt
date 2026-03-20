package com.miyabi0619.subsonicclient.eq

/**
 * 1バンド分のゲイン（dB）.
 */
data class EqBand(
    val centerHz: Float,
    val gainDb: Float,
    val minGainDb: Float = -12f,
    val maxGainDb: Float = 12f
)

/**
 * 現在のEQ状態（10バンド＋オン/オフ）.
 */
data class EqState(
    val bands: List<EqBand>,
    val enabled: Boolean = true,
    val presetName: String? = null,
    val hardwareAvailable: Boolean? = null  // null=未確認, true=利用可, false=非対応
) {
    fun bandGains(): FloatArray = FloatArray(bands.size) { i -> bands.getOrNull(i)?.gainDb ?: 0f }
}

/** 10バンドの中心周波数（Hz）. */
val DEFAULT_EQ_CENTER_HZ = listOf(
    32f, 64f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f
)

fun createFlatEqState(): EqState = EqState(
    bands = DEFAULT_EQ_CENTER_HZ.map { EqBand(centerHz = it, gainDb = 0f) }
)

fun createEqStateFromGains(gains: List<Float>): EqState {
    val bands = DEFAULT_EQ_CENTER_HZ.mapIndexed { i, hz ->
        EqBand(centerHz = hz, gainDb = gains.getOrNull(i) ?: 0f)
    }
    return EqState(bands = bands)
}
