package io.github.takusan23.akaridroid.canvasrender.itemrender

import android.content.Context
import android.opengl.Matrix
import androidx.core.net.toUri
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.graphics.AkariGraphicsSurfaceTexture
import io.github.takusan23.akaricore.graphics.mediacodec.AkariVideoDecoder
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.canvasrender.itemrender.feature.DrawSurfaceTextureInterface
import io.github.takusan23.akaridroid.canvasrender.itemrender.feature.PreDrawInterface
import io.github.takusan23.akaridroid.canvasrender.itemrender.feature.ProcessorDestroyInterface
import io.github.takusan23.akaridroid.canvasrender.itemrender.feature.TimelineLifecycleRenderer
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

class VideoRenderer(
    private val context: Context,
    private val video: RenderData.CanvasItem.Video,
    texId: Int
) : TimelineLifecycleRenderer(), DrawSurfaceTextureInterface, PreDrawInterface, ProcessorDestroyInterface {

    /** 動画デコーダー */
    private var akariVideoDecoder: AkariVideoDecoder? = null

    /** デコードした動画フレームの出力先 Surface。OpenGL ES で使えるテクスチャ */
    override var akariGraphicsSurfaceTexture = AkariGraphicsSurfaceTexture(texId)

    override val layerIndex: Int
        get() = video.layerIndex

    /** クロマキーにする色。null で無効。TODO 動画以外にクロマキーしたいところがなく [DrawSurfaceTextureInterface] に置くわけにも行かない。キャストが必要で面倒 */
    val chromaKeyColorOrNull: Int?
        get() = video.chromaKeyColor

    // 動画のデコーダーは有限なので、タイムラインで必要になるまで作らない
    override suspend fun enterTimeline() {
        super.enterTimeline()
        akariVideoDecoder = AkariVideoDecoder().apply {
            prepare(
                input = when (video.filePath) {
                    is RenderData.FilePath.File -> File(video.filePath.filePath).toAkariCoreInputOutputData()
                    is RenderData.FilePath.Uri -> video.filePath.uriPath.toUri().toAkariCoreInputOutputData(context)
                },
                outputSurface = akariGraphicsSurfaceTexture.surface
            )
        }
    }

    override suspend fun leaveTimeline() {
        super.leaveTimeline()
        akariVideoDecoder?.destroy()
        akariVideoDecoder = null
    }

    override suspend fun destroyProcessorGl() {
        akariGraphicsSurfaceTexture.detachGl()
    }

    override suspend fun preDraw(durationMs: Long, currentPositionMs: Long) {
        // 動画のフレーム取得は時間がかかるので、preDraw で取得する
        val akariVideoDecoder = akariVideoDecoder ?: return
        // 再生速度、オフセットを考慮した、動画のフレーム取得時間を出す
        val framePositionMs = video.calcVideoFramePositionMs(currentPositionMs = currentPositionMs)
        // 指定位置のフレームを取得するためシークする
        akariVideoDecoder.seekTo(framePositionMs)
    }

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return renderItem == video
    }

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean {
        return currentPositionMs in video.displayTime
    }

    override fun draw(mvpMatrix: FloatArray, width: Int, height: Int) {
        val (x, y) = video.position
        val (videoWidth, videoHeight) = video.size

        // scale 0..1 の範囲にする
        val scaleX = videoWidth / width.toFloat()
        val scaleY = videoHeight / height.toFloat()
        // translate は -1..1 の範囲にする
        val halfWidth = videoWidth / 2
        val halfHeight = videoHeight / 2
        val transX = (((x + halfWidth) / width) * 2) - 1
        val transY = (((y + halfHeight) / height) * 2) - 1

        // 行列の適用は多分順番がある
        // テクスチャ座標は反転してるので負の値
        // 回転は最後かな？
        Matrix.translateM(mvpMatrix, 0, transX, -transY, 1f)
        Matrix.scaleM(mvpMatrix, 0, scaleX, scaleY, 1f)
        Matrix.rotateM(mvpMatrix, 0, video.rotation.toFloat(), 0f, 0f, 1f)
    }

    companion object {
        const val CHROMAKEY_THRESHOLD = 0.3f // TODO ユーザー入力で変更できるようにする
    }
}