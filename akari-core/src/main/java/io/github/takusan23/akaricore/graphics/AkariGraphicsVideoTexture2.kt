package io.github.takusan23.akaricore.graphics

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** [AkariGraphicsProcessor]で動画を描画する */
class AkariGraphicsVideoTexture2(initTexName: Int) : MediaCodec.Callback() {

    private var mediaCodec: MediaCodec? = null
    private var mediaExtractor: MediaExtractor? = null

    /** 映像をテクスチャとして利用できるやつ */
    val akariSurfaceTexture = AkariGraphicsSurfaceTexture(initTexName)

    // MediaCodec の非同期コールバックが呼び出されるスレッド（Handler）
    private val handlerThread = HandlerThread("MediaCodecHandlerThread")
    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private var currentJob: Job? = null
    private val mediaCodecCallbackChannel = Channel<MediaCodecAsyncState>()

    fun prepareDecoder(filePath: String) {
        // 動画トラックを探す
        val mediaExtractor = MediaExtractor().apply {
            setDataSource(filePath)
        }
        this.mediaExtractor = mediaExtractor

        val videoTrackIndex = (0 until mediaExtractor.trackCount)
            .map { mediaExtractor.getTrackFormat(it) }
            .withIndex()
            .first { it.value.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true }
            .index

        mediaExtractor.selectTrack(videoTrackIndex)
        val mediaFormat = mediaExtractor.getTrackFormat(videoTrackIndex)
        val codecName = mediaFormat.getString(MediaFormat.KEY_MIME)!!

        handlerThread.start()
        mediaCodec = MediaCodec.createDecoderByType(codecName).apply {
            setCallback(this@AkariGraphicsVideoTexture2, Handler(handlerThread.looper))
            configure(mediaFormat, akariSurfaceTexture.surface, null, 0)
            start()
        }
    }


    fun play() {
        scope.launch {
            currentJob?.cancelAndJoin()
            currentJob = scope.launch {
                // 無限ループでコールバックを待つ
                while (isActive) {
                    when (val receiveAsyncState = mediaCodecCallbackChannel.receive()) {
                        is MediaCodecAsyncState.InputBuffer -> {
                            val inputIndex = receiveAsyncState.index
                            val inputBuffer = mediaCodec?.getInputBuffer(inputIndex) ?: break
                            val size = mediaExtractor?.readSampleData(inputBuffer, 0) ?: break
                            if (size > 0) {
                                // デコーダーへ流す
                                mediaCodec?.queueInputBuffer(inputIndex, 0, size, mediaExtractor!!.sampleTime, 0)
                                mediaExtractor?.advance()
                            }
                        }

                        is MediaCodecAsyncState.OutputBuffer -> {
                            val outputIndex = receiveAsyncState.index
                            mediaCodec?.releaseOutputBuffer(outputIndex, true)
                            // delay(33)
                        }

                        is MediaCodecAsyncState.OutputFormat -> {
                            // デコーダーでは使われないはず
                        }
                    }
                }
            }
        }
    }

    fun pause() {
        currentJob?.cancel()
    }

    fun seekTo(positionMs: Long) {
        scope.launch {
            currentJob?.cancelAndJoin()
            currentJob = scope.launch {
                val mediaExtractor = mediaExtractor ?: return@launch
                val mediaCodec = mediaCodec ?: return@launch

                if (mediaExtractor.sampleTime <= positionMs * 1_000) {
                    // 一番近いキーフレームまでシーク
                    mediaExtractor.seekTo(positionMs * 1_000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                } else {
                    // もし時間が巻き戻る方にシークする場合
                    // デコーダーをリセットする
                    mediaExtractor.seekTo(positionMs * 1_000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    mediaCodec.flush()
                    mediaCodec.start()
                }

                // 無限ループでコールバックを待つ
                while (isActive) {
                    when (val receiveAsyncState = mediaCodecCallbackChannel.receive()) {
                        is MediaCodecAsyncState.InputBuffer -> {
                            val inputIndex = receiveAsyncState.index
                            val inputBuffer = mediaCodec.getInputBuffer(inputIndex) ?: break
                            val size = mediaExtractor.readSampleData(inputBuffer, 0)
                            if (size > 0) {
                                // デコーダーへ流す
                                mediaCodec.queueInputBuffer(inputIndex, 0, size, mediaExtractor.sampleTime, 0)
                                mediaExtractor.advance()
                            }
                        }

                        is MediaCodecAsyncState.OutputBuffer -> {
                            val outputIndex = receiveAsyncState.index
                            val info = receiveAsyncState.info
                            if (positionMs * 1_000 <= info.presentationTimeUs) {
                                // 指定時間なら、Surface に送信して break
                                mediaCodec.releaseOutputBuffer(outputIndex, true)
                                break
                            } else {
                                mediaCodec.releaseOutputBuffer(outputIndex, false)
                            }
                        }

                        is MediaCodecAsyncState.OutputFormat -> {
                            // デコーダーでは使われないはず
                        }
                    }
                }
            }
        }
    }

    fun destroy() {
        mediaExtractor?.release()
        mediaCodec?.release()
        akariSurfaceTexture.destroy()
        handlerThread.quit()
        scope.cancel()
    }

    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
        runBlocking {
            mediaCodecCallbackChannel.send(MediaCodecAsyncState.InputBuffer(codec, index))
        }
    }

    override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
        runBlocking {
            mediaCodecCallbackChannel.send(MediaCodecAsyncState.OutputBuffer(codec, index, info))
        }
    }

    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        // do nothing
    }

    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        runBlocking {
            mediaCodecCallbackChannel.send(MediaCodecAsyncState.OutputFormat(codec, format))
        }
    }

    private sealed interface MediaCodecAsyncState {
        data class InputBuffer(val codec: MediaCodec, val index: Int) : MediaCodecAsyncState
        data class OutputBuffer(val codec: MediaCodec, val index: Int, val info: MediaCodec.BufferInfo) : MediaCodecAsyncState
        data class OutputFormat(val codec: MediaCodec, val format: MediaFormat) : MediaCodecAsyncState
    }
}
