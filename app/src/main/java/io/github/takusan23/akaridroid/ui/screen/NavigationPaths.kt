package io.github.takusan23.akaridroid.ui.screen

/**
 * 画面遷移先一覧
 *
 * @param string パス
 */
enum class NavigationPaths(val path: String) {

    /** 最初の画面 */
    Home("home"),

    /** 動画作成画面 */
    CreateVideo("create_video"),

    /** 編集画面 */
    VideoEditor("editor?project_id={project_id}");

    companion object {

        /** [VideoEditor]を開く */
        fun createEditorPath(projectId: String): String = "editor?project_id=${projectId}"
    }
}