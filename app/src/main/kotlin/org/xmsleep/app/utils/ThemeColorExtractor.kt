package org.xmsleep.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

class ThemeColorExtractor(private val context: Context) {

    fun extractDominantColorSync(@DrawableRes drawableResId: Int): Color? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.resources, drawableResId)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setTargetSampleSize(2)
                }
            } else {
                @Suppress("DEPRECATION")
                android.graphics.BitmapFactory.decodeResource(
                    context.resources,
                    drawableResId,
                    android.graphics.BitmapFactory.Options().apply {
                        inSampleSize = 2
                    }
                )
            }
            bitmap?.let { extractFromBitmap(it) }
        } catch (e: Exception) {
            Logger.e("ThemeColorExtractor", "同步提取主题色失败: ${e.message}", e)
            null
        }
    }

    fun extractDominantColorFromUri(uri: Uri): Color? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setTargetSampleSize(2)
                }
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    .let { bmp ->
                        Bitmap.createScaledBitmap(bmp, bmp.width / 2, bmp.height / 2, true)
                    }
            }
            bitmap?.let { extractFromBitmap(it) }
        } catch (e: FileNotFoundException) {
            Logger.e("ThemeColorExtractor", "文件未找到: $uri", e)
            null
        } catch (e: Exception) {
            Logger.e("ThemeColorExtractor", "从URI提取主题色失败: ${e.message}", e)
            null
        }
    }

    private fun extractFromBitmap(bitmap: Bitmap): Color {
        val palette = Palette.from(bitmap)
            .maximumColorCount(24)
            .generate()
        val extractedColor = selectBestThemeColor(palette)
        bitmap.recycle()
        val color = Color(extractedColor.toULong() or 0xFF000000UL)
        Logger.d("ThemeColorExtractor", "提取颜色成功: #${extractedColor.toString(16).uppercase()}")
        return color
    }

    suspend fun extractThemeColor(
        @DrawableRes drawableResId: Int
    ): Color = withContext(Dispatchers.IO) {
        extractDominantColorSync(drawableResId) ?: Color(0xFF6750A4)
    }

    private fun selectBestThemeColor(palette: Palette): Int {
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

        palette.dominantSwatch?.let { swatch ->
            Logger.d("ThemeColorExtractor", "✓ 选择 Dominant 色: #${Integer.toHexString(swatch.rgb).uppercase()}")
            return swatch.rgb
        }
        palette.vibrantSwatch?.let { swatch ->
            Logger.d("ThemeColorExtractor", "✓ 选择 Vibrant 色: #${Integer.toHexString(swatch.rgb).uppercase()}")
            return swatch.rgb
        }
        Logger.d("ThemeColorExtractor", "✗ 使用默认颜色")
        return 0xFF6750A4.toInt()
    }

    suspend fun extractThemeColors(
        drawableResIds: List<Int>
    ): List<Color> = withContext(Dispatchers.IO) {
        drawableResIds.map { resId ->
            extractThemeColor(resId)
        }
    }
}
