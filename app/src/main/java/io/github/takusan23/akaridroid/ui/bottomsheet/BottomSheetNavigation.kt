package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import io.github.takusan23.akaridroid.ui.bottomsheet.data.BottomSheetInitData
import io.github.takusan23.akaridroid.ui.bottomsheet.data.BottomSheetResultData

/**
 * 動画編集画面のボトムシート
 *
 * @param canvasElementData 状態管理
 * @param onClose 閉じる際に呼ぶ
 */
@Composable
fun BottomSheetNavigation(
    canvasElementData: BottomSheetState,
    onClose: () -> Unit,
) {
    // ボトムシートで表示させる内容
    val bottomSheetStateInitData = canvasElementData.bottomSheetInitData.collectAsState()

    Surface {
        when (val initData = bottomSheetStateInitData.value) {
            // テキスト編集画面
            is BottomSheetInitData.CanvasElementInitData -> {
                TextEditBottomSheet(
                    initCanvasElementData = initData.canvasElementData,
                    onUpdate = { canvasElementData.sendResult(BottomSheetResultData.CanvasElementResult(it)) },
                    onClose = onClose
                )
            }
            is BottomSheetInitData.VideoEditMenuInitData -> {
                VideoEditMenuBottomSheet(
                    onClick = { canvasElementData.sendResult(BottomSheetResultData.VideoEditMenuResult(it)) }
                )
            }
            null -> {
                // The initial value must have an associated anchor. 対策。何もない状態だとエラーが出るので適当においておく
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}