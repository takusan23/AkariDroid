package io.github.takusan23.akaridroid.data

import kotlinx.serialization.Serializable

/**
 * BGMなどの音声素材のデータクラス
 *
 * @param audioFilePath 音声素材の保存先
 * @param volume ボリューム。BGMうるさい場合に使う。0..1f で
 */
@Serializable
data class AudioAssetData(
    val audioFilePath: String,
    val volume: Float = 1f
)