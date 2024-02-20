package io.github.takusan23.akaridroid.v2.audiorender

/** [AudioItemRender]のインターフェース。今のところ実装してるの一個しか無いけど */
interface AudioRenderInterface {

    /** [readPcmData]の前に呼び出されます */
    suspend fun prepareRead()

    /**
     * 音声素材の PCM データを読み出して、[ByteArray]で返す。
     * 音量調整とかが必要な場合はここで適用して[ByteArray]に入れて返す。
     *
     * @param readSize ByteArray のサイズ
     * @return 読み出したデータが入った[ByteArray]
     */
    suspend fun readPcmData(readSize: Int): ByteArray

    /**
     * 再生すべき時間を渡すので、再生すべきかどうかを返す
     *
     * @param currentPositionMs 描画する時間
     * @return true の場合描画する
     */
    fun isDisplayPosition(currentPositionMs: Long): Boolean

    /** 破棄する */
    fun destroy()
}