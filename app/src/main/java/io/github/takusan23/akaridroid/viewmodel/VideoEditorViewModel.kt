package io.github.takusan23.akaridroid.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.canvasrender.itemrender.TextRender
import io.github.takusan23.akaridroid.preview.VideoEditorPreviewPlayer
import io.github.takusan23.akaridroid.tool.UriTool
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditorBottomSheetRouteRequestData
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditorBottomSheetRouteResultData
import io.github.takusan23.akaridroid.ui.component.VideoEditorBottomBarAddItem
import io.github.takusan23.akaridroid.ui.component.data.TimeLineData
import io.github.takusan23.akaridroid.ui.component.data.TouchEditorData
import io.github.takusan23.akaridroid.ui.component.data.groupByLane
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/** [io.github.takusan23.akaridroid.ui.screen.VideoEditorScreen]用の ViewModel */
class VideoEditorViewModel(private val application: Application) : AndroidViewModel(application) {
    private val context: Context
        get() = application.applicationContext

    private val _renderData = MutableStateFlow(
        RenderData(
            durationMs = 60_000L,
            videoSize = RenderData.Size(1280, 720),
            canvasRenderItem = emptyList(),
            audioRenderItem = emptyList()
        )
    )
    private val _bottomSheetRouteData = MutableStateFlow<VideoEditorBottomSheetRouteRequestData?>(null)
    private val _timeLineData = MutableStateFlow(
        TimeLineData(
            durationMs = _renderData.value.durationMs,
            laneCount = 5,
            itemList = emptyList()
        )
    )
    private val _touchEditorData = MutableStateFlow(
        TouchEditorData(
            videoSize = _renderData.value.videoSize,
            visibleTouchEditorItemList = emptyList()
        )
    )

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
    val timeLineData = _timeLineData.asStateFlow()

    /** タッチ操作でキャンバス要素編集できるやつ */
    val touchEditorData = _touchEditorData.asStateFlow()

