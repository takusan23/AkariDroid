package io.github.takusan23.akaridroid.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.akaridroid.encoder.EncoderService
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditorBottomSheetRouteRequestData
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditorBottomSheetRouter
import io.github.takusan23.akaridroid.ui.component.PreviewPlayerController
import io.github.takusan23.akaridroid.ui.component.TimeLine
import io.github.takusan23.akaridroid.ui.component.VideoEditorBottomBar
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
    val isEncoding = encoderService.value?.isRunningEncode?.collectAsStateWithLifecycle()
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

    // エンコード中の場合
    if (isEncoding?.value == true) {
        Text(text = "エンコード中です")
        return
    }

    // ボトムシート
    if (bottomSheetRouteData.value != null) {
        VideoEditorBottomSheetRouter(
            videoEditorBottomSheetRouteRequestData = bottomSheetRouteData.value!!,
            onResult = { routeResultData ->
                viewModel.resolveBottomSheetResult(routeResultData)
                viewModel.closeBottomSheet()
            },
            onClose = { viewModel.closeBottomSheet() }
        )
    }

    Scaffold(
        bottomBar = {
            VideoEditorBottomBar(
                onCreateRenderItem = { addItem ->
                    // 素材を追加する
                    viewModel.resolveVideoEditorBottomBarAddItem(addItem)
                },
                onEncodeClick = {
                    encoderService.value?.encodeAkariCore(
                        renderData = renderData.value,
                        projectFolder = viewModel.projectFolder
                    )
                },
                onVideoInfoClick = {
                    viewModel.openBottomSheet(VideoEditorBottomSheetRouteRequestData.OpenVideoInfo(renderData.value))
                },
                onSettingClick = {
                    onNavigate(NavigationPaths.Setting)
                },
                onTimeLineReset = {
                    viewModel.resetRenderItem()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

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
            PreviewPlayerController(
                modifier = Modifier.padding(5.dp),
                playerStatus = previewPlayerStatus.value,
                onSeek = { viewModel.videoEditorPreviewPlayer.seekTo(it) },
                onPlayOrPause = { if (previewPlayerStatus.value.isPlaying) viewModel.videoEditorPreviewPlayer.pause() else viewModel.videoEditorPreviewPlayer.playInRepeat() }
            )

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
    }
}