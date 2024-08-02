package io.github.takusan23.akaridroid.preview

import android.content.Context
import android.graphics.Color
import android.view.SurfaceHolder
import io.github.takusan23.akaricore.audio.AkariCoreAudioProperties
import io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.audiorender.AudioRender
import io.github.takusan23.akaridroid.canvasrender.CanvasRender
import io.github.takusan23.akaridroid.framerender.AkariCoreFrameRender
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** [RenderData.CanvasItem]をプレビューする */
class VideoEditorPreviewPlayer(
    private val context: Context,
    projectFolder: File
) {
    /** プレイヤー再生用コルーチンスコープ。キャンセル用 */
    private val playerScope = CoroutineScope(Dispatchers.Default + Job())

    /**
     * [CanvasRender]に複数スレッドから同時アクセスされないように
     * 直列にしないと、例えば描画途中に素材が破棄される→MediaCodec も破棄→破棄された MediaCodec に対して操作をする→落ちてしまう。
     */
    private val canvasRenderMutex = Mutex()

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

    private val _playerStatus = MutableStateFlow(
        PlayerStatus(
            isPlaying = false,
            currentPositionMs = 0,
            durationMs = 0,
            isPrepareCompleteAudio = true,
            isPrepareCompleteCanvas = true
        )
    )

    private val _surfaceHolderFlow = MutableStateFlow<SurfaceHolder?>(null)
    private val _canvasItemListFlow = MutableStateFlow<List<RenderData.CanvasItem>>(emptyList())
    private val _videoSizeFlow = MutableStateFlow<RenderData.Size?>(null)

    /** プレビュープレイヤーのプレイヤー状態 Flow */
    val playerStatus = _playerStatus.asStateFlow()

    init {
        // SurfaceView の準備がいつになるか分からないので（多分 Window に追加したときだろうけど）
        // SurfaceView の holder を Flow で監視しておく。
        playerScope.launch {
            _surfaceHolderFlow.collectLatest { holder ->
                holder ?: return@collectLatest

                // TODO width / height をちゃんと埋める。やっぱ RenderData が必要
                val (width, height) = _videoSizeFlow.filterNotNull().first()

                // glViewport に合わせる
                holder.setFixedSize(width, height)

                // OpenGL ES の上に構築された、動画のフレーム作成クラス
                val previewAkariGraphicsProcessor = AkariGraphicsProcessor(
                    outputSurface = holder.surface,
                    width = width,
                    height = height
                ).apply { prepare() }

                // タイムラインの素材を AkariGraphicsProcessor で描画するやつ
                val frameRender = AkariCoreFrameRender(
                    context = context,
                    genTextureId = { previewAkariGraphicsProcessor.genTextureId { it } }
                )

                try {
                    listOf(
                        // プレビュー再生を PlayerStatus の値変更をトリガーに行う
                        launch {
                            watchOneShotDrawFrame(previewAkariGraphicsProcessor, frameRender)
                        },
                        launch {
                            watchRepeatDrawFrame(previewAkariGraphicsProcessor, frameRender)
                        },
                        // タイムラインの素材が更新されたら反映
                        launch {
                            _canvasItemListFlow.collectLatest { canvasItemList ->
                                setProgress(ProgressType.CANVAS) {
                                    frameRender.setRenderData(canvasItemList)
                                }
                                // フレーム更新
                                previewAkariGraphicsProcessor.drawOneshot {
                                    drawCanvas { drawColor(Color.BLACK) } // なんか知らないけどこれがないとエラーになる
                                    frameRender.draw(
                                        textureRenderer = this,
                                        durationMs = playerStatus.value.durationMs,
                                        currentPositionMs = playerStatus.value.currentPositionMs
                                    )
                                }
                            }
                        }
                    ).joinAll()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: RuntimeException) {
                    // java.lang.RuntimeException: glBindFramebuffer: glError 1285
                    // Google Tensor だけ静止画撮影・動画撮影切り替え時に頻繁に落ちる
                    // Snapdragon だと落ちないのでガチで謎
                    // もうどうしようもないので checkGlError() の例外をここは無視する
                    // Google Tensor 、、、許すまじ
                    e.printStackTrace()
                } finally {
                    withContext(NonCancellable) {
                        frameRender.destroy()
                        previewAkariGraphicsProcessor.destroy()
                    }
                }
            }
        }
    }

    /**
     * SurfaceView の Holder を UI からもらう
     *
     * @param holder 生成されてら[SurfaceHolder]。破棄されたら null
     */
    fun setSurfaceHolder(holder: SurfaceHolder?) {
        _surfaceHolderFlow.value = holder
    }

    /** 動画の情報をセットする */
    fun setVideoInfo(
        videoWidth: Int,
        videoHeight: Int,
        durationMs: Long
    ) {
        _playerStatus.update { it.copy(durationMs = durationMs) }
        _videoSizeFlow.value = RenderData.Size(videoWidth, videoHeight)
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
        _canvasItemListFlow.value = canvasItemList.toList()
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
     * 単発の動画フレーム更新が必要か監視して、必要なら描画する
     * isPlaying が false のとき
     * 再生位置か動画の長さが変化したら
     * collectLatest で新しい値が来たら既存のブロックをキャンセルするよう
     */
    private suspend fun watchOneShotDrawFrame(
        processor: AkariGraphicsProcessor,
        frameRender: AkariCoreFrameRender
    ) {
        playerStatus
            .filter { it.isPrepareCompleteCanvas }
            .filter { !it.isPlaying }
            .map { status -> status.currentPositionMs to status.durationMs }
            .distinctUntilChanged()
            .collectLatest { (currentPositionMs, durationMs) ->
                canvasRenderMutex.withLock {
                    processor.drawOneshot {
                        drawCanvas { drawColor(Color.BLACK) } // なんか知らないけどこれがないとエラーになる
                        frameRender.draw(
                            textureRenderer = this,
                            durationMs = durationMs,
                            currentPositionMs = currentPositionMs
                        )
                    }
                }
            }
    }

    /**
     * 連続のフレーム更新が必要か監視して、必要なら描画する
     * isPlaying のみ受け取り、それ以外で起動しない
     * collectLatest で新しい値が来たら既存のブロックをキャンセルするよう
     */
    private suspend fun watchRepeatDrawFrame(
        processor: AkariGraphicsProcessor,
        frameRender: AkariCoreFrameRender
    ) = coroutineScope {
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
                    processor.drawLoop {
                        // 生成すべき時間
                        val (_, currentPositionMs, durationMs) = _playerStatus.value

                        // フレームを生成する
                        // OpenGL ES で書く
                        canvasRenderMutex.withLock {
                            drawCanvas { drawColor(Color.BLACK) } // なんか知らないけどこれがないとエラーになる
                            frameRender.draw(
                                textureRenderer = this,
                                durationMs = durationMs,
                                currentPositionMs = currentPositionMs
                            )
                        }

                        // 動画の1フレーム分の音声を取り出して再生する
                        // 1フレーム分の音声が再生されるまで止まる
                        audioRender.seek(currentPositionMs)
                        audioRender.readPcmByteArray(pcmByteArrayFromOneVideoFrame)
                        pcmPlayer.writePcmData(pcmByteArrayFromOneVideoFrame)

                        // 次のフレームのために時間を進める
                        if (_playerStatus.value.currentPositionMs <= _playerStatus.value.durationMs) {
                            val frameMs = 1000 / fps // TODO fps を RenderData から取り出す
                            _playerStatus.update { it.copy(currentPositionMs = it.currentPositionMs + frameMs) }
                            true
                        } else {
                            // 動画時間超えたら終わり
                            false
                        }
                    }
                } finally {
                    pcmPlayer.pause()
                    pause()
                }
            }
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