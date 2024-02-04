package io.github.takusan23.akaridroid.v2.audiorender

import io.github.takusan23.akaridroid.v2.canvasrender.RenderData
import java.io.File

/** [AudioItemRender]のインターフェース。今ん所継承してるの一個しか無いけど */
interface AudioRenderInterface {

    /** PCM にデコードしたファイル */
    val outPcmFile: File

    /** いつ描画すべきか */
    val displayTime: RenderData.DisplayTime

    /**
     * PCM にデコードする
     *
     * @param tempFolder 一時的に使えるフォルダ。終わったら削除して良い
     */
    suspend fun decode(tempFolder: File)

    /** 同じデータかどうかを返す */
    suspend fun isEquals(item: RenderData.AudioItem): Boolean

}