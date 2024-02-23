package io.github.takusan23.akaridroid.ui.component.data

/**
 * タイムラインにアイテム（素材）を表示するためのデータ
 *
 * @param id 識別に使える一意の値。[io.github.takusan23.akaridroid.RenderData.RenderItem.id]と同じになるよう使ってる
 * @param laneIndex レーン番号。レイヤーというか
 * @param startMs 開始位置
 * @param stopMs 終了位置
 * @param label アイテムが何なのかの文字
 * @param iconResId アイコン。drawable リソースID
 */
data class TimeLineItemData(
    val id: Long = System.currentTimeMillis(),
    val laneIndex: Int,
    val startMs: Long,
    val stopMs: Long,
    val label: String,
    val iconResId: Int
)

/** 表示時間の範囲を[LongRange]にする */
val TimeLineItemData.timeRange: LongRange
    get() = this.startMs until this.stopMs

/** 表示時間を時間にする */
val TimeLineItemData.durationMs: Long
    get() = this.stopMs - this.startMs