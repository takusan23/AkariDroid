package io.github.takusan23.akaridroid.data

/**
 * キャンバスに描画する要素のデータ
 *
 * @param xPos X座標
 * @param yPos Y座標
 * @param startMs 描画開始時間（ミリ秒）
 * @param endMs 描画終了時間（ミリ秒）
 * @param elementType 描画する種類
 */
data class CanvasElementData(
    val xPos: Float,
    val yPos: Float,
    val startMs: Long,
    val endMs: Long,
    val elementType: CanvasElementType
)