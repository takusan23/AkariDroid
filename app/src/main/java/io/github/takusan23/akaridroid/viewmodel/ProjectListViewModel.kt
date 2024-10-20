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

    /** プロジェクト一覧 */
    val projectListFlow = _projectListFlow.asStateFlow()

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
     * @param portableName zip ファイル名
     * @param zipUri zip ファイルの保存先
     */
    fun exportPortableProject(name: String, portableName: String, zipUri: Uri) {
        viewModelScope.launch {
            // TODO 下位互換性
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ProjectFolderManager.exportPortableProject(context, name, portableName, zipUri)

                // TODO Snackbar
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
            ProjectFolderManager.importPortableProject(context, zipUri)

            // TODO Snackbar
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.project_list_bottomsheet_menu_import_successful), Toast.LENGTH_SHORT).show()
            }
            loadProjectList()
        }
    }

    /** プロジェクト一覧を取得する */
    private suspend fun loadProjectList() {
        _projectListFlow.value = ProjectFolderManager.loadProjectList(context)
    }

}