package org.xmsleep.app.ui.components

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil.load
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmsleep.app.R
import org.xmsleep.app.ui.BackgroundSelection

@OptIn(UnstableApi::class)
@Composable
fun AnimatedBackground(
    backgroundSelection: BackgroundSelection,
    customBackgroundUri: String? = null,
    customBackgroundColor: Color? = null,
    backgroundOpacity: Float = 0.2f,
    backgroundBlurRadius: Float = 0f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (backgroundSelection == BackgroundSelection.Custom && customBackgroundUri != null) {
        val uri = Uri.parse(customBackgroundUri)
        val path = uri.path ?: ""
        val isVideo = path.endsWith(".mp4", ignoreCase = true)

        val bgColorInt = customBackgroundColor?.let {
            if (it != Color.Unspecified) it.toArgb() else null
        }

        if (isVideo) {
            var isReady by remember { mutableStateOf(false) }

            Box(modifier = modifier.fillMaxSize()) {
                if (!isReady) {
                    bgColorInt?.let { color ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(color))
                        )
                    }
                }

                key(customBackgroundUri) {
                    CrossfadeVideoPlayer(
                        uri = uri,
                        backgroundOpacity = backgroundOpacity,
                        backgroundBlurRadius = backgroundBlurRadius,
                        onReady = { isReady = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            val imageUri = uri
            var imageDrawable by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }

            LaunchedEffect(imageUri) {
                withContext(Dispatchers.IO) {
                    val result = try {
                        val file = imageUri.path?.let { java.io.File(it) }
                        if (file?.exists() == true) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                val source = android.graphics.ImageDecoder.createSource(file)
                                android.graphics.ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                                    val maxDimension = 1080
                                    val max = maxOf(info.size.width, info.size.height)
                                    if (max > maxDimension) {
                                        val scale = maxDimension.toFloat() / max
                                        decoder.setTargetSize(
                                            (info.size.width * scale).toInt(),
                                            (info.size.height * scale).toInt()
                                        )
                                    }
                                }
                            } else {
                                val opts = android.graphics.BitmapFactory.Options().apply {
                                    inJustDecodeBounds = true
                                }
                                android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)
                                val maxDimension = 1080
                                val max = maxOf(opts.outWidth, opts.outHeight)
                                opts.inSampleSize = if (max > maxDimension) max / maxDimension else 1
                                opts.inJustDecodeBounds = false
                                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)
                                bitmap?.let { android.graphics.drawable.BitmapDrawable(null, it) }
                            }
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                    withContext(Dispatchers.Main) {
                        imageDrawable = result
                    }
                }
            }

            AndroidView(
                factory = { ctx ->
                    android.widget.ImageView(ctx).apply {
                        bgColorInt?.let { color -> setBackgroundColor(color) }
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    }
                },
                update = { view ->
                    val drawable = imageDrawable
                    if (drawable != null) {
                        view.setImageDrawable(drawable)
                        if (drawable is android.graphics.drawable.AnimatedImageDrawable) {
                            drawable.repeatCount = android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE
                            if (!drawable.isRunning) drawable.start()
                        }
                    } else {
                        view.load(imageUri)
                    }
                },
                modifier = modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = backgroundOpacity
                        if (backgroundBlurRadius > 0f && Build.VERSION.SDK_INT >= 31) {
                            renderEffect = AndroidRenderEffect.createBlurEffect(
                                backgroundBlurRadius, backgroundBlurRadius, Shader.TileMode.CLAMP
                            ).asComposeRenderEffect()
                        }
                    }
            )
        }
    } else if (backgroundSelection != BackgroundSelection.None && backgroundSelection.resourceId != null) {
        if (backgroundSelection.isResourceValid(context)) {
            AndroidView(
                factory = { ctx ->
                    android.widget.ImageView(ctx).apply {
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    }
                },
                update = { view ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = android.graphics.ImageDecoder.createSource(
                            context.resources,
                            backgroundSelection.resourceId
                        )
                        val drawable = android.graphics.ImageDecoder.decodeDrawable(source)
                        if (drawable is android.graphics.drawable.AnimatedImageDrawable) {
                            drawable.repeatCount = android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE
                            drawable.start()
                        }
                        view.setImageDrawable(drawable)
                    } else {
                        view.setImageResource(backgroundSelection.resourceId)
                    }
                },
                modifier = modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = backgroundOpacity
                        if (backgroundBlurRadius > 0f && Build.VERSION.SDK_INT >= 31) {
                            renderEffect = AndroidRenderEffect.createBlurEffect(
                                backgroundBlurRadius, backgroundBlurRadius, Shader.TileMode.CLAMP
                            ).asComposeRenderEffect()
                        }
                    }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun CrossfadeVideoPlayer(
    uri: Uri,
    backgroundOpacity: Float = 0.2f,
    backgroundBlurRadius: Float = 0f,
    onReady: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            val container = FrameLayout(ctx)

            val playerView = LayoutInflater.from(ctx).inflate(R.layout.video_background_player, null) as PlayerView
            val player = ExoPlayer.Builder(ctx).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
                prepare()
            }
            playerView.player = player
            container.addView(playerView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))

            onReady()
            container
        },
        update = { _ -> },
        onRelease = { view ->
            (view as? FrameLayout)?.let { container ->
                for (i in 0 until container.childCount) {
                    val pv = container.getChildAt(i) as? PlayerView
                    pv?.player?.release()
                    pv?.player = null
                }
            }
        },
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = backgroundOpacity
                if (backgroundBlurRadius > 0f && Build.VERSION.SDK_INT >= 31) {
                    renderEffect = AndroidRenderEffect.createBlurEffect(
                        backgroundBlurRadius, backgroundBlurRadius, Shader.TileMode.CLAMP
                    ).asComposeRenderEffect()
                }
            }
    )
}
