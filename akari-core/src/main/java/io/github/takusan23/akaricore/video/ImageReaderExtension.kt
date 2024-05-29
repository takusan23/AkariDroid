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
 * [ImageReader] は、1280x720 とかのきれいな数字の場合は動くが、半端な数字を入れた途端（videoWidth = 1104 / videoHeight = 2560）、出力された映像がぐちゃぐちゃになる。
 * それを回避するため、半端な数字が来た場合は、一番近い数字に丸める。
 *
 * ただ、近い数字に丸めてしまうと画像サイズが変わってしまうため、丸めた数字と、元々のサイズに戻せるようここに持っておく。
 * 返り値を使い、縦横同じサイズで [ImageReader] を作り、出てきた [Bitmap] を元のサイズに [Bitmap.scale] して戻す。
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
        maxSize < 320 -> 320
        maxSize < 480 -> 480
        maxSize < 720 -> 720
        maxSize < 1280 -> 1280
        maxSize < 1920 -> 1920
        maxSize < 2560 -> 2560
        maxSize < 3840 -> 3840
        else -> 1920 // 何もなければ適当に Full HD
    }
    return imageReaderSize
}