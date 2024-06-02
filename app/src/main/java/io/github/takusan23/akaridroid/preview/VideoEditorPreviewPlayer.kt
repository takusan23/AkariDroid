package io.github.takusan23.akaridroid.preview

import android.content.Context
import android.graphics.Bitmap
import io.github.takusan23.akaricore.audio.AkariCoreAudioProperties
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.audiorender.AudioRender
import io.github.takusan23.akaridroid.canvasrender.CanvasRender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
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

    /** [drawVideoFrame]キャンセル用 Job。複数スレッドからデコーダーにアクセスしてほしくない */
    private var drawVideoFrameJob: Job? = null

    private val canvasRender = CanvasRender(context)
    private val audioRender = AudioRender(
        context = context,
        outPcmFile = projectFolder.resolve(OUT_PCM_FILE_NAME),
        outputDecodePcmFolder = projectFolder.resolve(DECODE_PCM_FOLDER_NAME).apply { mkdir() },
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
            durationMs = 0,
            isPrepareCompleteAudio = true,
            isPrepareCompleteCanvas = true
        )
    )

    /** プレビュープレイヤーのプレイヤー状態 Flow */
    val playerStatus = _playerStatus.asStateFlow()

    /** プレビュー画像として[Bitmap]を流す Flow */
    val previewBitmap = bitmapCanvasController.latestBitmap

    init {

        // プレビュー再生を PlayerStatus の値変更をトリガーに行う

        playerScope.launch {
            // 単発の動画フレーム更新
            // isPlaying が false のとき
            // 再生位置か動画の長さが変化したら
            // collectLatest で新しい値が来たら既存のブロックをキャンセルするよう
            playerStatus
                .filterPrepareCompleted()
                .filter { !it.isPlaying }
                .map { status -> status.currentPositionMs to status.durationMs }
                .distinctUntilChanged()
                .collectLatest { (currentPositionMs, durationMs) ->
                    drawVideoFrame(durationMs, currentPositionMs)
                }
        }

        playerScope.launch {
            // 連続のフレーム更新
            // isPlaying のみ受け取り、それ以外で起動しない
            // collectLatest で新しい値が来たら既存のブロックをキャンセルするよう
            playerStatus
                .filterPrepareCompleted()
                .map { it.isPlaying }
                .distinctUntilChanged()
                .collectLatest { isPlaying ->

                    // false なら起動しない
                    if (!isPlaying) return@collectLatest

                    // TODO fps を RenderData からもらう
                    val fps = 60

                    // 1フレームの時間（60fps なら16ミリ秒）の PCM 音声を再生するのに必要なバイト配列
                    val pcmByteArrayFromOneVideoFrame = ByteArray(AkariCoreAudioProperties.ONE_SECOND_PCM_DATA_SIZE / fps)
                    // 再生を開始
                    pcmPlayer.play()

                    try {
                        // 終わるかするまで
                        while (isActive) {

                            // 生成すべき時間
                            val (_, currentPositionMs, durationMs) = _playerStatus.value

                            // フレームを生成する
                            // Canvas に書く
                            drawVideoFrame(durationMs, currentPositionMs)

                            // 動画の1フレーム分の音声を取り出して再生する
                            audioRender.seek(currentPositionMs)
                            audioRender.readPcmByteArray(pcmByteArrayFromOneVideoFrame)
                            pcmPlayer.writePcmData(pcmByteArrayFromOneVideoFrame)

                            // 次のフレームのために時間を進める
                            if (_playerStatus.value.currentPositionMs <= _playerStatus.value.durationMs) {
                                val frameMs = 1000 / fps // TODO fps を RenderData から取り出す
                                _playerStatus.update { it.copy(currentPositionMs = it.currentPositionMs + frameMs) }
                            } else {
                                // 動画時間超えたら終わり
                                break
                            }
                        }
                    } finally {
                        pcmPlayer.pause()
                        pause()
                    }
                }
        }
    }

    /** 動画の情報をセットする */
    fun setVideoInfo(
        videoWidth: Int,
        videoHeight: Int,
        durationMs: Long
    ) {
        _playerStatus.update { it.copy(durationMs = durationMs) }
        bitmapCanvasController.createCanvas(videoWidth, videoHeight)
    }

    /**
     * 動画で再生する音声素材をセットする。
     * 詳しくは[AudioRender.setRenderData]
     */
    suspend fun setAudioRenderItem(
        audioRenderItemList: List<RenderData.AudioItem> = emptyList()
    ) = withContext(Dispatchers.Default) {
        setProgress(ProgressType.AUDIO) {
            audioRender.setRenderData(audioRenderItemList, _playerStatus.value.durationMs)
        }
    }

    /**
     * 動画で再生するキャンパス要素をセットする
     * 詳しくは[CanvasRender.setRenderData]
     */
    suspend fun setCanvasRenderItem(
        canvasItemList: List<RenderData.CanvasItem> = emptyList(),
    ) = withContext(Dispatchers.Default) {
        // 準備とプレビュー再生が反映されるまで prepare に
        setProgress(ProgressType.CANVAS) {
            canvasRender.setRenderData(canvasItemList)
            drawVideoFrame()
        }
    }

    /** シークする */
    fun seekTo(seekToMs: Long) {
        _playerStatus.update { it.copy(currentPositionMs = seekToMs) }
    }

    /**
     * プレビュー再生を開始する。停止するまで時間が進み続けます。
     * プレビュー再生は動画のフレーム（画像）生成に時間がかかるのでかくかくします。
     * （これでも自前クラス[io.github.takusan23.akaricore.video.VideoFrameBitmapExtractor]のおかげでかなり早いです）
     */
    fun playInRepeat() {
        _playerStatus.update { it.copy(isPlaying = true) }
    }

    /** 再生を一時停止する */
    fun pause() {
        _playerStatus.update { it.copy(isPlaying = false) }
    }

    /** 破棄する */
    fun destroy() {
        playerScope.cancel()
        pcmPlayer.destroy()
        audioRender.destroy()
    }

    /**
     * [PlayerStatus.isPrepareCompleteAudio]や[PlayerStatus.isPrepareCompleteCanvas]のフラグを書き換える
     * [task]ブロックを抜けたら自動で false になります。
     */
    private inline fun setProgress(type: ProgressType, task: () -> Unit) {
        _playerStatus.update {
            when (type) {
                ProgressType.AUDIO -> it.copy(isPrepareCompleteAudio = false)
                ProgressType.CANVAS -> it.copy(isPrepareCompleteCanvas = false)
            }
        }
        task()
        _playerStatus.update {
            when (type) {
                ProgressType.AUDIO -> it.copy(isPrepareCompleteAudio = true)
                ProgressType.CANVAS -> it.copy(isPrepareCompleteCanvas = true)
            }
        }
    }

    /**
     * 再生準備ができたときのみ、このあとの Flow を流す
     * [PlayerStatus.isPrepareCompleteAudio]と[PlayerStatus.isPrepareCompleteCanvas]
     */
    private fun Flow<PlayerStatus>.filterPrepareCompleted() = this.filter { it.isPrepareCompleteAudio && it.isPrepareCompleteCanvas }

    /**
     * 指定した時間の動画フレームを Canvas で描画する
     *
     * @param durationMs 動画の時間。ミリ秒
     * @param currentPositionMs 再生位置。ミリ秒
     */
    private suspend fun drawVideoFrame(
        durationMs: Long = playerStatus.value.durationMs,
        currentPositionMs: Long = playerStatus.value.currentPositionMs
    ) = coroutineScope {
        drawVideoFrameJob?.cancelAndJoin()
        drawVideoFrameJob = launch {
            bitmapCanvasController.update { canvas ->
                canvasRender.draw(canvas, durationMs, currentPositionMs)
            }
        }
    }

    /**
     * プレイヤーの状態
     *
     * @param isPlaying 再生中かどうか
     * @param currentPositionMs 再生位置
     * @param durationMs 動画の時間
     * @param isPrepareCompleteAudio 音声プレビューが利用できる場合は true。準備中の場合は false
     * @param isPrepareCompleteCanvas 映像（キャンバス）プレビューが利用できる場合は true。準備中の場合は false
     */
    data class PlayerStatus(
        val isPlaying: Boolean,
        val currentPositionMs: Long,
        val durationMs: Long,
        val isPrepareCompleteAudio: Boolean,
        val isPrepareCompleteCanvas: Boolean
    )

    /** [setProgress]に渡す引数 */
    enum class ProgressType {
        /** [PlayerStatus.isPrepareCompleteAudio] */
        AUDIO,

        /** [PlayerStatus.isPrepareCompleteCanvas] */
        CANVAS
    }

    companion object {
        // TODO この3つのあたい、AudioRender の static のがよくない？

        /** 編集した音声の PCM 出力先 */
        const val OUT_PCM_FILE_NAME = "outpcm_file"

        /** 音声素材をデコードした結果を持っておくフォルダ */
        const val DECODE_PCM_FOLDER_NAME = "decode_pcm_folder"

        /** 一時的なフォルダ */
        const val TEMP_FOLDER_NAME = "temp_folder"
    }
}