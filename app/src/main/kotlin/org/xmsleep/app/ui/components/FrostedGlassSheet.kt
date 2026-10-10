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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
 * - 使用 spring（中等回弹）驱动入场/退场，手感对齐系统权限弹窗
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
    blurRadius: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val resolvedHazeState = hazeState ?: LocalFrostedHazeState.current
    // 有效可见性：FrostedPopup 内由 Local 下发真实 visible + 宿主退场状态（本参数通常恒为 true），
    // 独立使用时退化为本参数。
    val effectiveVisible = visible && LocalFrostedPopupVisible.current && !LocalFrostedHostDismissing.current

    // 动画拆分为两条通道（手感关键）：
    // - alpha 用快速 tween 淡入淡出 → 立即可见、不拖泥带水；
    // - scale 用 spring 弹起/回弹 → 轻盈浮起。
    // 之前 alpha 直接跟随 spring 值：spring 起步慢导致淡入迟滞、观感僵硬。
    val alphaProgress = remember { Animatable(0f) }
    val scaleProgress = remember { Animatable(0.94f) }
    // 仅在入场开始/退场结束各翻转一次，避免在组合期逐帧读动画值导致内容反复重组（复杂弹窗会掉帧）。
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
                    scaleProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
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
                    scaleProgress.animateTo(
                        targetValue = 0.94f,
                        animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)
                    )
                }
            }
            rendered = false
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surface

    if (!rendered) return

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
        // alpha/scale 在绘制期读取（graphicsLayer lambda），动画过程不触发重组
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
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    alpha = alphaProgress.value.coerceIn(0f, 1f)
                    scaleX = scaleProgress.value
                    scaleY = scaleProgress.value
                },
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
