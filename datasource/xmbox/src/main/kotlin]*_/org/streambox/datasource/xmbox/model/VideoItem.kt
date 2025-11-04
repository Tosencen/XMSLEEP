package org.streambox.datasource.xmbox.model

import kotlinx.serialization.Serializable

/**
 * 视频项数据模型
 * 用于表示视频列表中的单个视频
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
    val playUrl: String = "" // 播放地址（如果有）
)

