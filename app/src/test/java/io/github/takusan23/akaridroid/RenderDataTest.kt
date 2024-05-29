package io.github.takusan23.akaridroid

import org.junit.Test
import kotlin.test.assertEquals

/** [RenderData]のテスト */
class RenderDataTest {

    @Test
    fun test_一つのDisplayTiemを2つに分割できる() {
        // 10s-20s
        val displayTimeA = RenderData.DisplayTime(startMs = 10_000, durationMs = 10_000)
        // 12s で分割する
        val (displayTimeB, displayTimeC) = displayTimeA.splitTime(12_000)
        // duration と一応開始、終了も見る
        assertEquals(displayTimeB.durationMs, 2_000, "displayTimeB.durationMs")
        assertEquals(displayTimeC.durationMs, 8_000, "displayTimeC.durationMs")
        assertEquals(displayTimeB.startMs, 10_000, "displayTimeB.startMs")
        assertEquals(displayTimeC.startMs, 12_000, "displayTimeC.startMs")
        assertEquals(displayTimeB.stopMs, 12_000, "displayTimeB.stopMs")
        assertEquals(displayTimeC.stopMs, 20_000, "displayTimeC.stopMs")
    }

    @Test
    fun test_速度調整ができる() {
        val displayTimeA = RenderData.DisplayTime(startMs = 0, durationMs = 10_000, playbackSpeed = 2f)
        assertEquals(displayTimeA.stopMs, 5_000)
        // 倍速
        val displayTimeB = RenderData.DisplayTime(startMs = 60_000, durationMs = 60_000, playbackSpeed = 2f)
        assertEquals(displayTimeB.stopMs, 90_000)
        // スローモーション
        val displayTimeC = RenderData.DisplayTime(startMs = 10_000, durationMs = 10_000, playbackSpeed = 0.5f)
        assertEquals(displayTimeC.stopMs, 30_000)
    }

    @Test
    fun test_一つのDisplayTiemを2つに分割できる_再生速度対応版_等倍速() {
        // 0s だと都合良すぎるので 60s とか入れてみた
        // 60s - 120s の DisplayTime
        val displayTimeA = RenderData.DisplayTime(startMs = 60_000, durationMs = 60_000, playbackSpeed = 1f)
        assertEquals(displayTimeA.startMs, 60_000, "displayTimeA.startMs")
        assertEquals(displayTimeA.durationMs, 60_000, "displayTimeA.durationMs")
        assertEquals(displayTimeA.stopMs, 120_000, "displayTimeA.stopMs") // 開始時間を足してかつ、再生速度が考慮されること

        // 90s で分割。60s-120s の中央。数直線めっちゃ苦手だった
        val (displayTimeB, displayTimeC) = displayTimeA.splitTime(90_000)
        // 再生速度は考慮されない。
        // 再生速度反映前のを半分にしたのと同じ
        assertEquals(displayTimeB.durationMs, 30_000, "displayTimeB.durationMs")
        assertEquals(displayTimeC.durationMs, 30_000, "displayTimeC.durationMs")
        // startMs は再生速度を考慮されない。
        assertEquals(displayTimeB.startMs, 60_000, "displayTimeB.startMs")
        assertEquals(displayTimeC.startMs, 90_000, "displayTimeC.startMs")
        // 一方 stopMs は再生速度を考慮する
        assertEquals(displayTimeB.stopMs, 90_000, "displayTimeB.stopMs")
        assertEquals(displayTimeC.stopMs, 120_000, "displayTimeC.stopMs")
    }

