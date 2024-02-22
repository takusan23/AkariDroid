package io.github.takusan23.akaridroid.v2.ui.screen

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/** 画面の切り替えを担当する */
@Composable
fun AkariDroidMainScreen() {
    // 画面遷移
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = NavigationPathsV2.VideoEditor.path) {
        composable(NavigationPathsV2.VideoEditor.path) {
            VideoEditorScreen()
        }
        composable(NavigationPathsV2.Setting.path) {
            SettingScreen()
        }
    }
}

/** 画面遷移先 */
enum class NavigationPathsV2(val path: String) {

    /** 動画編集画面 */
    VideoEditor("editor"),

    /** 設定画面 */
    Setting("setting")
}