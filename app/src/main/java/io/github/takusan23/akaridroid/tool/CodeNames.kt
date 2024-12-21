package io.github.takusan23.akaridroid.tool

/**
 * コードネームみたいなのを付けたい
 * @param versionCode build.gradle.kts
 * @param codeName なまえ
 */
enum class CodeNames(val versionCode: Int, val codeName: String) {

    VERSION_1X(1, "ねぎとろ"),
    VERSION_2X(2, "サーモン"),
    VERSION_3X(4, "まぐろ"),
    VERSION_4X(8, "まぐろたたき");

    companion object {

        /** 最新バージョンを返す */
        val latestVersionCodeName: CodeNames
            get() = entries.last()

    }
}