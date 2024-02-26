package io.github.takusan23.akaridroid.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R

/** 設定画面、仮 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    onBack: () -> Unit,
    onNavigate: (NavigationPaths) -> Unit
) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "setting") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(id = R.drawable.ic_outline_arrow_back_24px), contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {

            item {
                SettingItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = "このアプリについて",
                    description = "GitHub のリンクとか",
                    onClick = { onNavigate(NavigationPaths.About) }
                )
            }

        }
    }
}

/**
 * 設定項目
 *
 * @param modifier [Modifier]
 * @param title タイトル
 * @param description 説明
 * @param onClick 押した時
 */
@Composable
private fun SettingItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, fontSize = 20.sp)
            Text(text = description)
        }
    }
}