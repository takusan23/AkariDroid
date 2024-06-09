package io.github.takusan23.akaridroid.tool

/** コードネームみたいなのを付けたい */
enum class CodeNames(val version: Int, val codeName: String) {

    VERSION_1X(1, "ねぎとろ"),
    VERSION_2X(2, "サーモン"),
    VERSION_3X(4, "まぐろ");

    companion object {

        /** 最新バージョンを返す */
        val latestVersionCodeName: CodeNames
            get() = entries.last()

    }
}