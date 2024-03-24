package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R

/**
 * メニューの隣のフローティングしている追加バー
 */
@Composable
fun FloatingAddBar(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(70.dp),
        color = MaterialTheme.colorScheme.secondary,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 3.dp
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FloatingAddBarItem(
                title = "追加",
                iconResId = R.drawable.ic_outlined_add_24px,
                onClick = { }
            )

            VerticalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                thickness = 3.dp,
            )

            repeat(3) {
                FloatingAddBarItem(
                    title = "テキスト",
                    iconResId = R.drawable.ic_outline_text_fields_24,
                    onClick = { }
                )
            }
        }
    }
}

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
            Text(text = title)
        }
    }
}