package org.xmsleep.app.quote

import kotlinx.serialization.Serializable

/**
 * 名句数据模型
 */
@Serializable
data class Quote(
    val id: Int = 0,
    val uuid: String = "",
    val hitokoto: String, // 名句内容
    val type: String = "", // 分类
    val from: String? = null, // 来源
    val from_who: String? = null, // 作者
    val creator: String = "",
    val creator_uid: Int = 0,
    val reviewer: Int = 0,
    val commit_from: String = "",
    val created_at: String = "",
    val length: Int = 0
) {
    // 便捷属性
    val text: String get() = hitokoto
    val author: String get() = from_who ?: "佚名"
}

/**
 * 本地名句清单
 */
@Serializable
data class QuotesManifest(
    val version: String,
    val quotes: List<LocalQuote>
)

/**
 * 本地名句数据模型
 */
@Serializable
data class LocalQuote(
    val id: Int,
    val text: String,
    val author: String,
    val from: String? = null,
    val category: String = "励志"
) {
    // 转换为Quote对象
    fun toQuote(): Quote {
        return Quote(
            id = id,
            hitokoto = text,
            from = from,
            from_who = author
        )
    }
}
