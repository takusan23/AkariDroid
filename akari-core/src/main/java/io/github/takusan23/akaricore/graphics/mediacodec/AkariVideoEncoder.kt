package io.github.takusan23.akaricore.graphics.mediacodec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import io.github.takusan23.akaricore.common.AkariCoreInputOutput
import io.github.takusan23.akaricore.muxer.AkariAndroidMuxer
import io.github.takusan23.akaricore.muxer.AkariEncodeMuxerInterface
import kotlinx.coroutines.yield

/**
 * 動画のエンコーダー
 * 目的としては[io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor]が描画した内容を録画する。
 * これ以外の目的でも（単に Surface を録画する）でも使えるかも。
 *
 * TODO 10-bit HDR 動画をエンコード出来るようになりましたが、今のところ 10-bit HDR のエンコードに対応しているか見る方法が無い？
 * TODO FEATURE_HdrEditing が false を返しても実際は HDR 動画がエンコード出来る場合があり、Android 13 以上だったらぶっつけ本番で試してみるしか無い気がしてきた。
 */
class AkariVideoEncoder {

    private var encodeMediaCodec: MediaCodec? = null

    private var muxerInterface: AkariEncodeMuxerInterface? = null

    /**
     * MediaCodec エンコーダーの準備をする
     *
     * @param output 出力先ファイル。全てのバージョンで動くのは[AkariCoreInputOutput.JavaFile]のみです。
     * @param codecName コーデック名
     * @param containerFormat コンテナフォーマット
     * @param bitRate ビットレート
     * @param frameRate フレームレート
     * @param keyframeInterval キーフレームの間隔
     * @param outputVideoWidth 動画の高さ
     * @param outputVideoHeight 動画の幅
     * @param tenBitHdrParametersOrNullSdr SDR 動画の場合は null。HDR でエンコードする場合は色域とガンマカーブを指定してください。
     */
    fun prepare(
        output: AkariCoreInputOutput.Output,
        containerFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
        outputVideoWidth: Int = 1280,
        outputVideoHeight: Int = 720,
        frameRate: Int = 30,
        bitRate: Int = 1_000_000,
        keyframeInterval: Int = 1,
        codecName: String = MediaFormat.MIMETYPE_VIDEO_HEVC,
        tenBitHdrParametersOrNullSdr: TenBitHdrParameters? = null
    ) = prepare(
        muxerInterface = AkariAndroidMuxer(output, containerFormat),
        outputVideoWidth = outputVideoWidth,
        outputVideoHeight = outputVideoHeight,
        frameRate = frameRate,
        bitRate = bitRate,
        keyframeInterval = keyframeInterval,
        codecName = codecName,
        tenBitHdrParametersOrNullSdr = tenBitHdrParametersOrNullSdr,
    )

