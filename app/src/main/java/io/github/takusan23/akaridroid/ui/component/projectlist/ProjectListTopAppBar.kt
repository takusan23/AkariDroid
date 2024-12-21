package io.github.takusan23.akaridroid.ui.component.projectlist

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.takusan23.akaridroid.R

/**
 * プロジェクト一覧画面のアプリバー
 *
 * @param modifier [Modifier]
 * @param scrollBehavior スクロールで隠れるように
 * @param onSettingClick 設定押した時
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListTopAppBar(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
    onSettingClick: () -> Unit
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(id = R.string.app_name)) },
        scrollBehavior = scrollBehavior,
        actions = {
            IconButton(onClick = onSettingClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_settings_24px),
                    contentDescription = null
                )
            }
        }
    )
}