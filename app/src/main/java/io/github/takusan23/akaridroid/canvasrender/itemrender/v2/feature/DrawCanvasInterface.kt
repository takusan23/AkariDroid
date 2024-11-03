package io.github.takusan23.akaridroid.canvasrender.itemrender.v2.feature

import android.graphics.Canvas

/** [Canvas]へ描画するもの */
interface DrawCanvasInterface {

    /**
     * 渡された[Canvas]に描画する。
     * サスペンド関数ですが、レイヤー順に直列で呼び出されるため、あんまり時間をかけないでください。
     *
     * @param canvas 描画先
     * @param durationMs 動画の合計時間
     * @param currentPositionMs 描画したい時間
     */
    suspend fun draw(
        canvas: Canvas,
        durationMs: Long,
        currentPositionMs: Long
    )

}