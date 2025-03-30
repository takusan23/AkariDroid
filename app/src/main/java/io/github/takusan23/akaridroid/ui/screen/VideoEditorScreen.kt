package io.github.takusan23.akaridroid.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.encoder.EncoderService
import io.github.takusan23.akaridroid.preview.HistoryManager
import io.github.takusan23.akaridroid.preview.VideoEditorPreviewPlayer
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditorBottomSheetRouteRequestData
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditorBottomSheetRouter
import io.github.takusan23.akaridroid.ui.component.AddRenderItemMenu
import io.github.takusan23.akaridroid.ui.component.ComposeSurfaceView
import io.github.takusan23.akaridroid.ui.component.PreviewContainer
import io.github.takusan23.akaridroid.ui.component.data.TimeLineMode
import io.github.takusan23.akaridroid.ui.component.data.TimeLineState
import io.github.takusan23.akaridroid.ui.component.data.rememberTimeLineState
import io.github.takusan23.akaridroid.ui.component.rememberRenderItemCreator
import io.github.takusan23.akaridroid.ui.component.timeline.DefaultTimeLine
import io.github.takusan23.akaridroid.ui.component.timeline.DefaultTimeLineHeader
import io.github.takusan23.akaridroid.ui.component.timeline.FileDragAndDropReceiveContainer
import io.github.takusan23.akaridroid.ui.component.timeline.FloatingTimeLineBar
import io.github.takusan23.akaridroid.ui.component.timeline.FloatingTimeLineItem
import io.github.takusan23.akaridroid.ui.component.timeline.FloatingTimeLineTitledItem
import io.github.takusan23.akaridroid.ui.component.timeline.MultiSelectTimeLine
import io.github.takusan23.akaridroid.ui.component.timeline.MultiSelectTimeLineHeader
import io.github.takusan23.akaridroid.ui.component.timeline.TimeLineContainer
import io.github.takusan23.akaridroid.viewmodel.VideoEditorViewModel

/**
 * 動画編集画面
 *
 * @param onNavigate 画面遷移時に呼ばれる
 * @param onBack 戻ってほしいときに呼ばれる
 */
