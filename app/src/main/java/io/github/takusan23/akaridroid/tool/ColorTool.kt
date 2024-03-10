package io.github.takusan23.akaridroid.tool

import androidx.compose.ui.graphics.Color

/** 色のユーティリティークラス */
object ColorTool {

    /** [Color]を R G B にして配列で返す。値は 0 から 0xFF まで */
    fun toRgbList(color: Color): List<Float> = listOf(color.red * 0xFF, color.green * 0xFF, color.blue * 0xFF)

    /** [Color]を #000000 みたいにカラーコードにする。 */
    fun toHexColorCode(color: Color): String = toRgbList(color)
        .joinToString(separator = "") { float -> "%02X".format(float.toInt()) }
        .let { "#$it" }

    /**
     * カラーコードをパースして[Color]にする
     *
     * @param hexColorCode #000000 とか
     * @return [Color]
     */
    fun parseColor(hexColorCode: String): Color? =
        runCatching { android.graphics.Color.parseColor(hexColorCode) }
            .map { Color(color = it) }
            .getOrNull()
}