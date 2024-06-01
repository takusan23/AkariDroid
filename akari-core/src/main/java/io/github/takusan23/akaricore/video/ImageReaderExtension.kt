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

/**
 * Qualcomm Snapdragon で、一部の動画を入れた場合（width = 268 / height = 480）、動画のフレームが乱れてしまう
 * 16 の倍数にしても修正出来ない場合の最終手段。でもこれを使うと[toFixImageReaderSupportValue]で直った動画がまた再発してしまうので困っている。
 *
 * @param width はば
 * @param height たかさ
 * @return 丸めたサイズ。これなら少なくとも ImageReader は動くはず
 */
internal fun nearestImageReaderAvailableSize(width: Int, height: Int): Int {
    // Surface 経由で Bitmap が取れる ImageReader つくる 。本来は、元動画の縦横サイズを入れるべきである。
    // しかし、一部の縦動画（画面録画）を入れるとどうしても乱れてしまう。
    // Google Pixel の場合は、縦と横にを 16 で割り切れる数字にすることで修正できたが、Snapdragon は直らなかった。
    // ・・・・
    // Snapdragon がどうやっても直んないので、別の方法を取る。
    // 色々いじってみた結果、Snapdragon も 320 / 480 / 720 / 1280 / 1920 / 2560 / 3840 とかのキリがいい数字は何故か動くので、もうこの値を縦と横に適用する。
    // その後元あった Bitmap のサイズに戻す。もう何もわからない。なんだよこれ・・
    val maxSize = maxOf(width, height)
    val imageReaderSize = when {
        maxSize <= 320 -> 320
        maxSize <= 480 -> 480
        maxSize <= 720 -> 720
        maxSize <= 1280 -> 1280
        maxSize <= 1920 -> 1920
        maxSize <= 2560 -> 2560
        maxSize <= 3840 -> 3840
        else -> 1920 // 何もなければ適当に Full HD
    }
    return imageReaderSize
}

/**
 * 数字を 16 で割り切れる数字に変換する
 * Qualcomm Snapdragon も Google Tensor も 16 で割り切れる数字じゃないと動画のフレームが乱れてしまった
 */
internal fun Int.toFixImageReaderSupportValue() = (this / 16) * 16