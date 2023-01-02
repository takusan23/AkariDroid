package io.github.takusan23.akaricore.data

/**
 * 音声エンコーダーに必要な情報
 *
 * null時は多分元データの情報が使われます
 *
 * @param codecName コーデックの名前
 * @param bitRate ビットレート
 */
data class AudioEncoderData(
    val codecName: String,
    val bitRate: Int? = null,
)