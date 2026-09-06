package io.github.takusan23.akaridroid.ui.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.encoder.EncoderParameters
import io.github.takusan23.akaridroid.tool.AkaLinkTool
import io.github.takusan23.akaridroid.ui.component.AddRenderItemMenuResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 大画面レイアウトで使う上に重なるシート
 * スマホ用であるボトムシートは BottomSheetRouter
 *
 * @param videoEditorBottomSheetRouteRequestData ボトムシートの表示に必要なデータ
 * @param onClose ボトムシート閉じたときに呼ばれる
 */
@Composable
fun VideoEditorOverlaySheetRouter(
    modifier: Modifier = Modifier,
    videoEditorBottomSheetRouteRequestData: VideoEditorBottomSheetRouteRequestData?,
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
    onMultiSelectClick: () -> Unit
) {

    if (videoEditorBottomSheetRouteRequestData != null) {
        OverlaySheet(
            modifier = modifier,
            onClose = onClose
        ) {
            VideoEditorSheetCommonRouter(
                videoEditorBottomSheetRouteRequestData = videoEditorBottomSheetRouteRequestData,
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
    }
}

@Composable
private fun OverlaySheet(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val state = remember { MutableTransitionState(false).apply { targetState = true } }

    fun animateAndClose() {
        scope.launch {
            state.targetState = false
            snapshotFlow { !state.currentState && state.isIdle }.first { it /* == true */ }
            onClose()
        }
    }

    AnimatedVisibility(
        modifier = modifier,
        visibleState = state,
        enter = fadeIn() + slideInVertically { it / 4 },
        exit = fadeOut() + slideOutVertically { it / 4 }
    ) {
        OutlinedCard(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(fraction = 0.5f),
            elevation = CardDefaults.outlinedCardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    modifier = Modifier.align(alignment = Alignment.End),
                    onClick = { animateAndClose() }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_outline_close_24),
                        contentDescription = null
                    )
                }

                content()
            }
        }
    }
}