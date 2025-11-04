package org.streambox.datasource.xmbox.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * XMBOX 视频源配置
 * 支持 JSON 格式和 JavaScript 格式
 */
@Serializable
sealed class XmboxConfig {
    /**
     * JSON 格式配置（点播 VOD）
     */
    @Serializable
    data class VodConfig(
        val url: String,
        val name: String = "",
        val searchable: Boolean = true,
        val changeable: Boolean = true,
        val quickSearch: Boolean = true,
        val timeout: Int = 15
    ) : XmboxConfig()
    
    /**
     * JSON 格式配置（直播 LIVE）
     */
    @Serializable
    data class LiveConfig(
        val url: String,
        val name: String = "",
        val ua: String? = null,
        val origin: String? = null,
        val referer: String? = null,
        val timeout: Int = 15
    ) : XmboxConfig()
    
    /**
     * JavaScript 格式配置（CatVodTV 格式）
     */
    @Serializable
    data class JavaScriptConfig(
        val url: String,
        val name: String = "",
        val jsCode: String,
        val type: SourceType = SourceType.VOD,
        val timeout: Int = 30
    ) : XmboxConfig()
}

enum class SourceType {
    VOD,  // 点播
    LIVE  // 直播
}

