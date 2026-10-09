package org.xmsleep.app.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

/**
 * 全局毛玻璃 Haze 状态。
 *
 * 由 MainScreen 在布局树顶部 provide，深层 Screen 内的弹窗
 * 无需逐层传参即可自动获取采样源。若某处未 provide，
 * [FrostedGlassSheet] 会降级为普通半透明遮罩（不模糊、不崩溃）。
 */
val LocalFrostedHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * 毛玻璃浮层弹窗（Frosted Glass Sheet）
 *
 * - 遮罩层为全屏磨砂 hazeChild（固定 [blurRadius]，默认 10dp），
 *   出场以透明度 0→1 淡入交叉淡化（页面保持清晰 → 磨砂层盖上）
 * - 面板底色默认 surfaceVariant.copy(alpha = 0.42f)，跟随主题
 * - 面板从 0.94 放大到 1 浮起
 * - 使用 cubic-bezier(0.2, 0.7, 0.2, 1) 入场 1 秒，关闭 300ms
 *
 * [hazeState] 为可选：不传则回退到 [LocalFrostedHazeState]，
 * 两者都无时降级为普通半透明遮罩。必须与 Haze 源（Modifier.haze）
 * 处于同一窗口/同一布局树，因此以布局内全屏浮层形式实现，而非独立 Dialog 窗口。
 */
@Composable
fun FrostedGlassSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
    sheetColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
    blurRadius: Dp = 10.dp,
    enterDurationMillis: Int = 1000,
    exitDurationMillis: Int = 300,
    content: @Composable ColumnScope.() -> Unit
) {
    val resolvedHazeState = hazeState ?: LocalFrostedHazeState.current
    // 单一 progress 驱动遮罩淡入/面板缩放，避免多状态失步
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (visible) enterDurationMillis else exitDurationMillis,
            easing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)
        ),
        label = "frosted_sheet_progress"
    )

    val surfaceColor = MaterialTheme.colorScheme.surface

    if (progress <= 0f) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismissRequest
            ),
        contentAlignment = Alignment.Center
    ) {
        // 磨砂遮罩：采样页面内容做固定半径模糊，整个层以 alpha 淡入出场
        // 与底部导航栏同一配方：hazeChild 内用不透明 surface 垫色，模糊画在垫色之上
        // 若无可用 haze 状态则降级为半透明 surface 遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress }
                .then(
                    if (resolvedHazeState != null) {
                        Modifier.hazeChild(state = resolvedHazeState) {
                            this.blurRadius = blurRadius
                            noiseFactor = 0.16f
                            backgroundColor = surfaceColor
                        }
                    } else {
                        Modifier.background(surfaceColor.copy(alpha = 0.6f))
                    }
                )
        )

        Surface(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .graphicsLayer(
                    alpha = progress,
                    scaleX = 0.94f + 0.06f * progress,
                    scaleY = 0.94f + 0.06f * progress
                ),
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(sheetColor)
                    .border(
                        border = BorderStroke(
                            width = 0.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp),
                content = content
            )
        }
    }
}
