package org.xmsleep.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

/**
 * 毛玻璃底部抽屉（Frosted Bottom Sheet）。
 *
 * 与 [FrostedGlassSheet] 同配方（Haze 背景模糊遮罩 + 面板），但面板贴底、圆角只在上方，
 * 入场从底部上滑、退场加速下滑。用于替换 Material3 的 ModalBottomSheet——后者是独立 window，
 * 无法用 Haze 采样主界面做背景模糊，故改为布局内底部浮层。
 *
 * 同样配合 [FrostedPopup] 使用以提升到有 hazeMaskState 的层级（全页模糊）。
 */
@Composable
fun FrostedBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
    sheetColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
    blurRadius: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val resolvedHazeState = hazeState ?: LocalFrostedHazeState.current
    // 有效可见性：FrostedPopup 内由 Local 下发真实 visible + 宿主退场状态；独立使用时退化为本参数
    val effectiveVisible = visible && LocalFrostedPopupVisible.current && !LocalFrostedHostDismissing.current
    // 入场从底部弹到 1（spring）；退场加速下滑（tween）。用 Animatable 从 0 起步才有入场动画。
    val progress = remember { Animatable(0f) }
    LaunchedEffect(effectiveVisible) {
        if (effectiveVisible) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing)
            )
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surface

    if (progress.value <= 0f) return

    // 上滑位移：用固定偏移（≥ 屏高）把面板推出屏幕底部，避免依赖 onSizeChanged 的测量时序
    val density = LocalDensity.current
    val slideOffsetPx = remember(density) { with(density) { 800.dp.toPx() } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismissRequest
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 磨砂遮罩：采样页面内容做固定半径模糊，整个层以 alpha 淡入
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
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
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = progress.value.coerceIn(0f, 1f)
                    translationY = (1f - progress.value) * slideOffsetPx
                },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
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
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .navigationBarsPadding()
                    .padding(24.dp),
                content = content
            )
        }
    }
}
