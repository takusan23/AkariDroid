package io.github.takusan23.akaridroid.canvasrender.itemrender

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.Matrix
import androidx.core.net.toUri
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.graphics.AkariGraphicsSurfaceTexture
import io.github.takusan23.akaricore.graphics.mediacodec.AkariVideoDecoder
import io.github.takusan23.akaridroid.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * とりあえずここに置かせて；；
 * 再生位置を渡して、素材の再生位置を出す。再生速度、オフセットが考慮される。
 *
 * @param currentPositionMs 動画全体の再生位置
 * @return 動画素材の取り出すフレームの時間
 */
fun RenderData.CanvasItem.Video.calcVideoFramePositionMs(currentPositionMs: Long): Long {
    // 素材の開始位置から見た positionMs にする
    val currentPositionMsInItem = currentPositionMs - displayTime.startMs
    // 再生速度を考慮した positionMsInItem にする
    val currentPositionMsInPlaybackSpeed = (currentPositionMsInItem * displayTime.playbackSpeed).toLong()
    // オフセット、読み飛ばす分を考慮
    // offset は再生速度考慮している
    return currentPositionMsInPlaybackSpeed + displayOffset.offsetFirstMs
}

/** 動画を描画する TODO VideoRenderer と紛らわしい */
class VideoRender(
    private val context: Context,
    private val video: RenderData.CanvasItem.Video,
    texId: Int
) : BaseItemRender(), DrawSurfaceTexture {

    private var akariVideoDecoder: AkariVideoDecoder? = null

    /** デコードした動画フレームの出力先。OpenGL ES で使えるテクスチャ */
    override var akariGraphicsSurfaceTexture = AkariGraphicsSurfaceTexture(texId)

    override val layerIndex: Int
        get() = video.layerIndex

    override suspend fun prepare() = withContext(Dispatchers.IO) {
        // クロマキーする場合
        val isEnableChromaKey = video.chromaKeyColor != null
        akariVideoDecoder = AkariVideoDecoder().apply {
            prepare(
                input = when (video.filePath) {
                    is RenderData.FilePath.File -> File(video.filePath.filePath).toAkariCoreInputOutputData()
                    is RenderData.FilePath.Uri -> video.filePath.uriPath.toUri().toAkariCoreInputOutputData(context)
                },
                outputSurface = akariGraphicsSurfaceTexture.surface,
                chromakeyThreshold = if (isEnableChromaKey) CHROMAKEY_THRESHOLD else null,
                chromakeyColor = if (isEnableChromaKey) video.chromaKeyColor!! else null
            )
        }
    }

    override suspend fun preDraw(durationMs: Long, currentPositionMs: Long) {
        super.preDraw(durationMs, currentPositionMs)

        // 動画のフレーム取得は時間がかかるので、preDraw で取得する
        val akariVideoDecoder = akariVideoDecoder ?: return

        // 再生速度、オフセットを考慮した、動画のフレーム取得時間を出す
        val framePositionMs = video.calcVideoFramePositionMs(currentPositionMs = currentPositionMs)

        // 指定位置のフレームを取得するためシークする
        akariVideoDecoder.seekTo(framePositionMs)
    }

    override suspend fun draw(canvas: Canvas, drawFrame: Bitmap, durationMs: Long, currentPositionMs: Long) = withContext(Dispatchers.IO) {
//        val preLoadBitmap = preLoadBitmap ?: return@withContext
//        val (x, y) = video.position
//        canvas.drawBitmap(preLoadBitmap, x, y, paint)
    }


    override fun draw(mvpMatrix: FloatArray, outputWidth: Int, outputHeight: Int) {
        val (x, y) = video.position
        val (width, height) = video.size

        // 0..1 の範囲にする
        val scaleX = width / outputWidth.toFloat()
        val scaleY = height / outputHeight.toFloat()
        val transX = ((x / outputWidth) * 2) - 1
        val transY = ((y / outputHeight) * 2) - 1

        // 行列の適用は多分順番がある
        Matrix.scaleM(mvpMatrix, 0, scaleX, scaleY, 1f)
        Matrix.translateM(mvpMatrix, 0, transX, -transY, 1f)
    }

    override fun destroy() {
        akariVideoDecoder?.destroy()
    }

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return renderItem == video
    }

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean = currentPositionMs in video.displayTime

    companion object {
        private const val CHROMAKEY_THRESHOLD = 0.3f // TODO ユーザー入力で変更できるようにする
    }
}