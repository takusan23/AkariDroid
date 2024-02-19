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

    private val _playerStatus = MutableStateFlow(
        PlayerStatus(
            isPlaying = false,
            currentPositionMs = 0,
            durationMs = 0
        )
    )

    /** プレビュープレイヤーのプレイヤー状態 Flow */
    val playerStatus = _playerStatus.asStateFlow()

    /** プレビュー画像として[Bitmap]を流す Flow */
    val previewBitmap = bitmapCanvasController.latestBitmap

    /** 動画の情報をセットする */
    fun setVideoInfo(
        videoWidth: Int,
        videoHeight: Int,
        durationMs: Long
    ) {
        _playerStatus.update { it.copy(durationMs = durationMs) }
        bitmapCanvasController.createCanvas(videoWidth, videoHeight)
    }

    /** 動画で再生する音声素材をセットする */
    suspend fun setAudioRenderItem(
        audioRenderItemList: List<RenderData.AudioItem> = emptyList()
    ) = withContext(Dispatchers.Default) {
        audioRender.setRenderData(audioRenderItemList, _playerStatus.value.durationMs)
    }

    /** 動画で再生するキャンパス要素をセットする */
    suspend fun setCanvasRenderItem(
        canvasItemList: List<RenderData.CanvasItem> = emptyList(),
    ) = withContext(Dispatchers.Default) {
        canvasRender.setRenderData(canvasItemList)
    }

    /** シークする */
    fun seekTo(seekToMs: Long) {
        _playerStatus.update { it.copy(currentPositionMs = seekToMs) }
        // プレビューを更新
        playInSingle()
    }

    /** 現在位置のプレビューを更新する。更新は一回だけ */
    fun playInSingle() {
        playerScope.launch {
            val (_, currentPosition, videoDuration) = _playerStatus.first()
            bitmapCanvasController.update { canvas ->
                canvasRender.draw(canvas, videoDuration, currentPosition)
            }
        }
    }

    /** プレビュー再生を開始する。停止するまで時間が進み続けます。 */
    fun playInRepeat() {

        _playerStatus.update { it.copy(isPlaying = true) }

        // 時間を進める
        playerScope.launch {
            while (isActive) {
                // 時間を進める
                if (_playerStatus.value.currentPositionMs <= _playerStatus.value.durationMs) {
                    // 1秒ごとなのは、AudioRender が時間の単位が秒なのでそれの最低値
                    _playerStatus.update { it.copy(currentPositionMs = it.currentPositionMs + 1_000) }
                    delay(1_000)
                } else {
                    // 動画時間超えたら折る
                    _playerStatus.update { it.copy(isPlaying = false) }
                    break
                }
            }
        }

        // Canvas に書く
        // プレビュー重たい
        // 速度優先のため1秒ごと
        playerScope.launch {
            _playerStatus.collect { (_, currentPosition, videoDuration) ->
                bitmapCanvasController.update { canvas ->
                    canvasRender.draw(canvas, videoDuration, currentPosition)
                }
            }
        }

        // 再生を開始
        pcmPlayer.play()
        // AudioRender も用意
        // 1秒間に必要な ByteArray を用意して読み出す
        val pcmByteArray = ByteArray(AkariCoreAudioProperties.ONE_SECOND_PCM_DATA_SIZE)
        playerScope.launch {
            _playerStatus.collect { (_, currentPosition, _) ->
                // シークする
                audioRender.seek((currentPosition / 1000).toInt())
                audioRender.readPcmByteArray(pcmByteArray)
                // スピーカーへ流す
                pcmPlayer.writePcmData(pcmByteArray)
            }
        }
    }

    /** 再生を一時停止する */
    fun pause() {
        _playerStatus.update { it.copy(isPlaying = false) }
        pcmPlayer.pause()
        playerScope.coroutineContext.cancelChildren()
    }

    /** 破棄する */
    fun destroy() {
        pause()
        pcmPlayer.destroy()
        audioRender.destroy()
    }

    /**
     * プレイヤーの状態
     *
     * @param isPlaying 再生中かどうか
     * @param currentPositionMs 再生位置
     * @param durationMs 動画の時間
     */
    data class PlayerStatus(
        val isPlaying: Boolean,
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