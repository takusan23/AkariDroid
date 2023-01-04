package io.github.takusan23.akaridroid.ui.bottomsheet.data

import io.github.takusan23.akaridroid.data.CanvasElementData

/** ボトムシート表示時に渡す */
sealed class BottomSheetInitData {

    /** 要素の編集画面を開く */
    data class CanvasElementInitData(val canvasElementData: CanvasElementData) : BottomSheetInitData()

}