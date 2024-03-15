package io.github.takusan23.akaridroid

import io.github.takusan23.akaridroid.preview.HistoryManager
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** [HistoryManager] のテスト */
class HistoryManagerTest {

    @Test
    fun test_現在の履歴が取れる() {
        val historyManager = HistoryManager<List<Int>>()
        historyManager.addHistory(listOf(1, 2, 3))
        historyManager.addHistory(listOf(1, 2, 3, 4))

        val latestData = historyManager.latestData()
        assertEquals(latestData.data, listOf(1, 2, 3, 4))
        assertTrue { latestData.state.hasUndo }
        assertFalse { latestData.state.hasRedo }
    }

    @Test
    fun test_履歴を使って元に戻せる() {
        val historyManager = HistoryManager<List<Int>>()
        historyManager.addHistory(listOf(1, 2, 3))
        historyManager.addHistory(listOf(1, 2, 3, 4))

        val before = historyManager.latestData()
        val undoResult = historyManager.undo()
        val after = historyManager.latestData()

        assertTrue { before.state.hasUndo }

        assertEquals(undoResult.data, listOf(1, 2, 3))
        assertTrue { undoResult.state.hasRedo }

        assertEquals(after.data, listOf(1, 2, 3))
        assertTrue { after.state.hasRedo }
    }

    @Test
    fun test_履歴を使って元に戻したのを戻せる() {
        val historyManager = HistoryManager<List<Int>>()
        historyManager.addHistory(listOf(1, 2, 3))
        historyManager.addHistory(listOf(1, 2, 3, 4))

        val before = historyManager.latestData()
        val undoResult = historyManager.undo()
        val redoResult = historyManager.redo()
        val after = historyManager.latestData()

        assertTrue { before.state.hasUndo }

        assertEquals(undoResult.data, listOf(1, 2, 3))
        assertTrue { undoResult.state.hasRedo }

        assertEquals(redoResult.data, listOf(1, 2, 3, 4))
        assertFalse { redoResult.state.hasRedo }

        assertEquals(after.data, listOf(1, 2, 3, 4))
        assertFalse { after.state.hasRedo }
    }

    @Test
    fun test_同じ履歴は追加されない() {
        val historyManager = HistoryManager<List<Int>>()
        historyManager.addHistory(listOf(1, 2, 3))
        historyManager.addHistory(listOf(1, 2, 3))
        historyManager.addHistory(listOf(1, 2, 3))

        assertEquals(historyManager.editHistoryLogSize, 1)
        assertFalse { historyManager.latestData().state.hasUndo }
    }

    @Test
    fun test_履歴は最大の数を超えない() {
        val historyManager = HistoryManager<List<Int>>()
        historyManager.addHistory(listOf(1))
        historyManager.addHistory(listOf(1, 2))
        historyManager.addHistory(listOf(1, 2, 3))
        historyManager.addHistory(listOf(1, 2, 3, 4))
        historyManager.addHistory(listOf(1, 2, 3, 4, 5))
        historyManager.addHistory(listOf(1, 2, 3, 4, 5, 6))
        historyManager.addHistory(listOf(1, 2, 3, 4, 5, 6, 7))
        historyManager.addHistory(listOf(1, 2, 3, 4, 5, 6, 7, 8))
        historyManager.addHistory(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9))
        historyManager.addHistory(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        historyManager.addHistory(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))

        assertEquals(historyManager.editHistoryLogSize, 10)
    }

    @Test
    fun test_redo操作が出来るかどうかのフラグは追加時にリセットされる() {
        val historyManager = HistoryManager<List<Int>>()
        historyManager.addHistory(listOf(1, 2, 3))
        historyManager.addHistory(listOf(1, 2, 3, 4))

        val undo = historyManager.undo()
        historyManager.addHistory(listOf(1, 2, 3, 4, 5))
        val current = historyManager.latestData()

        assertTrue { undo.state.hasRedo }
        assertFalse { current.state.hasRedo }
    }
}