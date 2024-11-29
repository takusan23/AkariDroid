package io.github.takusan23.akaricore.muxer

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * エンコーダーのコンテナフォーマットに書き込む処理を自前で実装できるようにするためのインターフェース
 * [android.media.MediaMuxer]相当の実装を自前で作れるように。
 *
 * [android.media.MediaMuxer]を使った実装は[AkariAndroidMuxer]
 */
interface AkariEncodeMuxerInterface {

    /**
     * エンコーダーから[MediaFormat]が出てきて確定した。
     * コンテナフォーマットにトラックを書き込む。
     *
     * @param mediaFormat エンコーダーから出てきた[MediaFormat]
     */
    suspend fun onOutputFormat(mediaFormat: MediaFormat)

    /**
     * エンコーダーからエンコード済みのデータが出てきた。
     *
     * @param byteBuffer エンコードしたデータ
     * @param bufferInfo キーフレームかどうかや、フレーム時間が入ってる
     */
    suspend fun onOutputData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo)

    /** エンコード終了時に呼び出される */
    suspend fun stop()
}