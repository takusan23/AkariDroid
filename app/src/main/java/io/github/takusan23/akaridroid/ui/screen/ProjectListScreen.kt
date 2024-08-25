package io.github.takusan23.akaridroid.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.encoder.EncoderService
import io.github.takusan23.akaridroid.ui.bottomsheet.projectlist.ProjectListBottomSheetRequestData
import io.github.takusan23.akaridroid.ui.bottomsheet.projectlist.ProjectListBottomSheetRouter
import io.github.takusan23.akaridroid.ui.component.projectlist.EncodingListItem
import io.github.takusan23.akaridroid.ui.component.projectlist.ProjectListItem
import io.github.takusan23.akaridroid.ui.component.projectlist.ProjectListMenu
import io.github.takusan23.akaridroid.viewmodel.ProjectListViewModel
import kotlinx.coroutines.launch

/**
 * プロジェクト一覧画面
 *
 * @param viewModel [ProjectListViewModel]
 * @param onOpen プロジェクト選択時。引数はプロジェクト名
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: ProjectListViewModel = viewModel(),
    onOpen: (String) -> Unit
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
    val bottomSheetRequestData = remember { mutableStateOf<ProjectListBottomSheetRequestData?>(null) }
    if (bottomSheetRequestData.value != null) {
        ProjectListBottomSheetRouter(
            requestData = bottomSheetRequestData.value!!,
            onDismiss = { bottomSheetRequestData.value = null },
            onCreate = { name ->
                scope.launch {
                    viewModel.createProject(name)
                    onOpen(name)
                }
            },
            onDelete = { name -> viewModel.deleteProject(name) },
            onExport = { name, portableName, uri -> viewModel.exportPortableProject(name, portableName, uri) }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(Modifier.padding(innerPadding)) {

            item {
                ProjectListMenu(
                    modifier = Modifier.padding(10.dp),
                    onCreate = { bottomSheetRequestData.value = ProjectListBottomSheetRequestData.CreateNewProject },
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
                        onClick = { onOpen(it.projectName) },
                        onMenuClick = { bottomSheetRequestData.value = ProjectListBottomSheetRequestData.ProjectMenu(it.projectName) }
                    )
                }
                HorizontalDivider()
            }

        }
    }
}