package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.tool.ColorTool

/**
 * CSS の named-color を選択肢にする。
 * https://developer.mozilla.org/ja/docs/Web/CSS/named-color
 */
private enum class CssNamedColors(val r: Int, val g: Int, val b: Int) {
    Aliceblue(240, 248, 255),
    Antiquewhite(250, 235, 215),
    Aqua(0, 255, 255),
    Aquamarine(127, 255, 212),
    Azure(240, 255, 255),
    Beige(245, 245, 220),
    Bisque(255, 228, 196),
    Black(0, 0, 0),
    Blanchedalmond(255, 235, 205),
    Blue(0, 0, 255),
    Blueviolet(138, 43, 226),
    Brown(165, 42, 42),
    Burlywood(222, 184, 135),
    Cadetblue(95, 158, 160),
    Chartreuse(127, 255, 0),
    Chocolate(210, 105, 30),
    Coral(255, 127, 80),
    Cornflowerblue(100, 149, 237),
    Cornsilk(255, 248, 220),
    Crimson(220, 20, 60),
    Cyan(0, 255, 255),
    Darkblue(0, 0, 139),
    Darkcyan(0, 139, 139),
    Darkgoldenrod(184, 134, 11),
    Darkgray(169, 169, 169),
    Darkgreen(0, 100, 0),
    Darkgrey(169, 169, 169),
    Darkkhaki(189, 183, 107),
    Darkmagenta(139, 0, 139),
    Darkolivegreen(85, 107, 47),
    Darkorange(255, 140, 0),
    Darkorchid(153, 50, 204),
    Darkred(139, 0, 0),
    Darksalmon(233, 150, 122),
    Darkseagreen(143, 188, 143),
    Darkslateblue(72, 61, 139),
    Darkslategray(47, 79, 79),
    Darkslategrey(47, 79, 79),
    Darkturquoise(0, 206, 209),
    Darkviolet(148, 0, 211),
    Deeppink(255, 20, 147),
    Deepskyblue(0, 191, 255),
    Dimgray(105, 105, 105),
    Dimgrey(105, 105, 105),
    Dodgerblue(30, 144, 255),
    Firebrick(178, 34, 34),
    Floralwhite(255, 250, 240),
    Forestgreen(34, 139, 34),
    Fuchsia(255, 0, 255),
    Gainsboro(220, 220, 220),
    Ghostwhite(248, 248, 255),
    Gold(255, 215, 0),
    Goldenrod(218, 165, 32),
    Gray(128, 128, 128),
    Green(0, 128, 0),
    Greenyellow(173, 255, 47),
    Grey(128, 128, 128),
    Honeydew(240, 255, 240),
    Hotpink(255, 105, 180),
    Indianred(205, 92, 92),
    Indigo(75, 0, 130),
    Ivory(255, 255, 240),
    Khaki(240, 230, 140),
    Lavender(230, 230, 250),
    Lavenderblush(255, 240, 245),
    Lawngreen(124, 252, 0),
    Lemonchiffon(255, 250, 205),
    Lightblue(173, 216, 230),
    Lightcoral(240, 128, 128),
    Lightcyan(224, 255, 255),
    Lightgoldenrodyellow(250, 250, 210),
    Lightgray(211, 211, 211),
    Lightgreen(144, 238, 144),
    Lightgrey(211, 211, 211),
    Lightpink(255, 182, 193),
    Lightsalmon(255, 160, 122),
    Lightseagreen(32, 178, 170),
    Lightskyblue(135, 206, 250),
    Lightslategray(119, 136, 153),
    Lightslategrey(119, 136, 153),
    Lightsteelblue(176, 196, 222),
    Lightyellow(255, 255, 224),
    Lime(0, 255, 0),
    Limegreen(50, 205, 50),
    Linen(250, 240, 230),
    Magenta(255, 0, 255),
    Maroon(128, 0, 0),
    Mediumaquamarine(102, 205, 170),
    Mediumblue(0, 0, 205),
    Mediumorchid(186, 85, 211),
    Mediumpurple(147, 112, 219),
    Mediumseagreen(60, 179, 113),
    Mediumslateblue(123, 104, 238),
    Mediumspringgreen(0, 250, 154),
    Mediumturquoise(72, 209, 204),
    Mediumvioletred(199, 21, 133),
    Midnightblue(25, 25, 112),
    Mintcream(245, 255, 250),
    Mistyrose(255, 228, 225),
    Moccasin(255, 228, 181),
    Navajowhite(255, 222, 173),
    Navy(0, 0, 128),
    Oldlace(253, 245, 230),
    Olive(128, 128, 0),
    Olivedrab(107, 142, 35),
    Orange(255, 165, 0),
    Orangered(255, 69, 0),
    Orchid(218, 112, 214),
    Palegoldenrod(238, 232, 170),
    Palegreen(152, 251, 152),
    Paleturquoise(175, 238, 238),
    Palevioletred(219, 112, 147),
    Papayawhip(255, 239, 213),
    Peachpuff(255, 218, 185),
    Peru(205, 133, 63),
    Pink(255, 192, 203),
    Plum(221, 160, 221),
    Powderblue(176, 224, 230),
    Purple(128, 0, 128),
    Rebeccapurple(102, 51, 153),
    Red(255, 0, 0),
    Rosybrown(188, 143, 143),
    Royalblue(65, 105, 225),
    Saddlebrown(139, 69, 19),
    Salmon(250, 128, 114),
    Sandybrown(244, 164, 96),
    Seagreen(46, 139, 87),
    Seashell(255, 245, 238),
    Sienna(160, 82, 45),
    Silver(192, 192, 192),
    Skyblue(135, 206, 235),
    Slateblue(106, 90, 205),
    Slategray(112, 128, 144),
    Slategrey(112, 128, 144),
    Snow(255, 250, 250),
    Springgreen(0, 255, 127),
    Steelblue(70, 130, 180),
    Tan(210, 180, 140),
    Teal(0, 128, 128),
    Thistle(216, 191, 216),
    Tomato(255, 99, 71),
    Turquoise(64, 224, 208),
    Violet(238, 130, 238),
    Wheat(245, 222, 179),
    White(255, 255, 255),
    Whitesmoke(245, 245, 245),
    Yellow(255, 255, 0),
    Yellowgreen(154, 205, 50);

