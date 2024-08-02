package io.github.takusan23.akaridroid.canvasrender.itemrender

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.Matrix
import androidx.core.net.toUri
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.graphics.AkariGraphicsTextureRenderer
import io.github.takusan23.akaricore.graphics.AkariGraphicsVideoTexture2
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
    private val akariGraphicsVideoTexture = AkariGraphicsVideoTexture2(initTexId)

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
        // サポート終了
    }

    override suspend fun draw(textureRenderer: AkariGraphicsTextureRenderer, durationMs: Long, currentPositionMs: Long) {
        akariGraphicsVideoTexture.seekToNext(currentPositionMs)
        textureRenderer.drawSurfaceTexture(akariGraphicsVideoTexture.akariSurfaceTexture) { mvpMatrix ->
            val (x, y) = video.position
            val (width, height) = video.size
            // 位置調整
            // OpenGL は 0, 0 が真ん中になる？ので、-1 から 1 の範囲にする。
            // 原点が左上じゃなくて真ん中なので、x と y も調整する。
            // 2倍して -1 すると、0 から 2 の範囲が -1 から 1 の範囲になる。
            val xPos = (x + (width / 2)) / textureRenderer.width.toFloat()
            val yPos = (y + (height / 2)) / textureRenderer.height.toFloat()
            val openGlXPos = (xPos * 2) - 1
            val openGlYPos = (yPos * 2) - 1
            // y はテクスチャ座標が反転しているため意図的にマイナスの値
            Matrix.translateM(mvpMatrix, 0, openGlXPos, -openGlYPos, 0f)
            // リサイズする場合
            val (videoWidth, videoHeight) = akariGraphicsVideoTexture.videoSize!!
            Matrix.scaleM(mvpMatrix, 0, width / videoWidth.toFloat(), height / videoHeight.toFloat(), 1f)
        }
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