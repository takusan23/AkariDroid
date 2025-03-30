package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R

/**
 * メモというか、補足説明をするためのカード型のメッセージ表示するやつ
 *
 * @param modifier [Modifier]
 * @param message メッセージ
 */
@Composable
fun MessageCard(
    modifier: Modifier = Modifier,
    message: String
) {
    OutlinedCard(modifier = modifier) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outlined_info_24px),
                contentDescription = null
            )

            Text(text = message)
        }
    }
}

/**
 * ボタン付き[MessageCard]
 *
 * @param modifier [Modifier]
 * @param message メッセージ
 * @param buttonText ボタンのテキスト
 * @param onClick ボタンを押したとき
 */
@Composable
fun MessageCard(
    modifier: Modifier = Modifier,
    message: String,
    buttonText: String,
    onClick: () -> Unit
) {
    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outlined_info_24px),
                    contentDescription = null
                )

                Text(text = message)
            }

            OutlinedButton(
                modifier = Modifier.align(Alignment.End),
                onClick = onClick
            ) {
                Text(text = buttonText)
            }
        }
    }
}