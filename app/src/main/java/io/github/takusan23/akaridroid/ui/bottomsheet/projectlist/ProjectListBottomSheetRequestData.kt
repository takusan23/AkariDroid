package io.github.takusan23.akaridroid.ui.bottomsheet.projectlist

/** プロジェクト一覧画面のボトムシートの種類 */
sealed interface ProjectListBottomSheetRequestData {

    /** 新規作成 */
    data object CreateNewProject : ProjectListBottomSheetRequestData
}