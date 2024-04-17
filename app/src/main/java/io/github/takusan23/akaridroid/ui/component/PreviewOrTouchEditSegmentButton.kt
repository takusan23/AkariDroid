package io.github.takusan23.akaridroid.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.PreviewOrTouchEditMode.Preview
import io.github.takusan23.akaridroid.ui.component.PreviewOrTouchEditMode.TouchEdit

/** [Preview] [TouchEdit] どちらを選択しているか */
enum class PreviewOrTouchEditMode {
    /** プレビューモード */
    Preview,

    /** タッチ編集モード */
    TouchEdit
}

/**
 * タッチ編集とプレビュー再生の切り替えセグメントボタン
 *
 * @param modifier [Modifier]
 * @param currentMode どちらを選択しているか [PreviewOrTouchEditMode]
 * @param onPreviewClick プレビュー再生押した時
 * @param onTouchEditClick タッチ編集押した時
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewOrTouchEditSegmentButton(
    modifier: Modifier = Modifier,
    currentMode: PreviewOrTouchEditMode,
    onPreviewClick: () -> Unit,
    onTouchEditClick: () -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        PreviewOrTouchEditMode.entries.forEachIndexed { index, mode ->
            val isSelected = currentMode == mode

            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = PreviewOrTouchEditMode.entries.size),
                onClick = when (mode) {
                    Preview -> onPreviewClick
                    TouchEdit -> onTouchEditClick
                },
                selected = isSelected
            ) {
                Icon(
                    painter = painterResource(
                        id = when (mode) {
                            Preview -> R.drawable.ic_outline_play_arrow_24
                            TouchEdit -> R.drawable.ic_outlined_touch_app_24px
                        }
                    ),
                    contentDescription = null
                )
            }
        }
    }
}