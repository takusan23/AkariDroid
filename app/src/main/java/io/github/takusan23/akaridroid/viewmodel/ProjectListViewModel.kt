package io.github.takusan23.akaridroid.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.takusan23.akaridroid.tool.ProjectFolderManager
import io.github.takusan23.akaridroid.tool.data.ProjectItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** [io.github.takusan23.akaridroid.ui.screen.ProjectListScreenKt]で使う ViewModel */
class ProjectListViewModel(private val application: Application) : AndroidViewModel(application) {

    private val context: Context
        get() = application.applicationContext

    private val _projectListFlow = MutableStateFlow(emptyList<ProjectItem>())

    /** プロジェクト一覧 */
    val projectListFlow = _projectListFlow.asStateFlow()

    init {
        viewModelScope.launch {
            _projectListFlow.value = ProjectFolderManager.loadProjectList(context)
        }
    }

}