package com.miyabi0619.subsonicclient.eq

/**
 * 内蔵プリセット定義（ロック／ポップなど）.
 * 各バンドのゲインは -12〜+12 dB、10バンド.
 */
enum class EqPresetType(val displayName: String, val gains: List<Float>) {
    Flat("フラット", List(10) { 0f }),
    Rock("ロック", listOf(5f, 4f, 2f, -1f, -2f, -1f, 1f, 3f, 4f, 5f)),
    Pop("ポップ", listOf(-1f, 1f, 3f, 4f, 3f, 1f, 0f, -1f, -1f, -1f)),
    Jazz("ジャズ", listOf(3f, 2f, 1f, 1f, 0f, 0f, 0f, 1f, 2f, 3f)),
    Classical("クラシック", listOf(4f, 3f, 2f, 1f, 0f, 0f, 1f, 2f, 3f, 4f)),
    Vocal("ボーカル", listOf(-2f, -1f, 1f, 3f, 4f, 4f, 3f, 1f, -1f, -2f));
}

fun EqPresetType.toEqState(): EqState = createEqStateFromGains(gains).copy(presetName = displayName)
