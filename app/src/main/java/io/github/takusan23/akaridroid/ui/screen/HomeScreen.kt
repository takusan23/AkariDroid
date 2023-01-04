package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.screen.NavigationPaths

/**
 * ホーム画面
 *
 * @param onNavigate 画面遷移の際に呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) }) }
    ) {
        Box(modifier = Modifier.padding(it)) {
            LazyColumn(contentPadding = PaddingValues(vertical = 5.dp)) {
                item {

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                    ) {
                        Text(text = "新規プロジェクトの作成")
                        Button(onClick = { onNavigate(NavigationPaths.CreateVideo.path) }) {
                            Text(text = "新規作成")
                        }
                        Button(onClick = { onNavigate(NavigationPaths.VideoEditor.path) }) {
                            Text(text = "編集画面")
                        }
                    }
                }
            }
        }
    }
}