package org.xmsleep.app.audio

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.xmsleep.app.audio.model.SoundSegment
import java.io.InputStream

/**
 * 从 JSON 文件加载片段元数据
 * 支持从 assets 或 raw 资源加载
 */
object SegmentsMetadataLoader {
    private val gson = Gson()
    
    /**
     * 片段元数据 JSON 结构
     */
    data class SegmentsMetadataJson(
        val audioId: String,
        val segments: List<SoundSegmentJson>,
        val segmentCount: Int
    )
    
    data class SoundSegmentJson(
        val name: String,
        val basePath: String,
        val isFree: Boolean = true,
        val localResourceId: Int? = null,
        val remoteUrl: String? = null
    )
    
    /**
     * 从 assets 加载片段元数据
     */
    fun loadFromAssets(context: Context, fileName: String): List<SoundSegment>? {
        return try {
            val inputStream = context.assets.open(fileName)
            loadFromInputStream(inputStream)
        } catch (e: Exception) {
            android.util.Log.e("SegmentsMetadataLoader", "加载片段元数据失败: ${e.message}")
            null
        }
    }
    
    /**
     * 从 raw 资源加载片段元数据
     */
    fun loadFromRaw(context: Context, resourceId: Int): List<SoundSegment>? {
        return try {
            val inputStream = context.resources.openRawResource(resourceId)
            loadFromInputStream(inputStream)
        } catch (e: Exception) {
            android.util.Log.e("SegmentsMetadataLoader", "加载片段元数据失败: ${e.message}")
            null
        }
    }
    
    /**
     * 从输入流加载片段元数据
     */
    private fun loadFromInputStream(inputStream: InputStream): List<SoundSegment>? {
        return try {
            val json = inputStream.bufferedReader().use { it.readText() }
            val metadata = gson.fromJson(json, SegmentsMetadataJson::class.java)
            
            metadata.segments.map { segJson ->
                SoundSegment(
                    name = segJson.name,
                    basePath = segJson.basePath,
                    isFree = segJson.isFree,
                    localResourceId = segJson.localResourceId,
                    remoteUrl = segJson.remoteUrl
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("SegmentsMetadataLoader", "解析片段元数据失败: ${e.message}")
            null
        } finally {
            inputStream.close()
        }
    }
    
    /**
     * 从远程 URL 加载片段元数据（异步）
     */
    fun loadFromRemote(
        url: String,
        callback: (List<SoundSegment>?) -> Unit
    ) {
        // 使用协程或 Retrofit 等网络库加载
        // 这里简化处理，实际需要网络请求
        android.util.Log.w("SegmentsMetadataLoader", "远程加载片段元数据尚未实现: $url")
        callback(null)
    }
}

