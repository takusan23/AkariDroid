package io.github.takusan23.akaridroid.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.video.VideoFrameBitmapExtractor
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import java.io.File

/**
 * クロマキー合成で、何色を抜くかを選択するダイアログ
 *
 * @param filePath 動画ファイルのパス。[RenderData.FilePath]参照。
 * @param previewPositionMs プレビューの再生位置
 * @param chromakeyColor クロマキーで透過にする色
 * @param onDismissRequest ダイアログを閉じる際に呼ばれる
 * @param onChange 色を選んだら呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChromaKeyColorDialog(
    filePath: RenderData.FilePath,
    previewPositionMs: Long,
    chromakeyColor: Color,
    onDismissRequest: () -> Unit,
    onChange: (Color) -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }
    val imageComponentSize = remember { mutableStateOf<IntSize?>(null) }

    // 動画からフレーム Bitmap 取り出す
    LaunchedEffect(key1 = filePath) {
        val fastExtractor = VideoFrameBitmapExtractor()
        fastExtractor.prepareDecoder(
            input = when (filePath) {
                is RenderData.FilePath.File -> File(filePath.filePath).toAkariCoreInputOutputData()
                is RenderData.FilePath.Uri -> filePath.uriPath.toUri().toAkariCoreInputOutputData(context)
            }
        )
        bitmap.value = fastExtractor.getVideoFrameBitmap(seekToMs = previewPositionMs)
        fastExtractor.destroy()
    }

    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier.padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {

                Text(
                    text = stringResource(id = R.string.dialog_chromakeycolor_title),
                    fontSize = 24.sp
                )

                Text(text = stringResource(id = R.string.dialog_chromakeycolor_description))

                if (bitmap.value != null) {
                    Image(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { imageComponentSize.value = it }
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    imageComponentSize.value ?: return@detectTapGestures

                                    // Bitmap の位置と押した位置はサイズが違うので、パーセントを出す
                                    val percentX = offset.x / imageComponentSize.value!!.width
                                    val percentY = offset.y / imageComponentSize.value!!.height
                                    val bitmapX = (bitmap.value!!.width * percentX).toInt()
                                    val bitmapY = (bitmap.value!!.height * percentY).toInt()

                                    // 押した位置の色を取り出す
                                    val selectColor = bitmap.value!!.getPixel(bitmapX, bitmapY)
                                    onChange(Color(selectColor))
                                }
                            },
                        bitmap = bitmap.value!!.asImageBitmap(),
                        contentDescription = null
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1.7f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                SelectColorPreview(currentColor = chromakeyColor)

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
            Text(text = stringResource(id = R.string.dialog_colorpicker_cancel))
        }

        OutlinedButton(onClick = onDoneClick) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_done_24), contentDescription = null)
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(id = R.string.dialog_colorpicker_confirm))
        }
    }
}
