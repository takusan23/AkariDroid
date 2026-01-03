package io.github.takusan23.akaridroid.ui.screen

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.takusan23.akaridroid.ui.screen.about.AboutScreen
import io.github.takusan23.akaridroid.ui.screen.about.AboutSushiScreen
import io.github.takusan23.akaridroid.ui.screen.setting.FontSettingScreen
import io.github.takusan23.akaridroid.ui.screen.setting.LicenseScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** 画面の切り替えを担当する */
@Composable
fun AkariDroidMainScreen() {
    val activity = LocalActivity.current
    val viewModelStoreOwner = LocalViewModelStoreOwner.current

    // 画面遷移
    val backStack = rememberNavBackStack(NavigationPaths.ProjectList)
    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<NavigationPaths.ProjectList> {
                ProjectListScreen(
                    viewModel = koinViewModel(),
                    onOpen = { projectName, isCreateNew -> backStack += NavigationPaths.VideoEditor(projectName, isCreateNew) },
                    onNavigate = { navigationPaths -> backStack += navigationPaths }
                )
            }
            entry<NavigationPaths.VideoEditor> { path ->
                VideoEditorScreen(
                    onNavigate = { navigationPaths -> backStack += navigationPaths },
                    onBack = { backStack.removeLastOrNull() },
                    // navigation3 は extras に Application のインスタンスが入ってない
                    // 自前で入れる
                    // また、ナビゲーションの引数を savedStateHandle に入れる機能もなくなっているため、ViewModel の Factory する
                    viewModel = koinViewModel {
                        parametersOf(path)
                    },
                )
            }
            entry<NavigationPaths.Setting> {
                SettingScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onNavigate = { navigationPaths -> backStack += navigationPaths }
                )
            }
            entry<NavigationPaths.About> {
                AboutScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onNavigate = { navigationPaths -> backStack += navigationPaths }
                )
            }
            entry<NavigationPaths.FontSetting> {
                FontSettingScreen(
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<NavigationPaths.SushiScreen> {
                AboutSushiScreen(
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<NavigationPaths.License> {
                LicenseScreen(
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}

/** 画面遷移先 */
sealed interface NavigationPaths : NavKey {
    /** プロジェクト一覧画面 */
    @Serializable
    data object ProjectList : NavigationPaths

    /**
     * 動画編集画面
     *
     * @param projectName プロジェクト名
     * @param isOpenVideoInfo 動画情報編集画面を開くか
     */
    @Serializable
    data class VideoEditor(
        val projectName: String,
        val isOpenVideoInfo: Boolean
    ) : NavigationPaths

    /** 設定画面 */
    @Serializable
    data object Setting : NavigationPaths

    /** フォント設定 */
    @Serializable
    data object FontSetting : NavigationPaths

    /** このアプリについて画面 */
    @Serializable
    data object About : NavigationPaths

    /** おまけ画面 */
    @Serializable
    data object SushiScreen : NavigationPaths

    /** ライセンス画面 */
    @Serializable
    data object License : NavigationPaths
}