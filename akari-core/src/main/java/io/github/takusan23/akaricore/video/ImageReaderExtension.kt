package io.github.takusan23.akaricore.video

import android.graphics.Bitmap
import android.media.ImageReader
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [ImageReader]から[Bitmap]を取り出す
 *
 * @param fixWidth サイズを変更する場合
 * @param fixHeight サイズを変更する場合
 * @return [Bitmap]
 */
internal suspend fun ImageReader.getImageReaderBitmap(
    fixWidth: Int?,
    fixHeight: Int?
): Bitmap = withContext(Dispatchers.Default) {
    val image = this@getImageReaderBitmap.acquireLatestImage()
    val width = image.width
    val height = image.height
    val buffer = image.planes.first().buffer
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(buffer)
    // 修正が必要な場合（例えば：アスペクト比を戻す）
    val resultBitmap = if (fixWidth != null && fixHeight != null) {
        bitmap.scale(fixWidth, fixHeight)
    } else {
        bitmap
    }
    // Image を close する
    image.close()
    return@withContext resultBitmap
}