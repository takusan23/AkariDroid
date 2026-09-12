package io.github.takusan23.akaridroid.ui.screen

import android.content.ClipData
import android.content.res.Configuration
import android.view.SurfaceHolder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.DragAndDropPermissionsCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.encoder.EncoderParameters
import io.github.takusan23.akaridroid.encoder.EncoderService
import io.github.takusan23.akaridroid.preview.HistoryManager
import io.github.takusan23.akaridroid.preview.VideoEditorPreviewPlayer
import io.github.takusan23.akaridroid.tool.AkaLinkTool
import io.github.takusan23.akaridroid.ui.component.AddRenderItemMenu
import io.github.takusan23.akaridroid.ui.component.AddRenderItemMenuResult
import io.github.takusan23.akaridroid.ui.component.ComposeSurfaceView
import io.github.takusan23.akaridroid.ui.component.LargeScreenMenuSwitchSegmentButton
import io.github.takusan23.akaridroid.ui.component.LargeScreenMenuSwitchSegmentMode
import io.github.takusan23.akaridroid.ui.component.PreviewContainer
import io.github.takusan23.akaridroid.ui.component.data.TimeLineData
import io.github.takusan23.akaridroid.ui.component.data.TimeLineMode
import io.github.takusan23.akaridroid.ui.component.data.TimeLineState
import io.github.takusan23.akaridroid.ui.component.data.TouchEditorData
import io.github.takusan23.akaridroid.ui.component.data.rememberTimeLineState
import io.github.takusan23.akaridroid.ui.component.rememberRenderItemCreator
import io.github.takusan23.akaridroid.ui.component.timeline.DefaultTimeLine
import io.github.takusan23.akaridroid.ui.component.timeline.DefaultTimeLineHeader
import io.github.takusan23.akaridroid.ui.component.timeline.FileDragAndDropReceiveContainer
import io.github.takusan23.akaridroid.ui.component.timeline.FloatingTimeLineBar
import io.github.takusan23.akaridroid.ui.component.timeline.FloatingTimeLineItem
import io.github.takusan23.akaridroid.ui.component.timeline.FloatingTimeLineTitledItem
import io.github.takusan23.akaridroid.ui.component.timeline.LargeScreenDefaultTimeLineHeader
import io.github.takusan23.akaridroid.ui.component.timeline.MultiSelectTimeLine
import io.github.takusan23.akaridroid.ui.component.timeline.MultiSelectTimeLineHeader
import io.github.takusan23.akaridroid.ui.component.timeline.TimeLineContainer
import io.github.takusan23.akaridroid.ui.sheet.AddRenderItemSheet
import io.github.takusan23.akaridroid.ui.sheet.MenuSheet
import io.github.takusan23.akaridroid.ui.sheet.VideoEditorBottomSheetRouteRequestData
import io.github.takusan23.akaridroid.ui.sheet.VideoEditorBottomSheetRouter
import io.github.takusan23.akaridroid.ui.sheet.VideoEditorOverlaySheetRouter
import io.github.takusan23.akaridroid.ui.snackbar.VideoEditorSnackbarRouter
import io.github.takusan23.akaridroid.ui.snackbar.VideoEditorSnackbarRouterRequestData
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
    viewModel: VideoEditorViewModel
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
    // Snackbar
    val snackbarRouteData = viewModel.snackbarRouteData.collectAsStateWithLifecycle()
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

    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    when {
        // 大画面用レイアウト
        windowSizeClass.isAtLeastBreakpoint(widthDpBreakpoint = WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND, heightDpBreakpoint = WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> LargeScreenLayout(
            renderData = renderData.value,
            touchEditorData = touchEditorData.value,
            playerStatus = previewPlayerStatus.value,
            bottomSheetRouteData = bottomSheetRouteData.value,
            timeLineMode = timeLineMode.value,
            timeLineState = timeLineState,
            previewPlayerStatus = previewPlayerStatus.value,
            timeLineMsWidthPx = timeLineMsWidthPx.intValue,
            historyState = historyState.value,
            snackbarRouterRequestData = snackbarRouteData.value,
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
            onSaveVideoFrameClick = { viewModel.saveCurrentVideoFrame() },
            onTimeLineReset = { viewModel.resetRenderItemList() },
            onSettingClick = { onNavigate(NavigationPaths.Setting) },
            onStartAkaLink = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenAkaLink) },
            onCloseSheet = { viewModel.closeBottomSheet() },
            onDefaultClick = { timeLineMode.value = TimeLineMode.Default },
            onMultiSelectClick = { timeLineMode.value = TimeLineMode.MultiSelect },
            onCreateSurface = { surfaceHolder -> viewModel.videoEditorPreviewPlayer.setPreviewSurfaceHolder(surfaceHolder) },
            onSizeChanged = { _, _ -> /* do nothing */ },
            onDestroySurface = { viewModel.videoEditorPreviewPlayer.setPreviewSurfaceHolder(null) },
            onDragAndDropEnd = { request -> viewModel.resolveTouchEditorDragAndDropRequest(request) },
            onSizeChangeRequest = { request -> viewModel.resolveTouchEditorSizeChangeRequest(request) },
            onSeek = { viewModel.videoEditorPreviewPlayer.seekTo(it) },
            onPlayOrPause = { if (previewPlayerStatus.value.isPlaying) viewModel.videoEditorPreviewPlayer.pause() else viewModel.videoEditorPreviewPlayer.playInRepeat() },
            onMenuClick = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenMenu) },
            onChangeTimeLineMsWidthPx = { timeLineMsWidthPx.intValue = it },
            onExitMultiSelectTimeLine = { timeLineMode.value = TimeLineMode.Default },
            onModeChangeClick = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenTimeLineModeChange) },
            onUndo = { viewModel.renderDataUndo() },
            onRedo = { viewModel.renderDataRedo() },
            onFileReceive = { clipData, dropPermission -> viewModel.resolveDragAndDrop(clipData, dropPermission) },
            onDragAndDropRequest = { request -> viewModel.resolveTimeLineDragAndDropRequest(request) },
            onEdit = { timeLineItem ->
                viewModel.getRenderItem(timeLineItem.id)?.also { renderItem ->
                    viewModel.openEditRenderItemSheet(renderItem)
                }
            },
            onCut = { timeLineItem -> viewModel.resolveTimeLineCutRequest(timeLineItem) },
            onDelete = { deleteItem -> viewModel.deleteTimeLineItemFromId(listOf(deleteItem.id)) },
            onDuplicate = { duplicateFromItem -> viewModel.duplicateRenderItem(duplicateFromItem.id) },
            onCopy = { copyItem -> viewModel.copyFromId(listOf(copyItem.id)) },
            onDurationChange = { request -> viewModel.resolveTimeLineDurationChangeRequest(request) },
            onSnackbarDismiss = { viewModel.closeSnackbar() },
            onMultipleCopy = { viewModel.copyFromId(idList = it) },
            onMultipleDelete = { viewModel.deleteTimeLineItemFromId(idList = it) }
        )

        // 満たない場合は縦横レイアウト分岐
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE -> CompactLandscapeLayout(
            renderData = renderData.value,
            touchEditorData = touchEditorData.value,
            playerStatus = previewPlayerStatus.value,
            bottomSheetRouteData = bottomSheetRouteData.value,
            timeLineMode = timeLineMode.value,
            recommendFloatingBarMenuList = recommendFloatingBarMenuList.value,
            timeLineState = timeLineState,
            previewPlayerStatus = previewPlayerStatus.value,
            timeLineMsWidthPx = timeLineMsWidthPx.intValue,
            historyState = historyState.value,
            snackbarRouterRequestData = snackbarRouteData.value,
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
            onSaveVideoFrameClick = { viewModel.saveCurrentVideoFrame() },
            onTimeLineReset = { viewModel.resetRenderItemList() },
            onSettingClick = { onNavigate(NavigationPaths.Setting) },
            onStartAkaLink = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenAkaLink) },
            onClose = { viewModel.closeBottomSheet() },
            onDefaultClick = { timeLineMode.value = TimeLineMode.Default },
            onMultiSelectClick = { timeLineMode.value = TimeLineMode.MultiSelect },
            onCreateSurface = { surfaceHolder -> viewModel.videoEditorPreviewPlayer.setPreviewSurfaceHolder(surfaceHolder) },
            onSizeChanged = { _, _ -> /* do nothing */ },
            onDestroySurface = { viewModel.videoEditorPreviewPlayer.setPreviewSurfaceHolder(null) },
            onDragAndDropEnd = { request -> viewModel.resolveTouchEditorDragAndDropRequest(request) },
            onSizeChangeRequest = { request -> viewModel.resolveTouchEditorSizeChangeRequest(request) },
            onSeek = { viewModel.videoEditorPreviewPlayer.seekTo(it) },
            onPlayOrPause = { if (previewPlayerStatus.value.isPlaying) viewModel.videoEditorPreviewPlayer.pause() else viewModel.videoEditorPreviewPlayer.playInRepeat() },
            onMenuClick = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenMenu) },
            onChangeTimeLineMsWidthPx = { timeLineMsWidthPx.intValue = it },
            onExitMultiSelectTimeLine = { timeLineMode.value = TimeLineMode.Default },
            onModeChangeClick = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenTimeLineModeChange) },
            onUndo = { viewModel.renderDataUndo() },
            onRedo = { viewModel.renderDataRedo() },
            onFileReceive = { clipData, dropPermission -> viewModel.resolveDragAndDrop(clipData, dropPermission) },
            onDragAndDropRequest = { request -> viewModel.resolveTimeLineDragAndDropRequest(request) },
            onEdit = { timeLineItem ->
                viewModel.getRenderItem(timeLineItem.id)?.also { renderItem ->
                    viewModel.openEditRenderItemSheet(renderItem)
                }
            },
            onCut = { timeLineItem -> viewModel.resolveTimeLineCutRequest(timeLineItem) },
            onDelete = { deleteItem -> viewModel.deleteTimeLineItemFromId(listOf(deleteItem.id)) },
            onDuplicate = { duplicateFromItem -> viewModel.duplicateRenderItem(duplicateFromItem.id) },
            onCopy = { copyItem -> viewModel.copyFromId(listOf(copyItem.id)) },
            onDurationChange = { request -> viewModel.resolveTimeLineDurationChangeRequest(request) },
            onSnackbarDismiss = { viewModel.closeSnackbar() },
            onRequestAddItemBottomSheet = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenAddRenderItem) },
            onRecommendResult = { viewModel.resolveRenderItemCreate(it) },
            onMultipleCopy = { viewModel.copyFromId(idList = it) },
            onMultipleDelete = { viewModel.deleteTimeLineItemFromId(idList = it) }
        )

        else -> CompactPortraitLayout(
            renderData = renderData.value,
            touchEditorData = touchEditorData.value,
            playerStatus = previewPlayerStatus.value,
            bottomSheetRouteData = bottomSheetRouteData.value,
            timeLineMode = timeLineMode.value,
            recommendFloatingBarMenuList = recommendFloatingBarMenuList.value,
            timeLineState = timeLineState,
            previewPlayerStatus = previewPlayerStatus.value,
            timeLineMsWidthPx = timeLineMsWidthPx.intValue,
            historyState = historyState.value,
            snackbarRouterRequestData = snackbarRouteData.value,
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
            onSaveVideoFrameClick = { viewModel.saveCurrentVideoFrame() },
            onTimeLineReset = { viewModel.resetRenderItemList() },
            onSettingClick = { onNavigate(NavigationPaths.Setting) },
            onStartAkaLink = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenAkaLink) },
            onClose = { viewModel.closeBottomSheet() },
            onDefaultClick = { timeLineMode.value = TimeLineMode.Default },
            onMultiSelectClick = { timeLineMode.value = TimeLineMode.MultiSelect },
            onCreateSurface = { surfaceHolder -> viewModel.videoEditorPreviewPlayer.setPreviewSurfaceHolder(surfaceHolder) },
            onSizeChanged = { _, _ -> /* do nothing */ },
            onDestroySurface = { viewModel.videoEditorPreviewPlayer.setPreviewSurfaceHolder(null) },
            onDragAndDropEnd = { request -> viewModel.resolveTouchEditorDragAndDropRequest(request) },
            onSizeChangeRequest = { request -> viewModel.resolveTouchEditorSizeChangeRequest(request) },
            onSeek = { viewModel.videoEditorPreviewPlayer.seekTo(it) },
            onPlayOrPause = { if (previewPlayerStatus.value.isPlaying) viewModel.videoEditorPreviewPlayer.pause() else viewModel.videoEditorPreviewPlayer.playInRepeat() },
            onMenuClick = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenMenu) },
            onChangeTimeLineMsWidthPx = { timeLineMsWidthPx.intValue = it },
            onExitMultiSelectTimeLine = { timeLineMode.value = TimeLineMode.Default },
            onModeChangeClick = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenTimeLineModeChange) },
            onUndo = { viewModel.renderDataUndo() },
            onRedo = { viewModel.renderDataRedo() },
            onFileReceive = { clipData, dropPermission -> viewModel.resolveDragAndDrop(clipData, dropPermission) },
            onDragAndDropRequest = { request -> viewModel.resolveTimeLineDragAndDropRequest(request) },
            onEdit = { timeLineItem ->
                viewModel.getRenderItem(timeLineItem.id)?.also { renderItem ->
                    viewModel.openEditRenderItemSheet(renderItem)
                }
            },
            onCut = { timeLineItem -> viewModel.resolveTimeLineCutRequest(timeLineItem) },
            onDelete = { deleteItem -> viewModel.deleteTimeLineItemFromId(listOf(deleteItem.id)) },
            onDuplicate = { duplicateFromItem -> viewModel.duplicateRenderItem(duplicateFromItem.id) },
            onCopy = { copyItem -> viewModel.copyFromId(listOf(copyItem.id)) },
            onDurationChange = { request -> viewModel.resolveTimeLineDurationChangeRequest(request) },
            onSnackbarDismiss = { viewModel.closeSnackbar() },
            onRequestAddItemBottomSheet = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenAddRenderItem) },
            onRecommendResult = { viewModel.resolveRenderItemCreate(it) },
            onMultipleCopy = { viewModel.copyFromId(idList = it) },
            onMultipleDelete = { viewModel.deleteTimeLineItemFromId(idList = it) }
        )
    }
}

