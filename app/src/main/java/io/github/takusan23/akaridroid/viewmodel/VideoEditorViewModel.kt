package io.github.takusan23.akaridroid.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.canvasrender.itemrender.TextRender
import io.github.takusan23.akaridroid.preview.HistoryManager
import io.github.takusan23.akaridroid.preview.VideoEditorPreviewPlayer
import io.github.takusan23.akaridroid.tool.AkaLinkTool
import io.github.takusan23.akaridroid.tool.AvAnalyze
import io.github.takusan23.akaridroid.tool.ProjectFolderManager
import io.github.takusan23.akaridroid.tool.UriTool
import io.github.takusan23.akaridroid.tool.data.IoType
import io.github.takusan23.akaridroid.tool.data.toIoType
import io.github.takusan23.akaridroid.tool.data.toRenderDataFilePath
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditorBottomSheetRouteRequestData
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditorBottomSheetRouteResultData
import io.github.takusan23.akaridroid.ui.component.VideoEditorBottomBarAddItem
import io.github.takusan23.akaridroid.ui.component.data.TimeLineData
import io.github.takusan23.akaridroid.ui.component.data.TouchEditorData
import io.github.takusan23.akaridroid.ui.component.data.groupByLane
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
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
            laneCount = 10,
            itemList = emptyList()
        )
    )
    private val _touchEditorData = MutableStateFlow(
        TouchEditorData(
            videoSize = _renderData.value.videoSize,
            visibleTouchEditorItemList = emptyList()
        )
    )

    /** 履歴機能。undo / redo */
    private val historyManager = HistoryManager<RenderData>()
    private val _historyState = MutableStateFlow(
        HistoryManager.HistoryState(
            hasUndo = false,
            hasRedo = false
        )
    )

    /** 作業用フォルダ。ここにデコードした音声素材とかが来る */
    val projectFolder = ProjectFolderManager.getProjectFolder(context)

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

    /** 履歴で undo / redo できるか */
    val historyState = _historyState.asStateFlow()

    init {
        // RenderData の保存、読み取り
        viewModelScope.launch {
            // 読み取る
            // 多分今後のアップデートで互換性が崩壊するので、try-catch する
            val readRenderData = runCatching {
                ProjectFolderManager.readRenderData(context)
            }.getOrNull() ?: renderData.value
            // 前回の利用から、すでに削除されて使えない素材（画像、動画、音声）を弾く
            val availableUriList = UriTool.getTakePersistableUriList(context)

            /** ファイルが有効かどうか */
            fun RenderData.FilePath.existsFilePath(): Boolean = when (this) {
                is RenderData.FilePath.File -> File(this.filePath).exists()
                is RenderData.FilePath.Uri -> this.uriPath.toUri() in availableUriList
            }

            val existsRenderItemList = (readRenderData.canvasRenderItem + readRenderData.audioRenderItem)
                .filter {
                    when (it) {
                        is RenderData.AudioItem.Audio -> it.filePath.existsFilePath()
                        is RenderData.CanvasItem.Image -> it.filePath.existsFilePath()
                        is RenderData.CanvasItem.Video -> it.filePath.existsFilePath()
                        is RenderData.CanvasItem.Shape, is RenderData.CanvasItem.Text -> true
                    }
                }

            // Flow に流して更新
            _renderData.value = readRenderData.copy(
                canvasRenderItem = existsRenderItemList.filterIsInstance<RenderData.CanvasItem>(),
                audioRenderItem = existsRenderItemList.filterIsInstance<RenderData.AudioItem>()
            )

            // RenderData が変化したら保存する
            // クラッシュ対策
            renderData.collectLatest { renderData ->
                ProjectFolderManager.writeRenderData(context, renderData)
            }
        }

        // Uri と File の管理。
        // File の場合は、利用していないファイルを削除する
        // Uri の場合は、Uri を永続化する。
        // フォトピッカーや Storage Access Framework で取得できる Uri は一時的なもので、プロセスが4んだら使えなくなる。
        // Uri を保存したい場合は Uri 自体を永続化するメソッドを呼び出す必要がある
        // https://developer.android.com/training/data-storage/shared/photopicker#persist-media-file-access
        viewModelScope.launch {
            // 前回のファイル
            var prevRenderItemList = emptyList<RenderData.FilePath>()

            /**
             * [RenderData.FilePath]が新しく追加されたときの処理
             * [Uri]の場合は永続化を行う
             */
            suspend fun RenderData.FilePath.add() = withContext(Dispatchers.IO) {
                if (this@add is RenderData.FilePath.Uri) {
                    UriTool.takePersistableUriPermission(context, this@add.uriPath.toUri())
                }
            }

            /**
             * [RenderData.FilePath]が前回から削除されたときの処理
             * [Uri]の場合は永続化を解除する。[File]の場合は削除する
             */
            suspend fun RenderData.FilePath.remove() = withContext(Dispatchers.IO) {
                when (this@remove) {
                    is RenderData.FilePath.File -> File(this@remove.filePath).delete()
                    is RenderData.FilePath.Uri -> UriTool.revokePersistableUriPermission(context, uriPath.toUri())
                }
            }

            // RenderItem 一覧を受け取って、ファイル管理する
            renderData
                .map { it.canvasRenderItem + it.audioRenderItem }
                // FilePath 一覧を出す
                .map { renderItemList ->
                    renderItemList.mapNotNull { renderItem ->
                        when (renderItem) {
                            is RenderData.AudioItem.Audio -> renderItem.filePath
                            is RenderData.CanvasItem.Image -> renderItem.filePath
                            is RenderData.CanvasItem.Video -> renderItem.filePath
                            is RenderData.CanvasItem.Shape, is RenderData.CanvasItem.Text -> null
                        }
                    }
                }
                .distinctUntilChanged()
                .collect { latestItemList ->
                    // 前回から無くなった分
                    (prevRenderItemList - latestItemList.toSet())
                        .filter { diff -> diff in prevRenderItemList }
                        .forEach { filePath -> filePath.remove() }

                    // 前回から増えた分
                    (latestItemList - prevRenderItemList.toSet())
                        .filter { diff -> diff in latestItemList }
                        .forEach { filePath -> filePath.add() }

                    // 次に備える
                    prevRenderItemList = latestItemList
                }
        }

        // 履歴機能のため、RenderData に更新があったら追加する
        viewModelScope.launch {
            renderData.collect {
                _historyState.value = historyManager.addHistory(it)
            }
        }

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
                                label = renderItem.resolveTimeLineLabel(),
                                iconResId = when (renderItem) {
                                    is RenderData.CanvasItem.Image -> R.drawable.ic_outline_add_photo_alternate_24px
                                    is RenderData.CanvasItem.Text -> R.drawable.ic_outline_text_fields_24
                                    is RenderData.CanvasItem.Video -> R.drawable.ic_outline_video_file_24
                                    is RenderData.CanvasItem.Shape -> R.drawable.ic_outline_category_24
                                },
                                // 動画以外は表示時間変更がタイムラインできるよう（動画と音声は面倒そう）
                                isChangeDuration = when (renderItem) {
                                    is RenderData.CanvasItem.Image, is RenderData.CanvasItem.Text, is RenderData.CanvasItem.Shape -> true
                                    is RenderData.CanvasItem.Video -> false
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
                                label = audioItem.resolveTimeLineLabel(),
                                iconResId = R.drawable.ic_outline_audiotrack_24,
                                isChangeDuration = false
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
                            // 重なる順でソートする
                            canvasRenderItem
                                .sortedBy { it.layerIndex }
                                .map { canvasItem ->
                                    val measure = canvasItem.measureSize()
                                    TouchEditorData.TouchEditorItem(
                                        id = canvasItem.id,
                                        size = measure,
                                        position = if (canvasItem is RenderData.CanvasItem.Text) {
                                            // Android Canvas 都合で、文字サイス分を考慮する必要
                                            canvasItem.position.copy(y = canvasItem.position.y - canvasItem.textSize)
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
            is VideoEditorBottomSheetRouteResultData.UpdateVideoInfo -> _renderData.update { routeResultData.renderData }
            is VideoEditorBottomSheetRouteResultData.UpdateAudio -> addOrUpdateAudioRenderItem(routeResultData.audio)
            is VideoEditorBottomSheetRouteResultData.UpdateCanvasItem -> addOrUpdateCanvasRenderItem(routeResultData.renderData)
            is VideoEditorBottomSheetRouteResultData.ReceiveAkaLink -> resolveAkaLinkResult(routeResultData.akaLinkResult)
            is VideoEditorBottomSheetRouteResultData.StartEncode -> {
                // ここには来ない。
            }
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
        // 素材の挿入位置。現在の位置
        val displayTimeStartMs = videoEditorPreviewPlayer.playerStatus.value.currentPositionMs

        val openEditItem = when (addItem) {
            // テキスト
            VideoEditorBottomBarAddItem.Text -> createTextCanvasItem(displayTimeStartMs)
                .also { text -> addOrUpdateCanvasRenderItem(text) }

            // 画像
            is VideoEditorBottomBarAddItem.Image -> createImageCanvasItem(displayTimeStartMs, addItem.uri.toIoType())
                ?.also { image -> addOrUpdateCanvasRenderItem(image) }

            // 音声
            is VideoEditorBottomBarAddItem.Audio -> createAudioItem(displayTimeStartMs, addItem.uri.toIoType())
                ?.also { audio -> addOrUpdateAudioRenderItem(audio) }

            // 動画
            is VideoEditorBottomBarAddItem.Video -> createVideoItem(displayTimeStartMs, addItem.uri.toIoType())
                .onEach { renderItem ->
                    when (renderItem) {
                        is RenderData.AudioItem -> addOrUpdateAudioRenderItem(renderItem)
                        is RenderData.CanvasItem -> addOrUpdateCanvasRenderItem(renderItem)
                    }
                }
                .firstOrNull()

            // 図形
            VideoEditorBottomBarAddItem.Shape -> createShapeCanvasItem(displayTimeStartMs)
                .also { shape -> addOrUpdateCanvasRenderItem(shape) }
        }

        // 編集画面を開く
        if (openEditItem != null) {
            openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenEditor(renderItem = openEditItem))
        }
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
        // マイナスに入らないように
        val dragAndDroppedDisplayTime = renderItem.displayTime.setTime(
            setTimeMs = max(request.dragAndDroppedStartMs, 0)
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

        // 空きがない場合は return
        if (!isAcceptable) return false
        val layerIndex = request.dragAndDroppedLaneIndex

        // RenderData の Flow からタイムラインの情報が再構築されるが、
        // resolveTimeLineLabel が結構遅いので、ここで Flow 経由で更新してもぎこちない動作になってしまう。
        // そこで、TimeLine の位置だけ真っ先に更新することにする。
        _timeLineData.update { before ->
            before.copy(itemList = before.itemList.map { item ->
                if (item.id == renderItem.id) {
                    item.copy(
                        laneIndex = layerIndex,
                        startMs = dragAndDroppedDisplayTime.startMs,
                        stopMs = dragAndDroppedDisplayTime.stopMs
                    )
                } else {
                    item
                }
            })
        }

        // RenderData を更新する
        // タイムラインも RenderData の Flow から再構築される
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

            is RenderData.CanvasItem.Shape -> addOrUpdateCanvasRenderItem(
                renderItem.copy(
                    displayTime = dragAndDroppedDisplayTime,
                    layerIndex = layerIndex
                )
            )
        }

        return true
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
            is RenderData.CanvasItem.Shape -> addOrUpdateCanvasRenderItem(renderItem.copy(position = request.position))
            // テキストは特別で（Android Canvas 都合）、文字の大きさの分がないので足す
            is RenderData.CanvasItem.Text -> addOrUpdateCanvasRenderItem(renderItem.copy(position = request.position.copy(y = request.position.y + renderItem.textSize)))
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

        // テキストと画像と図形はそのまま2つに分ければいい
        fun processTextOrImageOrShape(): List<RenderData.RenderItem> = listOf(displayTimeA, displayTimeB)
            .mapNotNull { displayTime ->
                when (targetItem) {
                    // TODO ユニークなIDを払い出す何か。currentTimeMillis をループの中で使うと同じの来そうで怖い
                    is RenderData.CanvasItem.Image -> targetItem.copy(id = Random.nextLong(), displayTime = displayTime)
                    is RenderData.CanvasItem.Text -> targetItem.copy(id = Random.nextLong(), displayTime = displayTime)
                    is RenderData.CanvasItem.Shape -> targetItem.copy(id = Random.nextLong(), displayTime = displayTime)
                    is RenderData.AudioItem.Audio, is RenderData.CanvasItem.Video -> null
                }
            }

        // 音声と映像の場合は再生位置の調整が必要なので分岐しています、、、！
        fun processAudioOrVideo(): List<RenderData.RenderItem> {
            // すでに DisplayOffset 持っていれば考慮（分割したアイテムをさらに分割）
            val targetOffsetFirstMs = when (targetItem) {
                is RenderData.AudioItem.Audio -> targetItem.displayOffset
                is RenderData.CanvasItem.Video -> targetItem.displayOffset
                is RenderData.CanvasItem.Image, is RenderData.CanvasItem.Text, is RenderData.CanvasItem.Shape -> null
            }?.offsetFirstMs!!
            // 動画の再生位置ではなく、アイテムの再生位置を出して、カットする地点とする
            val displayOffsetA = RenderData.DisplayOffset(targetOffsetFirstMs)
            val displayOffsetB = RenderData.DisplayOffset(offsetFirstMs = targetOffsetFirstMs + (displayTimeB.startMs - displayTimeA.startMs))
            return listOf(
                displayTimeA to displayOffsetA,
                displayTimeB to displayOffsetB
            ).mapNotNull { (displayTime, displayOffset) ->
                when (targetItem) {
                    is RenderData.CanvasItem.Image, is RenderData.CanvasItem.Text, is RenderData.CanvasItem.Shape -> null
                    is RenderData.AudioItem.Audio -> targetItem.copy(id = Random.nextLong(), displayTime = displayTime, displayOffset = displayOffset)
                    is RenderData.CanvasItem.Video -> targetItem.copy(id = Random.nextLong(), displayTime = displayTime, displayOffset = displayOffset)
                }
            }
        }

        // テキストと画像。音声と映像ではやるべきことが違うので
        val cutItemList = when (targetItem) {
            is RenderData.CanvasItem.Image, is RenderData.CanvasItem.Text, is RenderData.CanvasItem.Shape -> processTextOrImageOrShape()
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

    /**
     * タイムラインのアイテムを削除する
     *
     * @param id 削除したいアイテムのID。[RenderData.RenderItem.id]
     */
    fun deleteTimeLineItem(id: Long) {
        val deleteItem = getRenderItem(id) ?: return
        deleteRenderItem(deleteItem)
    }

    /**
     * タイムラインから来た、長さ調整リクエストをさばく
     *
     * @param request 長さ調整したいアイテムの [TimeLineData.DurationChangeRequest]
     */
    fun resolveTimeLineDurationChangeRequest(request: TimeLineData.DurationChangeRequest) {
        // 長さ調整
        // 現状映像、音声は来ないので return
        val newDurationRenderItem = when (val renderItem = getRenderItem(request.id)!!) {
            is RenderData.CanvasItem.Image -> renderItem.copy(displayTime = renderItem.displayTime.setDuration(request.newDurationMs))
            is RenderData.CanvasItem.Text -> renderItem.copy(displayTime = renderItem.displayTime.setDuration(request.newDurationMs))
            is RenderData.CanvasItem.Shape -> renderItem.copy(displayTime = renderItem.displayTime.setDuration(request.newDurationMs))
            is RenderData.AudioItem.Audio, is RenderData.CanvasItem.Video -> return
        }
        // 上記の通り来ないので...
        addOrUpdateCanvasRenderItem(newDurationRenderItem)
    }

    /**
     * プレビューのタッチ編集から来たサイズ変更リクエストをさばく
     *
     * @param request サイズ変更死体アイテムの [TouchEditorData.SizeChangeRequest]
     */
    fun resolveTouchEditorSizeChangeRequest(request: TouchEditorData.SizeChangeRequest) {
        // サイズ変更したいアイテム
        val newSizeRenderItem = when (val renderItem = getRenderItem(request.id)!!) {
            is RenderData.AudioItem.Audio -> return // 音声は来ない
            is RenderData.CanvasItem.Image -> renderItem.copy(size = request.size)
            is RenderData.CanvasItem.Text -> renderItem.copy(textSize = TextRender.analyzeTextSize(renderItem, request.size.height))
            is RenderData.CanvasItem.Video -> renderItem.copy(size = request.size)
            is RenderData.CanvasItem.Shape -> renderItem.copy(size = request.size)
        }
        // 上記の通り来ないので...
        addOrUpdateCanvasRenderItem(newSizeRenderItem)
    }

    /** [RenderData.RenderItem.id] から [RenderData.RenderItem] を返す */
    fun getRenderItem(id: Long): RenderData.RenderItem? = (_renderData.value.canvasRenderItem + _renderData.value.audioRenderItem)
        .firstOrNull { it.id == id }

    /** タイムラインの素材を全て破棄する。 */
    fun resetRenderItem() {
        _renderData.value = renderData.value.copy(
            canvasRenderItem = emptyList(),
            audioRenderItem = emptyList()
        )
    }

    /** 履歴を使って、一個前に戻す */
    fun renderDataUndo() {
        // 戻す
        val undo = historyManager.undo()
        _renderData.value = undo.data
        _historyState.value = undo.state
    }

    /** 履歴を使って、戻す操作を取り消す */
    fun renderDataRedo() {
        val redo = historyManager.redo()
        _renderData.value = redo.data
        _historyState.value = redo.state
    }

    /** [AkaLinkTool.AkaLinkResult]を捌いてタイムラインに追加する */
    private fun resolveAkaLinkResult(akaLinkResult: AkaLinkTool.AkaLinkResult) = viewModelScope.launch {
        val filePath = akaLinkResult.filePath
        val file = File(filePath)

        // 素材の挿入位置。現在の位置
        val displayTimeStartMs = videoEditorPreviewPlayer.playerStatus.value.currentPositionMs

        when (akaLinkResult) {
            is AkaLinkTool.AkaLinkResult.Image -> createImageCanvasItem(displayTimeStartMs, file.toIoType())
                ?.also { image -> addOrUpdateCanvasRenderItem(image) }

            is AkaLinkTool.AkaLinkResult.Audio -> createAudioItem(displayTimeStartMs, file.toIoType())
                ?.also { audio -> addOrUpdateAudioRenderItem(audio) }

            is AkaLinkTool.AkaLinkResult.Video -> createVideoItem(displayTimeStartMs, file.toIoType())
                .forEach { renderItem ->
                    when (renderItem) {
                        is RenderData.AudioItem -> addOrUpdateAudioRenderItem(renderItem)
                        is RenderData.CanvasItem -> addOrUpdateCanvasRenderItem(renderItem)
                    }
                }
        }
    }

    /**
     * [RenderData.CanvasItem]を追加する。[RenderData.RenderItem.id]が同じ場合は更新される。
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
     * [RenderData.AudioItem]を追加する。[RenderData.RenderItem.id]が同じ場合は更新される。
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
            is RenderData.CanvasItem.Shape -> this.size
            is RenderData.CanvasItem.Text -> TextRender.analyzeDrawSize(this) // テキストには Size が生えていないので計算する
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

    /**
     * [RenderData.CanvasItem.Text]を作成する
     *
     * @param displayTimeStartMs 開始位置
     * @return [RenderData.CanvasItem.Text]
     */
    private fun createTextCanvasItem(displayTimeStartMs: Long): RenderData.CanvasItem.Text {
        val displayTime = RenderData.DisplayTime(displayTimeStartMs, displayTimeStartMs + 10_000)

        return RenderData.CanvasItem.Text(
            text = "",
            displayTime = displayTime,
            position = renderData.value.centerPosition(),
            layerIndex = calcInsertableLaneIndex(displayTime)
        )
    }

    /**
     * [RenderData.CanvasItem.Image]を作成する。エラーになったら null
     *
     * @param displayTimeStartMs 開始位置
     * @param ioType Uri か File
     * @return [RenderData.CanvasItem.Image]
     */
    private suspend fun createImageCanvasItem(displayTimeStartMs: Long, ioType: IoType): RenderData.CanvasItem.Image? {
        val size = AvAnalyze.analyzeImage(context, ioType)?.size ?: return null
        val displayTime = RenderData.DisplayTime(displayTimeStartMs, displayTimeStartMs + 10_000)

        return RenderData.CanvasItem.Image(
            filePath = ioType.toRenderDataFilePath(),
            displayTime = displayTime,
            position = renderData.value.centerPosition(),
            size = RenderData.Size(size.width, size.height),
            layerIndex = calcInsertableLaneIndex(displayTime)
        )
    }

    /**
     * [RenderData.AudioItem.Audio]を作成する。エラーになったら null
     *
     * @param displayTimeStartMs 開始位置
     * @param ioType Uri か File
     * @return [RenderData.AudioItem.Audio]
     */
    private suspend fun createAudioItem(displayTimeStartMs: Long, ioType: IoType): RenderData.AudioItem.Audio? {
        val durationMs = AvAnalyze.analyzeAudio(context, ioType)?.durationMs ?: return null
        val displayTime = RenderData.DisplayTime(displayTimeStartMs, displayTimeStartMs + durationMs)

        return RenderData.AudioItem.Audio(
            filePath = ioType.toRenderDataFilePath(),
            displayTime = displayTime,
            layerIndex = calcInsertableLaneIndex(displayTime)
        )
    }

    /**
     * [RenderData.CanvasItem.Video]、[RenderData.AudioItem.Audio]を作成する。エラーになったら空の配列。
     *
     * @param displayTimeStartMs 開始位置
     * @param ioType Uri か File
     * @return 動画トラックと音声トラックが入った配列
     */
    private suspend fun createVideoItem(displayTimeStartMs: Long, ioType: IoType): List<RenderData.RenderItem> {
        val analyzeVideo = AvAnalyze.analyzeVideo(context, ioType) ?: return emptyList()
        val durationMs = analyzeVideo.durationMs
        val displayTime = RenderData.DisplayTime(displayTimeStartMs, displayTimeStartMs + durationMs)

        val resultList = listOfNotNull(
            // 映像トラックを追加
            RenderData.CanvasItem.Video(
                filePath = ioType.toRenderDataFilePath(),
                displayTime = displayTime,
                position = renderData.value.centerPosition(),
                size = RenderData.Size(analyzeVideo.size.width, analyzeVideo.size.height),
                layerIndex = calcInsertableLaneIndex(displayTime)
            ),

            // 音声トラックもあれば追加
            if (analyzeVideo.hasAudioTrack) {
                createAudioItem(displayTimeStartMs, ioType)
            } else {
                null
            }
        )
        return resultList
    }

    /**
     * [RenderData.CanvasItem.Shape]を作成する
     *
     * @param displayTimeStartMs 開始位置
     * @return [RenderData.CanvasItem.Shape]
     */
    private fun createShapeCanvasItem(displayTimeStartMs: Long): RenderData.CanvasItem.Shape {
        val displayTime = RenderData.DisplayTime(displayTimeStartMs, displayTimeStartMs + 10_000)

        return RenderData.CanvasItem.Shape(
            displayTime = displayTime,
            position = renderData.value.centerPosition(),
            layerIndex = calcInsertableLaneIndex(displayTime),
            color = "#ffffff",
            size = RenderData.Size(300, 300),
            shapeType = RenderData.CanvasItem.Shape.ShapeType.Rect
        )
    }

    /** キャンバスの真ん中の座標を出す */
    private fun RenderData.centerPosition(): RenderData.Position = RenderData.Position(
        x = (this.videoSize.width / 2).toFloat(),
        y = (this.videoSize.height / 2).toFloat()
    )

    /** [RenderData.RenderItem]からタイムラインの表示で使う名前を取り出す */
    private suspend fun RenderData.RenderItem.resolveTimeLineLabel(): String {

        // FilePath or Uri で名前を取り出す
        suspend fun RenderData.FilePath.name(): String = when (this) {
            is RenderData.FilePath.File -> File(this.filePath).name
            is RenderData.FilePath.Uri -> UriTool.getFileName(context, this.uriPath.toUri())!!
        }

        return when (this) {
            is RenderData.AudioItem.Audio -> this.filePath.name()
            is RenderData.CanvasItem.Image -> this.filePath.name()
            is RenderData.CanvasItem.Shape -> "図形"
            is RenderData.CanvasItem.Text -> this.text
            is RenderData.CanvasItem.Video -> this.filePath.name()
        }
    }

    /** [RenderData.RenderItem]から、[RenderData.FilePath.Uri]が利用されている場合は[Uri]を返す */
    private fun RenderData.RenderItem.getUriOrNull(): Uri? {
        val filePath = when (this) {
            is RenderData.AudioItem.Audio -> this.filePath
            is RenderData.CanvasItem.Image -> this.filePath
            is RenderData.CanvasItem.Video -> this.filePath
            is RenderData.CanvasItem.Shape, is RenderData.CanvasItem.Text -> null
        }
        return (filePath as? RenderData.FilePath.Uri)?.uriPath?.toUri()
    }
}