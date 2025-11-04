package org.streambox.datasource.xmbox.model

import kotlinx.serialization.json.*

/**
 * 解析单个VideoItem的扩展函数
 * 从XMBOX的Vod类移植
 */
fun parseVideoItem(itemJson: JsonElement): VideoItem? {
    return try {
        if (itemJson !is JsonObject) return null
        
        VideoItem(
            id = itemJson["vod_id"]?.jsonPrimitive?.content
                ?: itemJson["id"]?.jsonPrimitive?.content
                ?: "",
            name = itemJson["vod_name"]?.jsonPrimitive?.content
                ?: itemJson["name"]?.jsonPrimitive?.content
                ?: itemJson["title"]?.jsonPrimitive?.content
                ?: "",
            pic = itemJson["vod_pic"]?.jsonPrimitive?.content
                ?: itemJson["pic"]?.jsonPrimitive?.content
                ?: itemJson["image"]?.jsonPrimitive?.content
                ?: itemJson["cover"]?.jsonPrimitive?.content
                ?: "",
            note = itemJson["vod_remarks"]?.jsonPrimitive?.content
                ?: itemJson["note"]?.jsonPrimitive?.content
                ?: itemJson["remarks"]?.jsonPrimitive?.content
                ?: itemJson["subtitle"]?.jsonPrimitive?.content
                ?: "",
            url = itemJson["vod_id"]?.jsonPrimitive?.content
                ?: itemJson["id"]?.jsonPrimitive?.content
                ?: "",
            vodId = itemJson["vod_id"]?.jsonPrimitive?.content
                ?: itemJson["id"]?.jsonPrimitive?.content
                ?: "",
            type = itemJson["type_id"]?.jsonPrimitive?.content
                ?: itemJson["type"]?.jsonPrimitive?.content
                ?: itemJson["type_name"]?.jsonPrimitive?.content
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
                ?: itemJson["content"]?.jsonPrimitive?.content
                ?: itemJson["description"]?.jsonPrimitive?.content
                ?: "",
            last = itemJson["vod_time"]?.jsonPrimitive?.content
                ?: itemJson["last"]?.jsonPrimitive?.content
                ?: itemJson["update_time"]?.jsonPrimitive?.content
                ?: "",
            // 播放源相关字段（XMBOX格式）
            vodPlayFrom = itemJson["vod_play_from"]?.jsonPrimitive?.content
                ?: itemJson["play_from"]?.jsonPrimitive?.content
                ?: "",
            vodPlayUrl = itemJson["vod_play_url"]?.jsonPrimitive?.content
                ?: itemJson["play_url"]?.jsonPrimitive?.content
                ?: itemJson["vod_play_urls"]?.jsonPrimitive?.content
                ?: ""
        )
    } catch (e: Exception) {
        android.util.Log.e("VideoItem", "解析单个视频项失败: ${e.message}")
        null
    }
}