@Composable
private fun LargeScreenLayout(
    bottomSheetRouteData: VideoEditorBottomSheetRouteRequestData? = null,
    renderData: RenderData,
    touchEditorData: TouchEditorData,
    playerStatus: VideoEditorPreviewPlayer.PlayerStatus,
    timeLineMode: TimeLineMode,
    timeLineState: TimeLineState,
    previewPlayerStatus: VideoEditorPreviewPlayer.PlayerStatus,
    timeLineMsWidthPx: Int,
    historyState: HistoryManager.HistoryState,
    snackbarRouterRequestData: VideoEditorSnackbarRouterRequestData?,
    onAudioUpdate: (RenderData.AudioItem) -> Unit,
    onCanvasUpdate: (RenderData.CanvasItem) -> Unit,
    onDeleteItem: (RenderData.RenderItem) -> Unit,
    onAddRenderItemResult: (AddRenderItemMenuResult) -> Unit,
    onReceiveAkaLink: (AkaLinkTool.AkaLinkResult) -> Unit,
    onRenderDataUpdate: (RenderData) -> Unit,
    onEncode: (String, EncoderParameters) -> Unit,
    onVideoInfoClick: () -> Unit,
    onEncodeClick: () -> Unit,
    onSaveVideoFrameClick: () -> Unit,
    onTimeLineReset: () -> Unit,
    onSettingClick: () -> Unit,
    onStartAkaLink: () -> Unit,
    onCloseSheet: () -> Unit,
    onDefaultClick: () -> Unit,
    onMultiSelectClick: () -> Unit,
    onCreateSurface: (SurfaceHolder) -> Unit,
    onSizeChanged: (width: Int, height: Int) -> Unit,
    onDestroySurface: () -> Unit,
    onDragAndDropEnd: (TouchEditorData.PositionUpdateRequest) -> Unit,
    onSizeChangeRequest: (TouchEditorData.SizeChangeRequest) -> Unit,
    onPlayOrPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onMenuClick: () -> Unit,
    onChangeTimeLineMsWidthPx: (Int) -> Unit,
    onExitMultiSelectTimeLine: () -> Unit,
    onModeChangeClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFileReceive: (ClipData, DragAndDropPermissionsCompat) -> Unit,
    onDragAndDropRequest: (List<TimeLineData.DragAndDropRequest>) -> Unit,
    onEdit: (TimeLineData.Item) -> Unit,
    onCut: (TimeLineData.Item) -> Unit,
    onDelete: (TimeLineData.Item) -> Unit,
    onDuplicate: (TimeLineData.Item) -> Unit,
    onCopy: (TimeLineData.Item) -> Unit,
    onDurationChange: (TimeLineData.DurationChangeRequest) -> Unit,
    onSnackbarDismiss: () -> Unit,
    onMultipleDelete: (List<Long>) -> Unit,
    onMultipleCopy: (List<Long>) -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceContainer) { paddingValues ->
        Box {
            Column {
                Row(
                    modifier = Modifier
                        .padding(
                            top = paddingValues.calculateTopPadding(),
                            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
                        )
                        .weight(1f)
                ) {

                    // タッチ編集・プレビュー
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        ComposeSurfaceView(
                            modifier = Modifier.aspectRatio(renderData.videoSize.width / renderData.videoSize.height.toFloat()),
                            onCreateSurface = onCreateSurface,
                            onSizeChanged = onSizeChanged,
                            onDestroySurface = onDestroySurface
                        )
                        PreviewContainer(
                            modifier = Modifier.matchParentSize(),
                            touchEditorData = touchEditorData,
                            showMenu = false,
                            onDragAndDropEnd = onDragAndDropEnd,
                            onSizeChangeRequest = onSizeChangeRequest,
                            playerStatus = playerStatus,
                            onPlayOrPause = onPlayOrPause,
                            onSeek = onSeek,
                            onMenuClick = onMenuClick
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val selectMenu = remember { mutableStateOf(LargeScreenMenuSwitchSegmentMode.Menu) }
                        LargeScreenMenuSwitchSegmentButton(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .fillMaxWidth(0.5f),
                            current = selectMenu.value,
                            onMenuSelect = { selectMenu.value = LargeScreenMenuSwitchSegmentMode.Menu },
                            onAddRenderItem = { selectMenu.value = LargeScreenMenuSwitchSegmentMode.AddRenderItem }
                        )
                        val sheetModifier = Modifier
                            .padding(horizontal = 10.dp)
                            .verticalScroll(rememberScrollState())
                        when (selectMenu.value) {
                            LargeScreenMenuSwitchSegmentMode.Menu -> MenuSheet(
                                modifier = sheetModifier,
                                onVideoInfoClick = onVideoInfoClick,
                                onEncodeClick = onEncodeClick,
                                onSaveVideoFrameClick = onSaveVideoFrameClick,
                                onTimeLineReset = onTimeLineReset,
                                onSettingClick = onSettingClick
                            )

                            LargeScreenMenuSwitchSegmentMode.AddRenderItem -> AddRenderItemSheet(
                                modifier = sheetModifier,
                                onAddRenderItemResult = onAddRenderItemResult
                            )
                        }
                    }
                }

                // タイムライン
                when (timeLineMode) {
                    TimeLineMode.Default -> LargeScreenVideoEditorDefaultTimeLine(
                        modifier = Modifier
                            .weight(1f)
                            .systemGestureExclusion(),
                        bottomPadding = paddingValues.calculateBottomPadding(),
                        timeLineState = timeLineState,
                        renderData = renderData,
                        previewPlayerStatus = previewPlayerStatus,
                        timeLineMsWidthPx = timeLineMsWidthPx,
                        historyState = historyState,
                        snackbarRouterRequestData = snackbarRouterRequestData,
                        onChangeTimeLineMsWidthPx = onChangeTimeLineMsWidthPx,
                        onModeChangeClick = onModeChangeClick,
                        onUndo = onUndo,
                        onRedo = onRedo,
                        onFileReceive = onFileReceive,
                        onDragAndDropRequest = { onDragAndDropRequest(listOf(it)) },
                        onSeek = onSeek,
                        onEdit = onEdit,
                        onCut = onCut,
                        onDelete = onDelete,
                        onDuplicate = onDuplicate,
                        onCopy = onCopy,
                        onDurationChange = onDurationChange,
                        onSnackbarDismiss = onSnackbarDismiss
                    )

                    TimeLineMode.MultiSelect -> LargeScreenVideoEditorMultiSelectTimeLine(
                        modifier = Modifier
                            .weight(1f)
                            .systemGestureExclusion(),
                        bottomPadding = paddingValues.calculateBottomPadding(),
                        timeLineState = timeLineState,
                        renderData = renderData,
                        previewPlayerStatus = previewPlayerStatus,
                        timeLineMsWidthPx = timeLineMsWidthPx,
                        historyState = historyState,
                        onChangeTimeLineMsWidthPx = onChangeTimeLineMsWidthPx,
                        onExitMultiSelectTimeLine = onExitMultiSelectTimeLine,
                        onUndo = onUndo,
                        onRedo = onRedo,
                        onDragAndDropRequest = onDragAndDropRequest,
                        onSeek = onSeek,
                        onMultipleDelete = onMultipleDelete,
                        onMultipleCopy = onMultipleCopy
                    )
                }
            }

            VideoEditorOverlaySheetRouter(
                modifier = Modifier
                    .align(alignment = Alignment.TopEnd)
                    .padding(paddingValues),
                videoEditorBottomSheetRouteRequestData = bottomSheetRouteData,
                onAudioUpdate = onAudioUpdate,
                onCanvasUpdate = onCanvasUpdate,
                onDeleteItem = onDeleteItem,
                onAddRenderItemResult = onAddRenderItemResult,
                onReceiveAkaLink = onReceiveAkaLink,
                onRenderDataUpdate = onRenderDataUpdate,
                onEncode = onEncode,
                onVideoInfoClick = onVideoInfoClick,
                onEncodeClick = onEncodeClick,
                onSaveVideoFrameClick = onSaveVideoFrameClick,
                onTimeLineReset = onTimeLineReset,
                onSettingClick = onSettingClick,
                onStartAkaLink = onStartAkaLink,
                onClose = onCloseSheet,
                onDefaultClick = onDefaultClick,
                onMultiSelectClick = onMultiSelectClick
            )
        }
    }
}

