package io.github.takusan23.akaridroid.v2.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** 設定画面、仮 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen() {
    Scaffold(
        topBar = {
            LargeTopAppBar(title = { Text(text = "setting") })
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {

        }
    }
}