    /** Jetpack Compose の[Color]にする。 */
    fun toColor(): Color = Color(red = r, green = g, blue = b)
}

/** 色を変更するダイアログのページ */
private enum class ColorPickerPage(val label: String) {
    /** 色一覧リスト */
    ColorList("色を選ぶ"),

    /** 色作成画面 */
    CreateColor("色を作る")
}

/**
 * 色を選択するダイアログ
 *
 * @param currentColor 現在の色
 * @param onDismissRequest ダイアログを閉じるリクエストが来た時
 * @param onChange 色が変化したら呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerDialog(
    currentColor: Color = Color.White,
    onDismissRequest: () -> Unit,
    onChange: (Color) -> Unit
) {
    val currentPage = remember { mutableStateOf(ColorPickerPage.ColorList) }

    AlertDialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier.padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {

                Text(
                    text = "色の設定",
                    fontSize = 24.sp
                )

                ColorPickerTab(
                    currentPage = currentPage.value,
                    onClick = { page -> currentPage.value = page }
                )

                when (currentPage.value) {
                    ColorPickerPage.ColorList -> {
                        CssNamedColorList(
                            modifier = Modifier.weight(1f),
                            currentColor = currentColor,
                            onSelectColor = { cssNamedColors -> onChange(cssNamedColors.toColor()) }
                        )
                    }

                    ColorPickerPage.CreateColor -> {
                        CreateColor(
                            modifier = Modifier.weight(1f),
                            currentColor = currentColor,
                            onChange = { color -> onChange(color) }
                        )
                    }
                }

                SelectColorStatus(currentColor = currentColor)

                DialogButton(
                    modifier = Modifier.align(alignment = Alignment.End),
                    onChannelClick = onDismissRequest,
                    onDoneClick = onDismissRequest
                )
            }
        }
    }
}

/**
 * ダイアログの下に表示するボタン
 *
 * @param modifier [Modifier]
 * @param onDoneClick 確定押したら
 * @param onChannelClick キャンセル押したら
 */
@Composable
private fun DialogButton(
    modifier: Modifier = Modifier,
    onDoneClick: () -> Unit,
    onChannelClick: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        TextButton(onClick = onChannelClick) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_close_24), contentDescription = null)
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = "戻る")
        }

        OutlinedButton(onClick = onDoneClick) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_done_24), contentDescription = null)
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = "確定")
        }
    }
}

/**
 * 色を RGB / カラーコードから作る画面
 *
 * @param modifier [Modifier]
 * @param currentColor 選択中の色
 * @param onChange 色変更時
 */
