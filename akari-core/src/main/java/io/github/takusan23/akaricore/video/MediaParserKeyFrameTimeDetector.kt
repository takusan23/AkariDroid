package io.github.takusan23.akaricore.video

import android.media.MediaCodec
import android.media.MediaParser
import android.media.MediaParser.SeekableInputReader
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * コンテナフォーマットを解析して、キーフレームの時間を取得する
 * [android.media.MediaParser]自体が Android 11 以降なので、それ以前ではキーフレームの時間を出せない。
 *
 * @param onCreateInputStream [InputStream]を作成する必要がある場合に呼ばれる。ContentResolver#openInputStream とかで。
 */
@RequiresApi(Build.VERSION_CODES.R)
internal class MediaParserKeyFrameTimeDetector(private val onCreateInputStream: () -> InputStream) {

    /** [MediaParser.SeekMap] */
    private var seekMap: MediaParser.SeekMap? = null

    /** パースを始める */
    suspend fun startParse() = withContext(Dispatchers.IO) {
        // onSeekFound が来たら解析終わってほしいので
        var isFoundSeekMap = false
        // パース結果コールバック
        val output = object : MediaParser.OutputConsumer {

            // 中身には興味がないので適当に入れ物だけ用意
            private val tempByteArray = ByteArray(4096)

            override fun onSeekMapFound(p0: MediaParser.SeekMap) {
                // 解析してシークできる位置が分かった
                seekMap = p0
                isFoundSeekMap = true
            }

            override fun onTrackCountFound(p0: Int) {
                // do nothing
            }

            override fun onTrackDataFound(p0: Int, p1: MediaParser.TrackData) {
                // do nothing
            }

            override fun onSampleDataFound(p0: Int, p1: MediaParser.InputReader) {
                // SeekMap が欲しいだけなのだが、InputReader#read しないと MediaParser#advance で止まってしまうので
                // サンプル通りに InputReader#read している。
                val readSize = p1.length.toInt()
                p1.read(tempByteArray, 0, minOf(tempByteArray.size, readSize))
            }

            override fun onSampleCompleted(p0: Int, p1: Long, p2: Int, p3: Int, p4: Int, p5: MediaCodec.CryptoInfo?) {
                // do nothing
            }
        }

        // InputStream を MediaParser.InputReader で使う
        val input = InputStreamSeekableInputReader(onCreateInputStream)

        // MP4 と WebM のコンテナを解析する
        val mediaParser = MediaParser.create(output, MediaParser.PARSER_NAME_MP4, MediaParser.PARSER_NAME_MATROSKA)
        while (isActive && !isFoundSeekMap && mediaParser.advance(input)) {
            // SeekMap が取れるまで while 回す
        }

        // リソース開放
        mediaParser.release()
        input.close()
    }

    /**
     * 指定時間より前の一番近いキーフレームの位置を返す
     *
     * @param currentPositionUs この時間より前のキーフレームの時間を探します
     * @return キーフレームの時間
     */
    fun getPrevKeyFrameTime(currentPositionUs: Long): Long? {
        return seekMap?.getSeekPoints(currentPositionUs)?.first?.timeMicros
    }

    /**
     * [SeekableInputReader]の[InputStream]実装例
     * [seekToPosition]で[InputStream]の読み取り位置を巻き戻せないので、作り直しが必要になる。そのための[onCreateInputStream]。
     *
     * @param onCreateInputStream [InputStream]を作成する必要がある場合に呼ばれる
     */
    class InputStreamSeekableInputReader(private val onCreateInputStream: () -> InputStream) : SeekableInputReader {

        /** InputStream。[seekToPosition]が呼び出された際には作り直す */
        private var currentInputStream = onCreateInputStream()

        /** read する前に available を呼ぶことでファイルの合計サイズを出す */
        private val fileSize = currentInputStream.available().toLong()

        override fun read(p0: ByteArray, p1: Int, p2: Int): Int = currentInputStream.read(p0, p1, p2)

        override fun getPosition(): Long = fileSize - currentInputStream.available()

        override fun getLength(): Long = fileSize

        override fun seekToPosition(p0: Long) {
            // ContentResolver#openInputStream だと mark/reset が使えない
            // InputStream を作り直す
            currentInputStream.close()
            currentInputStream = onCreateInputStream()
            currentInputStream.skip(p0)
        }

        /** InputStream を閉じる */
        fun close() {
            currentInputStream.close()
        }
    }
}