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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.ProjectListItem
import io.github.takusan23.akaridroid.viewmodel.ProjectListViewModel

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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val projectList = viewModel.projectListFlow.collectAsState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.project_list)) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(Modifier.padding(innerPadding)) {

            items(projectList.value) { item ->
                ProjectListItem(
                    projectItem = item,
                    onClick = { onOpen(it.projectName) }
                )
                HorizontalDivider()
            }

        }
    }
}