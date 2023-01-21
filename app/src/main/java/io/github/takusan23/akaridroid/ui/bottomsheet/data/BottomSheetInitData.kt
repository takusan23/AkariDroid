package io.github.takusan23.akaridroid.ui.bottomsheet.data

import io.github.takusan23.akaridroid.data.AudioAssetData
import io.github.takusan23.akaridroid.data.CanvasElementData

/** ボトムシート表示時に渡す */
sealed class BottomSheetInitData {

    /** 要素の編集画面を開く */
    data class CanvasElementInitData(val canvasElementData: CanvasElementData) : BottomSheetInitData()

    /** メニュー画面 */
    object VideoEditMenuInitData : BottomSheetInitData()

    /** 音声素材の編集画面 */
    data class AudioAssetInitData(val audioAssetInitData: AudioAssetData) : BottomSheetInitData()
}