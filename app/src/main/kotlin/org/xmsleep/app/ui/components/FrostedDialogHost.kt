package org.xmsleep.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

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
 * 屏内弹窗包装：有宿主时把 [content] 提升到 MainScreen 顶层渲染（全页模糊、tab 在遮罩下），
 * 无宿主时降级为就地渲染。用法：
 * ```
 * FrostedPopup(visible = showDialog) {
 *     SomeDialog(onDismiss = { showDialog = false }, ...)
 * }
 * ```
 * 弹窗自身的 onDismiss 仍由调用方把 visible 置 false，宿主据此卸载。
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
    // 用 rememberUpdatedState 保证宿主持有的 lambda 始终调用最新 content
    val currentContent by rememberUpdatedState(content)
    // visible 变化时同步到宿主：true → 提升渲染；false → 卸载。
    // 用 DisposableEffect 而非 LaunchedEffect：组合体被移除时（如 LazyColumn item 滚出）
    // 通过 onDispose 卸载宿主，避免宿主残留一个已销毁的 content 组合体导致崩溃。
    DisposableEffect(visible) {
        if (visible) {
            host.show { currentContent() }
        } else {
            host.dismiss()
        }
        onDispose {
            host.dismiss()
        }
    }
}
