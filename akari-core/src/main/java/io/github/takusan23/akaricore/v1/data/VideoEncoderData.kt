package io.github.takusan23.akaricore.v1.data

/**
 * 動画エンコーダーに必要な情報
 *
 * @param codecName コーデックの名前
 * @param width 動画の幅
 * @param height 動画の高さ
 * @param bitRate ビットレート
 * @param frameRate フレームレート
 */
data class VideoEncoderData(
    val codecName: String,
    val width: Int = 1280,
    val height: Int = 720,
    val bitRate: Int = 1_000_000,
    val frameRate: Int = 30,
)