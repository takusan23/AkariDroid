package io.github.takusan23.akaridroid.ui.component.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.AddRenderItemMenu
import io.github.takusan23.akaridroid.ui.component.AddRenderItemMenuResult
import io.github.takusan23.akaridroid.ui.component.rememberRenderItemCreator

/**
 * メニューの隣のフローティングしている追加バー。
 * 3つまでメニューがおけます。[AddRenderItemMenu]。
 *
 * @param modifier [Modifier]
 * @param onOpenMenu 追加ボトムシートを開く
 * @param recommendedMenuList 出したいメニュー[AddRenderItemMenu]
 * @param onRecommendMenuClick メニューを押した時
 */
@Composable
fun FloatingAddRenderItemBar(
    modifier: Modifier = Modifier,
    onOpenMenu: () -> Unit,
    recommendedMenuList: List<AddRenderItemMenu>,
    onRecommendMenuClick: (AddRenderItemMenuResult) -> Unit
) {
    val creator = rememberRenderItemCreator(onResult = onRecommendMenuClick)

    Surface(
        modifier = modifier.height(70.dp),
        color = MaterialTheme.colorScheme.tertiary,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 3.dp
    ) {
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {

            FloatingAddBarItem(
                title = stringResource(id = R.string.video_edit_floating_add_bar_add),
                iconResId = R.drawable.ic_outlined_add_24px,
                onClick = onOpenMenu
            )

            VerticalDivider(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .clip(RoundedCornerShape(2.dp)),
                thickness = 4.dp,
                color = LocalContentColor.current
            )

            recommendedMenuList.forEach { menu ->
                FloatingAddBarItem(
                    title = stringResource(id = menu.labelResId),
                    iconResId = menu.iconResId,
                    onClick = { creator.create(menu) }
                )
            }
        }
    }
}

/**
 * フローティングバーのボタン
 *
 * @param modifier [Modifier]
 * @param title なまえ
 * @param iconResId　アイコン
 * @param onClick 押した時
 */
@Composable
fun FloatingAddBarItem(
    modifier: Modifier = Modifier,
    title: String,
    iconResId: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null
            )
            Text(
                text = title,
                maxLines = 1
            )
        }
    }
}