@Composable
private fun CompactLandscapeLayout(
    bottomSheetRouteData: VideoEditorBottomSheetRouteRequestData? = null,
    renderData: RenderData,
    touchEditorData: TouchEditorData,
    playerStatus: VideoEditorPreviewPlayer.PlayerStatus,
    timeLineMode: TimeLineMode,
    recommendFloatingBarMenuList: List<AddRenderItemMenu>,
    timeLineState: TimeLineState,
    previewPlayerStatus: VideoEditorPreviewPlayer.PlayerStatus,
    timeLineMsWidthPx: Int,
    historyState: HistoryManager.HistoryState,
    snackbarRouterRequestData: VideoEditorSnackbarRouterRequestData?,
    onAudioUpdate: (RenderData.AudioItem) -> Unit,
    onCanvasUpdate: (RenderData.CanvasItem) -> Unit,
    onDeleteItem: (RenderData.RenderItem) -> Unit,
    onAddRenderItemResult: (AddRenderItemMenuResult) -> Unit,
    onReceiveAkaLink: (AkaLinkTool.AkaLinkResult) -> Unit,
    onRenderDataUpdate: (RenderData) -> Unit,
    onEncode: (String, EncoderParameters) -> Unit,
    onVideoInfoClick: () -> Unit,
    onEncodeClick: () -> Unit,
    onSaveVideoFrameClick: () -> Unit,
    onTimeLineReset: () -> Unit,
    onSettingClick: () -> Unit,
    onStartAkaLink: () -> Unit,
    onClose: () -> Unit,
    onDefaultClick: () -> Unit,
    onMultiSelectClick: () -> Unit,
    onCreateSurface: (SurfaceHolder) -> Unit,
    onSizeChanged: (width: Int, height: Int) -> Unit,
    onDestroySurface: () -> Unit,
    onDragAndDropEnd: (TouchEditorData.PositionUpdateRequest) -> Unit,
    onSizeChangeRequest: (TouchEditorData.SizeChangeRequest) -> Unit,
    onPlayOrPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onMenuClick: () -> Unit,
    onChangeTimeLineMsWidthPx: (Int) -> Unit,
    onExitMultiSelectTimeLine: () -> Unit,
    onModeChangeClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFileReceive: (ClipData, DragAndDropPermissionsCompat) -> Unit,
    onDragAndDropRequest: (List<TimeLineData.DragAndDropRequest>) -> Unit,
    onEdit: (TimeLineData.Item) -> Unit,
    onCut: (TimeLineData.Item) -> Unit,
    onDelete: (TimeLineData.Item) -> Unit,
    onDuplicate: (TimeLineData.Item) -> Unit,
    onCopy: (TimeLineData.Item) -> Unit,
    onDurationChange: (TimeLineData.DurationChangeRequest) -> Unit,
    onSnackbarDismiss: () -> Unit,
    onRequestAddItemBottomSheet: () -> Unit,
    onRecommendResult: (AddRenderItemMenuResult) -> Unit,
    onMultipleDelete: (List<Long>) -> Unit,
    onMultipleCopy: (List<Long>) -> Unit
) {
    // ボトムシート
    if (bottomSheetRouteData != null) {
        VideoEditorBottomSheetRouter(
            videoEditorBottomSheetRouteRequestData = bottomSheetRouteData,
            onAudioUpdate = onAudioUpdate,
            onCanvasUpdate = onCanvasUpdate,
            onDeleteItem = onDeleteItem,
            onAddRenderItemResult = onAddRenderItemResult,
            onReceiveAkaLink = onReceiveAkaLink,
            onRenderDataUpdate = onRenderDataUpdate,
            onEncode = onEncode,
            onVideoInfoClick = onVideoInfoClick,
            onEncodeClick = onEncodeClick,
            onSaveVideoFrameClick = onSaveVideoFrameClick,
            onTimeLineReset = onTimeLineReset,
            onSettingClick = onSettingClick,
            onStartAkaLink = onStartAkaLink,
            onClose = onClose,
            onDefaultClick = onDefaultClick,
            onMultiSelectClick = onMultiSelectClick
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceContainer) { paddingValues ->
        Row(
            modifier = Modifier
                // タイムラインはナビゲーションバーの領域まで描画してほしいので bottom 以外
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
                )
                .fillMaxSize()
        ) {
            // タッチ編集・プレビュー
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f),
                contentAlignment = Alignment.Center
            ) {

                ComposeSurfaceView(
                    modifier = Modifier.aspectRatio(renderData.videoSize.width / renderData.videoSize.height.toFloat()),
                    onCreateSurface = onCreateSurface,
                    onSizeChanged = onSizeChanged,
                    onDestroySurface = onDestroySurface
                )

                PreviewContainer(
                    modifier = Modifier.matchParentSize(),
                    touchEditorData = touchEditorData,
                    showMenu = true,
                    onDragAndDropEnd = onDragAndDropEnd,
                    onSizeChangeRequest = onSizeChangeRequest,
                    playerStatus = playerStatus,
                    onPlayOrPause = onPlayOrPause,
                    onSeek = onSeek,
                    onMenuClick = onMenuClick
                )
            }

            // タイムライン
            when (timeLineMode) {
                TimeLineMode.Default -> VideoEditorDefaultTimeLine(
                    modifier = Modifier
                        .weight(1f)
                        .systemGestureExclusion(),
                    bottomPadding = paddingValues.calculateBottomPadding(),
                    recommendFloatingBarMenuList = recommendFloatingBarMenuList,
                    timeLineState = timeLineState,
                    renderData = renderData,
                    previewPlayerStatus = previewPlayerStatus,
                    timeLineMsWidthPx = timeLineMsWidthPx,
                    historyState = historyState,
                    snackbarRouterRequestData = snackbarRouterRequestData,
                    onChangeTimeLineMsWidthPx = onChangeTimeLineMsWidthPx,
                    onModeChangeClick = onModeChangeClick,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onFileReceive = onFileReceive,
                    onDragAndDropRequest = { onDragAndDropRequest(listOf(it)) },
                    onSeek = onSeek,
                    onEdit = onEdit,
                    onCut = onCut,
                    onDelete = onDelete,
                    onDuplicate = onDuplicate,
                    onCopy = onCopy,
                    onDurationChange = onDurationChange,
                    onSnackbarDismiss = onSnackbarDismiss,
                    onRequestAddItemBottomSheet = onRequestAddItemBottomSheet,
                    onRecommendResult = onRecommendResult
                )

                TimeLineMode.MultiSelect -> VideoEditorMultiSelectTimeLine(
                    modifier = Modifier
                        .weight(1f)
                        .systemGestureExclusion(),
                    bottomPadding = paddingValues.calculateBottomPadding(),
                    timeLineState = timeLineState,
                    renderData = renderData,
                    previewPlayerStatus = previewPlayerStatus,
                    timeLineMsWidthPx = timeLineMsWidthPx,
                    historyState = historyState,
                    onChangeTimeLineMsWidthPx = onChangeTimeLineMsWidthPx,
                    onExitMultiSelectTimeLine = onExitMultiSelectTimeLine,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onDragAndDropRequest = onDragAndDropRequest,
                    onSeek = onSeek,
                    onMultipleDelete = onMultipleDelete,
                    onMultipleCopy = onMultipleCopy
                )
            }
        }
    }
}