@Composable
fun VideoEditorScreen(
    onNavigate: (NavigationPaths) -> Unit,
    onBack: () -> Unit,
    viewModel: VideoEditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current

    // タイムライン拡大率
    val timeLineMsWidthPx = remember { mutableIntStateOf(20) }
    // タイムラインのモード
    val timeLineMode = remember { mutableStateOf(TimeLineMode.Default) }

    // バックグラウンドでエンコードできるようにエンコーダーサービス
    val encoderService = remember { EncoderService.bindEncoderService(context, lifecycle) }.collectAsStateWithLifecycle(initialValue = null)
    // 動画の素材や情報が入ったデータ
    val renderData = viewModel.renderData.collectAsStateWithLifecycle()
    // プレビューのプレイヤー状態
    val previewPlayerStatus = viewModel.videoEditorPreviewPlayer.playerStatus.collectAsStateWithLifecycle()
    // ボトムシート
    val bottomSheetRouteData = viewModel.bottomSheetRouteData.collectAsStateWithLifecycle()
    // タイムライン
    val timeLineData = viewModel.timeLineData.collectAsStateWithLifecycle()
    // タッチ編集
    val touchEditorData = viewModel.touchEditorData.collectAsStateWithLifecycle()
    // 履歴機能。undo / redo
    val historyState = viewModel.historyState.collectAsStateWithLifecycle()
    // フローティングバーに出すメニュー
    val recommendFloatingBarMenuList = viewModel.floatingMenuBarMultiArmedBanditManager.pullItemList.collectAsStateWithLifecycle()
    // タイムラインの状態
    val timeLineState = rememberTimeLineState(
        timeLineData = timeLineData.value,
        msWidthPx = timeLineMsWidthPx.intValue
    )

    // ボトムシート
    if (bottomSheetRouteData.value != null) {
        VideoEditorBottomSheetRouter(
            videoEditorBottomSheetRouteRequestData = bottomSheetRouteData.value!!,
            onAudioUpdate = { viewModel.addOrUpdateRenderItem(listOf(it)) },
            onCanvasUpdate = { viewModel.addOrUpdateRenderItem(listOf(it)) },
            onDeleteItem = { viewModel.deleteTimeLineItemFromId(listOf(it.id)) },
            onAddRenderItemResult = { viewModel.resolveRenderItemCreate(it) },
            onReceiveAkaLink = { viewModel.resolveAkaLinkResult(it) },
            onRenderDataUpdate = { viewModel.updateRenderData(it) },
            onEncode = { fileName, parameters ->
                encoderService.value?.encodeAkariCore(
                    renderData = renderData.value,
                    projectName = viewModel.projectName,
                    resultFileName = fileName,
                    encoderParameters = parameters
                )
                // TODO ここで戻しているのは AudioDecodeManager を破棄させるため。エンコード側でも AudioDecodeManager を使うのでプレビュー側を破棄
                onBack()
            },
            onVideoInfoClick = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenVideoInfo(renderData.value)) },
            onEncodeClick = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenEncode(renderData.value.videoSize, renderData.value.colorSpace)) },
            onTimeLineReset = { viewModel.resetRenderItemList() },
            onSettingClick = { onNavigate(NavigationPaths.Setting) },
            onStartAkaLink = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenAkaLink) },
            onClose = { viewModel.closeBottomSheet() },
            onDefaultClick = { timeLineMode.value = TimeLineMode.Default },
            onMultiSelectClick = { timeLineMode.value = TimeLineMode.MultiSelect }
        )
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                // タイムラインはナビゲーションバーの領域まで描画してほしいので bottom 以外
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
                )
                .fillMaxSize()
        ) {
            Column {
                // タッチ編集・プレビュー
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f),
                    contentAlignment = Alignment.Center
                ) {

                    ComposeSurfaceView(
                        modifier = Modifier.aspectRatio(renderData.value.videoSize.width / renderData.value.videoSize.height.toFloat()),
                        onCreateSurface = { surfaceHolder -> viewModel.videoEditorPreviewPlayer.setPreviewSurfaceHolder(surfaceHolder) },
                        onSizeChanged = { _, _ -> /* do nothing */ },
                        onDestroySurface = { viewModel.videoEditorPreviewPlayer.setPreviewSurfaceHolder(null) }
                    )

                    PreviewContainer(
                        modifier = Modifier.matchParentSize(),
                        touchEditorData = touchEditorData.value,
                        onDragAndDropEnd = { request -> viewModel.resolveTouchEditorDragAndDropRequest(request) },
                        onSizeChangeRequest = { request -> viewModel.resolveTouchEditorSizeChangeRequest(request) },
                        playerStatus = previewPlayerStatus.value,
                        onSeek = { viewModel.videoEditorPreviewPlayer.seekTo(it) },
                        onPlayOrPause = { if (previewPlayerStatus.value.isPlaying) viewModel.videoEditorPreviewPlayer.pause() else viewModel.videoEditorPreviewPlayer.playInRepeat() },
                        onMenuClick = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenMenu) }
                    )
                }

                // タイムライン
                when (timeLineMode.value) {
                    TimeLineMode.Default -> VideoEditorDefaultTimeLine(
                        modifier = Modifier.weight(1f),
                        viewModel = viewModel,
                        bottomPadding = paddingValues.calculateBottomPadding(),
                        recommendFloatingBarMenuList = recommendFloatingBarMenuList.value,
                        timeLineState = timeLineState,
                        renderData = renderData.value,
                        previewPlayerStatus = previewPlayerStatus.value,
                        timeLineMsWidthPx = timeLineMsWidthPx.intValue,
                        historyState = historyState.value,
                        onChangeTimeLineMsWidthPx = { timeLineMsWidthPx.intValue = it }
                    )

                    TimeLineMode.MultiSelect -> VideoEditorMultiSelectTimeLine(
                        modifier = Modifier.weight(1f),
                        viewModel = viewModel,
                        bottomPadding = paddingValues.calculateBottomPadding(),
                        timeLineState = timeLineState,
                        renderData = renderData.value,
                        previewPlayerStatus = previewPlayerStatus.value,
                        timeLineMsWidthPx = timeLineMsWidthPx.intValue,
                        historyState = historyState.value,
                        onChangeTimeLineMsWidthPx = { timeLineMsWidthPx.intValue = it },
                        onExitMultiSelectTimeLine = { timeLineMode.value = TimeLineMode.Default }
                    )
                }
            }
        }
    }
}

