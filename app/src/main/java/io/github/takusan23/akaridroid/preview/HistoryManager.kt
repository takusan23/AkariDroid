package io.github.takusan23.akaridroid.preview

/**
 * [T]のデータを元に戻す、やり直す。undo / redo のやつ。
 * [T]のデータ全部を持つことになるので、メモリ的にはよろしく無い。
 */
class HistoryManager<T : Any> {

    /** [T]の変更があると、リストの最後に追加されていく。なので、最後の要素が現在の状態。になるはず。 */
    private var editHistoryLogList = listOf<T>()

    /** [redo]のため、[undo]を呼んだら[editHistoryLogList]からこっちに移動してくる */
    private var undoList = listOf<T>()

    /** 履歴数 */
    val editHistoryLogSize: Int
        get() = editHistoryLogList.size

    /**
     * 履歴に追加する
     *
     * @return [HistoryState]
     */
    fun addHistory(data: T): HistoryState {
        // 履歴に積む
        // 最後に追加した値と同じ場合は積まない（例：undo した後に addHistory）
        if (editHistoryLogList.lastOrNull() == data) {
            return createHistoryState()
        }

        // undoList に何かあれば消す
        // undo -> addHistory -> redo は出来ないようにする
        undoList = emptyList()

        // 積む。最大数を超えないよう
        editHistoryLogList += data
        if (editHistoryLogList.size > MAX_COUNT) {
            editHistoryLogList = editHistoryLogList.drop(editHistoryLogList.size - MAX_COUNT)
        }

        return createHistoryState()
    }

    /** 一つ前に戻す */
    fun undo(): UndoRedoResult<T> {
        // 最新の状態を取り出して、redo で戻せるように追加
        val last = editHistoryLogList.last()
        editHistoryLogList -= last
        // 戻せるように undo に積む
        undoList += last

        // historyList から最新の状態を取った後のリストで最後の要素を適用する事で戻せる
        val undoItem = editHistoryLogList.last()
        return UndoRedoResult(
            data = undoItem,
            state = createHistoryState()
        )
    }

    /** 戻す操作を取り消す */
    fun redo(): UndoRedoResult<T> {
        // undo から取り出す
        val first = undoList.last()
        undoList -= first

        // undo から取り出したのを履歴に積む
        editHistoryLogList += first

        return UndoRedoResult(
            data = first,
            state = createHistoryState()
        )
    }

    /**
     * 最後に[addHistory]で追加した値を返す
     *
     * @return [UndoRedoResult]
     */
    fun latestData(): UndoRedoResult<T> {
        return UndoRedoResult(
            data = editHistoryLogList.last(),
            state = createHistoryState()
        )
    }

    private fun createHistoryState() = HistoryState(
        hasUndo = editHistoryLogList.size > 1,
        hasRedo = undoList.isNotEmpty()
    )

    /**
     * 履歴の状態
     *
     * @param hasUndo [undo]が使えるか
     * @param hasRedo [redo]が使えるか
     */
    data class HistoryState(
        val hasUndo: Boolean,
        val hasRedo: Boolean
    )

    /**
     * [undo]、[redo]の結果
     *
     * @param data データ
     * @param state [HistoryState]
     */
    data class UndoRedoResult<T>(
        val data: T,
        val state: HistoryState
    )

    companion object {
        /** 最大保持数 */
        private const val MAX_COUNT = 10
    }
}

// こんにちは
// こんかいは
// undo / redo を
// こんかいは