/** スマホ縦持ちレイアウト */
@Composable
private fun CompactPortraitLayout(
    bottomSheetRouteData: VideoEditorBottomSheetRouteRequestData? = null,
    renderData: RenderData,
    touchEditorData: TouchEditorData,
    playerStatus: VideoEditorPreviewPlayer.PlayerStatus,
    timeLineMode: TimeLineMode,
    recommendFloatingBarMenuList: List<AddRenderItemMenu>,
    timeLineState: TimeLineState,
    previewPlayerStatus: VideoEditorPreviewPlayer.PlayerStatus,
    timeLineMsWidthPx: Int,
    historyState: HistoryManager.HistoryState,
    snackbarRouterRequestData: VideoEditorSnackbarRouterRequestData?,
    onAudioUpdate: (RenderData.AudioItem) -> Unit,
    onCanvasUpdate: (RenderData.CanvasItem) -> Unit,
    onDeleteItem: (RenderData.RenderItem) -> Unit,
    onAddRenderItemResult: (AddRenderItemMenuResult) -> Unit,
    onReceiveAkaLink: (AkaLinkTool.AkaLinkResult) -> Unit,
    onRenderDataUpdate: (RenderData) -> Unit,
    onEncode: (String, EncoderParameters) -> Unit,
    onVideoInfoClick: () -> Unit,
    onEncodeClick: () -> Unit,
    onSaveVideoFrameClick: () -> Unit,
    onTimeLineReset: () -> Unit,
    onSettingClick: () -> Unit,
    onStartAkaLink: () -> Unit,
    onClose: () -> Unit,
    onDefaultClick: () -> Unit,
    onMultiSelectClick: () -> Unit,
    onCreateSurface: (SurfaceHolder) -> Unit,
    onSizeChanged: (width: Int, height: Int) -> Unit,
    onDestroySurface: () -> Unit,
    onDragAndDropEnd: (TouchEditorData.PositionUpdateRequest) -> Unit,
    onSizeChangeRequest: (TouchEditorData.SizeChangeRequest) -> Unit,
    onPlayOrPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onMenuClick: () -> Unit,
    onChangeTimeLineMsWidthPx: (Int) -> Unit,
    onExitMultiSelectTimeLine: () -> Unit,
    onModeChangeClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFileReceive: (ClipData, DragAndDropPermissionsCompat) -> Unit,
    onDragAndDropRequest: (List<TimeLineData.DragAndDropRequest>) -> Unit,
    onEdit: (TimeLineData.Item) -> Unit,
    onCut: (TimeLineData.Item) -> Unit,
    onDelete: (TimeLineData.Item) -> Unit,
    onDuplicate: (TimeLineData.Item) -> Unit,
    onCopy: (TimeLineData.Item) -> Unit,
    onDurationChange: (TimeLineData.DurationChangeRequest) -> Unit,
    onSnackbarDismiss: () -> Unit,
    onRequestAddItemBottomSheet: () -> Unit,
    onRecommendResult: (AddRenderItemMenuResult) -> Unit,
    onMultipleDelete: (List<Long>) -> Unit,
    onMultipleCopy: (List<Long>) -> Unit
) {
    // ボトムシート
    if (bottomSheetRouteData != null) {
        VideoEditorBottomSheetRouter(
            videoEditorBottomSheetRouteRequestData = bottomSheetRouteData,
            onAudioUpdate = onAudioUpdate,
            onCanvasUpdate = onCanvasUpdate,
            onDeleteItem = onDeleteItem,
            onAddRenderItemResult = onAddRenderItemResult,
            onReceiveAkaLink = onReceiveAkaLink,
            onRenderDataUpdate = onRenderDataUpdate,
            onEncode = onEncode,
            onVideoInfoClick = onVideoInfoClick,
            onEncodeClick = onEncodeClick,
            onSaveVideoFrameClick = onSaveVideoFrameClick,
            onTimeLineReset = onTimeLineReset,
            onSettingClick = onSettingClick,
            onStartAkaLink = onStartAkaLink,
            onClose = onClose,
            onDefaultClick = onDefaultClick,
            onMultiSelectClick = onMultiSelectClick
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceContainer) { paddingValues ->
        Column(
            modifier = Modifier
                // タイムラインはナビゲーションバーの領域まで描画してほしいので bottom 以外
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
                )
                .fillMaxSize()
        ) {
            // タッチ編集・プレビュー
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f),
                contentAlignment = Alignment.Center
            ) {

                ComposeSurfaceView(
                    modifier = Modifier.aspectRatio(renderData.videoSize.width / renderData.videoSize.height.toFloat()),
                    onCreateSurface = onCreateSurface,
                    onSizeChanged = onSizeChanged,
                    onDestroySurface = onDestroySurface
                )

                PreviewContainer(
                    modifier = Modifier.matchParentSize(),
                    touchEditorData = touchEditorData,
                    showMenu = true,
                    onDragAndDropEnd = onDragAndDropEnd,
                    onSizeChangeRequest = onSizeChangeRequest,
                    playerStatus = playerStatus,
                    onPlayOrPause = onPlayOrPause,
                    onSeek = onSeek,
                    onMenuClick = onMenuClick
                )
            }

            // タイムライン
            when (timeLineMode) {
                TimeLineMode.Default -> VideoEditorDefaultTimeLine(
                    modifier = Modifier
                        .weight(1f)
                        .systemGestureExclusion(),
                    bottomPadding = paddingValues.calculateBottomPadding(),
                    recommendFloatingBarMenuList = recommendFloatingBarMenuList,
                    timeLineState = timeLineState,
                    renderData = renderData,
                    previewPlayerStatus = previewPlayerStatus,
                    timeLineMsWidthPx = timeLineMsWidthPx,
                    historyState = historyState,
                    snackbarRouterRequestData = snackbarRouterRequestData,
                    onChangeTimeLineMsWidthPx = onChangeTimeLineMsWidthPx,
                    onModeChangeClick = onModeChangeClick,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onFileReceive = onFileReceive,
                    onDragAndDropRequest = { onDragAndDropRequest(listOf(it)) },
                    onSeek = onSeek,
                    onEdit = onEdit,
                    onCut = onCut,
                    onDelete = onDelete,
                    onDuplicate = onDuplicate,
                    onCopy = onCopy,
                    onDurationChange = onDurationChange,
                    onSnackbarDismiss = onSnackbarDismiss,
                    onRequestAddItemBottomSheet = onRequestAddItemBottomSheet,
                    onRecommendResult = onRecommendResult
                )

                TimeLineMode.MultiSelect -> VideoEditorMultiSelectTimeLine(
                    modifier = Modifier
                        .weight(1f)
                        .systemGestureExclusion(),
                    bottomPadding = paddingValues.calculateBottomPadding(),
                    timeLineState = timeLineState,
                    renderData = renderData,
                    previewPlayerStatus = previewPlayerStatus,
                    timeLineMsWidthPx = timeLineMsWidthPx,
                    historyState = historyState,
                    onChangeTimeLineMsWidthPx = onChangeTimeLineMsWidthPx,
                    onExitMultiSelectTimeLine = onExitMultiSelectTimeLine,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onDragAndDropRequest = onDragAndDropRequest,
                    onSeek = onSeek,
                    onMultipleDelete = onMultipleDelete,
                    onMultipleCopy = onMultipleCopy
                )
            }
        }
    }
}

