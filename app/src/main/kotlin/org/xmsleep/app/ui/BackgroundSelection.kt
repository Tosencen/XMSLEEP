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
    val thumbnailResourceId: Int?, // 新增：缩略图资源ID（用于提取主题色）
    val themeColor: Color? // 每个背景的推荐主题色（已废弃，改用缩略图提取）
) {
    /**
     * 无背景
     */
    None("none", null, null, null),
    
    /**
     * 背景动画 1 - 紫蓝色调
     * 提取的主导颜色: #515487 (RGB: 81, 84, 135)
     */
    Background1("bg_1", R.drawable.bg_animation_1, R.drawable.bg_animation_1_thumb_png, Color(0xFF515487)),
    
    /**
     * 背景动画 2 - 深蓝色调
     * 提取的主导颜色: #264E70 (RGB: 38, 78, 112)
     */
    Background2("bg_2", R.drawable.bg_animation_2, R.drawable.bg_animation_2_thumb_png, Color(0xFF264E70)),
    
    /**
     * 背景动画 3 - 玫瑰红色调
     * 提取的主导颜色: #B94E67 (RGB: 185, 78, 103)
     */
    Background3("bg_3", R.drawable.bg_animation_3, R.drawable.bg_animation_3_thumb_png, Color(0xFFB94E67)),
    
    /**
     * 背景动画 4 - 黄绿色调
     * 提取的主导颜色: #B7C66A (RGB: 183, 198, 106)
     */
    Background4("bg_4", R.drawable.bg_animation_4, R.drawable.bg_animation_4_thumb_png, Color(0xFFB7C66A)),
    
    /**
     * 背景动画 5 - 青绿色调
     * 提取的主导颜色: #8DA89C (RGB: 141, 168, 156)
     */
    Background5("bg_5", R.drawable.bg_animation_5, R.drawable.bg_animation_5_thumb_png, Color(0xFF8DA89C));
    
    companion object {
        /**
         * 从字符串值解析枚举
         * 
         * @param value 存储在 SharedPreferences 中的字符串值
         * @return 对应的枚举值，如果解析失败则返回 None
         */
        fun fromValue(value: String): BackgroundSelection {
            return try {
                values().find { it.value == value } ?: None
            } catch (e: Exception) {
                Logger.e("BackgroundSelection", "Failed to parse value: $value", e)
                None
            }
        }
    }
    
    /**
     * 获取本地化的显示名称
     * 
     * @param context Android Context
     * @return 本地化的显示名称
     */
    fun getDisplayName(context: Context): String {
        return when (this) {
            None -> context.getString(R.string.no_background)
            Background1 -> context.getString(R.string.background_1)
            Background2 -> context.getString(R.string.background_2)
            Background3 -> context.getString(R.string.background_3)
            Background4 -> context.getString(R.string.background_4)
            Background5 -> context.getString(R.string.background_5)
        }
    }
    
    /**
     * 验证资源是否有效
     * 
     * @param context Android Context
     * @return 如果资源有效则返回 true，否则返回 false
     */
    fun isResourceValid(context: Context): Boolean {
        if (this == None) return true
        return try {
            context.resources.getDrawable(resourceId!!, null)
            true
        } catch (e: Exception) {
            Logger.e("BackgroundSelection", "Failed to load resource: $resourceId", e)
            false
        }
    }
}
