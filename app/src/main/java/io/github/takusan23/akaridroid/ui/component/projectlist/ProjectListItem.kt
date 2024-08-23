package io.github.takusan23.akaridroid.ui.component.projectlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.tool.data.ProjectItem
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * プロジェクト一覧の各アイテム
 *
 * @param modifier [Modifier]
 * @param projectItem [ProjectItem]
 * @param onClick 押した時
 */
@Composable
fun ProjectListItem(
    modifier: Modifier = Modifier,
    projectItem: ProjectItem,
    onClick: (ProjectItem) -> Unit,
    onMenuClick: (ProjectItem) -> Unit
) {
    val lastModifiedFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()) }
    val durationFormat = remember { SimpleDateFormat("mm:ss", Locale.getDefault()) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = { onClick(projectItem) }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = projectItem.projectName,
                    fontSize = 20.sp
                )

                Text(text = lastModifiedFormat.format(projectItem.lastModifiedDate))
            }

            Text(text = durationFormat.format(projectItem.videoDurationMs))

            IconButton(onClick = { onMenuClick(projectItem) }) {
                Icon(painter = painterResource(id = R.drawable.ic_outlined_more_vert_24), contentDescription = null)
            }
        }
    }
}