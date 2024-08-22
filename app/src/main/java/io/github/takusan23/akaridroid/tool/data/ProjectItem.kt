package io.github.takusan23.akaridroid.tool.data

/**
 * プロジェクト一覧の各アイテム
 *
 * @param projectName プロジェクト名
 * @param lastModifiedDate 最終更新日
 * @param videoDurationMs 動画の時間
 */
data class ProjectItem(
    val projectName: String,
    val lastModifiedDate: Long,
    val videoDurationMs: Long
)