package io.github.takusan23.akaridroid.v2.canvasrender.itemrender

import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import io.github.takusan23.akaridroid.v2.canvasrender.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 動画を描画する */
class VideoRender(
    private val video: RenderData.RenderItem.Video
) : ItemRenderInterface {

    /** Bitmap を取り出す */
    private var mediaMetadataRetriever: MediaMetadataRetriever? = null

    private val paint = Paint()

    override val displayTime: RenderData.DisplayTime
        get() = video.displayTime

    override suspend fun prepare() = withContext(Dispatchers.IO) {
        mediaMetadataRetriever = MediaMetadataRetriever().apply {
            setDataSource(video.filePath)
        }
    }

    override suspend fun draw(canvas: Canvas, currentPositionMs: Long) = withContext(Dispatchers.Default) {
        val framePosition = video.displayTime.startMs - currentPositionMs
        val (x, y) = video.position
        val bitmap = mediaMetadataRetriever?.getFrameAtTime(framePosition * 1_000) ?: return@withContext
        canvas.drawBitmap(bitmap, x, y, paint)
    }

    override suspend fun destroy() {
        mediaMetadataRetriever?.release()
    }

    override suspend fun isEquals(renderItem: RenderData.RenderItem): Boolean {
        return renderItem != video
    }
}