/** 動画編集画面のタイムライン部分の UI */
@Composable
private fun VideoEditorDefaultTimeLine(
    modifier: Modifier = Modifier,
    bottomPadding: Dp,
    recommendFloatingBarMenuList: List<AddRenderItemMenu>,
    timeLineState: TimeLineState,
    renderData: RenderData,
    previewPlayerStatus: VideoEditorPreviewPlayer.PlayerStatus,
    timeLineMsWidthPx: Int,
    historyState: HistoryManager.HistoryState,
    snackbarRouterRequestData: VideoEditorSnackbarRouterRequestData?,
    onChangeTimeLineMsWidthPx: (Int) -> Unit,
    onModeChangeClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFileReceive: (ClipData, DragAndDropPermissionsCompat) -> Unit,
    onDragAndDropRequest: (request: TimeLineData.DragAndDropRequest) -> Unit,
    onSeek: (positionMs: Long) -> Unit,
    onEdit: (TimeLineData.Item) -> Unit,
    onCut: (TimeLineData.Item) -> Unit,
    onDelete: (TimeLineData.Item) -> Unit,
    onDuplicate: (TimeLineData.Item) -> Unit,
    onCopy: (TimeLineData.Item) -> Unit,
    onDurationChange: (TimeLineData.DurationChangeRequest) -> Unit,
    onSnackbarDismiss: () -> Unit,
    onRequestAddItemBottomSheet: () -> Unit,
    onRecommendResult: (AddRenderItemMenuResult) -> Unit
) {
    Box(modifier = modifier) {

        Column {
            // 戻る進むとかのヘッダー
            DefaultTimeLineHeader(
                msWidthPx = timeLineMsWidthPx,
                fillMaxWidth = true,
                onModeChangeClick = onModeChangeClick,
                onZoomIn = { onChangeTimeLineMsWidthPx(timeLineMsWidthPx + 1) },
                onZoomOut = { onChangeTimeLineMsWidthPx(maxOf(timeLineMsWidthPx - 1, 1)) },
                hasUndo = historyState.hasUndo,
                hasRedo = historyState.hasRedo,
                onUndo = onUndo,
                onRedo = onRedo
            )

            // 線
            HorizontalDivider()

            // タイムラインの共有部分
            TimeLineContainer(
                modifier = Modifier,
                timeLineState = timeLineState,
                durationMs = { renderData.durationMs },
                currentPositionMs = { previewPlayerStatus.currentPositionMs }
            ) {
                // ドラッグアンドドロップが受け入れできるように
                FileDragAndDropReceiveContainer(onReceive = onFileReceive) {
                    DefaultTimeLine(
                        modifier = Modifier,
                        timeLineState = timeLineState,
                        currentPositionMs = { previewPlayerStatus.currentPositionMs },
                        onDragAndDropRequest = onDragAndDropRequest,
                        onSeek = onSeek,
                        onEdit = onEdit,
                        onCut = onCut,
                        onDelete = onDelete,
                        onDuplicate = onDuplicate,
                        onCopy = onCopy,
                        onDurationChange = onDurationChange
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 20.dp)
                .padding(bottom = bottomPadding)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Snackbar
            // バーの上に出すため
            if (snackbarRouterRequestData != null) {
                VideoEditorSnackbarRouter(
                    routerRequestData = snackbarRouterRequestData,
                    onSnackbarDismiss = onSnackbarDismiss
                )
            }

            // フローティングしているバー
            // ナビゲーションバーの分も padding 入れておく
            FloatingTimeLineBar {

                FloatingTimeLineTitledItem(
                    title = stringResource(id = R.string.video_edit_floating_add_bar_add),
                    iconResId = R.drawable.ic_outlined_add_24px,
                    onClick = onRequestAddItemBottomSheet
                )

                // 使うメニュー推論
                recommendFloatingBarMenuList.forEach { recommendMenu ->
                    val creator = rememberRenderItemCreator(onResult = onRecommendResult)

                    FloatingTimeLineItem(
                        iconResId = recommendMenu.iconResId,
                        onClick = { creator.create(recommendMenu) }
                    )
                }
            }
        }

    }
}

/** 複数選択モード時の elevation、TopBar() もこれくらいやろ */
private val MultiSelectHeaderBackgroundColor
    @Composable
    get() = MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp)

/** 複数選択モード時のタイムライン部分の UI */
@Composable
private fun VideoEditorMultiSelectTimeLine(
    modifier: Modifier = Modifier,
    bottomPadding: Dp,
    timeLineState: TimeLineState,
    renderData: RenderData,
    previewPlayerStatus: VideoEditorPreviewPlayer.PlayerStatus,
    timeLineMsWidthPx: Int,
    historyState: HistoryManager.HistoryState,
    onChangeTimeLineMsWidthPx: (Int) -> Unit,
    onExitMultiSelectTimeLine: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDragAndDropRequest: (request: List<TimeLineData.DragAndDropRequest>) -> Unit,
    onSeek: (positionMs: Long) -> Unit,
    onMultipleDelete: (List<Long>) -> Unit,
    onMultipleCopy: (List<Long>) -> Unit
) {
    // 複数選択中のアイテム
    val multiSelectItemIdList = remember { mutableStateOf(emptyList<Long>()) }

    Box(modifier = modifier) {

        Column {
            // 戻る進むとかのヘッダー
            MultiSelectTimeLineHeader(
                modifier = Modifier.background(MultiSelectHeaderBackgroundColor),
                fillMaxWidth = true,
                onExitMultiSelect = onExitMultiSelectTimeLine,
                msWidthPx = timeLineMsWidthPx,
                onZoomIn = { onChangeTimeLineMsWidthPx(timeLineMsWidthPx + 1) },
                onZoomOut = { onChangeTimeLineMsWidthPx(maxOf(timeLineMsWidthPx - 1, 1)) },
                hasUndo = historyState.hasUndo,
                hasRedo = historyState.hasRedo,
                onUndo = onUndo,
                onRedo = onRedo
            )

            // 線
            HorizontalDivider()

            // タイムラインの共有部分
            TimeLineContainer(
                modifier = Modifier,
                timeLineState = timeLineState,
                durationMs = { renderData.durationMs },
                currentPositionMs = { previewPlayerStatus.currentPositionMs }
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
                    onSeek = onSeek,
                    onDragAndDropRequest = onDragAndDropRequest
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
                onClick = { onMultipleCopy(multiSelectItemIdList.value) }
            )
            FloatingTimeLineTitledItem(
                title = stringResource(R.string.video_edit_floating_multi_delete),
                iconResId = R.drawable.ic_outline_delete_24px,
                onClick = {
                    onMultipleDelete(multiSelectItemIdList.value)
                    multiSelectItemIdList.value = emptyList()
                }
            )
        }
    }
}

/** 大画面用 動画編集画面のタイムライン部分の UI */
@Composable
private fun LargeScreenVideoEditorDefaultTimeLine(
    modifier: Modifier = Modifier,
    bottomPadding: Dp,
    timeLineState: TimeLineState,
    renderData: RenderData,
    previewPlayerStatus: VideoEditorPreviewPlayer.PlayerStatus,
    timeLineMsWidthPx: Int,
    historyState: HistoryManager.HistoryState,
    snackbarRouterRequestData: VideoEditorSnackbarRouterRequestData?,
    onChangeTimeLineMsWidthPx: (Int) -> Unit,
    onModeChangeClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFileReceive: (ClipData, DragAndDropPermissionsCompat) -> Unit,
    onDragAndDropRequest: (request: TimeLineData.DragAndDropRequest) -> Unit,
    onSeek: (positionMs: Long) -> Unit,
    onEdit: (TimeLineData.Item) -> Unit,
    onCut: (TimeLineData.Item) -> Unit,
    onDelete: (TimeLineData.Item) -> Unit,
    onDuplicate: (TimeLineData.Item) -> Unit,
    onCopy: (TimeLineData.Item) -> Unit,
    onDurationChange: (TimeLineData.DurationChangeRequest) -> Unit,
    onSnackbarDismiss: () -> Unit
) {
    Box(modifier = modifier) {

        // タイムラインの共有部分
        TimeLineContainer(
            modifier = Modifier,
            timeLineState = timeLineState,
            durationMs = { renderData.durationMs },
            currentPositionMs = { previewPlayerStatus.currentPositionMs }
        ) {
            // ドラッグアンドドロップが受け入れできるように
            FileDragAndDropReceiveContainer(onReceive = onFileReceive) {
                DefaultTimeLine(
                    modifier = Modifier,
                    timeLineState = timeLineState,
                    currentPositionMs = { previewPlayerStatus.currentPositionMs },
                    onDragAndDropRequest = onDragAndDropRequest,
                    onSeek = onSeek,
                    onEdit = onEdit,
                    onCut = onCut,
                    onDelete = onDelete,
                    onDuplicate = onDuplicate,
                    onCopy = onCopy,
                    onDurationChange = onDurationChange
                )
            }
        }

        // 戻る進むとかはドラッグで移動できる
        LargeScreenDefaultTimeLineHeader(modifier = Modifier.align(Alignment.TopEnd)) {
            DefaultTimeLineHeader(
                msWidthPx = timeLineMsWidthPx,
                fillMaxWidth = false,
                onModeChangeClick = onModeChangeClick,
                onZoomIn = { onChangeTimeLineMsWidthPx(timeLineMsWidthPx + 1) },
                onZoomOut = { onChangeTimeLineMsWidthPx(maxOf(timeLineMsWidthPx - 1, 1)) },
                hasUndo = historyState.hasUndo,
                hasRedo = historyState.hasRedo,
                onUndo = onUndo,
                onRedo = onRedo
            )
        }

        // Snackbar
        if (snackbarRouterRequestData != null) {
            VideoEditorSnackbarRouter(
                modifier = Modifier
                    .padding(vertical = 10.dp, horizontal = 20.dp)
                    .padding(bottom = bottomPadding)
                    .align(Alignment.BottomCenter),
                routerRequestData = snackbarRouterRequestData,
                onSnackbarDismiss = onSnackbarDismiss
            )
        }
    }
}

/** 複数選択モード時のタイムライン部分の UI */
@Composable
private fun LargeScreenVideoEditorMultiSelectTimeLine(
    modifier: Modifier = Modifier,
    bottomPadding: Dp,
    timeLineState: TimeLineState,
    renderData: RenderData,
    previewPlayerStatus: VideoEditorPreviewPlayer.PlayerStatus,
    timeLineMsWidthPx: Int,
    historyState: HistoryManager.HistoryState,
    onChangeTimeLineMsWidthPx: (Int) -> Unit,
    onExitMultiSelectTimeLine: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDragAndDropRequest: (request: List<TimeLineData.DragAndDropRequest>) -> Unit,
    onSeek: (positionMs: Long) -> Unit,
    onMultipleDelete: (List<Long>) -> Unit,
    onMultipleCopy: (List<Long>) -> Unit
) {
    // 複数選択中のアイテム
    val multiSelectItemIdList = remember { mutableStateOf(emptyList<Long>()) }

    Box(modifier = modifier) {

        // タイムラインの共有部分
        TimeLineContainer(
            modifier = Modifier,
            timeLineState = timeLineState,
            durationMs = { renderData.durationMs },
            currentPositionMs = { previewPlayerStatus.currentPositionMs }
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
                onSeek = onSeek,
                onDragAndDropRequest = onDragAndDropRequest
            )
        }

        // 戻る進むとかはドラッグで移動できる
        LargeScreenDefaultTimeLineHeader(
            modifier = Modifier.align(Alignment.TopEnd),
            color = MultiSelectHeaderBackgroundColor
        ) {
            MultiSelectTimeLineHeader(
                onExitMultiSelect = onExitMultiSelectTimeLine,
                fillMaxWidth = false,
                msWidthPx = timeLineMsWidthPx,
                onZoomIn = { onChangeTimeLineMsWidthPx(timeLineMsWidthPx + 1) },
                onZoomOut = { onChangeTimeLineMsWidthPx(maxOf(timeLineMsWidthPx - 1, 1)) },
                hasUndo = historyState.hasUndo,
                hasRedo = historyState.hasRedo,
                onUndo = onUndo,
                onRedo = onRedo
            )
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
                onClick = { onMultipleCopy(multiSelectItemIdList.value) }
            )
            FloatingTimeLineTitledItem(
                title = stringResource(R.string.video_edit_floating_multi_delete),
                iconResId = R.drawable.ic_outline_delete_24px,
                onClick = {
                    onMultipleDelete(multiSelectItemIdList.value)
                    multiSelectItemIdList.value = emptyList()
                }
            )
        }
    }
}
