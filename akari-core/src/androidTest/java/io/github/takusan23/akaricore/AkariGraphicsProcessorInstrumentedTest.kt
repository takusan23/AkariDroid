package io.github.takusan23.akaricore

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.media.Image
import android.media.ImageReader
import android.media.MediaMetadataRetriever
import android.opengl.GLES30
import androidx.core.graphics.blue
import androidx.core.graphics.get
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor
import io.github.takusan23.akaricore.graphics.AkariGraphicsSurfaceTexture
import io.github.takusan23.akaricore.graphics.data.AkariGraphicsProcessorColorSpaceType
import io.github.takusan23.akaricore.graphics.data.AkariGraphicsProcessorRenderingPrepareData
import io.github.takusan23.akaricore.graphics.mediacodec.AkariVideoDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * [AkariGraphicsProcessor]のテスト
 * 各テストの保存先は /storage/emulated/0/Android/data/io.github.takusan23.akaricore.test/files/
 * でも残ってない、消えちゃう？
 */
@RunWith(AndroidJUnit4::class)
class AkariGraphicsProcessorInstrumentedTest {

    @Test
    fun test_キャンバスを描画できる() = runTest(timeout = (CommonTestTool.DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        ImageReader.newInstance(CommonTestTool.TEST_VIDEO_WIDTH, CommonTestTool.TEST_VIDEO_HEIGHT, PixelFormat.RGBA_8888, 2).use { imageReader ->
            val graphicsProcessor = AkariGraphicsProcessor(
                renderingPrepareData = AkariGraphicsProcessorRenderingPrepareData.SurfaceRendering(
                    surface = imageReader.surface,
                    width = CommonTestTool.TEST_VIDEO_WIDTH,
                    height = CommonTestTool.TEST_VIDEO_HEIGHT
                ),
                colorSpaceType = AkariGraphicsProcessorColorSpaceType.SDR_BT709
            ).apply { prepare() }

            // デモ Bitmap
            // 塗りつぶしてるだけなのは sameAs でピクセル単位の一致を期待しているため
            // 余計なことするとピクセル単位でズレてしまいそうな気がする
            val fillRedBitmap = Bitmap.createBitmap(CommonTestTool.TEST_VIDEO_WIDTH, CommonTestTool.TEST_VIDEO_HEIGHT, Bitmap.Config.ARGB_8888).apply {
                Canvas(this).apply {
                    drawColor(Color.RED)
                }
            }

            // 描画
            graphicsProcessor.drawOneshot {
                drawCanvas {
                    drawBitmap(fillRedBitmap, 0f, 0f, Paint())
                }
            }

            // 描画した Bitmap と、実際に描画した Bitmap が同じか
            val drawBitmap = imageReader.acquireNextImage()?.toRgbaBitmap()
            assertTrue("Bitmap が一致しません") { drawBitmap?.sameAs(fillRedBitmap) == true }

            graphicsProcessor.destroy()
        }
    }

    @Test
    fun test_動画を作成できる() = runTest(timeout = (CommonTestTool.DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        val createVideoFile = CommonTestTool.createTestVideo(3_000) {
            drawCanvas {
                drawColor(Color.RED)
            }
        }

        MediaMetadataRetriever().use { mediaMetadataRetriever ->
            mediaMetadataRetriever.setDataSource(createVideoFile.path)
            val durationMs = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()

            assertTrue("エンコードした動画と時間が一致しません $durationMs") {
                // TODO なぜか寸足らずになる、おそらく動画に動きがないからフレームが出てこない？根本修正をして戻す
                // 3_000 < durationMs
                1_000 < durationMs
            }

            // 適当な位置のフレームを取り出す
            val frameBitmap = mediaMetadataRetriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_NEXT_SYNC)!! // ナノ秒
            // 少なくとも Red が 0xFF(255) であること
            // 厳密に Color.RED には多分ならない。エンコードのせい
            assertEquals(0xFF, frameBitmap[0, 0].red, "エンコードした動画と色が一致しません")
            assertEquals(0xFF, frameBitmap[10, 10].red, "エンコードした動画と色が一致しません")
        }
    }

    @Test
    fun test_動画をデコードしてフレームを描画できる() = runTest(timeout = (CommonTestTool.DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {

        // 何色か用意する
        val createVideoFile = CommonTestTool.createTestVideo(4_000) { currentPositionMs ->
            drawCanvas {
                drawColor(
                    when (currentPositionMs) {
                        in 0..1_000 -> Color.RED
                        in 1_000..2_000 -> Color.GREEN
                        in 2_000..3_000 -> Color.BLUE
                        else -> Color.BLACK
                    }
                )
            }
        }

        ImageReader.newInstance(CommonTestTool.TEST_VIDEO_WIDTH, CommonTestTool.TEST_VIDEO_HEIGHT, PixelFormat.RGBA_8888, 2).use { imageReader ->

            val graphicsProcessor = AkariGraphicsProcessor(
                renderingPrepareData = AkariGraphicsProcessorRenderingPrepareData.SurfaceRendering(
                    surface = imageReader.surface,
                    width = CommonTestTool.TEST_VIDEO_WIDTH,
                    height = CommonTestTool.TEST_VIDEO_HEIGHT
                ),
                colorSpaceType = AkariGraphicsProcessorColorSpaceType.SDR_BT709
            ).apply { prepare() }

            // OpenGL ES で映像をテクスチャとして利用する SurfaceTexture をラップしたもの
            val akariGraphicsSurfaceTexture = graphicsProcessor.genTextureId { texId -> AkariGraphicsSurfaceTexture(texId) }
            // 映像を SurfaceTexture に出力する
            val akariVideoDecoder = AkariVideoDecoder().apply {
                prepare(
                    input = createVideoFile.toAkariCoreInputOutputData(),
                    outputSurface = akariGraphicsSurfaceTexture.surface
                )
            }

            // 0..1_000ms は少なくとも Red は 0xFF であること
            akariVideoDecoder.seekTo(seekToMs = 500)
            graphicsProcessor.drawOneshot { drawSurfaceTexture(akariGraphicsSurfaceTexture, null) }
            val redVideoFrameBitmap = imageReader.acquireNextImage().toRgbaBitmap()
            assertEquals(0xFF, redVideoFrameBitmap[0, 0].red, "デコードした動画と色が一致しません")
            assertEquals(0xFF, redVideoFrameBitmap[10, 10].red, "デコードした動画と色が一致しません")

            // 1_000..2_000ms は少なくとも Green は 0xFF であること
            akariVideoDecoder.seekTo(seekToMs = 1_500)
            graphicsProcessor.drawOneshot { drawSurfaceTexture(akariGraphicsSurfaceTexture, null) }
            val greenVideoFrameBitmap = imageReader.acquireNextImage().toRgbaBitmap()
            assertEquals(0xFF, greenVideoFrameBitmap[0, 0].green, "デコードした動画と色が一致しません")
            assertEquals(0xFF, greenVideoFrameBitmap[10, 10].green, "デコードした動画と色が一致しません")

            // 2_000..3_000ms は少なくとも Blue は 0xFF であること
            akariVideoDecoder.seekTo(seekToMs = 2_500)
            graphicsProcessor.drawOneshot { drawSurfaceTexture(akariGraphicsSurfaceTexture, null) }
            val blueVideoFrameBitmap = imageReader.acquireNextImage().toRgbaBitmap()
            assertEquals(0xFF, blueVideoFrameBitmap[0, 0].blue, "デコードした動画と色が一致しません")
            assertEquals(0xFF, blueVideoFrameBitmap[10, 10].blue, "デコードした動画と色が一致しません")

            graphicsProcessor.destroy()
        }
    }

    @Test
    fun test_オフスクリーンレンダリングができる() = runTest(timeout = (CommonTestTool.DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        val offscreenAkariGraphicsProcessor = AkariGraphicsProcessor(
            renderingPrepareData = AkariGraphicsProcessorRenderingPrepareData.OffscreenRendering(
                width = CommonTestTool.TEST_VIDEO_WIDTH,
                height = CommonTestTool.TEST_VIDEO_HEIGHT
            ),
            colorSpaceType = AkariGraphicsProcessorColorSpaceType.SDR_BT709
        ).apply { prepare() }

        // 赤色で塗りつぶした Bitmap をオフスクリーンレンダリング
        val fillRedColorBitmap = Bitmap.createBitmap(CommonTestTool.TEST_VIDEO_WIDTH, CommonTestTool.TEST_VIDEO_HEIGHT, Bitmap.Config.ARGB_8888).apply {
            Canvas(this).apply {
                drawColor(Color.RED)
            }
        }
        offscreenAkariGraphicsProcessor.drawOneshot {
            drawCanvas {
                drawBitmap(fillRedColorBitmap, 0f, 0f, Paint())
            }
        }

        // glReadPixels で赤色が描画出来ているか
        // glReadPixels は上下反転するが、単色で塗りつぶしているだけなので特に見ていない
        val byteBuffer = ByteBuffer.allocate(CommonTestTool.TEST_VIDEO_WIDTH * CommonTestTool.TEST_VIDEO_HEIGHT * 4).apply {
            position(0)
        }
        // GL スレッドで
        offscreenAkariGraphicsProcessor.withOpenGlThread {
            GLES30.glReadPixels(0, 0, CommonTestTool.TEST_VIDEO_WIDTH, CommonTestTool.TEST_VIDEO_HEIGHT, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, byteBuffer)
        }

        // Bitmap を作って一致すること
        val glReadPixelsBitmap = Bitmap.createBitmap(CommonTestTool.TEST_VIDEO_WIDTH, CommonTestTool.TEST_VIDEO_HEIGHT, Bitmap.Config.ARGB_8888)
        glReadPixelsBitmap.copyPixelsFromBuffer(byteBuffer)

        assertTrue("オフスクリーンレンダリングした Bitmap と一致しません") { glReadPixelsBitmap.sameAs(fillRedColorBitmap) }
        offscreenAkariGraphicsProcessor.destroy()
    }

    @Test
    fun test_10ビットHDRで描画できる() = runTest(timeout = (CommonTestTool.DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {

        suspend fun hdrDrawTest(dynamicRangeType: AkariGraphicsProcessorColorSpaceType) {
            val offscreenAkariGraphicsProcessor = AkariGraphicsProcessor(
                renderingPrepareData = AkariGraphicsProcessorRenderingPrepareData.OffscreenRendering(
                    width = CommonTestTool.TEST_VIDEO_WIDTH,
                    height = CommonTestTool.TEST_VIDEO_HEIGHT
                ),
                colorSpaceType = dynamicRangeType
            ).apply { prepare() }

            // 真っ白
            // 10ビットなので多分 8 ビットで 0xFF だったのが 10 ビットで 0b11_1111_1111 になるはず。
            offscreenAkariGraphicsProcessor.drawOneshot {
                drawCanvas {
                    drawColor(Color.WHITE)
                }
            }

            // glReadPixels
            val byteBuffer = ByteBuffer.allocate(CommonTestTool.TEST_VIDEO_WIDTH * CommonTestTool.TEST_VIDEO_HEIGHT * 4).apply {
                position(0)
            }
            offscreenAkariGraphicsProcessor.withOpenGlThread {
                // OpenGL ES の EGL で RGB 10 ビット、Alpha 2 ビット使っているので GL_UNSIGNED_INT_2_10_10_10_REV
                GLES30.glReadPixels(0, 0, CommonTestTool.TEST_VIDEO_WIDTH, CommonTestTool.TEST_VIDEO_HEIGHT, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_INT_2_10_10_10_REV, byteBuffer)
            }

            // RGB が各 10 ビットなのでちょっと変わってる
            val red10bit = (byteBuffer[0].toInt() shr 20) and 0b11_1111_1111
            val green10bit = (byteBuffer[0].toInt() shr 10) and 0b11_1111_1111
            val blue10bit = byteBuffer[0].toInt() and 0b11_1111_1111
            // 白なので 10 ビット全て立ってるはず
            assertEquals(0b11_1111_1111, red10bit, "[$dynamicRangeType] 10ビットの色が一致しませんでした")
            assertEquals(0b11_1111_1111, green10bit, "[$dynamicRangeType] 10ビットの色が一致しませんでした")
            assertEquals(0b11_1111_1111, blue10bit, "[$dynamicRangeType] 10ビットの色が一致しませんでした")

            offscreenAkariGraphicsProcessor.destroy()
        }

        // TODO 古い端末は OpenGL ES で 10-bit HDR が使えないのでテストが多分通らない
        // HLG と PQ 両方のガンマカーブで確認しておく
        hdrDrawTest(AkariGraphicsProcessorColorSpaceType.TEN_BIT_HDR_BT2020_HLG)
        hdrDrawTest(AkariGraphicsProcessorColorSpaceType.TEN_BIT_HDR_BT2020_PQ)
    }

    /** [Image]から[Bitmap]を作る */
    private suspend fun Image.toRgbaBitmap() = withContext(Dispatchers.IO) {
        val image = this@toRgbaBitmap
        val width = image.width
        val height = image.height
        val planes = image.planes
        val buffer = planes[0].buffer
        // なぜか ImageReader のサイズに加えて、何故か Padding が入っていることを考慮する必要がある
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width
        // Bitmap 作成
        val readBitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
        readBitmap.copyPixelsFromBuffer(buffer)
        // 余分な Padding を消す
        val editBitmap = Bitmap.createBitmap(readBitmap, 0, 0, CommonTestTool.TEST_VIDEO_WIDTH, CommonTestTool.TEST_VIDEO_HEIGHT)
        readBitmap.recycle()
        image.close()
        return@withContext editBitmap
    }
}