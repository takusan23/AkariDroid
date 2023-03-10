package io.github.takusan23.akaridroid.ui.bottomsheet.data

import io.github.takusan23.akaridroid.data.AudioAssetData
import io.github.takusan23.akaridroid.data.CanvasElementData
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditMenuBottomSheetMenu

/** [BottomSheetState]で返す値 */
sealed class BottomSheetResultData {

    /** 要素の編集画面から戻ってきた際に呼ばれる */
    data class CanvasElementResult(val canvasElementData: CanvasElementData) : BottomSheetResultData()

    /** 要素を削除してほしい際に呼ばれる */
    data class CanvasElementDeleteResult(val deleteElementData: CanvasElementData) : BottomSheetResultData()

    /** 動画編集画面のメニュー選択時 */
    data class VideoEditMenuResult(val menu: VideoEditMenuBottomSheetMenu) : BottomSheetResultData()

    /** 音声素材の編集画面から戻ってきた際に呼ばれる */
    data class AudioAssetResult(val audioAssetData: AudioAssetData) : BottomSheetResultData()

    /** 音声素材を削除してほしい際に呼ばれる */
    data class AudioAssetDeleteResult(val audioAssetData: AudioAssetData) : BottomSheetResultData()
}