package io.github.takusan23.akaridroid

import io.github.takusan23.akaridroid.tool.MultiArmedBanditManager
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/** [MultiArmedBanditManager]のテスト */
class MultiArmedBanditManagerTest {

    @Test
    fun test_初期値がある() {
        val initList = BANDIT_MACHINE_LIST.subList(0, 3)
        val multiArmedBanditManager = MultiArmedBanditManager(
            epsilon = 0.9f,
            banditMachineList = BANDIT_MACHINE_LIST,
            pullSize = 3,
            initList = initList
        )
        assertEquals(multiArmedBanditManager.pullItemList.value, initList)
    }

    @Test
    fun test_報酬を与えると結果が変わること() {
        val multiArmedBanditManager = MultiArmedBanditManager(
            epsilon = 0.9f,
            banditMachineList = BANDIT_MACHINE_LIST,
            pullSize = 3,
            initList = emptyList()
        )

        // iPhone を多く選んでおく
        // 少なくとも報酬を与えたものが入っていること
        repeat(5) {
            multiArmedBanditManager.reward("iPhone")
        }
        assertContains(multiArmedBanditManager.pullItemList.value, "iPhone")

        // それよりも Pixel を選んでみる
        // これも報酬を与えたものが入っていること
        repeat(10) {
            multiArmedBanditManager.reward("Pixel")
        }
        assertContains(multiArmedBanditManager.pullItemList.value, "Pixel")
    }

    companion object {

        /** バンディットマシーン一覧 */
        private val BANDIT_MACHINE_LIST = listOf(
            "iPhone",
            "Pixel",
            "Xperia",
            "AQUOS",
            "Galaxy",
            "Xiaomi"
        )
    }
}