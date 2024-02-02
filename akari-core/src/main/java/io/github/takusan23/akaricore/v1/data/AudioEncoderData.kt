package io.github.takusan23.akaricore.v1.data

/**
 * 音声エンコーダーに必要な情報
 *
 * null時は多分元データの情報が使われます
 *
 * @param codecName コーデックの名前
 * @param bitRate ビットレート
 * @param mixingVolume 音声素材（動画以外、BGM）の音量を 0..1f で
 */
data class AudioEncoderData(
    val codecName: String,
    val bitRate: Int = 128_000,
    val mixingVolume: Float = 0.10f
)