package io.github.takusan23.akaridroid.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.akaridroid.encoder.EncoderService
import io.github.takusan23.akaridroid.ui.bottomsheet.projectlist.ProjectListBottomSheetRequestData
import io.github.takusan23.akaridroid.ui.bottomsheet.projectlist.ProjectListBottomSheetRouter
import io.github.takusan23.akaridroid.ui.component.projectlist.EncodingListItem
import io.github.takusan23.akaridroid.ui.component.projectlist.ProjectListDialogRouter
import io.github.takusan23.akaridroid.ui.component.projectlist.ProjectListItem
import io.github.takusan23.akaridroid.ui.component.projectlist.ProjectListMenu
import io.github.takusan23.akaridroid.ui.component.projectlist.ProjectListTopAppBar
import io.github.takusan23.akaridroid.ui.component.projectlist.data.ProjectListDialogRequestData
import io.github.takusan23.akaridroid.viewmodel.ProjectListViewModel
import kotlinx.coroutines.launch

/**
 * プロジェクト一覧画面
 *
 * @param viewModel [ProjectListViewModel]
 * @param onOpen プロジェクト選択時。引数はプロジェクト名と、新規作成かどうか
 * @param onNavigate 画面遷移時に呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: ProjectListViewModel = viewModel(),
    onOpen: (projectName: String, isCreateNew: Boolean) -> Unit,
    onNavigate: (NavigationPaths) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val projectList = viewModel.projectListFlow.collectAsState()

    // バックグラウンドでエンコードできるようにエンコーダーサービス
    val encoderService = remember { EncoderService.bindEncoderService(context, lifecycle) }.collectAsStateWithLifecycle(initialValue = null)
    // エンコード中かどうか
    val encodeStatus = encoderService.value?.encodeStatusFlow?.collectAsStateWithLifecycle()

    // ボトムシート
    val bottomSheetRequestData = viewModel.bottomSheetRequestFlow.collectAsStateWithLifecycle()
    if (bottomSheetRequestData.value != null) {
        ProjectListBottomSheetRouter(
            requestData = bottomSheetRequestData.value!!,
            onDismiss = { viewModel.closeBottomSheet() },
            onCreate = { name ->
                scope.launch {
                    viewModel.createProject(name)
                    onOpen(name, true)
                }
            },
            onDeleteMenuClick = { name -> viewModel.showDialog(ProjectListDialogRequestData.ProjectDeleteDialog(name)) },
            onExportMenuClick = { name, uri -> viewModel.exportPortableProject(name, uri) }
        )
    }

    // ダイアログ
    val dialogRequestData = viewModel.dialogRequestFlow.collectAsStateWithLifecycle()
    if (dialogRequestData.value != null) {
        ProjectListDialogRouter(
            dialogRequestData = dialogRequestData.value!!,
            onDismiss = { viewModel.closeDialog() },
            onDelete = { name -> viewModel.deleteProject(name) }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ProjectListTopAppBar(
                scrollBehavior = scrollBehavior,
                onSettingClick = { onNavigate(NavigationPaths.Setting) }
            )
        }
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {

            item {
                ProjectListMenu(
                    modifier = Modifier.padding(10.dp),
                    onCreate = { viewModel.showBottomSheet(ProjectListBottomSheetRequestData.CreateNewProject) },
                    onImport = { uri -> viewModel.importPortableProject(uri) }
                )
            }

            items(projectList.value) { item ->
                // エンコード用のリスト表示と分岐
                if (encodeStatus?.value?.projectName == item.projectName) {
                    EncodingListItem(
                        projectItem = item,
                        encodeStatus = encodeStatus.value!!,
                        onCancel = { encoderService.value?.stop() }
                    )
                } else {
                    ProjectListItem(
                        projectItem = item,
                        onClick = { onOpen(it.projectName, false) },
                        onMenuClick = { viewModel.showBottomSheet(ProjectListBottomSheetRequestData.ProjectMenu(it.projectName)) }
                    )
                }
                HorizontalDivider()
            }

        }
    }
}