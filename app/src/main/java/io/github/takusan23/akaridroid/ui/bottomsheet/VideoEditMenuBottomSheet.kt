package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R

/**
 * 動画編集画面のメニュー
 *
 * @param onClick メニュー選択時に呼ばれる。列挙型が押したメニュー
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditMenuBottomSheet(
    onClick: (VideoEditMenuBottomSheetMenu) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .padding(start = 10.dp, top = 10.dp, end = 10.dp)
            .fillMaxHeight(0.5f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            VideoEditMenuBottomSheetItem(
                text = "動画を出力する",
                icon = painterResource(id = R.drawable.ic_outline_save_24),
                onClick = { onClick(VideoEditMenuBottomSheetMenu.EncodeMenu) }
            )
        }
        item {
            VideoEditMenuBottomSheetItem(
                text = "プロジェクトを保存する",
                icon = painterResource(id = R.drawable.ic_outline_save_24),
                onClick = { onClick(VideoEditMenuBottomSheetMenu.SaveMenu) }
            )
        }
    }
}

/** メニュー */
enum class VideoEditMenuBottomSheetMenu {
    /** エンコード */
    EncodeMenu,

    /** 保存する */
    SaveMenu
}

/**
 * メニューの各アイテム
 *
 * @param modifier [Modifier]
 * @param icon アイコン
 * @param text テキスト
 * @param onClick 押したときに呼ばれる
 */
@ExperimentalMaterial3Api
@Composable
private fun VideoEditMenuBottomSheetItem(
    modifier: Modifier = Modifier,
    text: String,
    icon: Painter,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = text)
        }
    }
}