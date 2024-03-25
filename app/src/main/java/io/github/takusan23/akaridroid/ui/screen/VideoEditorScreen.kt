package io.github.takusan23.akaridroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.akaridroid.encoder.EncoderService
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditorBottomSheetRouteRequestData
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditorBottomSheetRouter
import io.github.takusan23.akaridroid.ui.component.EncodingStatus
import io.github.takusan23.akaridroid.ui.component.FloatingAddBar
import io.github.takusan23.akaridroid.ui.component.FloatingMenuButton
import io.github.takusan23.akaridroid.ui.component.PreviewPlayerController
import io.github.takusan23.akaridroid.ui.component.TimeLine
import io.github.takusan23.akaridroid.ui.component.UndoRedoButton
import io.github.takusan23.akaridroid.ui.component.VideoPlayerPreviewAndTouchEditor
import io.github.takusan23.akaridroid.viewmodel.VideoEditorViewModel

/**
 * 動画編集画面
 *
 * @param onNavigate 画面遷移時に呼ばれる
 */
@Composable
fun VideoEditorScreen(
    viewModel: VideoEditorViewModel = viewModel(),
    onNavigate: (NavigationPaths) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current

    // バックグラウンドでエンコードできるようにエンコーダーサービス
    val encoderService = remember { EncoderService.bindEncoderService(context, lifecycle) }.collectAsStateWithLifecycle(initialValue = null)
    // エンコード中かどうか
    val encodeStatus = encoderService.value?.encodeStatusFlow?.collectAsStateWithLifecycle()
    // 動画の素材や情報が入ったデータ
    val renderData = viewModel.renderData.collectAsStateWithLifecycle()
    // プレビューのプレイヤー状態
    val previewPlayerStatus = viewModel.videoEditorPreviewPlayer.playerStatus.collectAsStateWithLifecycle()
    // プレビューのBitmap
    val previewBitmap = viewModel.videoEditorPreviewPlayer.previewBitmap.collectAsStateWithLifecycle()
    // ボトムシート
    val bottomSheetRouteData = viewModel.bottomSheetRouteData.collectAsStateWithLifecycle()
    // タイムライン
    val timeLineData = viewModel.timeLineData.collectAsStateWithLifecycle()
    // タッチ編集
    val touchEditorData = viewModel.touchEditorData.collectAsStateWithLifecycle()
    // 履歴機能。undo / redo
    val historyState = viewModel.historyState.collectAsStateWithLifecycle()

    // エンコード中の場合は別の UI を出して return する
    if (encodeStatus?.value != null) {
        Scaffold { paddingValues ->
            EncodingStatus(
                modifier = Modifier.padding(paddingValues),
                encodeStatus = encodeStatus.value!!,
                onCancel = { encoderService.value?.stop() }
            )
        }
        return
    }

    // ボトムシート
    if (bottomSheetRouteData.value != null) {
        VideoEditorBottomSheetRouter(
            videoEditorBottomSheetRouteRequestData = bottomSheetRouteData.value!!,
            onAudioUpdate = { viewModel.addOrUpdateAudioRenderItem(it) },
            onCanvasUpdate = { viewModel.addOrUpdateCanvasRenderItem(it) },
            onDeleteItem = { viewModel.deleteTimeLineItem(it.id) },
            onAddTimeLineItem = { viewModel.resolveAddRenderItem(it) },
            onRenderDataUpdate = { viewModel.updateRenderData(it) },
            onEncode = { fileName, parameters ->
                encoderService.value?.encodeAkariCore(
                    renderData = renderData.value,
                    projectFolder = viewModel.projectFolder,
                    resultFileName = fileName,
                    encoderParameters = parameters
                )
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
                // プレビュー
                VideoPlayerPreviewAndTouchEditor(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f),
                    previewBitmap = previewBitmap.value?.asImageBitmap(),
                    touchEditorData = touchEditorData.value,
                    onDragAndDropEnd = { request -> viewModel.resolveTouchEditorDragAndDropRequest(request) },
                    onSizeChangeRequest = { request -> viewModel.resolveTouchEditorSizeChangeRequest(request) }
                )

                // シークバーとか
                Row {
                    PreviewPlayerController(
                        modifier = Modifier
                            .weight(1f)
                            .padding(5.dp),
                        playerStatus = previewPlayerStatus.value,
                        onSeek = { viewModel.videoEditorPreviewPlayer.seekTo(it) },
                        onPlayOrPause = { if (previewPlayerStatus.value.isPlaying) viewModel.videoEditorPreviewPlayer.pause() else viewModel.videoEditorPreviewPlayer.playInRepeat() }
                    )
                    UndoRedoButton(
                        hasUndo = historyState.value.hasUndo,
                        hasRedo = historyState.value.hasRedo,
                        onUndo = { viewModel.renderDataUndo() },
                        onRedo = { viewModel.renderDataRedo() }
                    )
                }

                // 線
                HorizontalDivider()

                // タイムライン
                TimeLine(
                    modifier = Modifier,
                    timeLineData = timeLineData.value,
                    currentPositionMs = previewPlayerStatus.value.currentPositionMs,
                    durationMs = renderData.value.durationMs,
                    onSeek = { positionMs -> viewModel.videoEditorPreviewPlayer.seekTo(positionMs) },
                    onDragAndDropRequest = { request -> viewModel.resolveTimeLineDragAndDropRequest(request) },
                    onEdit = { timeLineItem ->
                        viewModel.getRenderItem(timeLineItem.id)?.also { renderItem ->
                            viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenEditor(renderItem))
                        }
                    },
                    onCut = { timeLineItem -> viewModel.resolveTimeLineCutRequest(timeLineItem) },
                    onDelete = { deleteItem -> viewModel.deleteTimeLineItem(deleteItem.id) },
                    onDurationChange = { request -> viewModel.resolveTimeLineDurationChangeRequest(request) }
                )
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

                FloatingMenuButton(onClick = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenMenu) })

                FloatingAddBar(
                    modifier = Modifier.weight(1f),
                    onOpenMenu = { viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenAddRenderItem) }
                )
            }
        }
    }
}