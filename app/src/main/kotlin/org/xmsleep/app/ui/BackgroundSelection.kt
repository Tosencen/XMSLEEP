package org.xmsleep.app.ui

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.xmsleep.app.R
import org.xmsleep.app.utils.Logger

/**
 * 背景选择枚举
 * 
 * 表示用户可以选择的背景动画选项
 */
enum class BackgroundSelection(
    val value: String, 
    val resourceId: Int?,
    val thumbnailResourceId: Int?,
    val themeColor: Color?
) {
    None("none", null, null, null),
    
    /**
     * 自定义背景（用户上传的图片/GIF/MP4）
     */
    Custom("custom", null, null, null),
    
    Background1("bg_1", R.drawable.bg_animation_1, R.drawable.bg_animation_1_thumb_png, Color(0xFF515487)),
    Background6("bg_6", R.drawable.bg_animation_6, R.drawable.bg_animation_6_thumb_png, Color(0xFF5B9BD5));

    companion object {
        fun fromValue(value: String): BackgroundSelection {
            return try {
                values().find { it.value == value } ?: None
            } catch (e: Exception) {
                Logger.e("BackgroundSelection", "Failed to parse value: $value", e)
                None
            }
        }
    }
    
    fun getDisplayName(context: Context): String {
        return when (this) {
            None -> context.getString(R.string.no_background)
            Custom -> context.getString(R.string.custom_background)
            Background1 -> context.getString(R.string.background_1)
            Background6 -> context.getString(R.string.background_6)
        }
    }
    
    fun isResourceValid(context: Context): Boolean {
        if (this == None || this == Custom) return true
        return try {
            context.resources.getDrawable(resourceId!!, null)
            true
        } catch (e: Exception) {
            Logger.e("BackgroundSelection", "Failed to load resource: $resourceId", e)
            false
        }
    }
}
