package io.github.takusan23.akaridroid.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.RenderData.RenderItem
import io.github.takusan23.akaridroid.preview.VideoEditorPreviewPlayer
import io.github.takusan23.akaridroid.tool.UriTool
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditorBottomSheetRouteRequestData
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditorBottomSheetRouteResultData
import io.github.takusan23.akaridroid.ui.component.VideoEditorBottomBarAddItem
import io.github.takusan23.akaridroid.ui.component.data.TimeLineItemData
import io.github.takusan23.akaridroid.ui.component.data.timeRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [io.github.takusan23.akaridroid.ui.screen.VideoEditorScreenKt]用の ViewModel */
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
    private val _timeLineItemDataList = MutableStateFlow<List<TimeLineItemData>>(emptyList())

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

    /** タイムラインに表示するデータ。[RenderData]と同期する */
    val timeLineItemDataList = _timeLineItemDataList.asStateFlow()

    init {
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

        // 素材が更新されたらタイムラインにも反映
        viewModelScope.launch {
            combine(
                _renderData.map { it.canvasRenderItem },
                _renderData.map { it.audioRenderItem }
            ) { canvas, audio -> canvas + audio }
                .distinctUntilChanged()
                .collect { renderItemList ->
                    // 入れる値
                    val timeLineItemDataArrayList = arrayListOf<TimeLineItemData>()
                    // キャンバス
                    renderItemList
                        .filterIsInstance<RenderData.CanvasItem>()
                        .forEach { renderItem ->
                            timeLineItemDataArrayList += TimeLineItemData(
                                id = renderItem.id,
                                laneIndex = renderItem.layerIndex,
                                startMs = renderItem.displayTime.startMs,
                                stopMs = renderItem.displayTime.stopMs,
                                label = when (renderItem) {
                                    is RenderData.CanvasItem.Image -> "画像"
                                    is RenderData.CanvasItem.Text -> "テキスト"
                                    is RenderData.CanvasItem.Video -> "動画"
                                },
                                iconResId = when (renderItem) {
                                    is RenderData.CanvasItem.Image -> R.drawable.ic_outline_add_photo_alternate_24px
                                    is RenderData.CanvasItem.Text -> R.drawable.ic_outline_text_fields_24
                                    is RenderData.CanvasItem.Video -> R.drawable.ic_outline_video_file_24
                                }
                            )
                        }
                    // 音声
                    renderItemList
                        .filterIsInstance<RenderData.AudioItem>()
                        .forEach { audioItem ->
                            timeLineItemDataArrayList += TimeLineItemData(
                                id = audioItem.id,
                                laneIndex = audioItem.layerIndex,
                                startMs = audioItem.displayTime.startMs,
                                stopMs = audioItem.displayTime.stopMs,
                                label = "音声",
                                iconResId = R.drawable.ic_outline_audiotrack_24
                            )
                        }
                    // 入れる
                    _timeLineItemDataList.value = timeLineItemDataArrayList
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

        // 追加できそうなレーン番号を出す
        fun calcLayerIndex(displayTime: RenderData.DisplayTime) = _timeLineItemDataList.value.calcInsertableLaneIndex(displayTime)

        val openEditItem = when (addItem) {
            // テキスト
            VideoEditorBottomBarAddItem.Text -> {
                val displayTime = RenderData.DisplayTime(0, 10_000)
                val text = RenderData.CanvasItem.Text(
                    text = "",
                    displayTime = displayTime,
                    position = RenderData.Position(0f, 0f),
                    layerIndex = calcLayerIndex(displayTime)
                )
                addOrUpdateCanvasRenderItem(text)
                text
            }
            // 画像
            is VideoEditorBottomBarAddItem.Image -> {
                val size = UriTool.analyzeImage(context, addItem.uri)?.size ?: return@launch
                val displayTime = RenderData.DisplayTime(0, 10_000)
                val image = RenderData.CanvasItem.Image(
                    filePath = RenderData.FilePath.Uri(addItem.uri.toString()),
                    displayTime = displayTime,
                    position = RenderData.Position(0f, 0f),
                    size = RenderData.Size(size.width, size.height),
                    layerIndex = calcLayerIndex(displayTime)
                )
                addOrUpdateCanvasRenderItem(image)
                image
            }
            // 音声
            is VideoEditorBottomBarAddItem.Audio -> {
                val durationMs = UriTool.analyzeAudio(context, addItem.uri)?.durationMs ?: return@launch
                val displayTime = RenderData.DisplayTime(0, durationMs)
                val audio = RenderData.AudioItem.Audio(
                    filePath = RenderData.FilePath.Uri(addItem.uri.toString()),
                    displayTime = displayTime,
                    layerIndex = calcLayerIndex(displayTime)
                )
                addOrUpdateAudioRenderItem(audio)
                audio
            }
            // 動画
            is VideoEditorBottomBarAddItem.Video -> {
                val analyzeVideo = UriTool.analyzeVideo(context, addItem.uri) ?: return@launch
                val durationMs = analyzeVideo.durationMs
                val displayTime = RenderData.DisplayTime(0, durationMs)

                // 映像トラックを追加
                val videoTrack = RenderData.CanvasItem.Video(
                    filePath = RenderData.FilePath.Uri(addItem.uri.toString()),
                    displayTime = displayTime,
                    position = RenderData.Position(0f, 0f),
                    size = RenderData.Size(analyzeVideo.size.width, analyzeVideo.size.height),
                    layerIndex = calcLayerIndex(displayTime)
                )
                addOrUpdateCanvasRenderItem(videoTrack)

                // 音声トラックもあれば追加
                if (analyzeVideo.hasAudioTrack) {
                    val audioTrack = RenderData.AudioItem.Audio(
                        id = System.currentTimeMillis() + 10,
                        filePath = RenderData.FilePath.Uri(addItem.uri.toString()),
                        displayTime = RenderData.DisplayTime(0, durationMs),
                        layerIndex = calcLayerIndex(displayTime)
                    )
                    addOrUpdateAudioRenderItem(audioTrack)
                }
                videoTrack
            }
        }

        // 編集画面を開く
        openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenEditor(renderItem = openEditItem))
    }

    /**
     * タイムラインの並び替え（ドラッグアンドドロップ）リクエストをさばく
     *
     * @param afterTarget 移動先の位置になっている[TimeLineItemData]。TODO これ全部はいらないから startMs / stopMs / id だけ貰えばいいはず
     * @param fromLane 移動元レーン番号
     * @param toLine 移動先レーン番号
     * @return true でドラッグアンドドロップを受け入れたことになる。
     */
    fun resolveDragAndDropRequest(afterTarget: TimeLineItemData, fromLane: Int, toLine: Int): Boolean {
        // 移動先のレーンに空きがあること
        // 同一レーンの移動の場合は自分自身も消す（同一レーンでの時間調整できなくなる）
        val isAcceptable = _timeLineItemDataList.value
            .filter { laneItem -> laneItem.laneIndex == toLine }
            .filter { laneItem -> laneItem.id != afterTarget.id }
            .all { laneItem ->
                // 空きがあること
                val hasFreeSpace = afterTarget.startMs !in laneItem.timeRange && afterTarget.stopMs !in laneItem.timeRange
                // 移動先に重なる形で自分より小さいアイテムが居ないこと
                val hasNotInclude = laneItem.startMs !in afterTarget.timeRange && laneItem.stopMs !in afterTarget.timeRange
                hasFreeSpace && hasNotInclude
            }

        // RenderData を更新する
        // タイムラインも RenderData の Flow から再構築される
        val displayTime = RenderData.DisplayTime(afterTarget.startMs, afterTarget.stopMs)
        val layerIndex = afterTarget.laneIndex
        val isCanvas = _renderData.value.canvasRenderItem.any { it.id == afterTarget.id }
        if (isCanvas) {
            val before = _renderData.value.canvasRenderItem.first { it.id == afterTarget.id }
            addOrUpdateCanvasRenderItem(
                when (before) {
                    is RenderData.CanvasItem.Image -> before.copy(displayTime = displayTime, layerIndex = layerIndex)
                    is RenderData.CanvasItem.Text -> before.copy(displayTime = displayTime, layerIndex = layerIndex)
                    is RenderData.CanvasItem.Video -> before.copy(displayTime = displayTime, layerIndex = layerIndex)
                }
            )
        } else {
            val before = _renderData.value.audioRenderItem.first { it.id == afterTarget.id }
            addOrUpdateAudioRenderItem(
                when (before) {
                    is RenderData.AudioItem.Audio -> before.copy(displayTime = displayTime, layerIndex = layerIndex)
                }
            )
        }

        return isAcceptable
    }

    /** [RenderData.RenderItem.id] から [RenderItem] を返す */
    fun getRenderItem(id: Long): RenderItem? = (_renderData.value.canvasRenderItem + _renderData.value.audioRenderItem)
        .firstOrNull { it.id == id }

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

    /**
     * [TimeLineItemData]を追加する際に、位置[RenderData.DisplayTime]に被らないレーン番号を取得する
     *
     * @param displayTime 追加したい時間（開始、終了位置）
     * @return 入れられるレーン番号。なければ今ある最大のレーン番号 + 1 を返す。レーンがない場合は 0 を返す。
     */
    private fun List<TimeLineItemData>.calcInsertableLaneIndex(
        displayTime: RenderData.DisplayTime
    ): Int = this // TODO まず最低レーン分確保する
        .firstOrNull { laneItem ->
            // 空きがあること
            val hasFreeSpace = displayTime.startMs !in laneItem.timeRange && displayTime.stopMs !in laneItem.timeRange
            // 移動先に重なる形で自分より小さいアイテムが居ないこと
            val hasNotInclude = laneItem.startMs !in displayTime && laneItem.stopMs !in displayTime
            hasFreeSpace && hasNotInclude
        }?.laneIndex
        ?: this.maxOfOrNull { it.laneIndex }?.plus(1) // 見つからなければ最大のレーン番号 + 1 を返す
        ?: 0 // どうしようもない

    companion object {
        /** プロジェクト保存先、複数プロジェクトが出来るようになればこの辺も分ける */
        private const val PROJECT_FOLDER_NAME = "akaridroid_project_20240216"
    }
}