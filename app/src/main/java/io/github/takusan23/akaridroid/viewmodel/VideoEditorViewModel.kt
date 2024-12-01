package io.github.takusan23.akaridroid.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.DragAndDropPermissionsCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.takusan23.akaricore.video.GpuShaderImageProcessor
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.canvasrender.itemrender.TextRenderer
import io.github.takusan23.akaridroid.preview.HistoryManager
import io.github.takusan23.akaridroid.preview.VideoEditorPreviewPlayer
import io.github.takusan23.akaridroid.tool.AkaLinkTool
import io.github.takusan23.akaridroid.tool.AvAnalyze
import io.github.takusan23.akaridroid.tool.MediaStoreTool
import io.github.takusan23.akaridroid.tool.MultiArmedBanditManager
import io.github.takusan23.akaridroid.tool.ProjectFolderManager
import io.github.takusan23.akaridroid.tool.UriTool
import io.github.takusan23.akaridroid.tool.data.IoType
import io.github.takusan23.akaridroid.tool.data.toIoType
import io.github.takusan23.akaridroid.tool.data.toRenderDataFilePath
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditorBottomSheetRouteRequestData
import io.github.takusan23.akaridroid.ui.component.AddRenderItemMenu
import io.github.takusan23.akaridroid.ui.component.AddRenderItemMenuResult
import io.github.takusan23.akaridroid.ui.component.data.TimeLineData
import io.github.takusan23.akaridroid.ui.component.data.TouchEditorData
import io.github.takusan23.akaridroid.ui.component.data.groupByLane
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

/**
 * [io.github.takusan23.akaridroid.ui.screen.VideoEditorScreen]用の ViewModel
 *
 * @param savedStateHandle プロジェクト名を Navigation で渡してもらうので、SavedStateHandle 経由で受け取る
 */
