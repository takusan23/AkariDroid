package io.github.takusan23.akaridroid.data

import kotlinx.serialization.Serializable

/**
 * 動画ファイルのデータ
 *
 * @param videoFilePath ファイルパス
 * @param fileName ファイル名
 */
@Serializable
data class VideoFileData(
    val videoFilePath: String,
    val fileName: String
)