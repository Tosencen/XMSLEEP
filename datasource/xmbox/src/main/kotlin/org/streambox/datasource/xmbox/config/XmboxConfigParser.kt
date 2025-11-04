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
     * 
     * XMBOX 支持的格式：
     * 1. 单源配置：包含 url, name, type 等字段的单个配置对象
     * 2. 多源配置：包含 sites 数组，每个 site 是一个视频源配置
     * 3. CatVodTV 格式：包含 sites 数组，每个 site 有 key, name, type, api 等字段
     */
    private fun parseJson(jsonString: String, name: String): XmboxConfig? {
        val jsonElement = json.parseToJsonElement(jsonString)
        
        if (jsonElement is JsonObject) {
            // 检查是否是包含 sites 数组的多源配置文件（CatVodTV/XMBOX 格式）
            val sitesArray = jsonElement["sites"]?.jsonArray
            if (sitesArray != null && sitesArray.isNotEmpty()) {
                // 这是一个多源配置文件，取第一个源作为默认源
                // 注意：XMBOX 会解析整个 sites 数组，但我们这里只返回第一个源
                val firstSite = sitesArray.firstOrNull()?.jsonObject ?: return null
                
                // 对于包含 sites 数组的 CatVodTV/XMBOX 多源配置文件
                // XMBOX 的处理方式是：将整个配置文件作为一个 JavaScript 配置
                // 在实际使用时，根据用户选择的具体源（通过 key 或 name）来加载对应的内容
                // 
                // 这里我们返回一个 JavaScriptConfig，其中：
                // - url: 原始配置文件的 URL（用于访问整个配置）
                // - jsCode: 整个 JSON 配置（序列化为字符串），这样可以在运行时解析 sites 数组
                // - name: 使用配置文件的名称，或者如果提供了 name 参数则使用它
                
                val configName = name.ifBlank { 
                    // 尝试从配置中提取名称
                    jsonElement["name"]?.jsonPrimitive?.content 
                        ?: "多源配置"
                }
                
                // 判断类型：检查 sites 数组中的类型
                val siteType = firstSite["type"]?.jsonPrimitive?.intOrNull ?: 0
                val sourceType = if (siteType == 2) SourceType.LIVE else SourceType.VOD
                
                // 返回 JavaScriptConfig，其中 jsCode 包含整个 JSON 配置
                // 这样在运行时可以解析并选择不同的源
                return XmboxConfig.JavaScriptConfig(
                    url = "", // URL 会在上层调用时设置
                    name = configName,
                    jsCode = jsonString, // 存储整个 JSON 配置
                    type = sourceType,
                    timeout = 30
                )
            }
            
            // 单源配置格式（标准的 XMBOX 格式）
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
            is XmboxConfig.JavaScriptConfig -> {
                // JavaScriptConfig 只需要 jsCode 不为空即可
                // url 可以为空，因为可能在后续设置（例如从 sites 数组中解析时）
                config.jsCode.isNotBlank()
            }
        }
    }
}

