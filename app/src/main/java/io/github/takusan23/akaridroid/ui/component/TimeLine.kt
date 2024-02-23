package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance

/**
 * タイムライン
 */
@Composable
fun TimeLine(
    modifier: Modifier = Modifier,
    maxLines: Int = 5
) {
    // すべての LazyRow のスクロール位置を同期させる
    val currentScrollPosition = remember { mutableFloatStateOf(0f) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

        Text(text = currentScrollPosition.floatValue.toString())

        // 時間を表示するやつ
        TimeLineSushiLane(
            currentScrollPositionPixelAbsolute = currentScrollPosition.floatValue,
            onScrollPositionPixelAbsolute = { position -> currentScrollPosition.floatValue = position }
        ) {
            repeat(10) { index ->
                Text(
                    modifier = Modifier.padding(start = (index + 100).dp),
                    text = "00:00"
                )
            }
        }
        Divider()

        // タイムラインのアイテム
        // レーンの数だけ
        repeat(maxLines) { index ->

            TimeLineSushiLane(
                modifier = Modifier.height(50.dp),
                currentScrollPositionPixelAbsolute = currentScrollPosition.floatValue,
                onScrollPositionPixelAbsolute = { position -> currentScrollPosition.floatValue = position }
            ) {
                TimeLineIndexText(
                    modifier = Modifier.fillMaxHeight(),
                    index = index + 1
                )

                repeat(10) { index ->
                    TimeLineView(
                        modifier = Modifier.padding(start = (index + 100).dp),
                        text = "画像 その $index",
                        size = 200,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onClick = { }
                    )
                }
            }

            Divider()
        }
    }
}

/**
 * タイムラインの各レーン。回転寿司。
 * TODO [LazyRow]で実装できないか考える。[androidx.compose.foundation.lazy.LazyListState]だとスクロール位置をピクセル単位で返す value 無いんだよね
 *
 * このタイムラインは、この[TimeLineSushiLane]を縦方向に並べることで作っている。
 * のでスクロール位置を合わせる必要がある。そのために[currentScrollPositionPixelAbsolute]がある。
 *
 * @param modifier [Modifier]
 * @param currentScrollPositionPixelAbsolute 他のレーンのスクロール位置、もしくは外からスクロール位置の変更があったら。単位はピクセル。スクロール中は無視される。
 * @param onScrollPositionPixelAbsolute スクロール時に呼び出す。単位はピクセル。他の[TimeLineSushiLane]のスクロール位置を合わせるために使う。
 * @param content [Row]と同じ
 */
@Composable
private fun TimeLineSushiLane(
    modifier: Modifier = Modifier,
    currentScrollPositionPixelAbsolute: Float,
    onScrollPositionPixelAbsolute: (Float) -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    val isInputTouch = remember { mutableStateOf(false) }

    LaunchedEffect(key1 = currentScrollPositionPixelAbsolute) {
        // 他のレーンとスクロール位置を合わせる
        // ただし、自分がスクロール中の場合は無視する
        if (!isInputTouch.value) {
            scrollState.scrollBy(currentScrollPositionPixelAbsolute - scrollState.value)
        }
    }

    LaunchedEffect(key1 = scrollState) {
        snapshotFlow { scrollState.value }
            .distinctUntilChanged()
            .collect {
                // 自分がスクロール中のみ
                if (isInputTouch.value) {
                    onScrollPositionPixelAbsolute(it.toFloat())
                }
            }
    }

    LaunchedEffect(key1 = Unit) {
        scrollState
            .interactionSource
            .interactions
            .filterIsInstance<DragInteraction>()
            .collect {
                when (it) {
                    is DragInteraction.Stop -> {
                        isInputTouch.value = false
                    }

                    is DragInteraction.Start -> {
                        isInputTouch.value = true
                    }
                }
            }
    }

    Row(
        modifier = modifier.horizontalScroll(scrollState),
        content = content
    )
}

/**
 * タイムラインに表示するアイテム
 */
@Composable
private fun TimeLineView(
    modifier: Modifier = Modifier,
    size: Int,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .width(size.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(5.dp),
        color = color,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_audiotrack_24), contentDescription = null)
            Text(
                text = text,
                maxLines = 1
            )
        }
    }
}

/**
 * タイムラインのレーン番号を表示するテキスト
 *
 * @param modifier [Modifier]
 * @param index 番号
 */
@Composable
private fun TimeLineIndexText(
    modifier: Modifier = Modifier,
    index: Int
) {
    Box(
        modifier = modifier
            .height(IntrinsicSize.Max)
            .aspectRatio(1f)
    ) {
        Text(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.Center),
            text = index.toString(),
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 20.sp
        )
        // 縦の区切り線
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .width(1.dp)
                .background(MaterialTheme.colorScheme.secondary)
        )
    }
}