    /**
     * MediaCodec エンコーダーの準備をする
     *
     * @param muxerInterface コンテナフォーマットに書き込む実装
     * @param codecName コーデック名
     * @param bitRate ビットレート
     * @param frameRate フレームレート
     * @param keyframeInterval キーフレームの間隔
     * @param outputVideoWidth 動画の高さ
     * @param outputVideoHeight 動画の幅
     * @param tenBitHdrParametersOrNullSdr SDR 動画の場合は null。HDR でエンコードする場合は色域とガンマカーブを指定してください。
     */
    fun prepare(
        muxerInterface: AkariEncodeMuxerInterface,
        outputVideoWidth: Int = 1280,
        outputVideoHeight: Int = 720,
        frameRate: Int = 30,
        bitRate: Int = 1_000_000,
        keyframeInterval: Int = 1,
        codecName: String = MediaFormat.MIMETYPE_VIDEO_HEVC,
        tenBitHdrParametersOrNullSdr: TenBitHdrParameters? = null
    ) {
        // エンコーダーにセットするMediaFormat
        // コーデックが指定されていればそっちを使う
        val videoMediaFormat = MediaFormat.createVideoFormat(codecName, outputVideoWidth, outputVideoHeight).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, keyframeInterval)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)

            // 10-bit HDR のパラメーターをセット
            if (tenBitHdrParametersOrNullSdr != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setInteger(MediaFormat.KEY_PROFILE, tenBitHdrParametersOrNullSdr.codecProfile)
                setInteger(MediaFormat.KEY_COLOR_STANDARD, tenBitHdrParametersOrNullSdr.colorStandard)
                setInteger(MediaFormat.KEY_COLOR_TRANSFER, tenBitHdrParametersOrNullSdr.colorTransfer)
                setFeatureEnabled(MediaCodecInfo.CodecCapabilities.FEATURE_HdrEditing, true)
            }
        }

        // マルチプレクサ
        this@AkariVideoEncoder.muxerInterface = muxerInterface

        encodeMediaCodec = MediaCodec.createEncoderByType(codecName).apply {
            configure(videoMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
    }

    /**
     * エンコーダーの入力になる[Surface]を取得する。
     * [io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor]等に渡す。
     */
    fun getInputSurface(): Surface = encodeMediaCodec!!.createInputSurface()

    /** エンコーダーを開始する */
    suspend fun start() {
        val encodeMediaCodec = encodeMediaCodec ?: return
        val muxerInterface = muxerInterface ?: return

        val bufferInfo = MediaCodec.BufferInfo()
        encodeMediaCodec.start()

        /**
         * エンコーダーからデータをもらう関数。
         * メインループと、signalEndOfInputStream() を投げた後の残り分をエンコードするため。
         *
         * @return もうデータがでてこない、最後の場合は true
         */
        suspend fun processEncode(): Boolean {
            // Surface経由でデータを貰って保存する
            val encoderStatus = encodeMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            when {
                0 <= encoderStatus -> {
                    if (0 < bufferInfo.size) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            val encodedData = encodeMediaCodec.getOutputBuffer(encoderStatus)!!
                            muxerInterface.onOutputData(encodedData, bufferInfo)
                        }
                    }
                    encodeMediaCodec.releaseOutputBuffer(encoderStatus, false)
                }

                encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    // MediaMuxerへ映像トラックを追加するのはこのタイミングで行う
                    // このタイミングでやると固有のパラメーターがセットされたMediaFormatが手に入る(csd-0 とか)
                    // 映像がぶっ壊れている場合（緑で塗りつぶされてるとか）は多分このあたりが怪しい
                    val newFormat = encodeMediaCodec.outputFormat
                    muxerInterface.onOutputFormat(newFormat)
                }
            }
            return bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
        }

        try {
            while (true) {
                // yield() で 占有しないよう
                yield()
                processEncode() // ループの中で関数呼び出ししてコストがあれかも、
            }
        } finally {
            // エンコーダーの終了シグナルを送る
            // 残った分をエンコード
            encodeMediaCodec.signalEndOfInputStream()
            while (true) {
                if (processEncode()) {
                    break
                }
            }

            encodeMediaCodec.stop()
            encodeMediaCodec.release()
            // コンテナフォーマットに書き込む処理も終了
            muxerInterface.stop()
        }
    }

    /**
     * 10-bit HDR の動画を作成するためのパラメーター。
     * 色域とガンマカーブを指定してください。
     *
     * HLG 形式の HDR の場合は[MediaFormat.COLOR_STANDARD_BT2020]、[MediaFormat.COLOR_TRANSFER_HLG]、[MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10]。
     * デフォルト引数は HLG。
     *
     * 定数自体は Android 7 からありますが、10-bit HDR の動画編集が（MediaCodec が？） 13 以上なので。
     *
     * @param codecProfile コーデックのプロファイル。[MediaFormat.KEY_PROFILE]に渡す値です。
     * @param colorStandard 色域
     * @param colorTransfer ガンマカーブ
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    data class TenBitHdrParameters(
        val codecProfile: Int = MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10,
        val colorStandard: Int = MediaFormat.COLOR_STANDARD_BT2020,
        val colorTransfer: Int = MediaFormat.COLOR_TRANSFER_HLG
    )

    companion object {
        /** タイムアウト */
        private const val TIMEOUT_US = 0L
    }
}