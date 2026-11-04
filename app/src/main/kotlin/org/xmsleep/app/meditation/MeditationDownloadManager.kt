package org.xmsleep.app.meditation

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.xmsleep.app.ui.meditation.MeditationSession
import org.xmsleep.app.utils.Logger
import org.xmsleep.app.utils.NetworkClient
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 冥想音频下载管理（离线播放）
 *
 * - 独立目录 filesDir/meditation_cache/，与白噪音 audio_cache（500MB 上限）互不抢占
 * - 文件名 med_<sessionId>.<ext>，启动时扫描目录重建 downloadedIds
 * - 下载先写 .part，完整性校验通过才改名落盘；下载在单例内进行，切页/退出不中断
 * - 播放侧通过 [getCachedFile] 命中本地文件，未命中回落 B 站流式解析
 */
class MeditationDownloadManager private constructor(context: Context) {

    companion object {
        private const val TAG = "MeditationDownload"
        private const val DIR_NAME = "meditation_cache"
        private const val FILE_PREFIX = "med_"
        private const val BUFFER_SIZE = 64 * 1024

        @Volatile
        private var instance: MeditationDownloadManager? = null

        fun getInstance(context: Context): MeditationDownloadManager =
            instance ?: synchronized(this) {
                instance ?: MeditationDownloadManager(context.applicationContext).also {
                    it.scanCache()
                    instance = it
                }
            }
    }

    private val appContext = context.applicationContext
    private val cacheDir = File(appContext.filesDir, DIR_NAME)
    private val filesById = ConcurrentHashMap<String, File>()

    private val client = NetworkClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(600, TimeUnit.SECONDS) // 长音频（30min 级）给足总时长
        .build()

    /** 已下载完成的会话 id 集合 */
    private val _downloadedIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedIds: StateFlow<Set<String>> = _downloadedIds

    /** 下载中的会话：id → 进度 0f..1f */
    private val _downloadingIds = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadingIds: StateFlow<Map<String, Float>> = _downloadingIds

    /** 扫描缓存目录，重建已下载索引（首次 getInstance 时执行，文件数 ≤ 40，开销可忽略） */
    private fun scanCache() {
        try {
            if (!cacheDir.exists()) return
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.startsWith(FILE_PREFIX) && file.length() > 0) {
                    val id = file.name.removePrefix(FILE_PREFIX).substringBeforeLast('.')
                    if (id.isNotEmpty() && !file.name.endsWith(".part")) {
                        filesById[id] = file
                    }
                }
            }
            _downloadedIds.value = filesById.keys.toSet()
            Logger.d(TAG, "扫描到已下载音频 ${filesById.size} 个")
        } catch (e: Exception) {
            Logger.e(TAG, "扫描缓存目录失败: ${e.message}", e)
        }
    }

    /** 获取本地已下载文件，未下载返回 null */
    fun getCachedFile(sessionId: String): File? = filesById[sessionId]

    /**
     * 下载会话音频（B 站 sourceId 先解析再落盘，避免临时 URL 过期）
     * @return true=下载成功或已存在；false=失败（网络/解析/完整性校验）
     */
    suspend fun download(session: MeditationSession): Boolean = withContext(Dispatchers.IO) {
        val id = session.id
        if (_downloadedIds.value.contains(id)) return@withContext true
        if (_downloadingIds.value.containsKey(id)) return@withContext true // 已在下载

        if (!cacheDir.exists()) cacheDir.mkdirs()
        _downloadingIds.value = _downloadingIds.value + (id to 0f)

        var partFile: File? = null
        try {
            val audioUrl = if (session.audioUrl.isNotEmpty()) {
                session.audioUrl
            } else if (session.sourceId.isNotEmpty()) {
                BilibiliAudioHelper.getAudioUrl(session.sourceId)
                    ?: throw IOException("音频地址解析失败")
            } else {
                throw IOException("会话无可用音频源")
            }

            val ext = extensionOf(audioUrl)
            val target = File(cacheDir, "$FILE_PREFIX$id$ext")
            val part = File(cacheDir, "$FILE_PREFIX$id$ext.part")
            partFile = part

            Logger.d(TAG, "开始下载 $id <- $audioUrl")
            val request = Request.Builder()
                .url(audioUrl)
                // 与 MeditationPlayerManager 播放器完全一致的请求头（B 站 CDN 校验 UA+Referer+Origin，缺一403）
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", "https://www.bilibili.com")
                .header("Origin", "https://www.bilibili.com")
                .get()
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                val body = resp.body ?: throw IOException("空响应")
                val total = body.contentLength()

                var received = 0L
                FileOutputStream(part).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var read: Int
                        var lastEmit = 0L
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            received += read
                            val now = System.currentTimeMillis()
                            if (now - lastEmit >= 150L) {
                                lastEmit = now
                                if (total > 0) {
                                    _downloadingIds.value =
                                        _downloadingIds.value + (id to received.toFloat() / total)
                                }
                            }
                        }
                    }
                }

                // 完整性校验：防截断
                if (total > 0 && received != total) {
                    throw IOException("下载不完整：$received/$total 字节")
                }

                if (target.exists()) target.delete()
                if (!part.renameTo(target)) {
                    part.copyTo(target, overwrite = true)
                    part.delete()
                }
                filesById[id] = target
                _downloadedIds.value = filesById.keys.toSet()
                Logger.d(TAG, "下载完成 $id ($received 字节)")
            }
            true
        } catch (e: Exception) {
            Logger.e(TAG, "下载失败 $id: ${e.message}", e)
            false
        } finally {
            partFile?.delete() // 清理残留半截文件
            _downloadingIds.value = _downloadingIds.value - id
        }
    }

    /** 删除单个已下载文件（供存储管理/二期使用） */
    fun remove(sessionId: String) {
        filesById.remove(sessionId)?.delete()
        _downloadedIds.value = filesById.keys.toSet()
    }

    /** 已下载总字节数（供存储管理/二期使用） */
    fun totalSize(): Long = filesById.values.sumOf { it.length() }

    private fun extensionOf(url: String): String {
        val path = try {
            Uri.parse(url).path ?: ""
        } catch (_: Exception) {
            ""
        }
        val ext = path.substringAfterLast('.', "")
        return if (ext.length in 2..5) ".$ext" else ".m4a"
    }
}
