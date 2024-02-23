package io.github.takusan23.akaridroid.ui.component.data

import androidx.compose.ui.unit.IntRect

/**
 * ドラッグアンドドロップで指を離したらときに貰えるデータ
 *
 * @param target 移動する[TimeLineItemData]
 * @param start ドラッグアンドドロップ開始時の位置
 * @param stop ドラッグアンドドロップ終了時の位置
 */
data class TimeLineDragAndDropData(
    val target: TimeLineItemData,
    val start: IntRect,
    val stop: IntRect
)