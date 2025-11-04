package org.streambox.datasource.xmbox

import android.content.Context
import org.streambox.datasource.xmbox.config.XmboxConfig
import org.streambox.datasource.xmbox.config.XmboxConfigParser
import org.streambox.datasource.xmbox.model.VideoItem

/**
 * XMBOX MediaSource 实现
 * 
 * 这是 StreamBox 项目中 XMBOX 视频源的 MediaSource 实现。
 * 它使用用户添加的 XMBOX 配置来搜索和获取视频资源。
 * 
 * 注意：这是一个简化的实现，主要用于 StreamBox 项目。
 * 如果需要与 Animeko 架构完全集成，需要添加 Animeko 的 datasource:api 依赖。
 */
class XmboxMediaSource(
    private val context: Context,
    val mediaSourceId: String,
    val config: XmboxConfig,
    private val videoSource: XmboxVideoSource = XmboxVideoSource.create(context)
) {
    
    /**
     * 数据源信息
     */
    val displayName: String
        get() = when (config) {
            is XmboxConfig.VodConfig -> config.name.ifBlank { "XMBOX 点播源" }
            is XmboxConfig.LiveConfig -> config.name.ifBlank { "XMBOX 直播源" }
            is XmboxConfig.JavaScriptConfig -> config.name.ifBlank { "XMBOX JavaScript 源" }
        }
    
    /**
     * 检查数据源连接
     */
    suspend fun checkConnection(): Boolean {
        return try {
            when (config) {
                is XmboxConfig.JavaScriptConfig -> {
                    // 对于 JavaScript 配置，尝试执行 home() 函数
                    videoSource.getHomeVideoList(config).isNotEmpty()
                }
                else -> {
                    // 对于简单的 JSON 配置，检查 URL 是否可访问
                    true // 简化处理，实际应该检查 URL 可访问性
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 搜索视频
     * 
     * @param query 搜索关键词（通常是条目名称或剧集名称）
     * @return 视频列表
     */
    suspend fun searchVideos(query: String): List<VideoItem> {
        return try {
            when (config) {
                is XmboxConfig.JavaScriptConfig -> {
                    // TODO: 实现搜索功能
                    // 对于 JavaScript 配置，需要调用 search() 函数
                    // 目前先返回首页列表
                    videoSource.getHomeVideoList(config)
                        .filter { item -> 
                            item.name.contains(query, ignoreCase = true) ||
                            item.des.contains(query, ignoreCase = true)
                        }
                }
                else -> {
                    // 对于简单配置，无法搜索，返回空列表
                    emptyList()
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 获取首页视频列表
     */
    suspend fun getHomeVideoList(): List<VideoItem> {
        return try {
            when (config) {
                is XmboxConfig.JavaScriptConfig -> {
                    videoSource.getHomeVideoList(config)
                }
                else -> {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 从视频 ID 获取播放 URL
     */
    suspend fun getPlayUrl(videoId: String): String? {
        return try {
            when (config) {
                is XmboxConfig.JavaScriptConfig -> {
                    videoSource.extractPlayUrl(videoId, config)
                }
                is XmboxConfig.VodConfig -> {
                    // 对于简单的 VOD 配置，videoId 可能就是 URL
                    if (videoId.startsWith("http")) {
                        videoId
                    } else {
                        config.url
                    }
                }
                is XmboxConfig.LiveConfig -> {
                    config.url
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    companion object {
        /**
         * 创建 XmboxMediaSource 实例
         */
        fun create(
            context: Context,
            mediaSourceId: String,
            configString: String,
            name: String = ""
        ): XmboxMediaSource? {
            val config = XmboxConfigParser.parse(configString, name) ?: return null
            if (!XmboxConfigParser.isValid(config)) return null
            
            return XmboxMediaSource(
                context = context,
                mediaSourceId = mediaSourceId,
                config = config
            )
        }
    }
}

