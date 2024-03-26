package io.github.takusan23.akaridroid.tool

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * 多腕バンディット問題をいちばん簡単なアルゴリズムでやらせる。
 * フローティングバーのレコメンドで使ってる。完全ランダムよりはいくぶんマシなはず。
 *
 * @param epsilon 活用をする確率
 * @param banditMachineList バンディットマシーン一覧。つまりバンディットマシーンで引く値
 * @param initList 初期値
 */
class MultiArmedBanditManager<T : Any>(
    private val epsilon: Float = 0.9f,
    private val banditMachineList: List<T>,
    private val pullSize: Int,
    initList: List<T>
) {
    private val _pullItemList = MutableStateFlow(initList)

    /** バンディットマシーンを引いた後、報酬を与えた回数を記録するため */
    private val rewardMap = hashMapOf<T, Int>()

    /** バンディットマシーンを引いた結果 */
    val pullItemList = _pullItemList.asStateFlow()

    /** 報酬を与える */
    fun reward(reward: T) {
        // 履歴に追加
        val count = rewardMap[reward] ?: 0
        rewardMap[reward] = count + 1

        // バンディット問題を解く
        _pullItemList.value = buildList {
            // 何回チャレンジするか
            repeat(pullSize) {
                // 成績がいいバンディットマシーンを引く or 成績がいいバンディットマシーンを探す
                val banditMachine = if (isNextPull()) {
                    // 一番成績がいいやつを出す。同じのが帰らないように
                    rewardMap.filter { it.key !in this }.maxByOrNull { it.value }?.key
                        ?: banditMachineList.filter { it !in this }.random()
                } else {
                    // 捜索を行う。多分完全ランダム？
                    banditMachineList.filter { it !in this }.random()
                }
                add(banditMachine)
            }
        }
    }

    /** 次の行動が活用（捜索ではない）場合は true */
    private fun isNextPull() = epsilon > Random.nextFloat()

}