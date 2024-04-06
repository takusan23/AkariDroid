package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * エンコード中の画面
 * 進捗を出す
 *
 * @param modifier [Modifier]
 * @param encodeStatus [AkariCoreEncoder.EncodeStatus]
 * @param onCancel キャンセルを押した時
 */
@Composable
fun EncodingStatus(
    modifier: Modifier = Modifier,
    encodeStatus: AkariCoreEncoder.EncodeStatus,
    onCancel: () -> Unit
) {
    val simpleDateFormat = remember { SimpleDateFormat("mm:ss.SSS", Locale.getDefault()) }

    OutlinedCard(modifier = modifier.padding(10.dp)) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text = stringResource(id = R.string.component_encode_status_title),
                fontSize = 24.sp
            )

            Icon(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(80.dp),
                painter = painterResource(id = R.drawable.akari_droid_icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            when (encodeStatus) {
                is AkariCoreEncoder.EncodeStatus.Progress -> Column {
                    Text(text = stringResource(id = R.string.component_encode_status_description))
                    Text(text = "${stringResource(id = R.string.component_encode_status_encoded_time)} : ${simpleDateFormat.format(encodeStatus.encodePositionMs)}")
                    Text(text = "${stringResource(id = R.string.component_encode_status_total_time)} : ${simpleDateFormat.format(encodeStatus.durationMs)}")
                }

                AkariCoreEncoder.EncodeStatus.Mixing -> Text(text = stringResource(id = R.string.component_encode_status_mixing))

                AkariCoreEncoder.EncodeStatus.MoveFile -> Text(text = stringResource(id = R.string.component_encode_status_move_file))
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
