package org.xmsleep.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * 三条竖线播放动画组件
 * 当音频播放时显示，类似音乐均衡器动画效果
 */
@Composable
fun PlayingAnimation(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    width: Dp = 24.dp,
    height: Dp = 20.dp,
    barCount: Int = 3,
    isPlaying: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "playing_animation")

    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animation_progress"
    )

    Canvas(modifier = modifier.size(width, height)) {
        val barWidth = size.width / (barCount * 2f)
        val spacing = barWidth
        val cornerRadius = barWidth / 2f

        for (i in 0 until barCount) {
            // 每根竖线使用不同的相位，创造波浪效果
            val phase = i * 0.8f
            val sinValue = sin((animationProgress * 2 * Math.PI + phase).toDouble()).toFloat()

            // 计算竖线高度（30% ~ 100%）
            val minHeightRatio = 0.3f
            val maxHeightRatio = 1.0f
            val heightRatio = if (isPlaying) {
                minHeightRatio + (maxHeightRatio - minHeightRatio) * (sinValue + 1f) / 2f
            } else {
                minHeightRatio
            }

            val barHeight = size.height * heightRatio
            val x = i * (barWidth + spacing) + spacing / 2
            val y = size.height - barHeight

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        }
    }
}
