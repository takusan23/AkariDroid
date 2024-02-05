package io.github.takusan23.akaridroid.v2.canvasrender.itemrender

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.core.graphics.scale
import io.github.takusan23.akaridroid.v2.canvasrender.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 動画を描画する */
class VideoRender(
    private val video: RenderData.CanvasItem.Video
) : ItemRenderInterface {

    /** Bitmap を取り出す */
    private var mediaMetadataRetriever: MediaMetadataRetriever? = null

    /** 動画の総フレーム数 */
    private var videoTotalFrameCount: Int? = null

    /**  */
    private var preLoadBitmap: Bitmap? = null

    private val paint = Paint()

    override val displayTime: RenderData.DisplayTime
        get() = video.displayTime

    override suspend fun prepare() = withContext(Dispatchers.IO) {
        mediaMetadataRetriever = MediaMetadataRetriever().apply {
            setDataSource(video.filePath)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            videoTotalFrameCount = mediaMetadataRetriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toInt()
        }
    }

    override suspend fun preDraw(canvas: Canvas, durationMs: Long, currentPositionMs: Long) = withContext(Dispatchers.IO) {
        super.preDraw(canvas, durationMs, currentPositionMs)
        println("currentPositionMs = $currentPositionMs")
        // 動画のフレーム取得は時間がかかるので、preDraw で取得する
        val mediaMetadataRetriever = mediaMetadataRetriever ?: return@withContext

        // カットする場合は考慮した時間を
        val framePositionMs = currentPositionMs - video.displayTime.startMs
        val cropIncludedFramePositionMs = framePositionMs - (video.cropTimeCrop?.cropStartMs ?: 0)

        // getFrameAtIndex が使える場合は使う。こっちのほうが高速
        preLoadBitmap = if (videoTotalFrameCount != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // 表示すべきフレームを計算する
            val currentFrameIndex = ((cropIncludedFramePositionMs.toFloat() / durationMs.toFloat()) * videoTotalFrameCount!!).toInt()
            mediaMetadataRetriever.getFrameAtIndex(currentFrameIndex, MediaMetadataRetriever.BitmapParams())
        } else {
            // 使えない場合は遅いけどこっちで
            mediaMetadataRetriever.getFrameAtTime(cropIncludedFramePositionMs * 1_000)
        }?.let { origin ->
            // リサイズする場合
            if (video.size != null) {
                val (width, height) = video.size
                origin.scale(width, height)
            } else {
                origin
            }
        }
    }

    override suspend fun draw(canvas: Canvas, durationMs: Long, currentPositionMs: Long) = withContext(Dispatchers.IO) {
        val preLoadBitmap = preLoadBitmap ?: return@withContext
        val (x, y) = video.position
        canvas.drawBitmap(preLoadBitmap, x, y, paint)
    }

    override suspend fun destroy() {
        mediaMetadataRetriever?.release()
    }

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return renderItem != video
    }

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean {
        // 範囲内にいること
        if (currentPositionMs !in video.displayTime) {
            return false
        }
        val framePositionMs = currentPositionMs - video.displayTime.startMs

        // 動画をカットする場合で、カットした時間外の場合
        if (video.cropTimeCrop != null && framePositionMs !in video.cropTimeCrop) {
            return false
        }

        return true
    }
}