package io.github.takusan23.akaridroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.takusan23.akaridroid.ui.screen.about.AboutScreen
import io.github.takusan23.akaridroid.ui.screen.about.AboutSushiScreen
import io.github.takusan23.akaridroid.ui.screen.setting.FontSettingScreen
import io.github.takusan23.akaridroid.ui.screen.setting.LicenseScreen

/** 画面の切り替えを担当する */
@Composable
fun AkariDroidMainScreen() {
    // 画面遷移
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = NavigationPaths.ProjectList.path) {
        composable(NavigationPaths.ProjectList.path) {
            ProjectListScreen(
                onOpen = { projectName -> navController.navigate("${NavigationPaths.VideoEditor.path}/${projectName}") }
            )
        }
        composable("${NavigationPaths.VideoEditor.path}/{projectName}") { backStackEntry ->
            val projectName = backStackEntry.arguments?.getString("projectName")!!
            VideoEditorScreen(
                projectName = projectName,
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
        composable(NavigationPaths.FontSetting.path) {
            FontSettingScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavigationPaths.SushiScreen.path) {
            AboutSushiScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavigationPaths.License.path) {
            LicenseScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/** 画面遷移先 */
enum class NavigationPaths(val path: String) {

    /** プロジェクト一覧画面 */
    ProjectList("project_list"),

    /** 動画編集画面。パラメーターを渡すと editor/{projectName} です。 */
    VideoEditor("editor"),

    /** 設定画面 */
    Setting("setting"),

    /** フォント設定 */
    FontSetting("setting_font"),

    /** このアプリについて画面 */
    About("about"),

    /** おまけ画面 */
    SushiScreen("sushi"),

    /** ライセンス画面 */
    License("license")
}