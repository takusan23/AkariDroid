package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R

/** [EditorMenuBar]の背景色の TonalElevation */
private val EditorMenuBarTonalElevation = 3.dp

/**
 * 編集画面の一番下のバー
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorMenuBar(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit,
    onVideoClick: () -> Unit,
    onTextClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        tonalElevation = EditorMenuBarTonalElevation
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            item {
                EditorMenuBarItem(
                    icon = painterResource(id = R.drawable.ic_outline_menu_24),
                    text = "メニュー",
                    onClick = onMenuClick
                )
            }
            item {
                EditorMenuBarItem(
                    icon = painterResource(id = R.drawable.ic_outline_video_file_24),
                    text = "動画のセット",
                    onClick = onVideoClick
                )
            }
            item {
                EditorMenuBarItem(
                    icon = painterResource(id = R.drawable.ic_outline_text_fields_24),
                    text = "テキストの追加",
                    onClick = onTextClick
                )
            }
        }
    }
}

/**
 * 編集画面の一番下のバーの各ボタン
 *
 * @param modifier [Modifier]
 * @param onClick 押したら呼ばれる
 */
@ExperimentalMaterial3Api
@Composable
private fun EditorMenuBarItem(
    modifier: Modifier = Modifier,
    icon: Painter,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(5.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(painter = icon, contentDescription = null)
            Spacer(modifier = Modifier.height(ButtonDefaults.IconSpacing))
            Text(text = text)
        }
    }
}