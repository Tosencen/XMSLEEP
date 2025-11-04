package org.streambox.datasource.xmbox.model

import kotlinx.serialization.Serializable

/**
 * 视频项数据模型
 * 用于表示视频列表中的单个视频
 * 从XMBOX的Vod类移植，支持所有XMBOX格式字段
 */
@Serializable
data class VideoItem(
    val id: String,
    val name: String,
    val pic: String = "",  // 封面图片URL
    val note: String = "",  // 备注/描述
    val url: String = "",   // 视频详情页URL
    val type: String = "",  // 视频类型
    val vodId: String = "", // 视频ID
    val year: String = "",  // 年份
    val area: String = "",  // 地区
    val actor: String = "", // 演员
    val director: String = "", // 导演
    val des: String = "",   // 描述
    val last: String = "",  // 最后更新
    val playUrl: String = "", // 播放地址（如果有）
    val vodPlayFrom: String = "", // 播放源列表（XMBOX格式：用$$$分隔）
    val vodPlayUrl: String = ""    // 播放地址列表（XMBOX格式：用$$$分隔）
) {
    /**
     * 解析播放源列表
     * 从XMBOX的setVodFlags()方法移植
     */
    fun parsePlaySources(): List<PlaySource> {
        return if (vodPlayFrom.isNotBlank() && vodPlayUrl.isNotBlank()) {
            PlaySourceParser.parsePlaySources(vodPlayFrom, vodPlayUrl)
        } else if (playUrl.isNotBlank()) {
            // 如果没有播放源列表，但有一个播放地址，创建一个默认播放源
            listOf(PlaySource(flag = "默认", episodes = listOf(Episode(name = name, url = playUrl))))
        } else {
            emptyList()
        }
    }
    
    /**
     * 获取第一个播放源的第一个播放地址（用于快速播放）
     */
    fun getFirstPlayUrl(): String? {
        val sources = parsePlaySources()
        return sources.firstOrNull()?.episodes?.firstOrNull()?.url?.ifBlank { null }
            ?: playUrl.ifBlank { null }
    }
}

