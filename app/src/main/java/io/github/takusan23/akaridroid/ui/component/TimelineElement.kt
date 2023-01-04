package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R

/**
 * タイムラインの各要素
 *
 * @param modifier [Modifier]
 * @param color 色
 * @param text テキスト
 * @param onClick 押したら呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineElement(
    modifier: Modifier = Modifier,
    color: Color,
    type: TimelineElementType,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(5.dp),
        color = color,
        contentColor = contentColorFor(color),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                contentDescription = null,
                painter = painterResource(
                    id = when (type) {
                        TimelineElementType.Text -> R.drawable.ic_outline_text_fields_24
                        TimelineElementType.Video -> R.drawable.ic_outline_video_file_24
                    }
                )
            )
            Text(text = text)
        }
    }
}

/** タイムラインの要素の種類 */
enum class TimelineElementType {
    /** テキスト */
    Text,

    /** 動画 */
    Video
}