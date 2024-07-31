package io.github.takusan23.akaridroid.canvasrender.itemrender

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.Matrix
import androidx.core.net.toUri
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.graphics.AkariGraphicsTextureRenderer
import io.github.takusan23.akaricore.graphics.AkariGraphicsVideoTexture
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

/** 動画を描画する */
class VideoRender(
    initTexId: Int,
    private val context: Context,
    private val video: RenderData.CanvasItem.Video
) : BaseItemRender() {

    /** 動画をデコードして、フレームを[AkariGraphicsTextureRenderer]へ描画するやつ */
    private val akariGraphicsVideoTexture = AkariGraphicsVideoTexture(initTexId)

    override val layerIndex: Int
        get() = video.layerIndex

    override suspend fun prepare() = withContext(Dispatchers.IO) {
        // クロマキーする場合
        val isEnableChromaKey = video.chromaKeyColor != null
        akariGraphicsVideoTexture.prepareDecoder(
            input = when (video.filePath) {
                is RenderData.FilePath.File -> File(video.filePath.filePath).toAkariCoreInputOutputData()
                is RenderData.FilePath.Uri -> video.filePath.uriPath.toUri().toAkariCoreInputOutputData(context)
            },
            chromakeyThreshold = if (isEnableChromaKey) CHROMAKEY_THRESHOLD else null,
            chromakeyColor = if (isEnableChromaKey) video.chromaKeyColor!! else null
        )
    }

    override suspend fun preDraw(durationMs: Long, currentPositionMs: Long) = withContext(Dispatchers.IO) {
        super.preDraw(durationMs, currentPositionMs)
    }

    override suspend fun draw(canvas: Canvas, drawFrame: Bitmap, durationMs: Long, currentPositionMs: Long) = withContext(Dispatchers.IO) {
    }

    override suspend fun draw(textureRenderer: AkariGraphicsTextureRenderer, durationMs: Long, currentPositionMs: Long) {
        akariGraphicsVideoTexture.draw(
            akariGraphicsTextureRenderer = textureRenderer,
            seekToMs = currentPositionMs,
            onTransform = { mvpMatrix ->
                // リサイズする場合
                val (width, height) = video.size
                Matrix.scaleM(mvpMatrix, 0, width / akariGraphicsVideoTexture.videoWidth.toFloat(), height / akariGraphicsVideoTexture.videoHeight.toFloat(), 1f)
                // 位置調整
                val (x, y) = video.position
                Matrix.translateM(mvpMatrix, 0, x / textureRenderer.width.toFloat(), y / textureRenderer.height.toFloat(), 1f)
            }
        )
    }

    override fun destroy() {
        akariGraphicsVideoTexture.destroy()
    }

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return renderItem == video
    }

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean = currentPositionMs in video.displayTime

    companion object {
        private const val CHROMAKEY_THRESHOLD = 0.3f // TODO ユーザー入力で変更できるようにする
    }
}