    @Test
    fun test_一つのDisplayTiemを2つに分割できる_再生速度対応版() {
        // 速度調整対応しているか
        // 60s から 300s 再生する DisplayTime。うどんタイマー。終了地点は 60s+300s=360s になる。
        // ただし 2 倍速なので 60s から 150s です。
        // 0----------60----------120----------180
        // <----60---->[<------150------>]
        val displayTimeA = RenderData.DisplayTime(startMs = 60_000, durationMs = 300_000, playbackSpeed = 2f)
        assertEquals(displayTimeA.startMs, 60_000, "displayTimeA.startMs")
        assertEquals(displayTimeA.durationMs, 300_000, "displayTimeA.durationMs")
        assertEquals(displayTimeA.playbackSpeedDurationMs, 150_000, "displayTimeA.playbackDurationMs")
        assertEquals(displayTimeA.stopMs, 60_000 + 150_000, "displayTimeA.stopMs") // 開始時間を足してかつ、durationMs の速度考慮を

        // 60s から 150s の間で適当に分割
        // 120s で
        val (displayTimeB, displayTimeC) = displayTimeA.splitTime(120_000)

        // 再生時間は再生速度が考慮されない
        // 120s で切れば 120s になる。残りも元の速度（300s）から引いた値になる
        // 違和感がありますか？実際のアプリのプレビューだと再生速度が反映されているので。。。
        assertEquals(displayTimeB.durationMs, 120_000, "displayTimeB.durationMs")
        assertEquals(displayTimeC.durationMs, 180_000, "displayTimeC.durationMs")
        // 再生速度対応版もある
        assertEquals(displayTimeB.playbackSpeedDurationMs, 60_000, "displayTimeB.playbackSpeedDurationMs")
        assertEquals(displayTimeC.playbackSpeedDurationMs, 90_000, "displayTimeC.playbackSpeedDurationMs")
        // startMs は再生速度を考慮されない。
        assertEquals(displayTimeB.startMs, 60_000, "displayTimeB.startMs")
        assertEquals(displayTimeC.startMs, 120_000, "displayTimeC.startMs")
        // 一方 stopMs は再生速度を考慮する
        // 開始時間を足してかつ、durationMs の速度考慮を
        assertEquals(displayTimeB.stopMs, 60_000 + 60_000, "displayTimeB.stopMs")
        assertEquals(displayTimeC.stopMs, 120_000 + 90_000, "displayTimeC.stopMs")
    }

    @Test
    fun test_一つのDisplayTiemを2つに分割できる_再生速度対応版2() {
        // 速度調整対応しているか
        // 150s 間再生する DisplayTime。終了地点は 60s スタートのため、60s+150s=210s になる。
        // ただし 0.5 倍速なので 300 秒まで再生されます。うどんタイマー
        val displayTimeA = RenderData.DisplayTime(startMs = 60_000, durationMs = 150_000, playbackSpeed = 0.5f)
        assertEquals(displayTimeA.startMs, 60_000, "displayTimeA.startMs")
        assertEquals(displayTimeA.durationMs, 150_000, "displayTimeA.durationMs")
        assertEquals(displayTimeA.playbackSpeedDurationMs, 300_000, "displayTimeA.playbackDurationMs")
        assertEquals(displayTimeA.stopMs, 60_000 + 300_000, "displayTimeA.stopMs")

        // 分割する
        // 今回は 3 分（180s）のところで。やっぱカップラーメンにする
        val (displayTimeB, displayTimeC) = displayTimeA.splitTime(60_000 + 180_000)
        // 再生時間は再生速度が考慮されない。ので 0.5 倍速
        assertEquals(displayTimeB.durationMs, 90_000, "displayTimeB.durationMs")
        assertEquals(displayTimeC.durationMs, 60_000, "displayTimeC.durationMs")
        // 再生速度対応版
        assertEquals(displayTimeB.playbackSpeedDurationMs, 180_000, "displayTimeB.playbackSpeedDurationMs")
        assertEquals(displayTimeC.playbackSpeedDurationMs, 120_000, "displayTimeC.playbackSpeedDurationMs")
        // startMs は再生速度を考慮されない。
        assertEquals(displayTimeB.startMs, 60_000, "displayTimeB.startMs")
        assertEquals(displayTimeC.startMs, 60_000 + 180_000, "displayTimeC.startMs")
        // 一方 stopMs は再生速度を考慮する
        // 開始時間を足してかつ、durationMs の速度考慮を
        assertEquals(displayTimeB.stopMs, (60_000) + 180_000, "displayTimeB.stopMs")
        assertEquals(displayTimeC.stopMs, (60_000 + 180_000) + 120_000, "displayTimeC.stopMs")
    }

}