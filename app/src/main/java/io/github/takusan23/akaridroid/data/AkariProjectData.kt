package io.github.takusan23.akaridroid.data

import kotlinx.serialization.Serializable

/**
 * プロジェクトデータ
 *
 * @param projectId プロジェクトのID
 * @param videoFilePath 動画パス
 * @param canvasElementList 描画する要素の配列
 */
@Serializable
data class AkariProjectData(
    val projectId: String = "project-2022-01-10", // TODO ハードコートしない
    val videoFilePath: String?,
    val canvasElementList: List<CanvasElementData>
)