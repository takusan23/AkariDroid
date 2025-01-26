package io.github.takusan23.akaridroid.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.tool.ProjectFolderManager
import io.github.takusan23.akaridroid.tool.data.ProjectItem
import io.github.takusan23.akaridroid.ui.bottomsheet.projectlist.ProjectListBottomSheetRequestData
import io.github.takusan23.akaridroid.ui.component.projectlist.data.ProjectListDialogRequestData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** [io.github.takusan23.akaridroid.ui.screen.ProjectListScreenKt]で使う ViewModel */
class ProjectListViewModel(private val application: Application) : AndroidViewModel(application) {

    private val context: Context
        get() = application.applicationContext

    private val _projectListFlow = MutableStateFlow(emptyList<ProjectItem>())
    private val _bottomSheetRequestFlow = MutableStateFlow<ProjectListBottomSheetRequestData?>(null)
    private val _dialogRequestFlow = MutableStateFlow<ProjectListDialogRequestData?>(null)

    /** プロジェクト一覧 */
    val projectListFlow = _projectListFlow.asStateFlow()

    /** 表示するボトムシート */
    val bottomSheetRequestFlow = _bottomSheetRequestFlow.asStateFlow()

    /** 表示するダイアログ */
    val dialogRequestFlow = _dialogRequestFlow.asStateFlow()

    init {
        viewModelScope.launch {
            loadProjectList()
        }
    }

    /**
     * プロジェクトを作成する
     *
     * @param name 名前
     * @return 名前。[io.github.takusan23.akaridroid.RenderData]の JSON が生成されているはず
     */
    suspend fun createProject(name: String): String {
        withContext(Dispatchers.IO) {
            ProjectFolderManager.createProject(context, name)
            loadProjectList()
        }
        return name
    }

    /**
     * プロジェクトを削除する
     *
     * @param name 名前
     */
    fun deleteProject(name: String) {
        viewModelScope.launch {
            ProjectFolderManager.deleteProject(context, name)
            loadProjectList()
        }
    }

    /**
     * プロジェクトを持ち出す
     *
     * @param name 名前
     * @param zipUri zip ファイルの保存先
     */
    fun exportPortableProject(name: String, zipUri: Uri) {
        viewModelScope.launch {
            // TODO 下位互換性
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                // 作業中はダイアログを出す
                ProjectFolderManager.exportPortableProject(
                    context = context,
                    name = name,
                    zipUri = zipUri,
                    onUpdateProgress = { current, total -> showDialog(ProjectListDialogRequestData.ProjectExportDialog(current, total)) }
                )

                // 終わり
                // TODO Snackbar とか欲しいかも
                closeDialog()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.project_list_bottomsheet_menu_export_successful), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * プロジェクトを取り込む
     *
     * @param zipUri 選択した zip ファイルの[Uri]
     */
    fun importPortableProject(zipUri: Uri) {
        viewModelScope.launch {

            // 作業中はダイアログを出す
            ProjectFolderManager.importPortableProject(
                context = context,
                zipUri = zipUri,
                onUpdateProgress = { current, total -> showDialog(ProjectListDialogRequestData.ProjectImportDialog(current, total)) }
            )

            // TODO Snackbar とか欲しいかも
            closeDialog()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.project_list_bottomsheet_menu_import_successful), Toast.LENGTH_SHORT).show()
            }
            loadProjectList()
        }
    }

    /**
     * ダイアログを表示させる
     *
     * @param dialogRequestData 表示したいダイアログ
     */
    fun showDialog(dialogRequestData: ProjectListDialogRequestData) {
        _dialogRequestFlow.value = dialogRequestData
    }

    /** ダイアログを閉じる */
    fun closeDialog() {
        _dialogRequestFlow.value = null
    }

    /**
     * ボトムシートを表示させる
     *
     * @param bottomSheetRequestData 表示させたいボトムシート
     */
    fun showBottomSheet(bottomSheetRequestData: ProjectListBottomSheetRequestData) {
        _bottomSheetRequestFlow.value = bottomSheetRequestData
    }

    /** ボトムシートを閉じる */
    fun closeBottomSheet() {
        _bottomSheetRequestFlow.value = null
    }

    /** プロジェクト一覧を取得する */
    private suspend fun loadProjectList() {
        _projectListFlow.value = ProjectFolderManager.loadProjectList(context)
    }

}