package io.github.takusan23.akaridroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.takusan23.akaridroid.ui.screen.about.AboutScreen
import io.github.takusan23.akaridroid.ui.screen.about.AboutSushiScreen

/** 画面の切り替えを担当する */
@Composable
fun AkariDroidMainScreen() {
    // 画面遷移
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = NavigationPaths.VideoEditor.path) {
        composable(NavigationPaths.VideoEditor.path) {
            VideoEditorScreen(
                onNavigate = { navigationPaths -> navController.navigate(navigationPaths.path) }
            )
        }
        composable(NavigationPaths.Setting.path) {
            SettingScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navigationPaths -> navController.navigate(navigationPaths.path) }
            )
        }
        composable(NavigationPaths.About.path) {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navigationPaths -> navController.navigate(navigationPaths.path) }
            )
        }
        composable(NavigationPaths.SushiScreen.path) {
            AboutSushiScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/** 画面遷移先 */
enum class NavigationPaths(val path: String) {

    /** 動画編集画面 */
    VideoEditor("editor"),

    /** 設定画面 */
    Setting("setting"),

    /** このアプリについて画面 */
    About("about"),

    /** おまけ画面 */
    SushiScreen("sushi")
}