/** 動画編集画面のタイムライン部分の UI */
@Composable
private fun VideoEditorDefaultTimeLine(
    modifier: Modifier = Modifier,
    viewModel: VideoEditorViewModel,
    bottomPadding: Dp,
    recommendFloatingBarMenuList: List<AddRenderItemMenu>,
    timeLineState: TimeLineState,
    renderData: RenderData,
    previewPlayerStatus: VideoEditorPreviewPlayer.PlayerStatus,
    timeLineMsWidthPx: Int,
    historyState: HistoryManager.HistoryState,
    onChangeTimeLineMsWidthPx: (Int) -> Unit
) {
    Box(modifier = modifier) {

        Column {
            // 戻る進むとかのヘッダー
            DefaultTimeLineHeader(
                msWidthPx = timeLineMsWidthPx,
                onModeChangeClick = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenTimeLineModeChange) },
                onZoomIn = { onChangeTimeLineMsWidthPx(timeLineMsWidthPx + 1) },
                onZoomOut = { onChangeTimeLineMsWidthPx(maxOf(timeLineMsWidthPx - 1, 1)) },
                hasUndo = historyState.hasUndo,
                hasRedo = historyState.hasRedo,
                onUndo = { viewModel.renderDataUndo() },
                onRedo = { viewModel.renderDataRedo() }
            )

            // 線
            HorizontalDivider()

            // タイムラインの共有部分
            TimeLineContainer(
                modifier = Modifier,
                timeLineMillisecondsWidthPx = timeLineState.timeLineMillisecondsWidthPx,
                verticalScroll = rememberScrollState(),
                horizontalScroll = timeLineState.horizontalScroll,
                durationMs = { renderData.durationMs },
                currentPositionMs = { previewPlayerStatus.currentPositionMs },
                onScrollContainerSizeChange = { timeLineState.timeLineParentWidth = it.width }
            ) {
                // ドラッグアンドドロップが受け入れできるように
                FileDragAndDropReceiveContainer(
                    onReceive = { clipData, dropPermission -> viewModel.resolveDragAndDrop(clipData, dropPermission) }
                ) {
                    DefaultTimeLine(
                        modifier = Modifier,
                        timeLineState = timeLineState,
                        currentPositionMs = { previewPlayerStatus.currentPositionMs },
                        onSeek = { positionMs -> viewModel.videoEditorPreviewPlayer.seekTo(positionMs) },
                        onDragAndDropRequest = { request -> viewModel.resolveTimeLineDragAndDropRequest(listOf(request)) },
                        onEdit = { timeLineItem ->
                            viewModel.getRenderItem(timeLineItem.id)?.also { renderItem ->
                                viewModel.openEditRenderItemSheet(renderItem)
                            }
                        },
                        onCut = { timeLineItem -> viewModel.resolveTimeLineCutRequest(timeLineItem) },
                        onDelete = { deleteItem -> viewModel.deleteTimeLineItemFromId(listOf(deleteItem.id)) },
                        onDuplicate = { duplicateFromItem -> viewModel.duplicateRenderItem(duplicateFromItem.id) },
                        onCopy = { copyItem -> viewModel.copyFromId(listOf(copyItem.id)) },
                        onDurationChange = { request -> viewModel.resolveTimeLineDurationChangeRequest(request) }
                    )
                }
            }
        }

        // フローティングしているバー
        // ナビゲーションバーの分も padding 入れておく
        FloatingTimeLineBar(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 20.dp)
                .padding(bottom = bottomPadding)
                .align(Alignment.BottomCenter)
        ) {

            FloatingTimeLineTitledItem(
                title = stringResource(id = R.string.video_edit_floating_add_bar_add),
                iconResId = R.drawable.ic_outlined_add_24px,
                onClick = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenAddRenderItem) }
            )

            // 使うメニュー推論
            recommendFloatingBarMenuList.forEach { recommendMenu ->
                val creator = rememberRenderItemCreator(onResult = { viewModel.resolveRenderItemCreate(it) })

                FloatingTimeLineItem(
                    iconResId = recommendMenu.iconResId,
                    onClick = { creator.create(recommendMenu) }
                )
            }
        }
    }
}