@Composable
private fun CreateColor(
    modifier: Modifier = Modifier,
    currentColor: Color,
    onChange: (Color) -> Unit
) {
    // 0 から 1。なので 0xFF (255) をかけるといい
    val red = remember(currentColor) { mutableFloatStateOf(currentColor.red) }
    val green = remember(currentColor) { mutableFloatStateOf(currentColor.green) }
    val blue = remember(currentColor) { mutableFloatStateOf(currentColor.blue) }

    // カラーコード
    val hexColorCode = remember(currentColor) { mutableStateOf(ColorTool.toHexColorCode(currentColor)) }

    fun change() {
        onChange(Color(red = red.floatValue, green = green.floatValue, blue = blue.floatValue))
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        ColorSlider(
            label = "赤",
            value = red.floatValue,
            onChange = {
                red.floatValue = it
                change()
            }
        )

        ColorSlider(
            label = "緑",
            value = green.floatValue,
            onChange = {
                green.floatValue = it
                change()
            }
        )

        ColorSlider(
            label = "青",
            value = blue.floatValue,
            onChange = {
                blue.floatValue = it
                change()
            }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = hexColorCode.value,
            label = { Text(text = "カラーコード") },
            onValueChange = { text ->
                hexColorCode.value = text
                ColorTool.parseColor(text)
                    ?.also { color -> onChange(color) }
            }
        )
    }
}

/**
 * RGB のシークバー。スライダー
 *
 * @param modifier [Modifier]
 * @param label 文言
 * @param value 値
 * @param onChange 値が変化したら
 */
@Composable
private fun ColorSlider(
    modifier: Modifier = Modifier,
    label: String,
    value: Float,
    onChange: (Float) -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label)
        Slider(modifier = Modifier.weight(1f), value = value, onValueChange = onChange)
        Text(text = (value * 0xFF).toInt().toString())
    }
}

/**
 * タブのコンポーネント
 *
 * @param modifier [Modifier]
 * @param currentPage 選択中の[ColorPickerPage]
 * @param onClick 押した時
 */
@Composable
private fun ColorPickerTab(
    modifier: Modifier = Modifier,
    currentPage: ColorPickerPage,
    onClick: (ColorPickerPage) -> Unit
) {
    val selectIndex = ColorPickerPage.entries.indexOf(currentPage)

    TabRow(
        modifier = modifier,
        selectedTabIndex = selectIndex,
        divider = { /* do nothing */ },
        indicator = { tabPositions ->
            Box(
                modifier = Modifier
                    .tabIndicatorOffset(tabPositions[selectIndex])
                    .padding(horizontal = 20.dp)
                    .height(3.dp)
                    .background(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            )
        }
    ) {
        ColorPickerPage.entries.forEach { page ->
            Tab(
                selected = page == currentPage,
                onClick = { onClick(page) }
            ) {
                Text(
                    modifier = Modifier.padding(vertical = 5.dp),
                    text = page.label
                )
            }
        }
    }
}

/**
 * 選択中の色を RGB / カラーコードで表示するやつ
 *
 * @param modifier [Modifier]
 * @param currentColor 選択中の色
 */
@Composable
private fun SelectColorStatus(
    modifier: Modifier = Modifier,
    currentColor: Color
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // RGB の配列
        val rgbValueList = ColorTool.toRgbList(currentColor)

        ColorItem(color = currentColor)

        Column {

            Row {
                listOf("R: ${rgbValueList[0]}", "G: ${rgbValueList[1]}", "B: ${rgbValueList[2]}").forEach { text ->
                    Text(
                        modifier = Modifier.weight(1f),
                        text = text
                    )
                }
            }

            Text(text = ColorTool.toHexColorCode(currentColor))
        }
    }
}

/**
 * CSS の named-color を表示する。
 * シャンペンサイダー に見えてきた...
 *
 * @param modifier [Modifier]
 * @param currentColor 選択中の色
 * @param onSelectColor 押した時
 */
@Composable
private fun CssNamedColorList(
    modifier: Modifier = Modifier,
    currentColor: Color,
    onSelectColor: (CssNamedColors) -> Unit
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(5),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(CssNamedColors.entries) { cssNamedColors ->
            val color = cssNamedColors.toColor()

            Box(contentAlignment = Alignment.Center) {

                ColorItem(
                    modifier = Modifier.size(50.dp),
                    color = color,
                    onClick = { onSelectColor(cssNamedColors) }
                )

                // 選択中の場合
                if (color == currentColor) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_outline_done_24),
                        contentDescription = null
                    )
                }
            }
        }

        item(span = { GridItemSpan(this.maxLineSpan) }) {
            Text(
                modifier = Modifier.padding(vertical = 5.dp),
                text = "この色たちは、CSS named-color と同じです。"
            )
        }
    }
}

/**
 * 色の選択とプレビューで使ってる
 *
 * @param modifier [Modifier]
 * @param color 色
 * @param onClick 押した時
 */
@Composable
private fun ColorItem(
    modifier: Modifier = Modifier,
    color: Color,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.size(50.dp),
        color = color,
        border = BorderStroke(width = 2.dp, color = LocalContentColor.current),
        shape = RoundedCornerShape(10.dp),
        onClick = onClick,
        content = { /* do nothing */ }
    )
}