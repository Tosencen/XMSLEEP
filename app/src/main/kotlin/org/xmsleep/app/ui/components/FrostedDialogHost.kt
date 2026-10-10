package org.xmsleep.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
 *
 * 宿主同时管理退场生命周期：无论调用方以 `visible` 驱动还是以 `if` 包裹卸载，
 * 都先置 [dismissing] 让内容播退场动画，延迟后再卸载，避免弹窗"瞬间消失"。
 */
@Stable
class FrostedDialogHostState {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var exitJob: Job? = null

    /** 当前提升渲染的弹窗内容；null 表示无弹窗 */
    internal var content by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    /** 是否处于退场中（内容仍挂载、正在播退场动画） */
    internal var dismissing by mutableStateOf(false)
        private set

    val isVisible: Boolean get() = content != null

    internal fun show(content: @Composable () -> Unit) {
        exitJob?.cancel()
        dismissing = false
        this.content = content
    }

    /** 开始退场：置 [dismissing] 让内容播退场动画，延迟后卸载。 */
    internal fun beginExit() {
        if (content == null || dismissing) return
        dismissing = true
        exitJob = scope.launch {
            delay(EXIT_UNMOUNT_DELAY_MS)
            content = null
            dismissing = false
        }
    }

    fun dismiss() {
        exitJob?.cancel()
        content = null
        dismissing = false
    }
}

/** MainScreen 在 Box B 之外 provide；屏内通过它把弹窗提升渲染。 */
val LocalFrostedDialogHost = staticCompositionLocalOf<FrostedDialogHostState?> { null }

/**
 * FrostedPopup 传给内容的真实可见性（false 表示调用方已把 visible 置 false、正在关闭）。
 * 不在 FrostedPopup 内（如独立使用）时默认 true。
 */
val LocalFrostedPopupVisible = staticCompositionLocalOf { true }

/** 宿主是否处于退场中（内容应播退场动画）。 */
val LocalFrostedHostDismissing = staticCompositionLocalOf { false }

/** 退场动画播放时长内保持挂载，之后再卸载（须 ≥ FrostedGlassSheet 退场 tween 时长）。 */
private const val EXIT_UNMOUNT_DELAY_MS = 300L

/**
 * 屏内弹窗包装：有宿主时把 [content] 提升到 MainScreen 顶层渲染（全页模糊、tab 在遮罩下），
 * 无宿主时降级为就地渲染。用法：
 * ```
 * // 方式一：visible 驱动（推荐，调用方常驻组合、用 visible 控制显隐）
 * FrostedPopup(visible = showDialog) {
 *     FrostedGlassSheet(visible = true, onDismissRequest = { showDialog = false }) { ... }
 * }
 * // 方式二：if 包裹的自定义弹窗（卸载时宿主仍会播退场动画再卸载）
 * if (showDialog) { SomeFrostedDialog(onDismiss = { showDialog = false }) }
 * ```
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

    // visible → 宿主挂载 / 开始退场
    LaunchedEffect(visible) {
        if (visible) {
            host.show {
                CompositionLocalProvider(
                    LocalFrostedPopupVisible provides currentVisible,
                    LocalFrostedHostDismissing provides host.dismissing
                ) {
                    currentContent()
                }
            }
        } else {
            host.beginExit()
        }
    }

    // 组合体被移除（if 包裹的自定义弹窗卸载 / 切屏 / LazyColumn 滚出）时：
    // 若宿主仍在显示则开始退场（播退场动画后再卸载），避免弹窗"瞬间消失"
    DisposableEffect(Unit) {
        onDispose {
            if (host.isVisible && !host.dismissing) host.beginExit()
        }
    }
}
