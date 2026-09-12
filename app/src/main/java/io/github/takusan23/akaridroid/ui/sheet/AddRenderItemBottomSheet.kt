package io.github.takusan23.akaridroid.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.AddRenderItemMenu
import io.github.takusan23.akaridroid.ui.component.AddRenderItemMenuResult
import io.github.takusan23.akaridroid.ui.component.BottomSheetMenuItem
import io.github.takusan23.akaridroid.ui.component.SheetHeader
import io.github.takusan23.akaridroid.ui.component.data.getRoundedShape
import io.github.takusan23.akaridroid.ui.component.rememberRenderItemCreator

/**
 * タイムラインに素材を追加するボトムシート
 *
 * @param onAddRenderItemResult 何を追加したか
 * @param onCloseClick 閉じるを押したとき
 */
@Composable
fun AddRenderItemBottomSheet(
    onAddRenderItemResult: (AddRenderItemMenuResult) -> Unit,
    onCloseClick: () -> Unit
) {

    Column(
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        SheetHeader(
            title = stringResource(id = R.string.video_edit_bottomsheet_timeline_add_title),
            onClose = onCloseClick
        )

        AddRenderItemSheet(onAddRenderItemResult = onAddRenderItemResult)
    }
}

/**
 * タイムラインに素材を追加するシート
 *
 * @param onAddRenderItemResult 何を追加したか
 */
@Composable
fun AddRenderItemSheet(
    modifier: Modifier = Modifier,
    onAddRenderItemResult: (AddRenderItemMenuResult) -> Unit
) {
    val creator = rememberRenderItemCreator(onResult = onAddRenderItemResult)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        AddRenderItemMenu.entries.forEachIndexed { index, menu ->
            BottomSheetMenuItem(
                menu = menu,
                shape = getRoundedShape(size = AddRenderItemMenu.entries.size, index = index),
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
    shape: Shape,
    onClick: () -> Unit
) {
    BottomSheetMenuItem(
        modifier = modifier,
        title = stringResource(id = menu.labelResId),
        description = stringResource(id = menu.descriptionResId),
        iconResId = menu.iconResId,
        shape = shape,
        onClick = onClick
    )
}