package org.xmsleep.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 主题色提取工具
 * 
 * 从背景动画图片中提取主色调，用于自动设置应用主题色
 */
class ThemeColorExtractor(private val context: Context) {
    
    /**
     * 同步提取主题色（用于应用启动时）
     * 
     * @param drawableResId Drawable 资源 ID
     * @return 提取的主题色，如果提取失败则返回 null
     */
    fun extractDominantColorSync(@DrawableRes drawableResId: Int): Color? {
        return try {
            // 1. 加载 WebP 的第一帧作为 Bitmap
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // API 28+ 使用 ImageDecoder
                val source = ImageDecoder.createSource(context.resources, drawableResId)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    // 缩小图片以加快处理速度
                    decoder.setTargetSampleSize(2)
                }
            } else {
                // API 28 以下使用传统方式
                @Suppress("DEPRECATION")
                android.graphics.BitmapFactory.decodeResource(
                    context.resources,
                    drawableResId,
                    android.graphics.BitmapFactory.Options().apply {
                        inSampleSize = 2
                    }
                )
            }
            
            // 2. 使用 Palette 提取颜色
            val palette = Palette.from(bitmap)
                .maximumColorCount(24)
                .generate()
            
            // 3. 智能选择最佳主题色
            val extractedColor = selectBestThemeColor(palette)
            
            // 4. 清理 Bitmap
            bitmap.recycle()
            
            // 5. 转换为 Compose Color (需要转换为 ULong)
            Color(extractedColor.toULong() or 0xFF000000UL)
        } catch (e: Exception) {
            Logger.e("ThemeColorExtractor", "同步提取主题色失败: ${e.message}")
            null
        }
    }
    
    /**
     * 从 Drawable 资源中提取主题色（异步）
     * 
     * 使用更智能的算法选择最佳主题色：
     * 1. 优先选择饱和度高的颜色（Vibrant）
     * 2. 如果没有鲜艳色，选择主导色（Dominant）
     * 3. 确保颜色适合作为主题色（不太暗、不太浅）
     * 
     * @param context Android Context
     * @param drawableResId Drawable 资源 ID
     * @return 提取的主题色，如果提取失败则返回默认的 Material 紫色
     */
    suspend fun extractThemeColor(
        @DrawableRes drawableResId: Int
    ): Color = withContext(Dispatchers.IO) {
        extractDominantColorSync(drawableResId) ?: Color(0xFF6750A4)
    }
    
    /**
     * 智能选择最佳主题色
     * 
     * 简化策略：直接使用 Dominant（主导色），这是图片中最常见的颜色
     */
    private fun selectBestThemeColor(palette: Palette): Int {
        // 打印所有可用的颜色用于调试
        Logger.d("ThemeColorExtractor", "=== 调色板分析 ===")
        palette.dominantSwatch?.let { 
            Logger.d("ThemeColorExtractor", "Dominant: #${Integer.toHexString(it.rgb).uppercase()}")
        }
        palette.vibrantSwatch?.let { 
            Logger.d("ThemeColorExtractor", "Vibrant: #${Integer.toHexString(it.rgb).uppercase()}")
        }
        palette.mutedSwatch?.let { 
            Logger.d("ThemeColorExtractor", "Muted: #${Integer.toHexString(it.rgb).uppercase()}")
        }
        
        // 直接使用 Dominant 色（主导色）
        palette.dominantSwatch?.let { swatch ->
            Logger.d("ThemeColorExtractor", "✓ 选择 Dominant 色: #${Integer.toHexString(swatch.rgb).uppercase()}")
            return swatch.rgb
        }
        
        // 如果没有 Dominant 色，尝试 Vibrant
        palette.vibrantSwatch?.let { swatch ->
            Logger.d("ThemeColorExtractor", "✓ 选择 Vibrant 色: #${Integer.toHexString(swatch.rgb).uppercase()}")
            return swatch.rgb
        }
        
        // 如果都没有，返回默认颜色
        Logger.d("ThemeColorExtractor", "✗ 使用默认颜色")
        return 0xFF6750A4.toInt()
    }
    
    /**
     * 批量提取多个背景的主题色
     * 
     * @param drawableResIds Drawable 资源 ID 列表
     * @return 提取的主题色列表
     */
    suspend fun extractThemeColors(
        drawableResIds: List<Int>
    ): List<Color> = withContext(Dispatchers.IO) {
        drawableResIds.map { resId ->
            extractThemeColor(resId)
        }
    }
}
