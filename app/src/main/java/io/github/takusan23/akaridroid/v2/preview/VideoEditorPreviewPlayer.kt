package io.github.takusan23.akaridroid.v2.preview

import android.content.Context
import android.graphics.Bitmap
import io.github.takusan23.akaricore.v2.audio.AkariCoreAudioProperties
import io.github.takusan23.akaridroid.v2.RenderData
import io.github.takusan23.akaridroid.v2.audiorender.AudioRender
import io.github.takusan23.akaridroid.v2.canvasrender.CanvasRender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** [RenderData.CanvasItem]をプレビューする */
class VideoEditorPreviewPlayer(
    context: Context,
    projectFolder: File
) {
    /** プレイヤー再生用コルーチンスコープ。キャンセル用 */
    private val playerScope = CoroutineScope(Dispatchers.Default + Job())

    private val canvasRender = CanvasRender(context)
    private val audioRender = AudioRender(
        context = context,
        outPcmFile = projectFolder.resolve(OUT_PCM_FILE_NAME),
        pcmFolder = projectFolder.resolve(DECODE_PCM_FOLDER_NAME).apply { mkdir() },
        tempFolder = projectFolder.resolve(TEMP_FOLDER_NAME).apply { mkdir() }
    )
    private val pcmPlayer = PcmPlayer(
        samplingRate = AkariCoreAudioProperties.SAMPLING_RATE,
        channelCount = AkariCoreAudioProperties.CHANNEL_COUNT,
        bitDepth = AkariCoreAudioProperties.BIT_DEPTH
    )
    private val bitmapCanvasController = BitmapCanvasController()

    private val _playerPosition = MutableStateFlow(PlayerPosition(0, 0))

    /** プレビュープレイヤーの再生位置と動画時間を流す Flow */
    val playerPosition = _playerPosition.asStateFlow()

    /** プレビュー画像として[Bitmap]を流す Flow */
    val previewBitmap = bitmapCanvasController.latestBitmap

    /** 動画の情報をセットする */
    fun setVideoInfo(
        videoWidth: Int,
        videoHeight: Int,
        durationMs: Long
    ) {
        _playerPosition.update { it.copy(durationMs = durationMs) }
        bitmapCanvasController.createCanvas(videoWidth, videoHeight)
    }

    /** 動画に再生する各種素材をセットする */
    suspend fun setRenderItem(
        audioRenderItemList: List<RenderData.AudioItem> = emptyList(),
        canvasItemList: List<RenderData.CanvasItem> = emptyList(),
    ) = withContext(Dispatchers.Default) {
        listOf(
            launch { audioRender.setRenderData(audioRenderItemList, _playerPosition.value.durationMs) },
            launch { canvasRender.setRenderData(canvasItemList) }
        ).joinAll()
    }

    /** シークする */
    fun seekTo(seekToMs: Long) {
        _playerPosition.update { it.copy(currentPositionMs = seekToMs) }
    }

    /** 現在位置のプレビューを更新する。更新は一回だけ */
    fun startSinglePlay() {
        playerScope.launch {
            val (currentPosition, videoDuration) = _playerPosition.first()
            bitmapCanvasController.update { canvas ->
                canvasRender.draw(canvas, videoDuration, currentPosition)
            }
        }
    }

    /** プレビュー再生を開始する。停止するまで時間が進み続けます。 */
    fun startRepeatPlay() {

        // 時間を進める
        playerScope.launch {
            while (isActive) {
                // 時間を進める
                if (_playerPosition.value.currentPositionMs <= _playerPosition.value.durationMs) {
                    // 1秒ごとなのは、AudioRender が時間の単位が秒なのでそれの最低値
                    _playerPosition.update { it.copy(currentPositionMs = it.currentPositionMs + 1_000) }
                    delay(1_000)
                } else {
                    break
                }
            }
        }

        // Canvas に書く
        // プレビュー重たい
        // 速度優先のため1秒ごと
        playerScope.launch {
            _playerPosition.collect { (currentPosition, videoDuration) ->
                bitmapCanvasController.update { canvas ->
                    canvasRender.draw(canvas, videoDuration, currentPosition)
                }
            }
        }

        // 再生を開始
        pcmPlayer.play()
        // AudioRender も用意
        // 1秒間に必要な ByteArray を用意して読み出す
        val pcmByteArray = ByteArray(AkariCoreAudioProperties.SAMPLING_RATE * AkariCoreAudioProperties.CHANNEL_COUNT * AkariCoreAudioProperties.BIT_DEPTH)
        playerScope.launch {
            _playerPosition.collect { (currentPosition, _) ->
                // シークする
                audioRender.seek((currentPosition / 1000).toInt())
                audioRender.readPcmByteArray(pcmByteArray)
                // スピーカーへ流す
                pcmPlayer.writePcmData(pcmByteArray)
            }
        }
    }

    /** 再生を一時停止する */
    fun stop() {
        pcmPlayer.pause()
        playerScope.coroutineContext.cancelChildren()
    }

    /** 破棄する */
    fun destroy() {
        stop()
        pcmPlayer.destroy()
        audioRender.destroy()
    }

    /**
     * プレイヤーの時間
     *
     * @param currentPositionMs 再生位置
     * @param durationMs 動画の時間
     */
    data class PlayerPosition(
        val currentPositionMs: Long,
        val durationMs: Long
    )

    companion object {
        /** 編集した音声の PCM 出力先 */
        private const val OUT_PCM_FILE_NAME = "outpcm_file"

        /** 音声素材をデコードした結果を持っておくフォルダ */
        private const val DECODE_PCM_FOLDER_NAME = "decode_pcm_folder"

        /** 一時的なフォルダ */
        private const val TEMP_FOLDER_NAME = "temp_folder"
    }
}