/** 複数選択モード時のタイムライン部分の UI */
@Composable
private fun VideoEditorMultiSelectTimeLine(
    modifier: Modifier = Modifier,
    viewModel: VideoEditorViewModel,
    bottomPadding: Dp,
    timeLineState: TimeLineState,
    renderData: RenderData,
    previewPlayerStatus: VideoEditorPreviewPlayer.PlayerStatus,
    timeLineMsWidthPx: Int,
    historyState: HistoryManager.HistoryState,
    onChangeTimeLineMsWidthPx: (Int) -> Unit,
    onExitMultiSelectTimeLine: () -> Unit
) {
    // 複数選択中のアイテム
    val multiSelectItemIdList = remember { mutableStateOf(emptyList<Long>()) }

    Box(modifier = modifier) {

        Column {
            // 戻る進むとかのヘッダー
            MultiSelectTimeLineHeader(
                onExitMultiSelect = onExitMultiSelectTimeLine,
                msWidthPx = timeLineMsWidthPx,
                onZoomIn = { onChangeTimeLineMsWidthPx(timeLineMsWidthPx + 1) },
                onZoomOut = { onChangeTimeLineMsWidthPx(maxOf(timeLineMsWidthPx - 1, 1)) },
                hasUndo = historyState.hasUndo,
                hasRedo = historyState.hasRedo,
                onUndo = { viewModel.renderDataUndo() },
                onRedo = { viewModel.renderDataRedo() }
            )

            // 線
            HorizontalDivider()

            // タイムラインの共有部分
            TimeLineContainer(
                modifier = Modifier,
                timeLineMillisecondsWidthPx = timeLineState.timeLineMillisecondsWidthPx,
                verticalScroll = rememberScrollState(),
                horizontalScroll = timeLineState.horizontalScroll,
                durationMs = { renderData.durationMs },
                currentPositionMs = { previewPlayerStatus.currentPositionMs },
                onScrollContainerSizeChange = { timeLineState.timeLineParentWidth = it.width }
            ) {
                // 複数選択
                MultiSelectTimeLine(
                    modifier = Modifier,
                    timeLineState = timeLineState,
                    selectedItemIdList = multiSelectItemIdList.value,
                    currentPositionMs = { previewPlayerStatus.currentPositionMs },
                    onItemSelect = { selectItem ->
                        // 無ければ追加、あれば消す
                        val id = selectItem.id
                        if (id in multiSelectItemIdList.value) {
                            multiSelectItemIdList.value -= id
                        } else {
                            multiSelectItemIdList.value += id
                        }
                    },
                    onSeek = { positionMs -> viewModel.videoEditorPreviewPlayer.seekTo(positionMs) },
                    onDragAndDropRequest = { requestList -> viewModel.resolveTimeLineDragAndDropRequest(requestList) }
                )
            }
        }

        // フローティングしているバー
        // ナビゲーションバーの分も padding 入れておく
        FloatingTimeLineBar(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 20.dp)
                .padding(bottom = bottomPadding)
                .align(Alignment.BottomCenter)
        ) {
            FloatingTimeLineTitledItem(
                title = stringResource(R.string.video_edit_floating_multi_select_copy),
                iconResId = R.drawable.content_paste_24px,
                onClick = { viewModel.copyFromId(multiSelectItemIdList.value) }
            )
            FloatingTimeLineTitledItem(
                title = stringResource(R.string.video_edit_floating_multi_delete),
                iconResId = R.drawable.ic_outline_delete_24px,
                onClick = {
                    viewModel.deleteTimeLineItemFromId(multiSelectItemIdList.value)
                    multiSelectItemIdList.value = emptyList()
                }
            )
        }
    }
}