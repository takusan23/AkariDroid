package io.github.takusan23.akaridroid.ui.component.projectlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.encoder.AkariCoreEncoder
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = { onClick(projectItem) }
    ) {
        CommonProjectListItem(
            modifier = Modifier.padding(10.dp),
            projectItem = projectItem,
            onMenuClick = onMenuClick
        )
    }
}

/**
 * エンコード中のリストアイテム
 *
 * @param modifier [Modifier]
 * @param projectItem [ProjectItem]
 * @param encodeStatus エンコード状態
 * @param onCancel キャンセル時
 */
@Composable
fun EncodingListItem(
    modifier: Modifier = Modifier,
    projectItem: ProjectItem,
    encodeStatus: AkariCoreEncoder.EncodeStatus,
    onCancel: () -> Unit
) {
    Surface(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            CommonProjectListItem(
                projectItem = projectItem,
                isEnableMenu = false
            )

            EncodingStatusInfo(
                encodeStatus = encodeStatus,
                onCancel = onCancel
            )
        }
    }
}

/** エンコード中の情報 */
@Composable
private fun EncodingStatusInfo(
    modifier: Modifier = Modifier,
    encodeStatus: AkariCoreEncoder.EncodeStatus,
    onCancel: () -> Unit
) {
    val simpleDateFormat = remember { SimpleDateFormat("mm:ss.SSS", Locale.getDefault()) }

    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Text(text = stringResource(id = R.string.component_encode_status_title))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator()

                when (encodeStatus) {
                    is AkariCoreEncoder.EncodeStatus.Progress -> Column {
                        Text(text = stringResource(id = R.string.component_encode_status_description))
                        Text(text = "${stringResource(id = R.string.component_encode_status_encoded_time)} : ${simpleDateFormat.format(encodeStatus.encodePositionMs)}")
                        Text(text = "${stringResource(id = R.string.component_encode_status_total_time)} : ${simpleDateFormat.format(encodeStatus.durationMs)}")
                    }

                    is AkariCoreEncoder.EncodeStatus.Mixing -> Text(text = stringResource(id = R.string.component_encode_status_mixing))
                    is AkariCoreEncoder.EncodeStatus.MoveFile -> Text(text = stringResource(id = R.string.component_encode_status_move_file))
                }
            }

            OutlinedButton(
                modifier = Modifier.align(Alignment.End),
                onClick = onCancel
            ) {
                Text(text = stringResource(id = R.string.component_encode_status_cancel))
            }
        }
    }
}

/** [ProjectListItem]、[ProjectListItem]共通部分 */
@Composable
private fun CommonProjectListItem(
    modifier: Modifier = Modifier,
    projectItem: ProjectItem,
    onMenuClick: (ProjectItem) -> Unit = {},
    isEnableMenu: Boolean = true
) {
    val lastModifiedFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()) }
    val durationFormat = remember { SimpleDateFormat("mm:ss", Locale.getDefault()) }

    Row(
        modifier = modifier,
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

        if (isEnableMenu) {
            IconButton(onClick = { onMenuClick(projectItem) }) {
                Icon(painter = painterResource(id = R.drawable.ic_outlined_more_vert_24), contentDescription = null)
            }
        }
    }
}