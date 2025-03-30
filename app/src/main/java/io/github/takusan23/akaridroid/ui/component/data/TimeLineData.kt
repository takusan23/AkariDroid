package io.github.takusan23.akaridroid.ui.component.data

/**
 * レーン番号をキーにしてそのレーンのアイテムをまとめた[Pair]を返す。
 *
 * @param laneCount [TimeLineData.laneCount]。[TimeLineData.itemList]を filter する等だと、利用しているレーンしか知ることが出来ないため。
 */
fun List<TimeLineData.Item>.groupByLane(laneCount: Int) = (0 until laneCount)
    .map { laneIndex -> laneIndex to filter { it.laneIndex == laneIndex } }

/**
 *
 * @param durationMs 動画の最大長
 * @param laneCount レーンの数
 * @param itemList 素材一覧。[TimeLineData.Item]の配列
 */
data class TimeLineData(
    val durationMs: Long,
    val laneCount: Int = 5,
    val itemList: List<Item>
) {

    /** [List.groupByLane]を呼び出す */
    fun groupByLane(): List<Pair<Int, List<Item>>> = itemList.groupByLane(laneCount)

    /**
     * タイムラインにアイテム（素材）を表示するためのデータ
     *
     * @param id 識別に使える一意の値。[io.github.takusan23.akaridroid.RenderData.RenderItem.id]と同じになるよう使ってる
     * @param laneIndex レーン番号。レイヤーというか
     * @param startMs 開始位置
     * @param stopMs 終了位置
     * @param label アイテムが何なのかの文字
     * @param iconResId アイコン。drawable リソースID
     * @param isChangeDuration 長さ調整（表示時間変更）ができるか。true の場合はつまみが表示されます。
     */
    data class Item(
        val id: Long = System.currentTimeMillis(),
        val laneIndex: Int,
        val startMs: Long,
        val stopMs: Long,
        val label: String,
        val iconResId: Int,
        val isChangeDuration: Boolean
    ) {

        /** 表示時間の範囲を[LongRange]にする */
        val timeRange: LongRange
            get() = this.startMs until this.stopMs

        /** 表示時間を返す */
        val durationMs: Long
            get() = this.stopMs - this.startMs
    }

    /**
     * ドラッグアンドドロップした対象の情報
     * ViewModel とかで判定するのでそのときに使う
     *
     * @param dragAndDroppedStartMs 移動先の開始時間
     * @param dragAndDroppedLaneIndex ドロップ先のレーン番号
     */
    data class DragAndDropRequest(
        val id: Long,
        val dragAndDroppedStartMs: Long,
        val dragAndDroppedLaneIndex: Int
    )

    /**
     * 長さ調整をした時の情報
     *
     * @param id [Item.id]と同じ
     * @param newDurationMs 長さ調整後の時間
     */
    data class DurationChangeRequest(
        val id: Long,
        val newDurationMs: Long
    )
}

