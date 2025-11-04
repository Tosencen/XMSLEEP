package org.streambox.datasource.xmbox.utils

import android.util.Log
import kotlinx.serialization.json.*
import org.streambox.datasource.xmbox.model.VideoItem
import org.streambox.datasource.xmbox.model.parseVideoItem

/**
 * Result解析工具类
 * 从XMBOX的Result类移植
 * 支持fromJson、fromXml、fromType等方法
 */
object ResultParser {
    
    private const val TAG = "ResultParser"
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    /**
     * 从JSON字符串解析Result，返回视频列表
     * 对应XMBOX的Result.fromJson()
     */
    fun fromJson(jsonString: String): List<VideoItem> {
        return try {
            val jsonElement = json.parseToJsonElement(jsonString)
            parseVideoList(jsonElement)
        } catch (e: Exception) {
            Log.e(TAG, "Result.fromJson失败: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * 从XML字符串解析Result，返回视频列表
     * 对应XMBOX的Result.fromXml()
     * 注意：XML解析暂未实现，需要xml解析库
     */
    fun fromXml(xmlString: String): List<VideoItem> {
        // TODO: 实现XML解析（需要使用XML解析库）
        Log.w(TAG, "XML解析暂未实现")
        return emptyList()
    }
    
    /**
     * 根据type类型解析内容
     * 对应XMBOX的Result.fromType()
     * type == 0: XML格式
     * type >= 1: JSON格式
     */
    fun fromType(type: Int, content: String): List<VideoItem> {
        return when (type) {
            0 -> fromXml(content) // XML格式
            else -> fromJson(content) // JSON格式
        }
    }
    
    /**
     * 解析视频列表
     */
    private fun parseVideoList(jsonElement: JsonElement): List<VideoItem> {
        return try {
            if (jsonElement is JsonObject) {
                // 检查code字段
                val code = jsonElement["code"]?.jsonPrimitive?.intOrNull ?: 1
                
                // 如果code != 1且有msg，记录警告
                if (code != 1) {
                    val msg = jsonElement["msg"]?.jsonPrimitive?.content
                    if (!msg.isNullOrBlank()) {
                        Log.w(TAG, "Result返回错误: code=$code, msg=$msg")
                    }
                    // 即使code != 1，也尝试解析list（某些源可能code=0但仍有数据）
                }
                
                // 尝试获取list字段
                val listJson = jsonElement["list"]?.jsonArray
                
                if (listJson != null) {
                    listJson.mapNotNull { itemJson ->
                        parseVideoItem(itemJson)
                    }
                } else {
                    // 如果没有list字段，检查是否是直接的视频对象
                    parseVideoItem(jsonElement)?.let { listOf(it) } ?: emptyList()
                }
            } else if (jsonElement is JsonArray) {
                // 直接是数组格式
                jsonElement.mapNotNull { itemJson ->
                    parseVideoItem(itemJson)
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析视频列表失败: ${e.message}", e)
            emptyList()
        }
    }
}

