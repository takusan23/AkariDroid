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

            // 10Bit HDR のパラメーターをセット
            if (tenBitHdrParametersOrNullSdr != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

        try {
            while (true) {
                // yield() で 占有しないよう
                yield()

                // Surface経由でデータを貰って保存する
                val encoderStatus = encodeMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (0 <= encoderStatus) {
                    if (bufferInfo.size > 0) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            val encodedData = encodeMediaCodec.getOutputBuffer(encoderStatus)!!
                            muxerInterface.onOutputData(encodedData, bufferInfo)
                        }
                    }
                    encodeMediaCodec.releaseOutputBuffer(encoderStatus, false)
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // MediaMuxerへ映像トラックを追加するのはこのタイミングで行う
                    // このタイミングでやると固有のパラメーターがセットされたMediaFormatが手に入る(csd-0 とか)
                    // 映像がぶっ壊れている場合（緑で塗りつぶされてるとか）は多分このあたりが怪しい
                    val newFormat = encodeMediaCodec.outputFormat
                    muxerInterface.onOutputFormat(newFormat)
                }
                if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    continue
                }
            }
        } finally {
            // エンコーダー終了
            encodeMediaCodec.signalEndOfInputStream()
            encodeMediaCodec.stop()
            encodeMediaCodec.release()
            // コンテナフォーマットに書き込む処理も終了
            muxerInterface.stop()
        }
    }

    /**
     * 10Bit HDR の動画を作成するためのパラメーター。
     * 色域とガンマカーブを指定してください。
     *
     * HLG 形式の HDR の場合は[MediaFormat.COLOR_STANDARD_BT2020]と[MediaFormat.COLOR_TRANSFER_HLG]。
     * デフォルト引数は HLG。
     *
     * 定数自体は Android 7 からありますが、10Bit HDR の動画編集が（MediaCodec が？） 13 以上なので。
     *
     * @param colorStandard 色域
     * @param colorTransfer ガンマカーブ
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    data class TenBitHdrParameters(
        val colorStandard: Int = MediaFormat.COLOR_STANDARD_BT2020,
        val colorTransfer: Int = MediaFormat.COLOR_TRANSFER_HLG
    )

    companion object {
        /** タイムアウト */
        private const val TIMEOUT_US = 0L
    }
}