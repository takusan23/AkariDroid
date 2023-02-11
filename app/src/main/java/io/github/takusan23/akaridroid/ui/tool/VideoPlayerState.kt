package io.github.takusan23.akaridroid.ui.tool

import android.content.Context
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.video.VideoSize
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

@Composable
fun rememberVideoPlayerState(context: Context, lifecycle: Lifecycle): VideoPlayerState {
    return remember {
        VideoPlayerState(context, lifecycle)
    }
}

/**
 * プレイヤーのロジック
 * ExoPlayer関連がここにある
 *
 * @param context [Context]
 */
class VideoPlayerState(context: Context, lifecycle: Lifecycle) : DefaultLifecycleObserver {

    private val _playWhenRelayFlow = MutableStateFlow(false)
    private val _currentPositionMsFlow = MutableStateFlow(PlayerPositionData(0L, 0L))
    private val _videoSizeData = MutableStateFlow<VideoSizeData?>(null)

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val exoPlayer = ExoPlayer.Builder(context).build().apply {
        addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                _playWhenRelayFlow.value = playWhenReady
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                _videoSizeData.value = VideoSizeData(videoSize.height, videoSize.width)
            }
        })
    }

    /** 再生状態 */
    val playWhenRelayFlow = _playWhenRelayFlow.asStateFlow()

    /** 現在位置 */
    val currentPositionMsFlow = _currentPositionMsFlow.asStateFlow()

    /** アスペクト比 */
    val videoSizeData = _videoSizeData.asStateFlow()

    /** 再生時間 ms */
    var currentPositionMs: Long
        get() = max(0, exoPlayer.currentPosition)
        set(value) = exoPlayer.seekTo(value)

    /** 動画時間 */
    val durationMs: Long
        get() = max(0, exoPlayer.duration)

    /** 再生状態 */
    var playWhenReady: Boolean
        get() = exoPlayer.playWhenReady
        set(value) {
            exoPlayer.playWhenReady = value
        }

    init {
        lifecycle.addObserver(this)

        scope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    _currentPositionMsFlow.value = PlayerPositionData(currentPositionMs, durationMs)
                    delay(100)
                }
            }
        }
    }


    /**
     * SurfaceViewをセットする
     *
     * @param surfaceView [SurfaceView]
     */
    fun setSurfaceView(surfaceView: SurfaceView) {
        exoPlayer.setVideoSurfaceView(surfaceView)
    }

    /**
     * 動画をセットする
     *
     * @param url 動画パス
     */
    fun setMediaItem(url: String) {
        val mediaItem = MediaItem.fromUri(url.toUri())
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        exoPlayer.release()
    }

    /**
     * プレイヤーの再生位置と再生時間
     *
     * @param currentPositionMs 再生位置
     * @param durationMs 動画時間
     */
    data class PlayerPositionData(
        val currentPositionMs: Long,
        val durationMs: Long
    )

    /**
     * 動画サイズ
     *
     * @param width 幅
     * @param height 高さ
     */
    data class VideoSizeData(
        val height: Int,
        val width: Int
    )
}