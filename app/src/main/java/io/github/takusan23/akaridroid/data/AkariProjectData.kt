package io.github.takusan23.akaridroid.data

import kotlinx.serialization.Serializable

/**
 * プロジェクトデータ
 *
 * @param projectId プロジェクトのID
 * @param videoFilePath 動画パス
 * @param videoOutputFormat 動画出力形式、エンコーダー設定
 * @param audioAssetList 音声素材の配列
 * @param canvasElementList 描画する要素の配列
 */
@Serializable
data class AkariProjectData(
    val projectId: String = "project-2022-01-10", // TODO ハードコートしない
    val videoFilePath: String? = null,
    val videoOutputFormat: VideoOutputFormat = VideoOutputFormat(),
    val audioAssetList: List<AudioAssetData> = emptyList(),
    val canvasElementList: List<CanvasElementData> = emptyList()
)