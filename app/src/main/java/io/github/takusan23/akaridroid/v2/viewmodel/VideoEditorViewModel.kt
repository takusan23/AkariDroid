package io.github.takusan23.akaridroid.v2.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.takusan23.akaridroid.v2.RenderData
import io.github.takusan23.akaridroid.v2.preview.VideoEditorPreviewPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [io.github.takusan23.akaridroid.v2.ui.screen.VideoEditorScreenV2Kt]用の ViewModel */
class VideoEditorViewModel(private val application: Application) : AndroidViewModel(application) {
    private val context: Context
        get() = application.applicationContext

    private val _renderData = MutableStateFlow(
        RenderData(
            durationMs = 10_000L,
            videoSize = RenderData.Size(1280, 720),
            canvasRenderItem = emptyList(),
            audioRenderItem = emptyList()
        )
    )

    /** プレビュー用プレイヤー */
    val videoEditorPreviewPlayer = VideoEditorPreviewPlayer(
        context = context,
        projectFolder = context.getExternalFilesDir(null)!!.resolve(PROJECT_FOLDER_NAME).apply { mkdir() }
    )

    /** 素材の情報 */
    val renderData = _renderData.asStateFlow()

    init {
        // TODO 適当に初期値を入れた
        _renderData.update {
            it.copy(
                canvasRenderItem = listOf(
                    RenderData.CanvasItem.Text(
                        text = "あかりどろいど",
                        displayTime = RenderData.DisplayTime(0, 10_000),
                        position = RenderData.Position(0f, 100f),
                        textSize = 100f
                    ),
                    RenderData.CanvasItem.Text(
                        text = "2024/02/16",
                        displayTime = RenderData.DisplayTime(0, 10_000),
                        position = RenderData.Position(0f, 150f),
                        textSize = 50f,
                        fontColor = "#ff0000"
                    )
                )
            )
        }

        // 動画の情報が更新されたら
        viewModelScope.launch {
            // Pair に詰めて distinct で変わったときだけ
            _renderData
                .map { Pair(it.videoSize, it.durationMs) }
                .distinctUntilChanged()
                .collect { (videoSize, durationMs) ->
                    videoEditorPreviewPlayer.setVideoInfo(videoSize.width, videoSize.height, durationMs)
                }
        }

        // 素材が更新されたらプレビューにも反映
        // TODO デコード処理が入るので重たい。UI にぐるぐるとか出す
        viewModelScope.launch {
            _renderData.collect { renderData ->
                videoEditorPreviewPlayer.setRenderItem(
                    audioRenderItemList = renderData.audioRenderItem,
                    canvasItemList = renderData.canvasRenderItem
                )
                // プレビューを更新
                videoEditorPreviewPlayer.startSinglePlay()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        videoEditorPreviewPlayer.destroy()
    }

    /**
     * [RenderData.CanvasItem]を追加する
     * 動画とか、テキストとか
     *
     * @param canvasItem [RenderData.CanvasItem]
     */
    fun addCanvasRenderItem(canvasItem: RenderData.CanvasItem) {
        _renderData.update {
            it.copy(canvasRenderItem = it.canvasRenderItem + canvasItem)
        }
    }

    /**
     * [RenderData.AudioItem]を追加する
     * BGM とか、動画の音声とか
     *
     * @param audioItem [RenderData.AudioItem]
     */
    fun addAudioRenderItem(audioItem: RenderData.AudioItem) {
        _renderData.update {
            it.copy(audioRenderItem = it.audioRenderItem + audioItem)
        }
    }

    /**
     * [RenderData.RenderItem]を削除する
     *
     * @param renderItem 削除したい
     */
    fun removeRenderItem(renderItem: RenderData.RenderItem) {
        _renderData.update {
            when (renderItem) {
                is RenderData.AudioItem -> it.copy(audioRenderItem = it.audioRenderItem.filter { it.id != renderItem.id })
                is RenderData.CanvasItem -> it.copy(canvasRenderItem = it.canvasRenderItem.filter { it.id != renderItem.id })
            }
        }
    }

    companion object {
        /** プロジェクト保存先、複数プロジェクトが出来るようになればこの辺も分ける */
        private const val PROJECT_FOLDER_NAME = "akaridroid_project_20240216"
    }
}