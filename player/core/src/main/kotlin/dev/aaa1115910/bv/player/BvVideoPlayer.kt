package dev.aaa1115910.bv.player

import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.ScaleAndRotateTransformation
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dev.aaa1115910.bv.player.impl.exo.ExoMediaPlayer
import dev.aaa1115910.bv.util.ifElse

@OptIn(UnstableApi::class)
@Composable
fun BvVideoPlayer(
    modifier: Modifier = Modifier,
    videoPlayer: AbstractVideoPlayer,
    playerListener: VideoPlayerListener,
    rotationDegrees: Float = 0f, // 新增参数，视频旋转角度
    danmakuPlayer: DanmakuPlayer? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val screenHeight = with(density) { context.resources.displayMetrics.heightPixels.toFloat() }
    val screenWidth = with(density) { context.resources.displayMetrics.widthPixels.toFloat() }


    DisposableEffect(Unit) {
        videoPlayer.setPlayerEventListener(playerListener)

        onDispose {
            videoPlayer.release()
        }
    }

    when (videoPlayer) {
        is ExoMediaPlayer -> {
            var surfaceView: SurfaceView? by remember { mutableStateOf(null) }
            var lastRotationDegrees by remember { mutableFloatStateOf(rotationDegrees) }

            fun clearVideoView() {
                surfaceView?.let {
                    videoPlayer.mPlayer?.clearVideoSurfaceView(it)
                    surfaceView = null
                }
            }


            // SurfaceView 渲染
            key(rotationDegrees) {
                AndroidView(
                    modifier = modifier
//                    .ifElse(
//                        rotationDegrees == 90f || rotationDegrees == -90f,
//                        Modifier.fillMaxWidth(),
//                        Modifier.fillMaxHeight()
//                    )
                        .fillMaxHeight(),
                    factory = { ctx ->
                        clearVideoView()
                        SurfaceView(ctx).also { sv ->
                            surfaceView = sv
                            videoPlayer.mPlayer?.setVideoSurfaceView(sv)
                        }
                    },
                    update = { sv ->
                        if (rotationDegrees != lastRotationDegrees) {
                            val time = videoPlayer.currentPosition
                            videoPlayer.stop()
                            lastRotationDegrees = rotationDegrees

                            val rotateEffect = ScaleAndRotateTransformation.Builder()
                                .setRotationDegrees(-rotationDegrees)
                                .build()
                            videoPlayer.mPlayer?.setVideoEffects(listOf(rotateEffect))


                            videoPlayer.prepare()
                            videoPlayer.seekTo(time)
                            danmakuPlayer?.seekTo(time)
                            danmakuPlayer?.pause()
                            videoPlayer.start()
                        }
                    }
                )
            }

            DisposableEffect(videoPlayer) {
                onDispose {
                    clearVideoView()
                }
            }
        }
    }
}