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
 * @param bottomSheetState 状態管理
 * @param onClose 閉じる際に呼ぶ
 */
@Composable
fun BottomSheetNavigation(
    bottomSheetState: BottomSheetState,
    onClose: () -> Unit,
) {
    // ボトムシートで表示させる内容
    val bottomSheetStateInitData = bottomSheetState.bottomSheetInitData.collectAsState()

    Surface {
        when (val initData = bottomSheetStateInitData.value) {
            // テキスト編集画面
            is BottomSheetInitData.CanvasElementInitData -> {
                TextEditBottomSheet(
                    initCanvasElementData = initData.canvasElementData,
                    onUpdate = { bottomSheetState.sendResult(BottomSheetResultData.CanvasElementResult(it)) },
                    onDelete = {
                        bottomSheetState.sendResult(BottomSheetResultData.CanvasElementDeleteResult(it))
                        bottomSheetState.close()
                    },
                    onClose = onClose
                )
            }
            is BottomSheetInitData.VideoEditMenuInitData -> {
                VideoEditMenuBottomSheet(
                    onClick = { bottomSheetState.sendResult(BottomSheetResultData.VideoEditMenuResult(it)) }
                )
            }
            null -> {
                // The initial value must have an associated anchor. 対策。何もない状態だとエラーが出るので適当においておく
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}