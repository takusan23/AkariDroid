package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R

/**
 * タイムライン
 */
@Composable
fun TimeLine(
    modifier: Modifier = Modifier,
    maxLines: Int = 5
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
    ) {

        // 時間を表示するやつ
        Row {
            repeat(10) { index ->
                Text(
                    modifier = Modifier.padding(start = (index + 100).dp),
                    text = "00:00"
                )
            }
        }

        // タイムラインのアイテム
        // レーンの数だけ
        repeat(maxLines) { index ->
            Row(modifier = Modifier.height(50.dp)) {

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
        }
    }
}

/**
 * タイムラインの各レーン。回転寿司。
 * TODO [LazyRow]で実装できないか考える。[androidx.compose.foundation.lazy.LazyListState]だとスクロール位置をピクセル単位で返す value 無いんだよね
 *
 * @param modifier [Modifier]
 * @param content [Row]と同じ
 */
@Composable
private fun TimeLineSushiLane(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val density = LocalDensity.current
    // Divider で線を引くために幅を取る必要がある
    val width = remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.onSizeChanged { width.intValue = it.width }) {
        Row(
            modifier = modifier,
            content = content
        )
        Divider(
            modifier = Modifier
                .width(with(density) { width.intValue.toDp() })
        )
    }
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
            modifier = Modifier.align(Alignment.Center),
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
