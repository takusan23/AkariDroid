package io.github.takusan23.akaridroid.canvasrender.itemrender

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.scale
import androidx.core.net.toUri
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.video.VideoFrameBitmapExtractor
import io.github.takusan23.akaridroid.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 動画を描画する */
class VideoRender(
    private val context: Context,
    private val video: RenderData.CanvasItem.Video
) : ItemRenderInterface {

    /** Bitmap を取り出す */
    private var videoFrameBitmapExtractor: VideoFrameBitmapExtractor? = null

    /** [preDraw]したときに取得する Bitmap */
    private var preLoadBitmap: Bitmap? = null

    private val paint = Paint()

    override val layerIndex: Int
        get() = video.layerIndex

    override suspend fun prepare() = withContext(Dispatchers.IO) {
        // ここで VideoFrameBitmapExtractor を起動してしまうと、MediaCodec のハードウェアデコーダーが足りなくなってしまう
        // TODO 使わなくなった VideoFrameBitmapExtractor を破棄するような処理。というか必要なときのみデコーダーを起動するような処理。
    }

    override suspend fun preDraw(canvas: Canvas, durationMs: Long, currentPositionMs: Long) = withContext(Dispatchers.IO) {
        super.preDraw(canvas, durationMs, currentPositionMs)

        // クロマキーする場合
        val isEnableChromaKey = video.chromaKeyColor != null
        if (videoFrameBitmapExtractor == null) {
            videoFrameBitmapExtractor = VideoFrameBitmapExtractor().apply {
                // Uri と File で分岐
                prepareDecoder(
                    input = when (video.filePath) {
                        is RenderData.FilePath.File -> File(video.filePath.filePath).toAkariCoreInputOutputData()
                        is RenderData.FilePath.Uri -> video.filePath.uriPath.toUri().toAkariCoreInputOutputData(context)
                    },
                    chromakeyThreshold = if (isEnableChromaKey) CHROMAKEY_THRESHOLD else null,
                    chromakeyColor = if (isEnableChromaKey) video.chromaKeyColor!! else null
                )
            }
        }

        // 動画のフレーム取得は時間がかかるので、preDraw で取得する
        val videoFrameBitmapExtractor = videoFrameBitmapExtractor ?: return@withContext

        // カットする場合は考慮した時間を
        val framePositionFromCurrentPositionMs = currentPositionMs - video.displayTime.startMs
        val includeOffsetFramePositionMs = framePositionFromCurrentPositionMs + video.displayOffset.offsetFirstMs
        preLoadBitmap = videoFrameBitmapExtractor.getVideoFrameBitmap(includeOffsetFramePositionMs)?.let { origin ->
            // リサイズする場合
            val (width, height) = video.size
            origin.scale(width, height)
        }
    }

    override suspend fun draw(canvas: Canvas, durationMs: Long, currentPositionMs: Long) = withContext(Dispatchers.IO) {
        val preLoadBitmap = preLoadBitmap ?: return@withContext
        val (x, y) = video.position
        canvas.drawBitmap(preLoadBitmap, x, y, paint)
    }

    override fun destroy() {
        videoFrameBitmapExtractor?.destroy()
    }

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return renderItem == video
    }

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean = currentPositionMs in video.displayTime

    companion object {
        private const val CHROMAKEY_THRESHOLD = 0.3f // TODO ユーザー入力で変更できるようにする
    }
}