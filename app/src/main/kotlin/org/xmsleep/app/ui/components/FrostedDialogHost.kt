package org.xmsleep.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.delay

/**
 * 屏内弹窗提升宿主。
 *
 * 屏内弹窗（About/Language/Volume/Timer 等）若直接在 NavHost（Box B）内渲染：
 * - 底部导航栏画在内容之后（之上），会盖住弹窗（tab 不该出现却出现）
 * - 弹窗 hazeChild 只能采样屏内内容，无法全页模糊（壁纸 + tab 不在采样源内）
 *
 * 通过本宿主把弹窗内容提升到 MainScreen Box B 之外渲染：
 * - 画在导航栏之上，tab 被遮罩盖住（而不是盖住弹窗）
 * - hazeChild 采样 hazeMaskState 成为兄弟 → 全页模糊（壁纸 + 内容 + tab 一起模糊）
 */
@Stable
class FrostedDialogHostState {
    /** 当前提升渲染的弹窗内容；null 表示无弹窗 */
    internal var content by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    internal fun show(content: @Composable () -> Unit) {
        this.content = content
    }

    fun dismiss() {
        this.content = null
    }

    val isVisible: Boolean get() = content != null
}

/** MainScreen 在 Box B 之外 provide；屏内通过它把弹窗提升渲染。 */
val LocalFrostedDialogHost = staticCompositionLocalOf<FrostedDialogHostState?> { null }

/**
 * FrostedPopup 传给内容的真实可见性。
 *
 * 调用方在 [FrostedPopup] 内写的 [FrostedGlassSheet] 通常固定 `visible = true`（显隐由
 * FrostedPopup 挂载/卸载控制）。但退场需要 FrostedGlassSheet 知道"正在关闭"才能播退场动画，
 * 因此由 FrostedPopup 通过本 Local 下发真实 visible，FrostedGlassSheet 以 `visible && 本值`
 * 作为有效可见性。不在 FrostedPopup 内（如独立使用）时默认 true，不影响原有行为。
 */
val LocalFrostedPopupVisible = staticCompositionLocalOf { true }

/** 退场动画播放时长内保持挂载，之后再卸载（须 ≥ FrostedGlassSheet 退场 tween 时长）。 */
private const val FROSTED_POPUP_EXIT_UNMOUNT_DELAY_MS = 300L

/**
 * 屏内弹窗包装：有宿主时把 [content] 提升到 MainScreen 顶层渲染（全页模糊、tab 在遮罩下），
 * 无宿主时降级为就地渲染。用法：
 * ```
 * FrostedPopup(visible = showDialog) {
 *     FrostedGlassSheet(visible = true, onDismissRequest = { showDialog = false }) { ... }
 * }
 * ```
 * 显示时立即挂载；隐藏时先保持挂载 [FROSTED_POPUP_EXIT_UNMOUNT_DELAY_MS] 让退场动画播完，
 * 再卸载宿主。
 */
@Composable
fun FrostedPopup(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    val host = LocalFrostedDialogHost.current
    if (host == null) {
        // 无宿主：就地渲染（降级，行为同旧）
        if (visible) content()
        return
    }
    // 用 rememberUpdatedState 保证宿主持有的 lambda 始终调用最新 content / visible
    val currentContent by rememberUpdatedState(content)
    val currentVisible by rememberUpdatedState(visible)

    // 挂载状态：显示立即挂载；隐藏延迟卸载，给退场动画留时间
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            mounted = true
        } else {
            delay(FROSTED_POPUP_EXIT_UNMOUNT_DELAY_MS)
            mounted = false
        }
    }

    // mounted 变化时同步宿主；组合体被移除（如切屏/LazyColumn 滚出）时兜底卸载
    DisposableEffect(mounted) {
        if (mounted) {
            host.show {
                CompositionLocalProvider(LocalFrostedPopupVisible provides currentVisible) {
                    currentContent()
                }
            }
        } else {
            host.dismiss()
        }
        onDispose {
            host.dismiss()
        }
    }
}
