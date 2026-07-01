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
    Background2("bg_2", R.drawable.bg_animation_2, R.drawable.bg_animation_2_thumb_png, Color(0xFF264E70)),
    Background3("bg_3", R.drawable.bg_animation_3, R.drawable.bg_animation_3_thumb_png, Color(0xFFB94E67)),
    Background4("bg_4", R.drawable.bg_animation_4, R.drawable.bg_animation_4_thumb_png, Color(0xFFB7C66A)),
    Background5("bg_5", R.drawable.bg_animation_5, R.drawable.bg_animation_5_thumb_png, Color(0xFF8DA89C)),
    Background6("bg_6", R.drawable.bg_animation_6, R.drawable.bg_animation_6_thumb_png, Color(0xFF5B9BD5)),
    Background7("bg_7", R.drawable.bg_animation_7, R.drawable.bg_animation_7_thumb_png, Color(0xFF7CB342));

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
            Background2 -> context.getString(R.string.background_2)
            Background3 -> context.getString(R.string.background_3)
            Background4 -> context.getString(R.string.background_4)
            Background5 -> context.getString(R.string.background_5)
            Background6 -> context.getString(R.string.background_6)
            Background7 -> context.getString(R.string.background_7)
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
