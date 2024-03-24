package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * インジケーターの角がまるい Tab
 *
 * @param modifier [Modifier]
 * @param tabLabelList タブのラベル配列
 * @param currentIndex 選択中の位置
 * @param onTabSelect タブを選択したら呼ばれる
 */
@Composable
fun RoundedIndicatorTab(
    modifier: Modifier = Modifier,
    tabLabelList: List<String>,
    currentIndex: Int,
    onTabSelect: (Int) -> Unit
) {
    TabRow(
        modifier = modifier,
        selectedTabIndex = currentIndex,
        divider = { /* do nothing */ },
        indicator = { tabPositions ->
            Box(
                modifier = Modifier
                    .tabIndicatorOffset(tabPositions[currentIndex])
                    .padding(horizontal = 20.dp)
                    .height(3.dp)
                    .background(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            )
        }
    ) {
        tabLabelList.forEachIndexed { index, label ->
            Tab(
                selected = index == currentIndex,
                onClick = { onTabSelect(index) }
            ) {
                Text(
                    modifier = Modifier.padding(vertical = 5.dp),
                    text = label
                )
            }
        }
    }
}