    init {
        // 動画の情報が更新されたら
        viewModelScope.launch {
            // Pair に詰めて distinct で変わったときだけ
            _renderData
                .map { Pair(it.videoSize, it.durationMs) }
                .distinctUntilChanged()
                .collect { (videoSize, durationMs) ->
                    videoEditorPreviewPlayer.setVideoInfo(videoSize.width, videoSize.height, durationMs)
                    _timeLineData.update { it.copy(durationMs = durationMs) }
                    _touchEditorData.update { it.copy(videoSize = videoSize) }
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
                    val timeLineItemDataArrayList = arrayListOf<TimeLineData.Item>()
                    // キャンバス
                    renderItemList
                        .filterIsInstance<RenderData.CanvasItem>()
                        .forEach { renderItem ->
                            timeLineItemDataArrayList += TimeLineData.Item(
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
                            timeLineItemDataArrayList += TimeLineData.Item(
                                id = audioItem.id,
                                laneIndex = audioItem.layerIndex,
                                startMs = audioItem.displayTime.startMs,
                                stopMs = audioItem.displayTime.stopMs,
                                label = "音声",
                                iconResId = R.drawable.ic_outline_audiotrack_24
                            )
                        }
                    // 入れる
                    _timeLineData.update { it.copy(itemList = timeLineItemDataArrayList) }
                }
        }

        // タッチ編集の更新
        viewModelScope.launch {
            // 時間が変化したらタッチ編集の方も更新。
            // あ、あと素材が変化したときも
            videoEditorPreviewPlayer.playerStatus
                .map { it.currentPositionMs }
                // 時間が変化したら、↓ の Flow 収集をキャンセルしてやり直すため collectLatest
                .collectLatest { currentPositionMs ->
                    renderData
                        .map { it.canvasRenderItem }
                        .map { canvasRenderItem -> canvasRenderItem.filter { currentPositionMs in it.displayTime } }
                        .map { canvasRenderItem ->
                            canvasRenderItem.map { canvasItem ->
                                val measure = canvasItem.measureSize()
                                TouchEditorData.TouchEditorItem(
                                    id = canvasItem.id,
                                    size = measure,
                                    position = if (canvasItem is RenderData.CanvasItem.Text) {
                                        canvasItem.position.copy(y = canvasItem.position.y - measure.height)
                                    } else {
                                        canvasItem.position
                                    }
                                )
                            }
                        }
                        .collect { visibleTouchEditorItemList ->
                            _touchEditorData.update {
                                it.copy(visibleTouchEditorItemList = visibleTouchEditorItemList)
                            }
                        }
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
            is VideoEditorBottomSheetRouteResultData.VideoInfoUpdate -> _renderData.update { routeResultData.renderData }
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

        // 真ん中らへんになるように
        val centerPosition = RenderData.Position(
            x = (renderData.value.videoSize.width / 2).toFloat(),
            y = (renderData.value.videoSize.height / 2).toFloat()
        )

        val openEditItem = when (addItem) {
            // テキスト
            VideoEditorBottomBarAddItem.Text -> {
                val displayTime = RenderData.DisplayTime(0, 10_000)
                val text = RenderData.CanvasItem.Text(
                    text = "",
                    displayTime = displayTime,
                    position = centerPosition,
                    layerIndex = calcInsertableLaneIndex(displayTime)
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
                    position = centerPosition,
                    size = RenderData.Size(size.width, size.height),
                    layerIndex = calcInsertableLaneIndex(displayTime)
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
                    layerIndex = calcInsertableLaneIndex(displayTime)
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
                    position = centerPosition,
                    size = RenderData.Size(analyzeVideo.size.width, analyzeVideo.size.height),
                    layerIndex = calcInsertableLaneIndex(displayTime)
                )
                addOrUpdateCanvasRenderItem(videoTrack)

                // 音声トラックもあれば追加
                if (analyzeVideo.hasAudioTrack) {
                    val audioTrack = RenderData.AudioItem.Audio(
                        id = System.currentTimeMillis() + 10,
                        filePath = RenderData.FilePath.Uri(addItem.uri.toString()),
                        displayTime = RenderData.DisplayTime(0, durationMs),
                        layerIndex = calcInsertableLaneIndex(displayTime)
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
     * @param request [io.github.takusan23.akaridroid.ui.component.TimeLine]からドラッグアンドドロップが終わると呼ばれる
     * @return true でドラッグアンドドロップを受け入れたことになる。
     */
    fun resolveTimeLineDragAndDropRequest(request: TimeLineData.DragAndDropRequest): Boolean {
        // ドラッグアンドドロップ対象の RenderItem を取る
        val renderItem = getRenderItem(request.id)!!
        // ドラッグアンドドロップ移動先に合った DisplayTime を作る
        val dragAndDroppedDisplayTime = RenderData.DisplayTime(
            startMs = request.dragAndDroppedStartMs,
            stopMs = request.dragAndDroppedStartMs + renderItem.displayTime.durationMs
        )

        // 移動先のレーンに空きがあること
        val isAcceptable = _timeLineData.value
            // すべてのレーンを取得したあと、指定レーンだけ取り出す
            .groupByLane()
            .first { (laneIndex, _) -> laneIndex == request.dragAndDroppedLaneIndex }
            .second
            // 同一レーンの移動の場合は自分自身も消す（同一レーンでの時間調整できなくなる）
            .filter { laneItem -> laneItem.id != request.id }
            // 判定を行う
            .all { laneItem ->
                // 空きがあること
                val hasFreeSpace = dragAndDroppedDisplayTime.startMs !in laneItem.timeRange && dragAndDroppedDisplayTime.stopMs !in laneItem.timeRange
                // 移動先に重なる形で自分より小さいアイテムが居ないこと
                val hasNotInclude = laneItem.startMs !in dragAndDroppedDisplayTime && laneItem.stopMs !in dragAndDroppedDisplayTime
                hasFreeSpace && hasNotInclude
            }

        // RenderData を更新する
        // タイムラインも RenderData の Flow から再構築される
        val layerIndex = request.dragAndDroppedLaneIndex
        when (renderItem) {
            is RenderData.AudioItem.Audio -> addOrUpdateAudioRenderItem(
                renderItem.copy(
                    displayTime = dragAndDroppedDisplayTime,
                    layerIndex = layerIndex
                )
            )

            is RenderData.CanvasItem.Image -> addOrUpdateCanvasRenderItem(
                renderItem.copy(
                    displayTime = dragAndDroppedDisplayTime,
                    layerIndex = layerIndex
                )
            )

            is RenderData.CanvasItem.Text -> addOrUpdateCanvasRenderItem(
                renderItem.copy(
                    displayTime = dragAndDroppedDisplayTime,
                    layerIndex = layerIndex
                )
            )

            is RenderData.CanvasItem.Video -> addOrUpdateCanvasRenderItem(
                renderItem.copy(
                    displayTime = dragAndDroppedDisplayTime,
                    layerIndex = layerIndex
                )
            )
        }

        return isAcceptable
    }

    /**
     * プレビューのタッチ編集から来たアイテム移動リクエストをさばく
     *
     * @param request 移動したアイテムの [TouchEditorData.PositionUpdateRequest]
     */
    fun resolveTouchEditorDragAndDropRequest(request: TouchEditorData.PositionUpdateRequest) {
        // RenderData を更新する
        // そのほかも RenderData の Flow から再構築される
        when (val renderItem = getRenderItem(request.id)!!) {
            is RenderData.CanvasItem.Image -> addOrUpdateCanvasRenderItem(renderItem.copy(position = request.position))
            is RenderData.CanvasItem.Video -> addOrUpdateCanvasRenderItem(renderItem.copy(position = request.position))
            is RenderData.CanvasItem.Text -> addOrUpdateCanvasRenderItem(renderItem.copy(position = request.position))
            is RenderData.AudioItem.Audio -> {
                // キャンバス要素だけなのでここに来ることはない
                // do nothing
            }
        }
    }

    /**
     * タイムラインのアイテム分割リクエストをさばく
     *
     * @param request 分割したいタイムラインのアイテム
     */
    fun resolveTimeLineCutRequest(request: TimeLineData.Item) {
        val cutPositionMs = videoEditorPreviewPlayer.playerStatus.value.currentPositionMs
        // そもそも範囲内にいない場合は
        if (cutPositionMs !in request.timeRange) return

        // 分割したいアイテム
        val targetItem = getRenderItem(request.id)!!
        // 2つに分けるので、表示する時間も2つにする
        val displayTimeA = targetItem.displayTime.copy(
            startMs = targetItem.displayTime.startMs,
            stopMs = cutPositionMs
        )
        val displayTimeB = targetItem.displayTime.copy(
            startMs = cutPositionMs,
            stopMs = targetItem.displayTime.stopMs
        )

        // テキストと画像はそのまま2つに分ければいい
        fun processTextOrImage(): List<RenderData.RenderItem> = listOf(displayTimeA, displayTimeB)
            .mapNotNull { displayTime ->
                when (targetItem) {
                    // TODO ユニークなIDを払い出す何か。currentTimeMillis をループの中で使うと同じの来そうで怖い
                    is RenderData.CanvasItem.Image -> targetItem.copy(id = Random.nextLong(), displayTime = displayTime)
                    is RenderData.CanvasItem.Text -> targetItem.copy(id = Random.nextLong(), displayTime = displayTime)
                    is RenderData.AudioItem.Audio, is RenderData.CanvasItem.Video -> null
                }
            }

        // 音声と映像の場合は再生位置の調整が必要なので分岐しています、、、！
        fun processAudioOrVideo(): List<RenderData.RenderItem> {
            // すでに DisplayOffset 持っていれば考慮（分割したアイテムをさらに分割）
            val targetOffsetFirstMs = when (targetItem) {
                is RenderData.AudioItem.Audio -> targetItem.displayOffset
                is RenderData.CanvasItem.Video -> targetItem.displayOffset
                is RenderData.CanvasItem.Image, is RenderData.CanvasItem.Text -> null
            }?.offsetFirstMs!!
            // 動画の再生位置ではなく、アイテムの再生位置を出して、カットする地点とする
            val displayOffsetA = RenderData.DisplayOffset(targetOffsetFirstMs)
            val displayOffsetB = RenderData.DisplayOffset(offsetFirstMs = targetOffsetFirstMs + (displayTimeB.startMs - displayTimeA.startMs))
            return listOf(
                displayTimeA to displayOffsetA,
                displayTimeB to displayOffsetB
            ).mapNotNull { (displayTime, displayOffset) ->
                when (targetItem) {
                    is RenderData.CanvasItem.Image, is RenderData.CanvasItem.Text -> null
                    is RenderData.AudioItem.Audio -> targetItem.copy(id = Random.nextLong(), displayTime = displayTime, displayOffset = displayOffset)
                    is RenderData.CanvasItem.Video -> targetItem.copy(id = Random.nextLong(), displayTime = displayTime, displayOffset = displayOffset)
                }
            }
        }

        // テキストと画像。音声と映像ではやるべきことが違うので
        val cutItemList = when (targetItem) {
            is RenderData.CanvasItem.Image, is RenderData.CanvasItem.Text -> processTextOrImage()
            is RenderData.AudioItem.Audio, is RenderData.CanvasItem.Video -> processAudioOrVideo()
        }
        // 分割前のアイテムは消す
        deleteRenderItem(targetItem)
        // 追加する
        cutItemList
            .filterIsInstance<RenderData.CanvasItem>()
            .forEach { addOrUpdateCanvasRenderItem(it) }
        cutItemList
            .filterIsInstance<RenderData.AudioItem>()
            .forEach { addOrUpdateAudioRenderItem(it) }
    }

    /** [RenderData.RenderItem.id] から [RenderData.RenderItem] を返す */
    fun getRenderItem(id: Long): RenderData.RenderItem? = (_renderData.value.canvasRenderItem + _renderData.value.audioRenderItem)
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

    /** [RenderData.CanvasItem]から[RenderData.Size]をだす */
    private fun RenderData.CanvasItem.measureSize(): RenderData.Size {
        return when (this) {
            is RenderData.CanvasItem.Image -> this.size
            is RenderData.CanvasItem.Video -> this.size
            is RenderData.CanvasItem.Text -> {
                // テキストには Size が生えていないので計算する
                val paint = TextRender.createPaint(this)
                val measure = paint.measureText(this.text)
                RenderData.Size(width = measure.toInt(), height = paint.textSize.toInt())
            }
        }
    }

    /**
     * [TimeLineData.Item]を追加する際に、位置[RenderData.DisplayTime]に被らないレーン番号を取得する。
     * 素材追加時にどのレーンに入れればいいかを判定する
     *
     * @param displayTime 追加したい時間（開始、終了位置）
     * @return 入れられるレーン番号。なければ今ある最大のレーン番号 + 1 を返す。レーンがない場合は 0 を返す。
     */
    private fun calcInsertableLaneIndex(displayTime: RenderData.DisplayTime): Int =
        _timeLineData.value
            // すべてのレーンから、空いているレーンを探す
            .groupByLane()
            .firstOrNull { (_, itemList) ->
                itemList.all { laneItem ->
                    // 空きがあること
                    val hasFreeSpace = displayTime.startMs !in laneItem.timeRange && displayTime.stopMs !in laneItem.timeRange
                    // 移動先に重なる形で自分より小さいアイテムが居ないこと
                    val hasNotInclude = laneItem.startMs !in displayTime && laneItem.stopMs !in displayTime
                    hasFreeSpace && hasNotInclude
                }
            }?.first
            ?: _timeLineData.value.groupByLane().maxOfOrNull { (laneIndex, _) -> laneIndex }?.plus(1) // 見つからなければ最大のレーン番号 + 1 を返す
            ?: 0 // どうしようもない

    companion object {
        /** プロジェクト保存先、複数プロジェクトが出来るようになればこの辺も分ける */
        private const val PROJECT_FOLDER_NAME = "akaridroid_project_20240216"
    }
}