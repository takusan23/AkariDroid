package io.github.takusan23.akaridroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.akaridroid.encoder.EncoderService
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditorBottomSheetRouteRequestData
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditorBottomSheetRouter
import io.github.takusan23.akaridroid.ui.component.AddRenderItemMenuResult
import io.github.takusan23.akaridroid.ui.component.ComposeSurfaceView
import io.github.takusan23.akaridroid.ui.component.FileDragAndDropReceiveContainer
import io.github.takusan23.akaridroid.ui.component.FloatingAddRenderItemBar
import io.github.takusan23.akaridroid.ui.component.FloatingMenuButton
import io.github.takusan23.akaridroid.ui.component.PreviewContainer
import io.github.takusan23.akaridroid.ui.component.TimeLine
import io.github.takusan23.akaridroid.ui.component.TimeLineZoomButtons
import io.github.takusan23.akaridroid.ui.component.UndoRedoButtons
import io.github.takusan23.akaridroid.ui.component.toMenu
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

    // 2箇所から呼ばれてるのでこれを呼ぶ
    fun VideoEditorViewModel.resolveRenderItemCreate(result: AddRenderItemMenuResult) {
        // Addable のみ。ボトムシートを出す必要があれば別途やる
        when (result) {
            is AddRenderItemMenuResult.Addable -> resolveAddRenderItem(result)
            is AddRenderItemMenuResult.BottomSheetOpenable -> openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenAkaLink)
        }
        // 報酬を与える
        floatingMenuBarMultiArmedBanditManager.reward(result.toMenu())
    }

    // ボトムシート
    if (bottomSheetRouteData.value != null) {
        VideoEditorBottomSheetRouter(
            videoEditorBottomSheetRouteRequestData = bottomSheetRouteData.value!!,
            onAudioUpdate = { viewModel.addOrUpdateAudioRenderItem(it) },
            onCanvasUpdate = { viewModel.addOrUpdateCanvasRenderItem(it) },
            onDeleteItem = { viewModel.deleteTimeLineItem(it.id) },
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
            onEncodeClick = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenEncode(renderData.value.videoSize)) },
            onTimeLineReset = { viewModel.resetRenderItemList() },
            onSettingClick = { onNavigate(NavigationPaths.Setting) },
            onStartAkaLink = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenAkaLink) },
            onClose = { viewModel.closeBottomSheet() }
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
                        onPlayOrPause = { if (previewPlayerStatus.value.isPlaying) viewModel.videoEditorPreviewPlayer.pause() else viewModel.videoEditorPreviewPlayer.playInRepeat() }
                    )
                }

                // 戻るボタンとか
                Row(modifier = Modifier.align(Alignment.End)) {
                    TimeLineZoomButtons(
                        msWidthPx = timeLineMsWidthPx.intValue,
                        onZoomIn = { timeLineMsWidthPx.intValue++ },
                        onZoomOut = { timeLineMsWidthPx.intValue = maxOf(timeLineMsWidthPx.intValue - 1, 1) }
                    )
                    UndoRedoButtons(
                        hasUndo = historyState.value.hasUndo,
                        hasRedo = historyState.value.hasRedo,
                        onUndo = { viewModel.renderDataUndo() },
                        onRedo = { viewModel.renderDataRedo() }
                    )
                }

                // 線
                HorizontalDivider()

                // ドラッグアンドドロップが受け入れできるように
                FileDragAndDropReceiveContainer(
                    onReceive = { mimeType, uri, dropPermission ->
                        viewModel.resolveDragAndDropReceiveUri(mimeType, uri, dropPermission)
                    }
                ) {
                    // タイムライン
                    TimeLine(
                        modifier = Modifier,
                        timeLineData = timeLineData.value,
                        currentPositionMs = { previewPlayerStatus.value.currentPositionMs },
                        durationMs = renderData.value.durationMs,
                        msWidthPx = timeLineMsWidthPx.intValue,
                        onSeek = { positionMs -> viewModel.videoEditorPreviewPlayer.seekTo(positionMs) },
                        onDragAndDropRequest = { request -> viewModel.resolveTimeLineDragAndDropRequest(request) },
                        onEdit = { timeLineItem ->
                            viewModel.getRenderItem(timeLineItem.id)?.also { renderItem ->
                                viewModel.openEditRenderItemSheet(renderItem)
                            }
                        },
                        onCut = { timeLineItem -> viewModel.resolveTimeLineCutRequest(timeLineItem) },
                        onDelete = { deleteItem -> viewModel.deleteTimeLineItem(deleteItem.id) },
                        onDuplicate = { duplicateFromItem -> viewModel.duplicateRenderItem(duplicateFromItem.id) },
                        onDurationChange = { request -> viewModel.resolveTimeLineDurationChangeRequest(request) }
                    )
                }
            }

            // フローティングしているバー
            // ナビゲーションバーの分も padding 入れておく
            Row(
                modifier = Modifier
                    .padding(vertical = 10.dp, horizontal = 20.dp)
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .fillMaxWidth()
                    .align(Alignment.BottomEnd),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                FloatingMenuButton(
                    onClick = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenMenu) }
                )

                FloatingAddRenderItemBar(
                    modifier = Modifier.weight(1f),
                    onOpenMenu = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenAddRenderItem) },
                    recommendedMenuList = recommendFloatingBarMenuList.value,
                    onRecommendMenuClick = { viewModel.resolveRenderItemCreate(it) }
                )
            }
        }
    }
}