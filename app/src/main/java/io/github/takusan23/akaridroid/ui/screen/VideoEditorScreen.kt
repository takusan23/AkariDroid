package io.github.takusan23.akaridroid.ui.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.akaridroid.service.EncoderService
import io.github.takusan23.akaridroid.ui.bottomsheet.BottomSheetNavigation
import io.github.takusan23.akaridroid.ui.bottomsheet.VideoEditMenuBottomSheetMenu
import io.github.takusan23.akaridroid.ui.bottomsheet.data.BottomSheetInitData
import io.github.takusan23.akaridroid.ui.bottomsheet.data.BottomSheetResultData
import io.github.takusan23.akaridroid.ui.bottomsheet.rememberBottomSheetState
import io.github.takusan23.akaridroid.ui.component.*
import io.github.takusan23.akaridroid.ui.tool.rememberVideoPlayerState
import io.github.takusan23.akaridroid.viewmodel.VideoEditorViewModel
import kotlinx.coroutines.launch

/** 編集画面 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun VideoEditorScreen(
    viewModel: VideoEditorViewModel = viewModel(
        factory = VideoEditorViewModel.Factory,
        extras = MutableCreationExtras((LocalViewModelStoreOwner.current as HasDefaultViewModelProviderFactory).defaultViewModelCreationExtras).apply {
            set(VideoEditorViewModel.PROJECT_ID, "project-2022-01-10")
        }
    )
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current

    // Storage Access Framework
    val videoPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri -> viewModel.setVideoFile(uri) }
    val audioPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri -> viewModel.addAudioFile(uri) }

    // エンコーダーサービスとバインドする
    val encoderService = remember { EncoderService.bindEncoderService(context, lifecycle) }.collectAsState(initial = null)
    val playerState = rememberVideoPlayerState(context = context, lifecycle = lifecycle.lifecycle)
    val bottomSheetState = rememberBottomSheetState { resultData ->
        // ボトムシートの結果
        when (resultData) {
            // Canvas要素の更新
            is BottomSheetResultData.CanvasElementResult -> {
                viewModel.updateElement(resultData.canvasElementData)
            }
            // Canvas要素の削除
            is BottomSheetResultData.CanvasElementDeleteResult -> {
                viewModel.deleteElement(resultData.deleteElementData)
            }
            // 音声要素の更新
            is BottomSheetResultData.AudioAssetResult -> {
                viewModel.updateAudioAssetData(resultData.audioAssetData)
            }
            // 音声要素の削除
            is BottomSheetResultData.AudioAssetDeleteResult -> {
                viewModel.deleteAudioAssetData(resultData.audioAssetData)
            }
            // メニューのコールバック
            is BottomSheetResultData.VideoEditMenuResult -> {
                when (resultData.menu) {
                    VideoEditMenuBottomSheetMenu.EncodeMenu -> {
                        scope.launch {
                            // ファイルに保存して、エンコーダーサービスにエンコードを依頼する
                            val projectData = viewModel.saveEncodeData()
                            encoderService.value?.encodeAkariProject(projectData)
                        }
                    }
                    VideoEditMenuBottomSheetMenu.EncoderFormatSetting -> {
                        // エンコーダー設定。まだ
                        Toast.makeText(context, "未実装", Toast.LENGTH_SHORT).show()
                    }
                    VideoEditMenuBottomSheetMenu.SaveMenu -> {
                        scope.launch { viewModel.saveEncodeData() }
                    }
                }
            }
        }
    }

    val isRunningEncode = encoderService.value?.isRunningEncode?.collectAsState()
    val isPlayingFlow = playerState.playWhenRelayFlow.collectAsState()
    val currentPositionFlow = playerState.currentPositionMsFlow.collectAsState()
    val videoFileData = viewModel.videoFileData.collectAsState()
    val canvasElementList = viewModel.canvasElementList.collectAsState()
    val audioAssetList = viewModel.audioAssetList.collectAsState()
    val videoOutputFormat = viewModel.videoOutputFormat.collectAsState()

    // 動画をセット
    LaunchedEffect(key1 = videoFileData.value) {
        videoFileData.value?.also { path -> playerState.setMediaItem(path.videoFilePath) }
    }

    ModalSheetScaffold(
        modifier = Modifier,
        modalBottomSheetState = bottomSheetState.modalBottomSheetState,
        bottomSheetContent = {
            // ボトムシート
            BottomSheetNavigation(
                bottomSheetState = bottomSheetState,
                onClose = { bottomSheetState.close() }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {

            // エンコード中用表示。かり
            if (isRunningEncode?.value == true) {
                TempEncodingMessage(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                )
                return@Column
            }

            // 動画プレイヤー
            Surface(
                modifier = Modifier
                    .padding(start = 10.dp, end = 10.dp)
                    .aspectRatio(1.7f),
                color = Color.Black,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                VideoPlayer(
                    modifier = Modifier.fillMaxSize(),
                    playerState = playerState
                )
                AkariCanvasCompose(
                    modifier = Modifier.fillMaxSize(),
                    elementList = canvasElementList.value,
                    videoWidth = videoOutputFormat.value.videoWidth,
                    videoHeight = videoOutputFormat.value.videoHeight
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
                videoFileData = videoFileData.value,
                elementList = canvasElementList.value,
                audioAssetList = audioAssetList.value,
                onElementClick = { element ->
                    // 編集ボトムシートを開く
                    bottomSheetState.open(BottomSheetInitData.CanvasElementInitData(element))
                },
                onAudioAssetClick = { audioAssetData ->
                    bottomSheetState.open(BottomSheetInitData.AudioAssetInitData(audioAssetData))
                }
            )
            // 下のバー
            EditorMenuBar(
                modifier = Modifier.fillMaxWidth(),
                onMenuClick = { bottomSheetState.open(BottomSheetInitData.VideoEditMenuInitData) },
                onVideoClick = { videoPicker.launch(PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                onTextClick = { viewModel.addTextElement().also { addTextElement -> bottomSheetState.open(BottomSheetInitData.CanvasElementInitData(addTextElement)) } },
                onAudioClick = { audioPicker.launch(arrayOf("audio/*")) }
            )
        }
    }
}