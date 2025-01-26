package io.github.takusan23.akaridroid.ui.component.projectlist.data

/** プロジェクト一覧画面のダイアログの種類 */
sealed interface ProjectListDialogRequestData {

    /** プロジェクト削除ダイアログ */
    data class ProjectDeleteDialog(val name: String) : ProjectListDialogRequestData

    /** プロジェクトをエクスポートしてるよダイアログ */
    data class ProjectExportDialog(val progress: Int, val totalCount: Int) : ProjectListDialogRequestData

    /** プロジェクトをインポートしているよダイアログ */
    data class ProjectImportDialog(val progress: Int, val totalCount: Int) : ProjectListDialogRequestData
}