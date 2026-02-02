package org.xmsleep.app.ui.components

import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import org.xmsleep.app.ui.BackgroundSelection

/**
 * 动画背景组件
 * 
 * 显示 WebP 动画背景，铺满整个屏幕并循环播放
 * 
 * @param backgroundSelection 选择的背景动画
 * @param modifier 修饰符
 */
@Composable
fun AnimatedBackground(
    backgroundSelection: BackgroundSelection,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 如果选择了背景动画（不是 None）
    if (backgroundSelection != BackgroundSelection.None && backgroundSelection.resourceId != null) {
        // 验证资源是否有效
        if (backgroundSelection.isResourceValid(context)) {
            // 使用 AndroidView 显示 WebP 动画
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    android.widget.ImageView(ctx).apply {
                        // 使用 CENTER_CROP 让背景铺满整个屏幕
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    }
                },
                update = { view ->
                    // 加载并播放 WebP 动画
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        // API 28+ 使用 ImageDecoder 加载动画 WebP
                        val source = android.graphics.ImageDecoder.createSource(
                            context.resources,
                            backgroundSelection.resourceId
                        )
                        val drawable = android.graphics.ImageDecoder.decodeDrawable(source)
                        
                        // 如果是动画 WebP，设置为无限循环播放
                        if (drawable is android.graphics.drawable.AnimatedImageDrawable) {
                            drawable.repeatCount = android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE
                            drawable.start()
                        }
                        
                        view.setImageDrawable(drawable)
                    } else {
                        // API 28 以下使用传统方式加载（不支持动画）
                        view.setImageResource(backgroundSelection.resourceId)
                    }
                },
                modifier = modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.2f } // 设置透明度为 0.2
            )
        }
    }
}
