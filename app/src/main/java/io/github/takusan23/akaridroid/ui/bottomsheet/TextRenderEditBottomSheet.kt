package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.tool.FontManager
import io.github.takusan23.akaridroid.ui.component.BottomSheetHeader
import io.github.takusan23.akaridroid.ui.component.OutlinedDropDownMenu
import io.github.takusan23.akaridroid.ui.component.OutlinedFloatTextField
import io.github.takusan23.akaridroid.ui.component.RenderItemColorEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemDisplayTimeEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemPositionEditComponent

/**
 * [RenderData.CanvasItem.Text]の編集ボトムシート
 *
 * @param renderItem キャンバスのテキストの情報
 * @param onUpdate 更新時に呼ばれる
 * @param onDelete 削除時に呼ばれる
 */
@Composable
fun TextRenderEditBottomSheet(
    renderItem: RenderData.CanvasItem.Text,
    onUpdate: (RenderData.CanvasItem.Text) -> Unit,
    onDelete: (RenderData.CanvasItem.Text) -> Unit,
) {
    val textItem = remember { mutableStateOf(renderItem) }

    fun update(copy: (RenderData.CanvasItem.Text) -> RenderData.CanvasItem.Text) {
        textItem.value = copy(textItem.value)
    }

    Column(
        modifier = Modifier
            .padding(10.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        BottomSheetHeader(
            title = "テキストの編集",
            onComplete = { onUpdate(textItem.value) },
            onDelete = { onDelete(textItem.value) }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = textItem.value.text,
            onValueChange = { text -> update { it.copy(text = text) } },
            label = { Text(text = "文字") }
        )

        RenderItemColorEditComponent(
            modifier = Modifier.fillMaxWidth(),
            hexColorCode = textItem.value.fontColor,
            onUpdate = { color -> update { it.copy(fontColor = color) } }
        )

        StrokeTextEditComponent(
            strokeColor = textItem.value.strokeColor,
            onUpdate = { color -> update { it.copy(strokeColor = color) } }
        )

        FontEditComponent(
            fontName = textItem.value.fontName,
            onUpdate = { fontName -> update { it.copy(fontName = fontName) } }
        )

        OutlinedFloatTextField(
            modifier = Modifier.fillMaxWidth(),
            value = textItem.value.textSize,
            onValueChange = { textSize -> update { it.copy(textSize = textSize) } },
            label = { Text(text = "文字サイズ") }
        )

        RenderItemPositionEditComponent(
            position = textItem.value.position,
            onUpdate = { position -> update { it.copy(position = position) } }
        )

        RenderItemDisplayTimeEditComponent(
            displayTime = textItem.value.displayTime,
            onUpdate = { displayTime -> update { it.copy(displayTime = displayTime) } }
        )
    }
}

/**
 * フォント選択ドロップダウンメニュー。
 * [FontManager]で登録していないと表示されない。
 *
 * @param fontName 設定中のフォント
 * @param onUpdate 更新時に呼ばれる
 */
@Composable
private fun FontEditComponent(
    fontName: String?,
    onUpdate: (String?) -> Unit
) {
    val context = LocalContext.current
    val fontManager = remember { FontManager(context) }
    val fontNameList = remember { mutableStateOf<List<String>>(emptyList()) }

    val isShowFontMenu = remember { mutableStateOf(fontName != null) }

    // 読み込む
    LaunchedEffect(key1 = Unit) {
        fontNameList.value = fontManager.getFontList().map { it.name }
    }

    if (fontNameList.value.isNotEmpty()) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outnline_font_download_24px),
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = "フォントを変更する"
                )
                Switch(
                    checked = isShowFontMenu.value,
                    onCheckedChange = {
                        isShowFontMenu.value = it
                        // ON にしたら適当に最初のやつを
                        onUpdate(if (it) fontNameList.value.first() else null)
                    }
                )
            }

            // フォントが FontManager にない場合もあるので注意
            // indexOf を nullable でほしい...
            if (fontName != null) {
                Box(modifier = Modifier.align(Alignment.End)) {
                    OutlinedDropDownMenu(
                        label = "フォント",
                        currentSelectIndex = fontNameList.value.indexOf(fontName).takeIf { it >= 0 } ?: 0,
                        menuList = fontNameList.value,
                        onSelect = { index -> onUpdate(fontNameList.value[index]) }
                    )
                }
            }
        }
    }
}

/**
 * 枠取り文字にするなら
 *
 * @param strokeColor 現在の色
 * @param onUpdate 変化時に呼ばれる
 */
@Composable
private fun StrokeTextEditComponent(
    strokeColor: String?,
    onUpdate: (String?) -> Unit
) {
    val isShowStrokeColor = remember { mutableStateOf(strokeColor != null) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_border_color_24px),
                contentDescription = null
            )
            Text(
                modifier = Modifier.weight(1f),
                text = "枠取り文字にする"
            )
            Switch(
                checked = isShowStrokeColor.value,
                onCheckedChange = {
                    isShowStrokeColor.value = it
                    onUpdate(if (it) "#000000" else null)
                }
            )
        }

        if (isShowStrokeColor.value) {
            RenderItemColorEditComponent(
                modifier = Modifier.fillMaxWidth(),
                hexColorCode = strokeColor!!,
                onUpdate = { color -> onUpdate(color) }
            )
        }
    }
}