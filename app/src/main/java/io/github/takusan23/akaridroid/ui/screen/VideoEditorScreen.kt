package io.github.takusan23.akaridroid.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.akaridroid.ui.bottomsheet.BottomSheetNavigation
import io.github.takusan23.akaridroid.ui.bottomsheet.data.BottomSheetInitData
import io.github.takusan23.akaridroid.ui.bottomsheet.data.BottomSheetResultData
import io.github.takusan23.akaridroid.ui.bottomsheet.rememberBottomSheetState
import io.github.takusan23.akaridroid.ui.component.*
import io.github.takusan23.akaridroid.ui.tool.rememberVideoPlayerState
import io.github.takusan23.akaridroid.viewmodel.VideoEditorViewModel

/** 編集画面 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun VideoEditorScreen(
    viewModel: VideoEditorViewModel = viewModel(
        factory = VideoEditorViewModel.Factory,
        extras = MutableCreationExtras((LocalViewModelStoreOwner.current as HasDefaultViewModelProviderFactory).defaultViewModelCreationExtras).apply {
            set(VideoEditorViewModel.PROJECT_ID, "xxxx")
        }
    )
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val playerState = rememberVideoPlayerState(context = context, lifecycle = lifecycle.lifecycle)
    val bottomSheetState = rememberBottomSheetState(onResult = { resultData ->
        // ボトムシートの結果
        when (resultData) {
            is BottomSheetResultData.CanvasElementResult -> {
                viewModel.updateElement(resultData.canvasElementData)
            }
        }
    })

    val isPlayingFlow = playerState.playWhenRelayFlow.collectAsState()
    val currentPositionFlow = playerState.currentPositionMsFlow.collectAsState()
    val canvasElementList = viewModel.canvasElementList.collectAsState()

    ModalSheetScaffold(
        modifier = Modifier,
        modalBottomSheetState = bottomSheetState.modalBottomSheetState,
        bottomSheetContent = {
            BottomSheetNavigation(
                canvasElementData = bottomSheetState,
                onClose = { bottomSheetState.close() }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            // 動画プレイヤー
            Surface(
                modifier = Modifier
                    .padding(start = 10.dp, end = 10.dp)
                    .aspectRatio(1.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                VideoPlayer(
                    modifier = Modifier.fillMaxSize(),
                    playerState = playerState
                )
                AkariCanvas(
                    modifier = Modifier.fillMaxSize(),
                    elementList = canvasElementList.value,
                )
            }
            // シークバー
            VideoPlayerController(
                modifier = Modifier.padding(bottom = 10.dp),
                isPlaying = isPlayingFlow.value,
                currentPosMs = currentPositionFlow.value.currentPositionMs,
                durationMs = currentPositionFlow.value.durationMs,
                onPlay = { isPlay -> playerState.playWhenReady = isPlay },
                onSeek = { posMs -> playerState.currentPositionMs = posMs }
            )
            // タイムライン
            Timeline(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 10.dp)
                    .fillMaxWidth(),
                elementList = canvasElementList.value,
                onElementClick = { element ->
                    // 編集ボトムシートを開く
                    bottomSheetState.open(BottomSheetInitData.CanvasElementInitData(element))
                }
            )
            // 下のバー
            EditorMenuBar(
                modifier = Modifier.fillMaxWidth(),
                onVideoClick = {},
                onTextClick = {}
            )
        }
    }
}