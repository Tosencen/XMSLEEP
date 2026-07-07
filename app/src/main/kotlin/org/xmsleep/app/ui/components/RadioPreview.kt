package org.xmsleep.app.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.xmsleep.app.ui.BackgroundSelection

@Composable
fun RadioPreview(
    backgroundSelection: BackgroundSelection,
    customBackgroundUri: String?,
    customBackgroundColor: Color?,
    backgroundOpacity: Float,
    backgroundBlurRadius: Float = 0f,
    customBackgroundThumbnail: String? = null,
    modifier: Modifier = Modifier
) {
    val blurMod = if (backgroundBlurRadius > 0f && Build.VERSION.SDK_INT >= 31) {
        Modifier.blur(backgroundBlurRadius.dp)
    } else {
        Modifier
    }

    val hasImageBg = backgroundSelection != BackgroundSelection.None

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(280.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            // Layer 1: Theme color base — always darkened for preview readability
            val baseColor = customBackgroundColor ?: MaterialTheme.colorScheme.surface
            val dimmedColor = run {
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(baseColor.copy(alpha = 1f).toArgb(), hsv)
                hsv[2] = (hsv[2] * 0.45f).coerceIn(0f, 1f)
                Color(android.graphics.Color.HSVToColor(hsv)).copy(alpha = 1f)
            }
            val baseAlpha = if (hasImageBg) backgroundOpacity else 1f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(baseAlpha)
                    .background(dimmedColor)
            )

            // Layer 2: Image/video on top with opacity
            if (hasImageBg) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(backgroundOpacity)
                        .then(blurMod)
                ) {
                    when {
                        backgroundSelection == BackgroundSelection.Background1 -> {
                            AnimatedWebPImage(
                                drawableResId = org.xmsleep.app.R.drawable.bg_animation_1,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                isPlaying = false
                            )
                        }
                        backgroundSelection == BackgroundSelection.Background6 -> {
                            AnimatedWebPImage(
                                drawableResId = org.xmsleep.app.R.drawable.bg_animation_6,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                isPlaying = false
                            )
                        }
                        backgroundSelection == BackgroundSelection.Custom && customBackgroundUri != null -> {
                            val isVideo = customBackgroundUri.endsWith(".mp4", ignoreCase = true)
                            if (isVideo && customBackgroundThumbnail != null) {
                                coil.compose.AsyncImage(
                                    model = customBackgroundThumbnail,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (isVideo) {
                                coil.compose.AsyncImage(
                                    model = customBackgroundUri.replace(".mp4", "_thumb.jpg"),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                coil.compose.AsyncImage(
                                    model = customBackgroundUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            // Layer 3: Mock radio page content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                // Status bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                    )
                    Text("9:41", fontSize = 7.sp, color = Color.White.copy(alpha = 0.6f))
                }
                Spacer(Modifier.height(6.dp))

                // Now playing card — mini player mock
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.3f))
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(52.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.5f))
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.3f))
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))

                // Bottom tab bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (i == 0) Color.White.copy(alpha = 0.7f)
                                    else Color.White.copy(alpha = 0.25f)
                                )
                        )
                    }
                }
            }
        }
    }
}
