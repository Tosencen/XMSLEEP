package org.xmsleep.app.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmsleep.app.audio.model.AudioSource
import org.xmsleep.app.audio.model.SoundMetadata
import org.xmsleep.app.audio.model.SoundsManifest
import java.io.File
import java.io.IOException

/**
 * 音频资源管理器
 * 负责管理所有音频资源（本地和网络）
 */
class AudioResourceManager private constructor(context: Context) {
    
    companion object {
        private const val TAG = "AudioResourceManager"
        
        @Volatile
        private var instance: AudioResourceManager? = null
        
        fun getInstance(context: Context): AudioResourceManager {
            return instance ?: synchronized(this) {
                instance ?: AudioResourceManager(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
    
    private val appContext: Context = context.applicationContext
    private val remoteLoader = RemoteAudioLoader(context)
    private val cacheManager = AudioCacheManager.getInstance(context)
    private val gson = Gson()
    
    // 音频清单缓存
    private var remoteManifest: SoundsManifest? = null
    
    // 持久化缓存文件名
    private val manifestCacheFile: File by lazy {
        File(appContext.cacheDir, "remote_manifest_cache.json")
    }
    
    /**
     * 初始化时操作：从 assets 加载默认数据，并检查版本更新
     */
    fun initializeDefaultManifest() {
        try {
            // 从 assets 读取最新清单
            val assetsJson = appContext.assets.open("sounds_remote.json").use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
            
            // 解析 assets 清单获取版本和音频数量
            val assetsManifest = gson.fromJson(assetsJson, SoundsManifest::class.java)
            val assetsVersion = assetsManifest.version
            val assetsSoundCount = assetsManifest.sounds.size
            
            // 检查是否需要更新缓存
            var needUpdate = false
            
            if (!manifestCacheFile.exists()) {
                // 缓存不存在，需要创建
                needUpdate = true
            } else {
                // 缓存存在，检查版本和音频数量
                try {
                    val cachedJson = manifestCacheFile.readText()
                    val cachedManifest = gson.fromJson(cachedJson, SoundsManifest::class.java)
                    val cachedVersion = cachedManifest.version
                    val cachedSoundCount = cachedManifest.sounds.size
                    
                    // 如果版本不同或音频数量不同，需要更新
                    if (cachedVersion != assetsVersion || cachedSoundCount != assetsSoundCount) {
                        needUpdate = true
                    }
                } catch (e: Exception) {
                    // 缓存文件损坏，需要重新创建
                    needUpdate = true
                    Log.w(TAG, "持久化缓存损坏，重新初始化: ${e.message}")
                }
            }
            
            // 如果需要更新，写入新的缓存
            if (needUpdate) {
                manifestCacheFile.writeText(assetsJson)
                // 同时更新内存缓存
                remoteManifest = fixManifestData(assetsManifest)
            }
        } catch (e: Exception) {
            Log.e(TAG, "初始化默认清单失败: ${e.message}")
        }
    }
    
    /**
     * 获取当前缓存的清单（同步，快速）
     * 优先返回内存缓存，如果没有则返回持久化缓存
     */
    fun getCachedManifest(): SoundsManifest? {
        return remoteManifest ?: loadPersistedManifest()
    }
    
    /**
     * 加载网络音频清单（优先从内存缓存，然后从持久化缓存，最后从网络）
     */
    suspend fun loadRemoteManifest(): SoundsManifest? {
        return try {
            // 先检查内存缓存
            if (remoteManifest != null) {
                return remoteManifest
            }
            
            // 再检查持久化缓存
            val persistedManifest = loadPersistedManifest()
            if (persistedManifest != null) {
                return persistedManifest
            }
            
            // 最后从网络加载
            remoteLoader.loadManifest().also { manifest ->
                remoteManifest = manifest
                // 保存到持久化缓存
                savePersistedManifest(manifest)
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载网络音频清单失败: ${e.javaClass.simpleName} - ${e.message}")
            // 当网络失败时，返回持久化缓存作为备选
            loadPersistedManifest()
        }
    }
    
    /**
     * 获取所有网络音频
     */
    suspend fun getRemoteSounds(): List<SoundMetadata> {
        return loadRemoteManifest()?.sounds ?: emptyList()
    }
    
    /**
     * 根据ID获取音频元数据
     */
    suspend fun getSoundMetadata(soundId: String): SoundMetadata? {
        // 查找网络资源
        return loadRemoteManifest()?.sounds?.find { it.id == soundId }
    }
    
    /**
     * 获取音频文件URI（用于播放）
     * 优先使用缓存文件，如果缓存不存在则返回网络 URL
     */
    suspend fun getSoundUri(metadata: SoundMetadata): Uri? {
        // 如果 source 为 null，先修复
        val fixedMetadata = if (metadata.source == null) {
            metadata.copy(
                source = if (metadata.remoteUrl != null) AudioSource.REMOTE else AudioSource.LOCAL
            )
        } else {
            metadata
        }
            
        return when (fixedMetadata.source) {
            AudioSource.LOCAL -> {
                fixedMetadata.localResourceId?.let { resourceId ->
                    Uri.parse("android.resource://${appContext.packageName}/$resourceId")
                }
            }
            AudioSource.REMOTE -> {
                // 优先检查缓存，确保使用本地文件而不是网络 URL
                val cachedFile = cacheManager.getCachedFile(fixedMetadata.id)
                if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 0) {
                    Uri.fromFile(cachedFile)
                } else {
                    // 如果未缓存，返回网络 URL（ExoPlayer支持流式播放）
                    fixedMetadata.remoteUrl?.let { Uri.parse(it) }
                }
            }
            AudioSource.IMPORTED -> {
                // 导入的音频文件，使用本地路径
                fixedMetadata.importedPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        Uri.fromFile(file)
                    } else {
                        Log.w(TAG, "导入的音频文件不存在: $path")
                        null
                    }
                }
            }
            null -> null
        }
    }
    
    /**
     * 确保音频已下载（网络资源）
     */
    suspend fun ensureSoundDownloaded(metadata: SoundMetadata): Result<File> {
        if (metadata.source != AudioSource.REMOTE) {
            return Result.failure(IllegalArgumentException("不是网络资源"))
        }
        
        val remoteUrl = metadata.remoteUrl ?: return Result.failure(
            IllegalArgumentException("网络URL为空")
        )
        
        // 检查缓存
        cacheManager.getCachedFile(metadata.id)?.let { file ->
            if (file.exists()) {
                return Result.success(file)
            }
        }
        
        // 下载音频
        return cacheManager.downloadAudio(remoteUrl, metadata.id)
    }
    
    /**
     * 刷新网络音频清单
     */
    suspend fun refreshRemoteManifest(): Result<SoundsManifest> {
        return try {
            val manifest = remoteLoader.loadManifest(forceRefresh = true)
            remoteManifest = manifest
            // 保存到持久化缓存
            savePersistedManifest(manifest)
            Result.success(manifest)
        } catch (e: Exception) {
            Log.e(TAG, "刷新失败: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 从持久化缓存加载清单（同步，快速）
     */
    fun loadPersistedManifest(): SoundsManifest? {
        return try {
            if (manifestCacheFile.exists() && manifestCacheFile.length() > 0) {
                val json = manifestCacheFile.readText()
                val manifest = gson.fromJson(json, SoundsManifest::class.java)
                // 修复不完整的数据（与 RemoteAudioLoader 保持一致）
                val fixedManifest = fixManifestData(manifest)
                // 同时更新内存缓存
                remoteManifest = fixedManifest
                fixedManifest
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "从持久化缓存加载清单失败: ${e.message}")
            null
        }
    }
    
    /**
     * 修复清单中不完整的音频数据
     */
    private fun fixManifestData(manifest: SoundsManifest): SoundsManifest {
        val fixedSounds = manifest.sounds.map { sound ->
            sound.copy(
                source = sound.source ?: (if (sound.remoteUrl != null) AudioSource.REMOTE else AudioSource.LOCAL),
                loopStart = sound.loopStart ?: 0L,
                loopEnd = sound.loopEnd ?: 0L
            )
        }
        return manifest.copy(sounds = fixedSounds)
    }
    
    /**
     * 保存清单到持久化缓存（异步）
     */
    private suspend fun savePersistedManifest(manifest: SoundsManifest) {
        withContext(Dispatchers.IO) {
            try {
                val json = gson.toJson(manifest)
                manifestCacheFile.writeText(json)
            } catch (e: Exception) {
                Log.e(TAG, "保存清单到持久化缓存失败: ${e.message}")
            }
        }
    }
}

