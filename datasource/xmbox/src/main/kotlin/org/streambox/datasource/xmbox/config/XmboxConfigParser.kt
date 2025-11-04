package org.streambox.datasource.xmbox.config

import kotlinx.serialization.json.*

/**
 * XMBOX 配置解析器
 * 支持解析 JSON 格式和 JavaScript 格式的配置
 */
object XmboxConfigParser {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    /**
     * 解析配置字符串
     * 
     * @param configString 配置字符串，可能是 JSON 或 JavaScript 代码
     * @param name 源名称（可选）
     * @return 解析后的配置
     */
    fun parse(configString: String, name: String = ""): XmboxConfig? {
        if (configString.isBlank()) return null
        
        return try {
            // 尝试解析为 JSON
            parseJson(configString, name)
        } catch (e: Exception) {
            // 如果 JSON 解析失败，尝试解析为 JavaScript
            parseJavaScript(configString, name)
        }
    }
    
    /**
     * 解析 JSON 格式配置
     */
    private fun parseJson(jsonString: String, name: String): XmboxConfig? {
        val jsonElement = json.parseToJsonElement(jsonString)
        
        if (jsonElement is JsonObject) {
            val url = jsonElement["url"]?.jsonPrimitive?.content
                ?: return null
            
            // 判断是点播还是直播
            val type = jsonElement["type"]?.jsonPrimitive?.content?.lowercase()
            
            return when (type) {
                "live", "直播" -> {
                    XmboxConfig.LiveConfig(
                        url = url,
                        name = jsonElement["name"]?.jsonPrimitive?.content ?: name,
                        ua = jsonElement["ua"]?.jsonPrimitive?.content,
                        origin = jsonElement["origin"]?.jsonPrimitive?.content,
                        referer = jsonElement["referer"]?.jsonPrimitive?.content,
                        timeout = jsonElement["timeout"]?.jsonPrimitive?.intOrNull ?: 15
                    )
                }
                else -> {
                    // 默认为点播
                    XmboxConfig.VodConfig(
                        url = url,
                        name = jsonElement["name"]?.jsonPrimitive?.content ?: name,
                        searchable = jsonElement["searchable"]?.jsonPrimitive?.intOrNull?.let { it == 1 } ?: true,
                        changeable = jsonElement["changeable"]?.jsonPrimitive?.intOrNull?.let { it == 1 } ?: true,
                        quickSearch = jsonElement["quickSearch"]?.jsonPrimitive?.intOrNull?.let { it == 1 } ?: true,
                        timeout = jsonElement["timeout"]?.jsonPrimitive?.intOrNull ?: 15
                    )
                }
            }
        }
        
        return null
    }
    
    /**
     * 解析 JavaScript 格式配置（CatVodTV 格式）
     * 
     * CatVodTV 格式的 JavaScript 代码包含以下函数：
     * - home() - 首页数据
     * - category(tid, page, filter, extend) - 分类数据
     * - detail(id) - 详情数据
     * - play(vid, flag) - 播放地址
     * - search(wd, quick) - 搜索
     */
    private fun parseJavaScript(jsCode: String, name: String): XmboxConfig? {
        if (jsCode.isBlank()) return null
        
        // 尝试从 JavaScript 代码中提取 URL
        val urlRegex = Regex("""['"](https?://[^'"]+)['"]""")
        val urlMatch = urlRegex.find(jsCode)
        val url = urlMatch?.groupValues?.get(1) ?: ""
        
        if (url.isBlank()) {
            // 如果没有找到 URL，这可能是纯 JavaScript 配置，需要从外部传入 URL
            return null
        }
        
        // 判断类型：检查是否包含直播相关的关键词
        val type = if (jsCode.contains(Regex("""live|直播|Live""", RegexOption.IGNORE_CASE))) {
            SourceType.LIVE
        } else {
            SourceType.VOD
        }
        
        return XmboxConfig.JavaScriptConfig(
            url = url,
            name = name,
            jsCode = jsCode,
            type = type
        )
    }
    
    /**
     * 验证配置是否有效
     */
    fun isValid(config: XmboxConfig): Boolean {
        return when (config) {
            is XmboxConfig.VodConfig -> config.url.isNotBlank()
            is XmboxConfig.LiveConfig -> config.url.isNotBlank()
            is XmboxConfig.JavaScriptConfig -> config.url.isNotBlank() && config.jsCode.isNotBlank()
        }
    }
}

