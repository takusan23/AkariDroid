package io.github.takusan23.akaridroid.v2.tool

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** JSONを扱う関数たち */
object JsonTool {

    /** JSONパースするときに使う */
    val jsonSerialization = Json {
        // JSONのキーが全部揃ってなくてもパース
        ignoreUnknownKeys = true
        // data class の省略時の値を使うように
        encodeDefaults = true
    }

    /**
     * 文字列からデータクラスへ変換する
     *
     * @param T @Serializable のついたデータクラス
     * @param string 文字列
     * @return [T]
     */
    inline fun <reified T> parse(string: String): T = jsonSerialization.decodeFromString(string)

    /**
     * データクラスから文字列に変換する
     *
     * @param data データクラス
     * @return 文字列
     */
    inline fun <reified T : Any> encode(data: T): String = jsonSerialization.encodeToString(data)

}