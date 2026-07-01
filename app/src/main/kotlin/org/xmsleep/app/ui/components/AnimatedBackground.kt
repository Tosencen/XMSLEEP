package org.xmsleep.app.ui.components

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil.load
import org.xmsleep.app.ui.BackgroundSelection

@Composable
fun AnimatedBackground(
    backgroundSelection: BackgroundSelection,
    customBackgroundUri: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (backgroundSelection == BackgroundSelection.Custom && customBackgroundUri != null) {
        val uri = Uri.parse(customBackgroundUri)
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val isVideo = mimeType.contains("mp4")

        if (isVideo) {
            AndroidView(
                factory = { ctx ->
                    android.widget.VideoView(ctx).apply {
                        setVideoURI(uri)
                        setOnPreparedListener { mp ->
                            mp.setLooping(true)
                            start()
                        }
                        scaleX = 1f
                        scaleY = 1f
                    }
                },
                update = { view ->
                    if (!view.isPlaying) {
                        view.setVideoURI(uri)
                        view.start()
                    }
                },
                modifier = modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.2f }
            )
        } else {
            val imageUri = uri
            key(imageUri) {
                AndroidView(
                    factory = { ctx ->
                        android.widget.ImageView(ctx).apply {
                            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        }.also { view ->
                            view.load(imageUri) {
                                crossfade(true)
                                listener(
                                    onSuccess = { _, result ->
                                        val drawable = result.drawable
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                                            drawable is android.graphics.drawable.AnimatedImageDrawable
                                        ) {
                                            drawable.repeatCount =
                                                android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE
                                            if (!drawable.isRunning) drawable.start()
                                        }
                                    }
                                )
                            }
                        }
                    },
                    modifier = modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.2f }
                )
            }
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
                    .graphicsLayer { alpha = 0.2f }
            )
        }
    }
}