class VideoEditorViewModel(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val context: Context
        get() = application.applicationContext

    private val _renderData = MutableStateFlow(RenderData())
    private val _bottomSheetRouteData = MutableStateFlow<VideoEditorBottomSheetRouteRequestData?>(null)
    private val _timeLineData = MutableStateFlow(
        TimeLineData(
            durationMs = _renderData.value.durationMs,
            laneCount = 20, // TODO FloatingMenuBar のせいで見えないから...なんとかしたい
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

    /** プロジェクト名 */
    val projectName: String = savedStateHandle["projectName"]!!

    /** 作業用フォルダ。ここにデコードした音声素材とかが来る */
    private val projectFolder = ProjectFolderManager.getProjectFolder(context, projectName)

    /** プレビュー用プレイヤー */
    val videoEditorPreviewPlayer = VideoEditorPreviewPlayer(
        context = context,
        projectFolder = projectFolder
    )

    /** フローティングバーに出すボタンを決定するやつ */
    val floatingMenuBarMultiArmedBanditManager = MultiArmedBanditManager(
        epsilon = 0.9f,
        banditMachineList = AddRenderItemMenu.entries,
        pullSize = 3,
        initList = listOf(AddRenderItemMenu.Text, AddRenderItemMenu.Image, AddRenderItemMenu.Video)
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
                ProjectFolderManager.readRenderData(context, projectName)
            }.getOrNull() ?: renderData.value

            /** ファイルが有効かどうか。もし存在しない場合は false。
             * Uri の場合は releasePersistableUriPermission する
             */
            suspend fun RenderData.FilePath.existsFilePath(): Boolean = when (this) {
                is RenderData.FilePath.File -> File(this.filePath).exists()
                is RenderData.FilePath.Uri -> {
                    val exists = UriTool.existsContentUri(context, this.uriPath.toUri())
                    if (!exists) {
                        // 存在しない Uri は releasePersistableUriPermission する
                        UriTool.revokePersistableUriPermission(context, this.uriPath.toUri())
                    }
                    exists
                }
            }

            // 前回の利用から、すでに削除されて使えない素材（画像、動画、音声）を弾く
            // 多分 MediaStore に問いただすしか無い
            val existsRenderItemList = (readRenderData.canvasRenderItem + readRenderData.audioRenderItem)
                .filter {
                    when (it) {
                        is RenderData.AudioItem.Audio -> it.filePath.existsFilePath()
                        is RenderData.CanvasItem.Image -> it.filePath.existsFilePath()
                        is RenderData.CanvasItem.Video -> it.filePath.existsFilePath()
                        is RenderData.CanvasItem.Shape, is RenderData.CanvasItem.Text, is RenderData.CanvasItem.Shader, is RenderData.CanvasItem.SwitchAnimation, is RenderData.CanvasItem.Effect -> true
                    }
                }

            // Flow に流して更新
            _renderData.value = readRenderData.copy(
                canvasRenderItem = existsRenderItemList.filterIsInstance<RenderData.CanvasItem>(),
                audioRenderItem = existsRenderItemList.filterIsInstance<RenderData.AudioItem>()
            )

            // RenderData が復元できた後に実行することを担保したい場合、このあとに書く。

            // RenderData が変化したら保存する
            // クラッシュ対策
            launch {
                renderData.collectLatest { renderData ->
                    ProjectFolderManager.writeRenderData(context, renderData, projectName)
                }
            }

            // 履歴機能のため、RenderData に更新があったら追加する
            launch {
                renderData.collect {
                    _historyState.value = historyManager.addHistory(it)
                }
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

            // File の削除、Uri の永続化解除を、タイムラインから消えた時点でやってしまうと、元に戻す機能が動かなくなってしまうので、
            // ViewModel が生きている間はしないように配列に入れておくだけにする
            val deleteFilePathList = arrayListOf<RenderData.FilePath>()
            addCloseable {
                deleteFilePathList.forEach { filePath ->
                    when (filePath) {
                        is RenderData.FilePath.File -> File(filePath.filePath).delete()
                        is RenderData.FilePath.Uri -> UriTool.revokePersistableUriPermission(context, filePath.uriPath.toUri())
                    }
                }
            }

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
            fun RenderData.FilePath.remove() {
                deleteFilePathList += this
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
                            is RenderData.CanvasItem.Shape, is RenderData.CanvasItem.Text, is RenderData.CanvasItem.Shader, is RenderData.CanvasItem.SwitchAnimation, is RenderData.CanvasItem.Effect -> null
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

        // 動画の情報が更新されたら
        viewModelScope.launch {
            // Pair に詰めて distinct で変わったときだけ
            _renderData
                .map { Triple(it.videoSize, it.durationMs, it.isEnableTenBitHdr) }
                .distinctUntilChanged()
                .collect { (videoSize, durationMs, isEnableTenBitHdr) ->
                    videoEditorPreviewPlayer.setVideoInfo(videoSize.width, videoSize.height, durationMs, isEnableTenBitHdr)
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
                .collectLatest { renderItem ->
                    videoEditorPreviewPlayer.setCanvasRenderItem(renderItem)
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
                                    is RenderData.CanvasItem.Shader -> R.drawable.android_akari_droid_shader_icon
                                    is RenderData.CanvasItem.SwitchAnimation -> R.drawable.transition_fade_24px
                                    is RenderData.CanvasItem.Effect -> R.drawable.imagesearch_roller_24px
                                },
                                // 動画以外は表示時間変更がタイムラインできるよう（動画と音声は面倒そう）
                                isChangeDuration = when (renderItem) {
                                    is RenderData.CanvasItem.Image, is RenderData.CanvasItem.Text, is RenderData.CanvasItem.Shape, is RenderData.CanvasItem.Shader, is RenderData.CanvasItem.SwitchAnimation, is RenderData.CanvasItem.Effect -> true
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

        // プロジェクト作成直後は動画情報編集ボトムシートを出す
        if (savedStateHandle.get<String>("openVideoInfo").toBoolean()) {
            openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenVideoInfo(renderData.value))
        }
    }

    override fun onCleared() {
        super.onCleared()
        videoEditorPreviewPlayer.destroy()
    }

    /** 素材追加リクエストの中でも [AddRenderItemMenuResult.Addable] をさばく。 */
    fun resolveAddRenderItem(addRenderItemMenuResult: AddRenderItemMenuResult.Addable) {
        viewModelScope.launch {
            // 素材の挿入位置。現在の位置
            val displayTimeStartMs = videoEditorPreviewPlayer.playerStatus.value.currentPositionMs

            // 編集画面を開きたい RenderItem を返す
            val openEditItem = when (addRenderItemMenuResult) {
                AddRenderItemMenuResult.Text -> createTextCanvasItem(displayTimeStartMs)
                    .also { text -> addOrUpdateCanvasRenderItem(text) }

                is AddRenderItemMenuResult.Image -> createImageCanvasItem(displayTimeStartMs, addRenderItemMenuResult.uri.toIoType())
                    ?.also { image -> addOrUpdateCanvasRenderItem(image) }

                is AddRenderItemMenuResult.Video -> createVideoItem(displayTimeStartMs, addRenderItemMenuResult.uri.toIoType())
                    .onEach { renderItem ->
                        when (renderItem) {
                            is RenderData.AudioItem -> addOrUpdateAudioRenderItem(renderItem)
                            is RenderData.CanvasItem -> addOrUpdateCanvasRenderItem(renderItem)
                        }
                    }
                    .firstOrNull()

                is AddRenderItemMenuResult.Audio -> createAudioItem(displayTimeStartMs, addRenderItemMenuResult.uri.toIoType())
                    ?.also { audio -> addOrUpdateAudioRenderItem(audio) }

                AddRenderItemMenuResult.Shape -> createShapeCanvasItem(displayTimeStartMs)
                    .also { shape -> addOrUpdateCanvasRenderItem(shape) }

                AddRenderItemMenuResult.Shader -> createShaderCanvasItem(displayTimeStartMs)
                    .also { shader -> addOrUpdateCanvasRenderItem(shader) }

                AddRenderItemMenuResult.SwitchAnimation -> createSwitchAnimationCanvasItem(displayTimeStartMs)
                    .also { shader -> addOrUpdateCanvasRenderItem(shader) }

                AddRenderItemMenuResult.Effect -> createEffectCanvasItem(displayTimeStartMs)
                    .also { shader -> addOrUpdateCanvasRenderItem(shader) }
            }

            // 編集画面を開く
            if (openEditItem != null) {
                openEditRenderItemSheet(openEditItem)
            }
        }
    }

    /** あかりんくの結果をさばく */
    fun resolveAkaLinkResult(result: AkaLinkTool.AkaLinkResult) {
        viewModelScope.launch {
            // 素材の挿入位置。現在の位置
            val displayTimeStartMs = videoEditorPreviewPlayer.playerStatus.value.currentPositionMs
            val file = File(result.filePath)

            // あかりんくの結果から RenderItem を作る
            val openEditItem = when (result) {
                is AkaLinkTool.AkaLinkResult.Image -> createImageCanvasItem(displayTimeStartMs, file.toIoType())
                    ?.also { image -> addOrUpdateCanvasRenderItem(image) }

                is AkaLinkTool.AkaLinkResult.Audio -> createAudioItem(displayTimeStartMs, file.toIoType())
                    ?.also { audio -> addOrUpdateAudioRenderItem(audio) }

                is AkaLinkTool.AkaLinkResult.Video -> createVideoItem(displayTimeStartMs, file.toIoType())
                    .onEach { renderItem ->
                        when (renderItem) {
                            is RenderData.AudioItem -> addOrUpdateAudioRenderItem(renderItem)
                            is RenderData.CanvasItem -> addOrUpdateCanvasRenderItem(renderItem)
                        }
                    }
                    .firstOrNull()
            }

            // 編集画面を開く
            if (openEditItem != null) {
                openEditRenderItemSheet(openEditItem)
            }
        }
    }

    /** タイムラインへ投げられた、ファイルのドラッグアンドドロップをさばく */
    fun resolveDragAndDropReceiveUri(mimeType: String, uri: Uri, dropPermission: DragAndDropPermissionsCompat) {
        // TODO takePersistableUriPermission は、PhotoPicker や、StorageAccessFramework 用なので、それ以外の Uri の永続化には使えない
        // TODO ので、悲しいけどアプリ固有のフォルダへコピーする
        // TODO やっぱり、アプリ固有にコピーすると、スマホの容量が2倍必要になるから考え直すわ。
        Toast.makeText(context, "将来的に実装", Toast.LENGTH_SHORT).show()
        dropPermission.release()
        return

        viewModelScope.launch {
            val localFolder = projectFolder.resolve("drag_and_drop_files").apply { mkdir() }
            val fileName = MediaStoreTool.getFileName(context, uri) ?: "${System.currentTimeMillis()}"
            val copyToFile = localFolder.resolve(fileName)
            MediaStoreTool.fileCopy(context, uri, copyToFile)

            // 素材の挿入位置。現在の位置
            val displayTimeStartMs = videoEditorPreviewPlayer.playerStatus.value.currentPositionMs

            // あかりんくの結果から RenderItem を作る
            val openEditItem = when {
                mimeType.startsWith("image/") -> createImageCanvasItem(displayTimeStartMs, copyToFile.toIoType())
                    ?.also { image -> addOrUpdateCanvasRenderItem(image) }

                mimeType.startsWith("audio/") -> createAudioItem(displayTimeStartMs, copyToFile.toIoType())
                    ?.also { audio -> addOrUpdateAudioRenderItem(audio) }

                mimeType.startsWith("video/") -> createVideoItem(displayTimeStartMs, copyToFile.toIoType())
                    .onEach { renderItem ->
                        when (renderItem) {
                            is RenderData.AudioItem -> addOrUpdateAudioRenderItem(renderItem)
                            is RenderData.CanvasItem -> addOrUpdateCanvasRenderItem(renderItem)
                        }
                    }
                    .firstOrNull()

                else -> return@launch
            }

            // 編集画面を開く
            if (openEditItem != null) {
                openEditRenderItemSheet(openEditItem)
            }

            // 終わったら
            dropPermission.release()
        }
    }

    /** ボトムシートを表示させる */
    fun openBottomSheet(bottomSheetRouteRequestData: VideoEditorBottomSheetRouteRequestData) {
        // 今表示中の場合はアニメーションのため若干遅らせる
        if (_bottomSheetRouteData.value != null) {
            viewModelScope.launch {
                _bottomSheetRouteData.value = null
                delay(100)
                _bottomSheetRouteData.value = bottomSheetRouteRequestData
            }
        } else {
            _bottomSheetRouteData.value = bottomSheetRouteRequestData
        }
    }

    /** タイムラインの素材を編集するボトムシートを表示させる */
    fun openEditRenderItemSheet(renderItem: RenderData.RenderItem) {
        // RenderItem と追加で値を渡したかったのでデータクラスに包む
        val editRenderItem = when (renderItem) {
            is RenderData.AudioItem.Audio -> VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Audio(renderItem)
            is RenderData.CanvasItem.Effect -> VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Effect(renderItem)
            is RenderData.CanvasItem.Image -> VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Image(renderItem)
            is RenderData.CanvasItem.Shader -> VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Shader(renderItem)
            is RenderData.CanvasItem.Shape -> VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Shape(renderItem)
            is RenderData.CanvasItem.SwitchAnimation -> VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.SwitchAnimation(renderItem)
            is RenderData.CanvasItem.Text -> VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Text(renderItem)
            is RenderData.CanvasItem.Video -> VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Video(
                renderItem,
                previewPositionMs = videoEditorPreviewPlayer.playerStatus.value.currentPositionMs,
                isProjectHdr = renderData.value.isEnableTenBitHdr
            )
        }
        openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenEditor(editRenderItem))
    }

    /** ボトムシートを閉じる */
    fun closeBottomSheet() {
        _bottomSheetRouteData.value = null
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
        val dragAndDroppedDisplayTime = renderItem.displayTime.copy(
            startMs = max(request.dragAndDroppedStartMs, 0)
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

            is RenderData.CanvasItem.Shader -> addOrUpdateCanvasRenderItem(
                renderItem.copy(
                    displayTime = dragAndDroppedDisplayTime,
                    layerIndex = layerIndex
                )
            )

            is RenderData.CanvasItem.SwitchAnimation -> addOrUpdateCanvasRenderItem(
                renderItem.copy(
                    displayTime = dragAndDroppedDisplayTime,
                    layerIndex = layerIndex
                )
            )

            is RenderData.CanvasItem.Effect -> addOrUpdateCanvasRenderItem(
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
            is RenderData.CanvasItem.Shader -> addOrUpdateCanvasRenderItem(renderItem.copy(position = request.position))
            is RenderData.CanvasItem.SwitchAnimation -> addOrUpdateCanvasRenderItem(renderItem.copy(position = request.position))
            is RenderData.CanvasItem.Effect -> addOrUpdateCanvasRenderItem(renderItem.copy(position = request.position))
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
        // 分割したいアイテム
        val targetItem = getRenderItem(request.id)!!
        // そもそも範囲内にいない場合は
        if (cutPositionMs !in targetItem.displayTime) return

        // 2つに分けるので、表示する時間も2つにする
        val (displayTimeA, displayTimeB) = targetItem.displayTime.splitTime(cutPositionMs)

        // テキストと画像と図形はそのまま2つに分ければいい
        fun processTextOrImageOrShapeOrShader(): List<RenderData.RenderItem> = listOf(displayTimeA, displayTimeB)
            .mapNotNull { displayTime ->
                when (targetItem) {
                    // TODO ユニークなIDを払い出す何か。currentTimeMillis をループの中で使うと同じの来そうで怖い
                    is RenderData.CanvasItem.Image -> targetItem.copy(id = Random.nextLong(), displayTime = displayTime)
                    is RenderData.CanvasItem.Text -> targetItem.copy(id = Random.nextLong(), displayTime = displayTime)
                    is RenderData.CanvasItem.Shape -> targetItem.copy(id = Random.nextLong(), displayTime = displayTime)
                    is RenderData.CanvasItem.Shader -> targetItem.copy(id = Random.nextLong(), displayTime = displayTime)
                    is RenderData.CanvasItem.SwitchAnimation -> targetItem.copy(id = Random.nextLong(), displayTime = displayTime)
                    is RenderData.CanvasItem.Effect -> targetItem.copy(id = Random.nextLong(), displayTime = displayTime)
                    is RenderData.AudioItem.Audio, is RenderData.CanvasItem.Video -> null
                }
            }

        // 音声と映像の場合は再生位置の調整が必要なので分岐しています、、、！
        fun processAudioOrVideo(): List<RenderData.RenderItem> {
            // すでに DisplayOffset 持っていれば考慮（分割したアイテムをさらに分割）
            val haveOffsetFirstMs = when (targetItem) {
                is RenderData.AudioItem.Audio -> targetItem.displayOffset
                is RenderData.CanvasItem.Video -> targetItem.displayOffset
                is RenderData.CanvasItem.Image, is RenderData.CanvasItem.Text, is RenderData.CanvasItem.Shape, is RenderData.CanvasItem.Shader, is RenderData.CanvasItem.SwitchAnimation, is RenderData.CanvasItem.Effect -> null
            }?.offsetFirstMs!!
            // 動画の再生位置ではなく、アイテムの再生位置を出して、カットする地点とする
            // 再生速度が設定された場合でも、読み飛ばす分は倍速しないので
            val displayOffsetA = RenderData.DisplayOffset(haveOffsetFirstMs)
            val displayOffsetB = RenderData.DisplayOffset(offsetFirstMs = haveOffsetFirstMs + displayTimeA.durationMs)
            return listOf(
                displayTimeA to displayOffsetA,
                displayTimeB to displayOffsetB
            ).mapNotNull { (displayTime, displayOffset) ->
                when (targetItem) {
                    is RenderData.CanvasItem.Image, is RenderData.CanvasItem.Text, is RenderData.CanvasItem.Shape, is RenderData.CanvasItem.Shader, is RenderData.CanvasItem.SwitchAnimation, is RenderData.CanvasItem.Effect -> null
                    is RenderData.AudioItem.Audio -> targetItem.copy(id = Random.nextLong(), displayTime = displayTime, displayOffset = displayOffset)
                    is RenderData.CanvasItem.Video -> targetItem.copy(id = Random.nextLong(), displayTime = displayTime, displayOffset = displayOffset)
                }
            }
        }

        // テキストと画像。音声と映像ではやるべきことが違うので
        val cutItemList = when (targetItem) {
            is RenderData.CanvasItem.Image, is RenderData.CanvasItem.Text, is RenderData.CanvasItem.Shape, is RenderData.CanvasItem.Shader, is RenderData.CanvasItem.SwitchAnimation, is RenderData.CanvasItem.Effect -> processTextOrImageOrShapeOrShader()
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
     * タイムラインのアイテムを複製する。字幕とかコピーしたいでしょ。
     * 複製したアイテムはタイムライン上に追加されます。
     *
     * @param id コピーしたいアイテムのID
     */
    fun duplicateRenderItem(id: Long) {
        val copyFromItem = getRenderItem(id) ?: return
        val layerIndex = calcInsertableLaneIndex(copyFromItem.displayTime)

        // 複製する
        val copyItem = when (copyFromItem) {
            is RenderData.AudioItem.Audio -> copyFromItem.copy(id = System.currentTimeMillis(), layerIndex = layerIndex)
            is RenderData.CanvasItem.Image -> copyFromItem.copy(id = System.currentTimeMillis(), layerIndex = layerIndex)
            is RenderData.CanvasItem.Shape -> copyFromItem.copy(id = System.currentTimeMillis(), layerIndex = layerIndex)
            is RenderData.CanvasItem.Text -> copyFromItem.copy(id = System.currentTimeMillis(), layerIndex = layerIndex)
            is RenderData.CanvasItem.Video -> copyFromItem.copy(id = System.currentTimeMillis(), layerIndex = layerIndex)
            is RenderData.CanvasItem.Shader -> copyFromItem.copy(id = System.currentTimeMillis(), layerIndex = layerIndex)
            is RenderData.CanvasItem.SwitchAnimation -> copyFromItem.copy(id = System.currentTimeMillis(), layerIndex = layerIndex)
            is RenderData.CanvasItem.Effect -> copyFromItem.copy(id = System.currentTimeMillis(), layerIndex = layerIndex)
        }
        when (copyItem) {
            is RenderData.AudioItem -> addOrUpdateAudioRenderItem(copyItem)
            is RenderData.CanvasItem -> addOrUpdateCanvasRenderItem(copyItem)
        }
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
            is RenderData.CanvasItem.Image -> renderItem.copy(displayTime = renderItem.displayTime.copy(durationMs = request.newDurationMs))
            is RenderData.CanvasItem.Text -> renderItem.copy(displayTime = renderItem.displayTime.copy(durationMs = request.newDurationMs))
            is RenderData.CanvasItem.Shape -> renderItem.copy(displayTime = renderItem.displayTime.copy(durationMs = request.newDurationMs))
            is RenderData.CanvasItem.Shader -> renderItem.copy(displayTime = renderItem.displayTime.copy(durationMs = request.newDurationMs))
            is RenderData.CanvasItem.SwitchAnimation -> renderItem.copy(displayTime = renderItem.displayTime.copy(durationMs = request.newDurationMs))
            is RenderData.CanvasItem.Effect -> renderItem.copy(displayTime = renderItem.displayTime.copy(durationMs = request.newDurationMs))
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
            is RenderData.CanvasItem.Text -> renderItem.copy(textSize = TextRenderer.analyzeTextSize(renderItem, request.size.height))
            is RenderData.CanvasItem.Video -> renderItem.copy(size = request.size)
            is RenderData.CanvasItem.Shape -> renderItem.copy(size = request.size)
            is RenderData.CanvasItem.Shader -> renderItem.copy(size = request.size)
            is RenderData.CanvasItem.SwitchAnimation -> renderItem.copy(size = request.size)
            is RenderData.CanvasItem.Effect -> renderItem.copy(size = request.size)
        }
        // 上記の通り来ないので...
        addOrUpdateCanvasRenderItem(newSizeRenderItem)
    }

    /** [RenderData.RenderItem.id] から [RenderData.RenderItem] を返す */
    fun getRenderItem(id: Long): RenderData.RenderItem? = (_renderData.value.canvasRenderItem + _renderData.value.audioRenderItem)
        .firstOrNull { it.id == id }

    /** タイムラインの素材を全て破棄する。 */
    fun resetRenderItemList() {
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

    /** 動画時間の変更とか、動画縦横サイズの変更とかを適用する用 */
    fun updateRenderData(renderData: RenderData) {
        _renderData.value = renderData
    }

    /**
     * [RenderData.CanvasItem]を追加する。[RenderData.RenderItem.id]が同じ場合は更新される。
     * 動画とか、テキストとか
     *
     * @param canvasItem [RenderData.CanvasItem]
     */
    fun addOrUpdateCanvasRenderItem(canvasItem: RenderData.CanvasItem) {
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
    fun addOrUpdateAudioRenderItem(audioItem: RenderData.AudioItem) {
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
            is RenderData.CanvasItem.Text -> TextRenderer.analyzeDrawSize(this) // テキストには Size が生えていないので計算する
            is RenderData.CanvasItem.Shader -> this.size
            is RenderData.CanvasItem.SwitchAnimation -> this.size
            is RenderData.CanvasItem.Effect -> this.size
        }
    }

    /**
     * [TimeLineData.Item]を追加する際に、位置[RenderData.DisplayTime]に被らないレーン番号を取得する。
     * 素材追加時にどのレーンに入れればいいかを判定する
     *
     * @param displayTime 追加したい時間（開始、終了位置）
     * @param ignoreLane 無視したいレーン番号。つまり空いていてもこのレーン番号に入れない。
     * @return 入れられるレーン番号。なければ今ある最大のレーン番号 + 1 を返す。レーンがない場合は 0 を返す。
     */
    private fun calcInsertableLaneIndex(
        displayTime: RenderData.DisplayTime,
        ignoreLane: List<Int> = emptyList()
    ): Int = _timeLineData.value
        // Pair にする。first = レーン番号 / second = レーンに入っているアイテムの配列
        .groupByLane()
        // 無視したいレーンは消す
        .filter { (laneIndex, _) -> !ignoreLane.contains(laneIndex) }
        // すべてのレーンから、空いているレーンを探す
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
        val displayTime = RenderData.DisplayTime(
            startMs = displayTimeStartMs,
            durationMs = 10_000
        )

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
        val displayTime = RenderData.DisplayTime(
            startMs = displayTimeStartMs,
            durationMs = 10_000
        )

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
        val displayTime = RenderData.DisplayTime(
            startMs = displayTimeStartMs,
            durationMs = durationMs
        )

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
        val displayTime = RenderData.DisplayTime(
            startMs = displayTimeStartMs,
            durationMs = durationMs
        )

        // 映像トラックの追加位置
        val videoTrackLayerIndex = calcInsertableLaneIndex(displayTime)

        val resultList = listOfNotNull(
            // 映像トラックを追加
            RenderData.CanvasItem.Video(
                filePath = ioType.toRenderDataFilePath(),
                displayTime = displayTime,
                position = renderData.value.centerPosition(),
                size = RenderData.Size(analyzeVideo.size.width, analyzeVideo.size.height),
                layerIndex = videoTrackLayerIndex,
                dynamicRange = if (analyzeVideo.tenBitHdrInfoOrSdrNull != null) {
                    // TODO HDR だからといって HLG 形式とは限らない
                    RenderData.CanvasItem.Video.DynamicRange.HDR_HLG
                } else {
                    RenderData.CanvasItem.Video.DynamicRange.SDR
                }
            ),

            // 音声トラックもあれば追加
            // 映像トラックとタイムライン上で重ならないように
            if (analyzeVideo.hasAudioTrack) {
                createAudioItem(displayTimeStartMs, ioType)
                    ?.copy(layerIndex = calcInsertableLaneIndex(displayTime, listOf(videoTrackLayerIndex)))
            } else {
                null
            }
        )
        return resultList
    }

    /**
     * [RenderData.CanvasItem.SwitchAnimation]を作成する
     *
     * @param displayTimeStartMs 開始位置
     * @return [RenderData.CanvasItem.SwitchAnimation]
     */
    private fun createSwitchAnimationCanvasItem(displayTimeStartMs: Long): RenderData.CanvasItem.SwitchAnimation {
        val displayTime = RenderData.DisplayTime(
            startMs = displayTimeStartMs,
            durationMs = 1_000
        )

        return RenderData.CanvasItem.SwitchAnimation(
            displayTime = displayTime,
            position = RenderData.Position(0f, 0f),
            layerIndex = calcInsertableLaneIndex(displayTime),
            size = renderData.value.videoSize,
            animationType = RenderData.CanvasItem.SwitchAnimation.SwitchAnimationType.FADE_IN_OUT
        )
    }

    /**
     * [RenderData.CanvasItem.Effect]を作成する
     *
     * @param displayTimeStartMs 開始位置
     * @return [RenderData.CanvasItem.Effect]
     */
    private fun createEffectCanvasItem(displayTimeStartMs: Long): RenderData.CanvasItem.Effect {
        val displayTime = RenderData.DisplayTime(
            startMs = displayTimeStartMs,
            durationMs = 1_000
        )

        return RenderData.CanvasItem.Effect(
            displayTime = displayTime,
            position = RenderData.Position(0f, 0f),
            layerIndex = calcInsertableLaneIndex(displayTime),
            size = renderData.value.videoSize,
            effectType = RenderData.CanvasItem.Effect.EffectType.MOSAIC
        )
    }

    /**
     * [RenderData.CanvasItem.Shader]を作成する
     *
     * @param displayTimeStartMs 開始位置
     * @return [RenderData.CanvasItem.Shader]
     */
    private fun createShaderCanvasItem(displayTimeStartMs: Long): RenderData.CanvasItem.Shader {
        val displayTime = RenderData.DisplayTime(
            startMs = displayTimeStartMs,
            durationMs = 10_000
        )

        return RenderData.CanvasItem.Shader(
            displayTime = displayTime,
            position = renderData.value.centerPosition(),
            layerIndex = calcInsertableLaneIndex(displayTime),
            size = renderData.value.videoSize, // シェーダーは多分、動画の縦横埋め尽くしたいと思うので
            name = "Fragment Shader #${Random.nextInt()}",
            fragmentShader = GpuShaderImageProcessor.FRAGMENT_SHADER_TEXTURE_RENDER
        )
    }

    /**
     * [RenderData.CanvasItem.Shape]を作成する
     *
     * @param displayTimeStartMs 開始位置
     * @return [RenderData.CanvasItem.Shape]
     */
    private fun createShapeCanvasItem(displayTimeStartMs: Long): RenderData.CanvasItem.Shape {
        val displayTime = RenderData.DisplayTime(
            startMs = displayTimeStartMs,
            durationMs = 10_000
        )

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
            is RenderData.FilePath.Uri -> UriTool.getFileName(context, this.uriPath.toUri()) ?: "null" // TODO 真面目にやる。というか期限切れ Uri はここに来ないようにする
        }

        return when (this) {
            is RenderData.AudioItem.Audio -> this.filePath.name()
            is RenderData.CanvasItem.Image -> this.filePath.name()
            is RenderData.CanvasItem.Shape -> context.getString(
                when (this.shapeType) {
                    RenderData.CanvasItem.Shape.ShapeType.Rect -> R.string.video_edit_bottomsheet_shape_rect
                    RenderData.CanvasItem.Shape.ShapeType.Circle -> R.string.video_edit_bottomsheet_shape_circle
                }
            )

            is RenderData.CanvasItem.Text -> this.text
            is RenderData.CanvasItem.Video -> this.filePath.name()
            is RenderData.CanvasItem.Shader -> this.name
            is RenderData.CanvasItem.SwitchAnimation -> context.getString(
                when (this.animationType) {
                    RenderData.CanvasItem.SwitchAnimation.SwitchAnimationType.FADE_IN_OUT -> R.string.video_edit_bottomsheet_switch_animation_type_fade_in_out
                    RenderData.CanvasItem.SwitchAnimation.SwitchAnimationType.SLIDE -> R.string.video_edit_bottomsheet_switch_animation_type_slide
                    RenderData.CanvasItem.SwitchAnimation.SwitchAnimationType.BLUR -> R.string.video_edit_bottomsheet_switch_animation_type_blur
                }
            )

            is RenderData.CanvasItem.Effect -> context.getString(
                when (this.effectType) {
                    RenderData.CanvasItem.Effect.EffectType.MOSAIC -> R.string.video_edit_bottomsheet_effect_type_mosaic
                    RenderData.CanvasItem.Effect.EffectType.MONOCHROME -> R.string.video_edit_bottomsheet_effect_type_monochrome
                    RenderData.CanvasItem.Effect.EffectType.THRESHOLD -> R.string.video_edit_bottomsheet_effect_type_threshold
                    RenderData.CanvasItem.Effect.EffectType.BLUR -> R.string.video_edit_bottomsheet_effect_type_blur
                }
            )
        }
    }

}