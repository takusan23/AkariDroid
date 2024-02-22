package io.github.takusan23.akaridroid.v2.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.takusan23.akaridroid.v2.RenderData
import io.github.takusan23.akaridroid.v2.preview.VideoEditorPreviewPlayer
import io.github.takusan23.akaridroid.v2.tool.UriTool
import io.github.takusan23.akaridroid.v2.ui.bottomsheet.VideoEditorBottomSheetRouteRequestData
import io.github.takusan23.akaridroid.v2.ui.bottomsheet.VideoEditorBottomSheetRouteResultData
import io.github.takusan23.akaridroid.v2.ui.component.VideoEditorBottomBarAddItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val _bottomSheetRouteData = MutableStateFlow<VideoEditorBottomSheetRouteRequestData?>(null)

    /** 作業用フォルダ。ここにデコードした音声素材とかが来る */
    val projectFolder = context.getExternalFilesDir(null)!!.resolve(PROJECT_FOLDER_NAME).apply { mkdir() }

    /** プレビュー用プレイヤー */
    val videoEditorPreviewPlayer = VideoEditorPreviewPlayer(
        context = context,
        projectFolder = projectFolder
    )

    /** 素材の情報 */
    val renderData = _renderData.asStateFlow()

    /** ボトムシートのルーティング */
    val bottomSheetRouteData = _bottomSheetRouteData.asStateFlow()

    init {
        // TODO 適当に初期値を入れた
        _renderData.update {
            it.copy(
                /*
                                canvasRenderItem = listOf(
                                    RenderData.CanvasItem.Text(
                                        id = 1,
                                        text = "あかりどろいど",
                                        displayTime = RenderData.DisplayTime(0, 10_000),
                                        position = RenderData.Position(0f, 100f),
                                        textSize = 100f
                                    ),
                                    RenderData.CanvasItem.Text(
                                        id = 2,
                                        text = "2024/02/16",
                                        displayTime = RenderData.DisplayTime(0, 10_000),
                                        position = RenderData.Position(0f, 150f),
                                        textSize = 50f,
                                        fontColor = "#ff0000"
                                    )
                                )
                */
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
            _renderData
                .map { it.audioRenderItem }
                .distinctUntilChanged()
                .collectLatest { renderItem ->
                    videoEditorPreviewPlayer.setAudioRenderItem(renderItem)
                }
        }
        viewModelScope.launch {
            _renderData
                .map { it.canvasRenderItem }
                .distinctUntilChanged()
                .collect { renderItem ->
                    videoEditorPreviewPlayer.setCanvasRenderItem(renderItem)
                    // プレビューを更新
                    videoEditorPreviewPlayer.playInSingle()
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        videoEditorPreviewPlayer.destroy()
    }

    /** ボトムシートの作業結果[VideoEditorBottomSheetRouteResultData]を捌く */
    fun resolveBottomSheetResult(routeResultData: VideoEditorBottomSheetRouteResultData) {
        when (routeResultData) {
            is VideoEditorBottomSheetRouteResultData.DeleteRenderItem -> deleteRenderItem(routeResultData.renderItem)
            is VideoEditorBottomSheetRouteResultData.TextCreateOrUpdate -> addOrUpdateCanvasRenderItem(routeResultData.text)
            is VideoEditorBottomSheetRouteResultData.VideoUpdate -> addOrUpdateCanvasRenderItem(routeResultData.video)
            is VideoEditorBottomSheetRouteResultData.AudioUpdate -> addOrUpdateAudioRenderItem(routeResultData.audio)
            is VideoEditorBottomSheetRouteResultData.ImageUpdate -> addOrUpdateCanvasRenderItem(routeResultData.image)
        }
    }

    /** ボトムシートを表示させる */
    fun openBottomSheet(bottomSheetRouteRequestData: VideoEditorBottomSheetRouteRequestData) {
        _bottomSheetRouteData.value = bottomSheetRouteRequestData
    }

    /** ボトムシートを閉じる */
    fun closeBottomSheet() {
        _bottomSheetRouteData.value = null
    }

    /** ボトムバーの[VideoEditorBottomBarAddItem]の結果を捌く */
    fun resolveVideoEditorBottomBarAddItem(addItem: VideoEditorBottomBarAddItem) = viewModelScope.launch {
        val addRenderItemList = when (addItem) {
            // テキスト
            VideoEditorBottomBarAddItem.Text -> listOf(
                RenderData.CanvasItem.Text(
                    text = "",
                    displayTime = RenderData.DisplayTime(0, 10_000),
                    position = RenderData.Position(0f, 0f)
                )
            )
            // 画像
            is VideoEditorBottomBarAddItem.Image -> {
                val size = UriTool.analyzeImage(context, addItem.uri)?.size ?: return@launch
                listOf(
                    RenderData.CanvasItem.Image(
                        filePath = RenderData.FilePath.Uri(addItem.uri.toString()),
                        displayTime = RenderData.DisplayTime(0, 10_000),
                        position = RenderData.Position(0f, 0f),
                        size = RenderData.Size(size.width, size.height)
                    )
                )
            }
            // 音声
            is VideoEditorBottomBarAddItem.Audio -> {
                val durationMs = UriTool.analyzeAudio(context, addItem.uri)?.durationMs ?: return@launch
                listOf(
                    RenderData.AudioItem.Audio(
                        filePath = RenderData.FilePath.Uri(addItem.uri.toString()),
                        displayTime = RenderData.DisplayTime(0, durationMs)
                    )
                )
            }
            // 動画
            is VideoEditorBottomBarAddItem.Video -> {
                val analyzeVideo = UriTool.analyzeVideo(context, addItem.uri) ?: return@launch
                val durationMs = analyzeVideo.durationMs
                listOf(
                    RenderData.CanvasItem.Video(
                        filePath = RenderData.FilePath.Uri(addItem.uri.toString()),
                        displayTime = RenderData.DisplayTime(0, durationMs),
                        position = RenderData.Position(0f, 0f),
                        size = RenderData.Size(analyzeVideo.size.width, analyzeVideo.size.height)
                    )
                ) + if (analyzeVideo.hasAudioTrack) {
                    listOf(
                        RenderData.AudioItem.Audio(
                            id = System.currentTimeMillis() + 10,
                            filePath = RenderData.FilePath.Uri(addItem.uri.toString()),
                            displayTime = RenderData.DisplayTime(0, durationMs)
                        )
                    )
                } else {
                    emptyList()
                }
            }
        }

        // 追加する
        addRenderItemList
            .filterIsInstance<RenderData.AudioItem>()
            .forEach { addOrUpdateAudioRenderItem(it) }
        addRenderItemList
            .filterIsInstance<RenderData.CanvasItem>()
            .forEach { addOrUpdateCanvasRenderItem(it) }

        // 編集画面を開く
        openBottomSheet(
            VideoEditorBottomSheetRouteRequestData.OpenEditor(
                renderItem = addRenderItemList.first()
            )
        )
    }

    /**
     * [RenderData.CanvasItem]を追加する
     * 動画とか、テキストとか
     *
     * @param canvasItem [RenderData.CanvasItem]
     */
    private fun addOrUpdateCanvasRenderItem(canvasItem: RenderData.CanvasItem) {
        _renderData.update { before ->
            // 更新なら
            if (before.canvasRenderItem.any { it.id == canvasItem.id }) {
                before.copy(canvasRenderItem = before.canvasRenderItem.map { item ->
                    if (item.id == canvasItem.id) canvasItem else item
                })
            } else {
                before.copy(canvasRenderItem = before.canvasRenderItem + canvasItem)
            }
        }
    }

    /**
     * [RenderData.AudioItem]を追加する
     * BGM とか、動画の音声とか
     *
     * @param audioItem [RenderData.AudioItem]
     */
    private fun addOrUpdateAudioRenderItem(audioItem: RenderData.AudioItem) {
        _renderData.update { before ->
            // 更新なら
            if (before.audioRenderItem.any { it.id == audioItem.id }) {
                before.copy(audioRenderItem = before.audioRenderItem.map { item ->
                    if (item.id == audioItem.id) audioItem else item
                })
            } else {
                before.copy(audioRenderItem = before.audioRenderItem + audioItem)
            }
        }
    }

    /**
     * [RenderData.RenderItem]を削除する
     *
     * @param renderItem 削除したい
     */
    private fun deleteRenderItem(renderItem: RenderData.RenderItem) {
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