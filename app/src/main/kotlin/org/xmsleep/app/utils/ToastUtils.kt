package org.xmsleep.app.utils

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * Toast 相关的 Composable 本地注入
 */
@Stable
val LocalToaster: ProvidableCompositionLocal<Toaster> = staticCompositionLocalOf {
    error("No LocalToaster provided")
}

/**
 * Toast 接口定义
 */
@Stable
interface Toaster {
    fun toast(text: String)

    fun show(text: String) {
        toast(text)
    }
}

/**
 * Toast ViewModel，管理 Toast 状态
 */
class ToastViewModel {
    val showing = MutableStateFlow(false)
    val content = MutableStateFlow("")
    private var currentTask: Job? = null

    fun show(text: String, scope: CoroutineScope) {
        currentTask?.cancel()
        showing.value = true
        content.value = text

        currentTask = scope.launch {
            delay(3000L)
            showing.value = false
        }
    }
}

/**
 * Toast 的 Composable UI 实现，完全跟随应用主题
 */
@Composable
fun ToastContent(
    showing: () -> Boolean,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) = BoxWithConstraints(
    modifier = modifier.fillMaxSize()
) {
    val px640dp = with(LocalDensity.current) { 640.dp.roundToPx() }
    val px100dp = with(LocalDensity.current) { 100.dp.roundToPx() }

    val minToastWidth = with(LocalDensity.current) { px100dp + 60.dp.roundToPx() * 2 }
    val maxToastWidth = max(minToastWidth, min(constraints.maxWidth, px640dp))

    val currentContent by rememberUpdatedState(content)

    AnimatedVisibility(
        visible = showing(),
        enter = fadeIn(tween(350, easing = LinearEasing)),
        exit = fadeOut(tween(350, easing = LinearEasing)),
        modifier = Modifier.layout { measurable, constraints ->
            val rawWidth = measurable.measure(
                constraints.copy(minWidth = 0, maxWidth = Int.MAX_VALUE)
            ).width

            val placeable = measurable.measure(
                constraints.copy(
                    minWidth = min(rawWidth, minToastWidth),
                    maxWidth = maxToastWidth,
                    minHeight = 0
                ),
            )

            val x = max(this@BoxWithConstraints.constraints.maxWidth - placeable.width, 0) / 2
            val y = constraints.maxHeight - placeable.height - px100dp

            layout(placeable.width, placeable.height) {
                placeable.place(x, y, 100f)
            }
        },
    ) {
        Surface(
            modifier = Modifier.padding(horizontal = 60.dp),
            shape = RoundedCornerShape(15.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 4.dp,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface
            ) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    currentContent()
                }
            }
        }
    }
}

/**
 * 统一的 Toast 工具类（保留旧 API 兼容性）
 */
object ToastUtils {
    fun showToast(context: Context, message: String, duration: Int = android.widget.Toast.LENGTH_SHORT) {
        // 使用原生 Toast 作为后备方案（在非 Compose 环境使用）
        android.widget.Toast.makeText(context, message, duration).show()
    }
}

