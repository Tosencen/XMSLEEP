package org.xmsleep.app.audio.model

import com.google.gson.annotations.SerializedName

/**
 * 音频来源类型
 */
enum class AudioSource {
    @SerializedName("LOCAL")
    LOCAL,      // 本地资源（打包在APK中）
    @SerializedName("REMOTE")
    REMOTE      // 网络资源（从GitHub下载）
}

/**
 * 音频元数据
 */
data class SoundMetadata(
    // 基本信息
    val id: String,                    // 唯一标识符
    val name: String,                  // 显示名称
    val nameEn: String? = null,        // 英文名称（可选）
    val category: String,              // 分类（如 "Nature", "Rain"）
    val icon: String? = null,          // 图标（emoji或资源ID）
    
    // 资源信息
    val source: AudioSource,           // 资源来源
    val localResourceId: Int? = null,   // 本地资源ID（R.raw.xxx）
    val remoteUrl: String? = null,     // 网络资源URL（GitHub raw URL）
    
    // 播放参数
    val loopStart: Long = 500L,         // 循环起点（毫秒）
    val loopEnd: Long,                  // 循环终点（毫秒）
    val isSeamless: Boolean = true,    // 是否无缝循环
    
    // 元数据
    val duration: Long? = null,         // 总时长（毫秒，可选）
    val fileSize: Long? = null,        // 文件大小（字节，可选）
    val format: String = "mp3",         // 音频格式（mp3, ogg, wav等）
    
    // 显示控制
    val isVisible: Boolean = true,      // 是否显示
    val order: Int = 0                  // 显示顺序
)

/**
 * 音频分类
 */
data class SoundCategory(
    val id: String,                    // 分类ID
    val name: String,                  // 分类名称
    val nameEn: String? = null,        // 英文名称（可选）
    val icon: String? = null,         // 图标（emoji或资源ID）
    val order: Int = 0                 // 显示顺序
)

/**
 * 音频清单
 */
data class SoundsManifest(
    val version: String,               // 清单版本
    val categories: List<SoundCategory>, // 分类列表
    val sounds: List<SoundMetadata>    // 音频列表
)

