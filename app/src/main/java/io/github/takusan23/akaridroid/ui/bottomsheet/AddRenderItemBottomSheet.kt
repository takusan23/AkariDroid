package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.AddRenderItemMenu
import io.github.takusan23.akaridroid.ui.component.AddRenderItemMenuResult
import io.github.takusan23.akaridroid.ui.component.BottomSheetMenuItem
import io.github.takusan23.akaridroid.ui.component.rememberRenderItemCreator

/**
 * タイムラインに素材を追加するボトムシート
 *
 * @param onAddRenderItemResult 何を追加したか
 */
@Composable
fun AddRenderItemBottomSheet(onAddRenderItemResult: (AddRenderItemMenuResult) -> Unit) {
    val creator = rememberRenderItemCreator(onResult = onAddRenderItemResult)

    Column(
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = stringResource(id = R.string.video_edit_bottomsheet_timeline_add_title),
            fontSize = 24.sp
        )

        AddRenderItemMenu.entries.forEach { menu ->
            BottomSheetMenuItem(
                menu = menu,
                onClick = { creator.create(menu) }
            )
        }
    }
}


/**
 * [BottomSheetMenuItem]の各メニュー
 *
 * @param modifier [Modifier]
 * @param menu メニュー
 * @param onClick 押したら呼ばれる
 */
@Composable
private fun BottomSheetMenuItem(
    modifier: Modifier = Modifier,
    menu: AddRenderItemMenu,
    onClick: () -> Unit
) {
    BottomSheetMenuItem(
        modifier = modifier,
        title = stringResource(id = menu.labelResId),
        description = stringResource(id = menu.descriptionResId),
        iconResId = menu.iconResId,
        onClick = onClick
    )
}