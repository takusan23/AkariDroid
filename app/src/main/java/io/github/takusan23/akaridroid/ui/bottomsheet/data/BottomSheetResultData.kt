package io.github.takusan23.akaridroid.ui.bottomsheet.data

import io.github.takusan23.akaridroid.data.CanvasElementData
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditMenuBottomSheetMenu

/** [BottomSheetState]で返す値 */
sealed class BottomSheetResultData {

    /** 要素の編集画面から戻ってきた際に呼ばれる */
    data class CanvasElementResult(val canvasElementData: CanvasElementData) : BottomSheetResultData()

    /** 動画編集画面のメニュー選択時 */
    data class VideoEditMenuResult(val menu: VideoEditMenuBottomSheetMenu) : BottomSheetResultData()
}