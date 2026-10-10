package org.xmsleep.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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

    // 动画拆分（与 FrostedGlassSheet 同思路）：alpha 用快速 tween 立即可见，位移用 tween 平滑上滑。
    // 之前位移用 spring 会过冲（面板冲过终点再回弹），底部抽屉不该这样弹。
    val alphaProgress = remember { Animatable(0f) }
    val slideProgress = remember { Animatable(0f) }
    // 仅在入场开始/退场结束各翻转一次，避免组合期逐帧读动画值导致内容反复重组。
    var rendered by remember { mutableStateOf(false) }

    LaunchedEffect(effectiveVisible) {
        if (effectiveVisible) {
            rendered = true
            coroutineScope {
                launch {
                    alphaProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 160, easing = FastOutLinearInEasing)
                    )
                }
                launch {
                    slideProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    )
                }
            }
        } else {
            coroutineScope {
                launch {
                    alphaProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)
                    )
                }
                launch {
                    slideProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)
                    )
                }
            }
            rendered = false
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surface

    if (!rendered) return

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
        // alpha/位移在绘制期读取（graphicsLayer lambda），动画过程不触发重组
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = alphaProgress.value.coerceIn(0f, 1f) }
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
                    alpha = alphaProgress.value.coerceIn(0f, 1f)
                    translationY = (1f - slideProgress.value) * slideOffsetPx
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
                    .navigationBarsPadding(),
                content = content
            )
        }
    }
}
