package org.streambox.datasource.xmbox

import android.content.Context
import org.streambox.datasource.xmbox.config.XmboxConfig
import org.streambox.datasource.xmbox.config.XmboxConfigParser
import org.streambox.datasource.xmbox.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/**
 * XMBOX 视频源识别器
 * 负责识别和处理 XMBOX 格式的视频源配置
 */
class XmboxVideoSource(
    private val context: Context,
    private val extractor: VideoUrlExtractor = WebViewVideoUrlExtractor(context)
) {
    private val jsExecutor = CatVodTVJavaScriptExecutor(context)
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    /**
     * 识别并解析视频源配置
     * 
     * @param configString 配置字符串（JSON 或 JavaScript）
     * @param name 源名称（可选）
     * @return 解析后的配置，如果解析失败则返回 null
     */
    suspend fun identify(configString: String, name: String = ""): XmboxConfig? {
        return withContext(Dispatchers.Default) {
            val config = XmboxConfigParser.parse(configString, name)
            if (config != null && XmboxConfigParser.isValid(config)) {
                config
            } else {
                null
            }
        }
    }
    
    /**
     * 从视频源配置中提取播放 URL
     * 
     * @param pageUrl 页面 URL 或视频 ID
     * @param config 视频源配置
     * @return 视频播放 URL，如果提取失败则返回 null
     */
    suspend fun extractPlayUrl(pageUrl: String, config: XmboxConfig): String? {
        return withContext(Dispatchers.Main) {
            try {
                extractor.extractVideoUrl(pageUrl, config)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * 验证配置是否有效
     */
    fun validateConfig(configString: String): Boolean {
        val config = XmboxConfigParser.parse(configString)
        return config != null && XmboxConfigParser.isValid(config)
    }
    
    /**
     * 获取首页视频列表
     * 
     * @param config 视频源配置
     * @return 视频列表，如果获取失败则返回空列表
     */
    suspend fun getHomeVideoList(config: XmboxConfig): List<VideoItem> {
        return withContext(Dispatchers.Default) {
            try {
                when (config) {
                    is XmboxConfig.VodConfig -> {
                        // 对于简单的 JSON 配置，可能需要直接访问 API
                        // 这里暂时返回空列表，后续可以根据实际需求扩展
                        emptyList()
                    }
                    
                    is XmboxConfig.LiveConfig -> {
                        // 直播源不需要首页列表
                        emptyList()
                    }
                    
                    is XmboxConfig.JavaScriptConfig -> {
                        // 对于 JavaScript 配置，执行 home() 函数
                        val jsonString = jsExecutor.executeHome(
                            config.url,
                            config.jsCode,
                            config.timeout.toLong()
                        )
                        
                        if (jsonString != null) {
                            parseVideoListJson(jsonString)
                        } else {
                            emptyList()
                        }
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * 解析视频列表 JSON
     */
    private fun parseVideoListJson(jsonString: String): List<VideoItem> {
        return try {
            val jsonElement = json.parseToJsonElement(jsonString)
            
            if (jsonElement is JsonObject) {
                // CatVodTV 格式：{ code: 1, list: [...] }
                val code = jsonElement["code"]?.jsonPrimitive?.intOrNull ?: 0
                if (code != 1) {
                    return emptyList()
                }
                
                val listJson = jsonElement["list"]?.jsonArray ?: jsonElement["data"]?.jsonArray
                
                listJson?.mapNotNull { itemJson ->
                    if (itemJson is JsonObject) {
                        try {
                            VideoItem(
                                id = itemJson["vod_id"]?.jsonPrimitive?.content
                                    ?: itemJson["id"]?.jsonPrimitive?.content
                                    ?: "",
                                name = itemJson["vod_name"]?.jsonPrimitive?.content
                                    ?: itemJson["name"]?.jsonPrimitive?.content
                                    ?: "",
                                pic = itemJson["vod_pic"]?.jsonPrimitive?.content
                                    ?: itemJson["pic"]?.jsonPrimitive?.content
                                    ?: "",
                                note = itemJson["vod_remarks"]?.jsonPrimitive?.content
                                    ?: itemJson["note"]?.jsonPrimitive?.content
                                    ?: "",
                                url = itemJson["vod_id"]?.jsonPrimitive?.content
                                    ?: itemJson["id"]?.jsonPrimitive?.content
                                    ?: "",
                                type = itemJson["type_id"]?.jsonPrimitive?.content
                                    ?: itemJson["type"]?.jsonPrimitive?.content
                                    ?: "",
                                year = itemJson["vod_year"]?.jsonPrimitive?.content
                                    ?: itemJson["year"]?.jsonPrimitive?.content
                                    ?: "",
                                area = itemJson["vod_area"]?.jsonPrimitive?.content
                                    ?: itemJson["area"]?.jsonPrimitive?.content
                                    ?: "",
                                actor = itemJson["vod_actor"]?.jsonPrimitive?.content
                                    ?: itemJson["actor"]?.jsonPrimitive?.content
                                    ?: "",
                                director = itemJson["vod_director"]?.jsonPrimitive?.content
                                    ?: itemJson["director"]?.jsonPrimitive?.content
                                    ?: "",
                                des = itemJson["vod_content"]?.jsonPrimitive?.content
                                    ?: itemJson["des"]?.jsonPrimitive?.content
                                    ?: "",
                                last = itemJson["vod_time"]?.jsonPrimitive?.content
                                    ?: itemJson["last"]?.jsonPrimitive?.content
                                    ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                } ?: emptyList()
            } else if (jsonElement is JsonArray) {
                // 直接是数组格式
                jsonElement.mapNotNull { itemJson ->
                    if (itemJson is JsonObject) {
                        try {
                            VideoItem(
                                id = itemJson["vod_id"]?.jsonPrimitive?.content
                                    ?: itemJson["id"]?.jsonPrimitive?.content
                                    ?: "",
                                name = itemJson["vod_name"]?.jsonPrimitive?.content
                                    ?: itemJson["name"]?.jsonPrimitive?.content
                                    ?: "",
                                pic = itemJson["vod_pic"]?.jsonPrimitive?.content
                                    ?: itemJson["pic"]?.jsonPrimitive?.content
                                    ?: "",
                                note = itemJson["vod_remarks"]?.jsonPrimitive?.content
                                    ?: itemJson["note"]?.jsonPrimitive?.content
                                    ?: "",
                                url = itemJson["vod_id"]?.jsonPrimitive?.content
                                    ?: itemJson["id"]?.jsonPrimitive?.content
                                    ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    companion object {
        /**
         * 创建实例
         */
        fun create(context: Context): XmboxVideoSource {
            return XmboxVideoSource(context)
        }
    }
}

