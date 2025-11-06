# 音频资源管理架构设计

## 概述

本架构设计支持两种音频资源来源：
- **首页（白噪音页面）**：使用内置音频资源（打包在APK中）
- **星空页面**：使用GitHub上的网络音频资源（动态下载）

## 架构层次

```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer (Compose)                    │
│  ┌──────────────┐              ┌──────────────┐        │
│  │ SoundsScreen │              │ StarSkyScreen│        │
│  │ (首页)       │              │ (星空页面)   │        │
│  └──────┬───────┘              └──────┬───────┘        │
└─────────┼──────────────────────────────┼────────────────┘
          │                              │
          ▼                              ▼
┌─────────────────────────────────────────────────────────┐
│              Audio Resource Manager Layer                │
│  ┌──────────────────────────────────────────────────┐  │
│  │         AudioResourceManager                     │  │
│  │  - 管理音频元数据                                │  │
│  │  - 区分本地和网络资源                            │  │
│  │  - 提供统一的音频访问接口                        │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────┐  ┌────────────────────────────┐  │
│  │ LocalAudioLoader │  │ RemoteAudioLoader         │  │
│  │ (本地资源加载)   │  │ (网络资源加载)             │  │
│  └──────────────────┘  └────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
          │                              │
          ▼                              ▼
┌─────────────────────────────────────────────────────────┐
│              Audio Playback Layer                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │              AudioManager                        │  │
│  │  - 统一播放接口                                  │  │
│  │  - 支持本地和网络音频                            │  │
│  │  - 多音频混合播放                                │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
          │                              │
          ▼                              ▼
┌─────────────────────────────────────────────────────────┐
│              Audio Cache Layer                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │         AudioCacheManager                        │  │
│  │  - 网络音频下载                                  │  │
│  │  - 本地缓存管理                                  │  │
│  │  - 缓存策略（LRU）                               │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## 核心组件设计

### 1. 音频元数据模型

```kotlin
/**
 * 音频来源类型
 */
enum class AudioSource {
    LOCAL,      // 本地资源（打包在APK中）
    REMOTE      // 网络资源（从GitHub下载）
}

/**
 * 音频元数据
 */
data class SoundMetadata(
    // 基本信息
    val id: String,                    // 唯一标识符
    val name: String,                  // 显示名称
    val nameEn: String? = null,        // 英文名称（可选）
    val category: String,              // 分类（如 "Nature", "Rain"）
    val icon: String? = null,          // 图标（emoji或资源ID）
    
    // 资源信息
    val source: AudioSource,           // 资源来源
    val localResourceId: Int? = null,   // 本地资源ID（R.raw.xxx）
    val remoteUrl: String? = null,     // 网络资源URL（GitHub raw URL）
    
    // 播放参数
    val loopStart: Long = 500L,         // 循环起点（毫秒）
    val loopEnd: Long,                  // 循环终点（毫秒）
    val isSeamless: Boolean = true,    // 是否无缝循环
    
    // 元数据
    val duration: Long? = null,         // 总时长（毫秒，可选）
    val fileSize: Long? = null,        // 文件大小（字节，可选）
    val format: String = "ogg",         // 音频格式（ogg, mp3等）
    
    // 显示控制
    val isVisible: Boolean = true,      // 是否显示
    val order: Int = 0                  // 显示顺序
)

/**
 * 音频分类
 */
data class SoundCategory(
    val id: String,                    // 分类ID
    val name: String,                  // 分类名称
    val nameEn: String? = null,        // 英文名称（可选）
    val icon: String? = null,         // 图标（emoji或资源ID）
    val order: Int = 0                 // 显示顺序
)

/**
 * 音频清单
 */
data class SoundsManifest(
    val version: String,               // 清单版本
    val categories: List<SoundCategory>, // 分类列表
    val sounds: List<SoundMetadata>    // 音频列表
)
```

### 2. 音频资源管理器

```kotlin
/**
 * 音频资源管理器
 * 负责管理所有音频资源（本地和网络）
 */
class AudioResourceManager private constructor(context: Context) {
    
    companion object {
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
    private val localLoader = LocalAudioLoader(context)
    private val remoteLoader = RemoteAudioLoader(context)
    private val cacheManager = AudioCacheManager.getInstance(context)
    
    // 音频清单缓存
    private var localManifest: SoundsManifest? = null
    private var remoteManifest: SoundsManifest? = null
    
    /**
     * 加载本地音频清单
     */
    suspend fun loadLocalManifest(): SoundsManifest {
        return localManifest ?: run {
            localLoader.loadManifest().also { localManifest = it }
        }
    }
    
    /**
     * 加载网络音频清单
     */
    suspend fun loadRemoteManifest(): SoundsManifest? {
        return try {
            remoteLoader.loadManifest().also { remoteManifest = it }
        } catch (e: Exception) {
            Log.e(TAG, "加载网络音频清单失败: ${e.message}")
            null
        }
    }
    
    /**
     * 获取所有本地音频
     */
    suspend fun getLocalSounds(): List<SoundMetadata> {
        return loadLocalManifest().sounds
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
        // 先查找本地资源
        loadLocalManifest().sounds.find { it.id == soundId }?.let {
            return it
        }
        // 再查找网络资源
        return loadRemoteManifest()?.sounds?.find { it.id == soundId }
    }
    
    /**
     * 获取音频文件URI（用于播放）
     */
    suspend fun getSoundUri(metadata: SoundMetadata): Uri? {
        return when (metadata.source) {
            AudioSource.LOCAL -> {
                metadata.localResourceId?.let { resourceId ->
                    Uri.parse("android.resource://${appContext.packageName}/$resourceId")
                }
            }
            AudioSource.REMOTE -> {
                // 先检查缓存
                cacheManager.getCachedFile(metadata.id)?.let { file ->
                    Uri.fromFile(file)
                } ?: run {
                    // 如果未缓存，返回网络URL（ExoPlayer支持流式播放）
                    metadata.remoteUrl?.let { Uri.parse(it) }
                }
            }
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
            Result.success(manifest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 3. 本地音频加载器

```kotlin
/**
 * 本地音频加载器
 * 负责加载打包在APK中的音频资源
 */
class LocalAudioLoader(private val context: Context) {
    
    companion object {
        private const val LOCAL_MANIFEST_FILE = "sounds_local.json"
    }
    
    /**
     * 加载本地音频清单
     */
    suspend fun loadManifest(): SoundsManifest {
        return withContext(Dispatchers.IO) {
            try {
                // 从assets文件夹读取清单文件
                val json = context.assets.open(LOCAL_MANIFEST_FILE)
                    .bufferedReader().use { it.readText() }
                
                // 解析JSON
                val gson = Gson()
                val manifest = gson.fromJson(json, SoundsManifest::class.java)
                
                // 验证并补充本地资源ID
                manifest.copy(
                    sounds = manifest.sounds.map { sound ->
                        if (sound.source == AudioSource.LOCAL) {
                            // 根据ID获取资源ID
                            val resourceId = getResourceId(sound.id)
                            sound.copy(localResourceId = resourceId)
                        } else {
                            sound
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "加载本地音频清单失败: ${e.message}")
                // 返回默认清单（使用硬编码的音频列表）
                createDefaultManifest()
            }
        }
    }
    
    /**
     * 根据音频ID获取资源ID
     */
    private fun getResourceId(soundId: String): Int? {
        return when (soundId) {
            "rain" -> R.raw.rain_sound_188158
            "campfire" -> R.raw.gouhuo
            "thunder" -> R.raw.dalei
            "cat_purring" -> R.raw.cat_purring
            "bird_chirping" -> R.raw.bird_chirping
            "night_insects" -> R.raw.xishuai_animation
            else -> null
        }
    }
    
    /**
     * 创建默认清单（硬编码）
     */
    private fun createDefaultManifest(): SoundsManifest {
        return SoundsManifest(
            version = "1.0.0",
            categories = listOf(
                SoundCategory("nature", "自然", "Nature", "🌲", 1),
                SoundCategory("animals", "动物", "Animals", "🐾", 2)
            ),
            sounds = listOf(
                SoundMetadata(
                    id = "rain",
                    name = "雨声",
                    nameEn = "Rain",
                    category = "nature",
                    icon = "🌧️",
                    source = AudioSource.LOCAL,
                    localResourceId = R.raw.rain_sound_188158,
                    loopStart = 500L,
                    loopEnd = 3400000L,
                    isSeamless = true,
                    order = 1
                ),
                // ... 其他内置音频
            )
        )
    }
}
```

### 4. 网络音频加载器

```kotlin
/**
 * 网络音频加载器
 * 负责从GitHub加载音频清单和音频文件
 */
class RemoteAudioLoader(private val context: Context) {
    
    companion object {
        // GitHub raw URL（需要替换为实际的仓库地址）
        private const val REMOTE_MANIFEST_URL = 
            "https://raw.githubusercontent.com/yourusername/xmsleep-audio/main/sounds_remote.json"
        
        // GitHub raw URL 基础路径
        private const val REMOTE_AUDIO_BASE_URL = 
            "https://raw.githubusercontent.com/yourusername/xmsleep-audio/main/audio/"
    }
    
    private val gson = Gson()
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * 加载网络音频清单
     */
    suspend fun loadManifest(forceRefresh: Boolean = false): SoundsManifest {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(REMOTE_MANIFEST_URL)
                    .apply {
                        if (forceRefresh) {
                            addHeader("Cache-Control", "no-cache")
                        }
                    }
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    throw IOException("加载清单失败: ${response.code}")
                }
                
                val json = response.body?.string() 
                    ?: throw IOException("响应体为空")
                
                val manifest = gson.fromJson(json, SoundsManifest::class.java)
                
                // 补充完整的远程URL
                manifest.copy(
                    sounds = manifest.sounds.map { sound ->
                        if (sound.source == AudioSource.REMOTE && sound.remoteUrl != null) {
                            // 如果是相对路径，补充完整URL
                            val fullUrl = if (sound.remoteUrl!!.startsWith("http")) {
                                sound.remoteUrl
                            } else {
                                "$REMOTE_AUDIO_BASE_URL${sound.remoteUrl}"
                            }
                            sound.copy(remoteUrl = fullUrl)
                        } else {
                            sound
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "加载网络音频清单失败: ${e.message}")
                throw e
            }
        }
    }
}
```

### 5. 音频缓存管理器

```kotlin
/**
 * 音频缓存管理器
 * 负责网络音频的下载和缓存管理
 */
class AudioCacheManager private constructor(context: Context) {
    
    companion object {
        private const val TAG = "AudioCacheManager"
        private const val CACHE_DIR_NAME = "audio_cache"
        private const val MAX_CACHE_SIZE = 100 * 1024 * 1024L // 100MB
        private const val MAX_CACHE_FILES = 50 // 最多缓存50个文件
        
        @Volatile
        private var instance: AudioCacheManager? = null
        
        fun getInstance(context: Context): AudioCacheManager {
            return instance ?: synchronized(this) {
                instance ?: AudioCacheManager(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
    
    private val appContext: Context = context.applicationContext
    private val cacheDir: File = File(appContext.cacheDir, CACHE_DIR_NAME)
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    init {
        // 确保缓存目录存在
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }
    
    /**
     * 获取缓存的音频文件
     */
    fun getCachedFile(soundId: String): File? {
        val file = File(cacheDir, "$soundId.ogg") // 假设都是OGG格式
        return if (file.exists() && file.length() > 0) {
            file
        } else {
            null
        }
    }
    
    /**
     * 下载音频文件
     */
    suspend fun downloadAudio(url: String, soundId: String): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                // 检查缓存
                getCachedFile(soundId)?.let { file ->
                    return@withContext Result.success(file)
                }
                
                // 检查缓存空间
                ensureCacheSpace()
                
                // 下载文件
                val request = Request.Builder()
                    .url(url)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    throw IOException("下载失败: ${response.code}")
                }
                
                val body = response.body ?: throw IOException("响应体为空")
                
                // 保存到缓存
                val file = File(cacheDir, "$soundId.ogg")
                body.byteStream().use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                Log.d(TAG, "音频下载成功: $soundId")
                Result.success(file)
            } catch (e: Exception) {
                Log.e(TAG, "下载音频失败: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * 下载音频文件（带进度回调）
     */
    suspend fun downloadAudioWithProgress(
        url: String,
        soundId: String,
        onProgress: (Long, Long) -> Unit
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                // 检查缓存
                getCachedFile(soundId)?.let { file ->
                    return@withContext Result.success(file)
                }
                
                // 检查缓存空间
                ensureCacheSpace()
                
                // 下载文件
                val request = Request.Builder()
                    .url(url)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    throw IOException("下载失败: ${response.code}")
                }
                
                val body = response.body ?: throw IOException("响应体为空")
                val contentLength = body.contentLength()
                
                // 保存到缓存
                val file = File(cacheDir, "$soundId.ogg")
                body.byteStream().use { input ->
                    file.outputStream().use { output ->
                        var totalBytesRead = 0L
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            // 更新进度
                            if (contentLength > 0) {
                                onProgress(totalBytesRead, contentLength)
                            }
                        }
                    }
                }
                
                Log.d(TAG, "音频下载成功: $soundId")
                Result.success(file)
            } catch (e: Exception) {
                Log.e(TAG, "下载音频失败: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * 确保缓存空间足够
     */
    private fun ensureCacheSpace() {
        val files = cacheDir.listFiles() ?: return
        
        // 按最后修改时间排序（LRU策略）
        val sortedFiles = files.sortedBy { it.lastModified() }
        
        // 计算当前缓存大小
        var currentSize = sortedFiles.sumOf { it.length() }
        
        // 如果超过最大缓存大小或文件数，删除最旧的文件
        while ((currentSize > MAX_CACHE_SIZE || sortedFiles.size > MAX_CACHE_FILES) 
                && sortedFiles.isNotEmpty()) {
            val oldestFile = sortedFiles.removeFirst()
            currentSize -= oldestFile.length()
            oldestFile.delete()
        }
    }
    
    /**
     * 获取缓存大小
     */
    fun getCacheSize(): Long {
        val files = cacheDir.listFiles() ?: return 0L
        return files.sumOf { it.length() }
    }
    
    /**
     * 清理缓存
     */
    fun clearCache() {
        val files = cacheDir.listFiles() ?: return
        files.forEach { it.delete() }
    }
    
    /**
     * 删除指定音频的缓存
     */
    fun deleteCache(soundId: String) {
        val file = File(cacheDir, "$soundId.ogg")
        if (file.exists()) {
            file.delete()
        }
    }
}
```

### 6. AudioManager 扩展

```kotlin
/**
 * 扩展 AudioManager 以支持元数据播放
 */
fun AudioManager.playSound(
    context: Context,
    metadata: SoundMetadata
) {
    when (metadata.source) {
        AudioSource.LOCAL -> {
            // 使用现有的播放逻辑
            val sound = convertToSoundEnum(metadata.id)
            playSound(context, sound)
        }
        AudioSource.REMOTE -> {
            // 播放网络音频
            playRemoteSound(context, metadata)
        }
    }
}

/**
 * 播放网络音频
 */
private fun AudioManager.playRemoteSound(
    context: Context,
    metadata: SoundMetadata
) {
    // 实现网络音频播放逻辑
    // 1. 检查缓存
    // 2. 如果未缓存，下载或流式播放
    // 3. 使用ExoPlayer播放
}
```

## 文件结构

```
app/src/main/
├── assets/
│   └── sounds_local.json          # 本地音频清单（JSON）
├── res/
│   └── raw/                        # 本地音频资源
│       ├── rain_sound_188158.mp3
│       ├── gouhuo.mp3
│       └── ...
└── kotlin/org/xmsleep/app/
    ├── audio/
    │   ├── AudioManager.kt         # 现有播放管理器（扩展）
    │   ├── AudioResourceManager.kt # 音频资源管理器（新增）
    │   ├── LocalAudioLoader.kt     # 本地音频加载器（新增）
    │   ├── RemoteAudioLoader.kt    # 网络音频加载器（新增）
    │   ├── AudioCacheManager.kt    # 音频缓存管理器（新增）
    │   └── model/
    │       ├── SoundMetadata.kt    # 音频元数据模型（新增）
    │       ├── SoundCategory.kt    # 音频分类模型（新增）
    │       └── SoundsManifest.kt   # 音频清单模型（新增）
    └── ui/
        ├── SoundsScreen.kt         # 首页（使用本地音频）
        └── StarSkyScreen.kt        # 星空页面（使用网络音频）
```

## 音频清单JSON格式

### 本地音频清单 (sounds_local.json)

```json
{
  "version": "1.0.0",
  "categories": [
    {
      "id": "nature",
      "name": "自然",
      "nameEn": "Nature",
      "icon": "🌲",
      "order": 1
    },
    {
      "id": "animals",
      "name": "动物",
      "nameEn": "Animals",
      "icon": "🐾",
      "order": 2
    }
  ],
  "sounds": [
    {
      "id": "rain",
      "name": "雨声",
      "nameEn": "Rain",
      "category": "nature",
      "icon": "🌧️",
      "source": "LOCAL",
      "loopStart": 500,
      "loopEnd": 3400000,
      "isSeamless": true,
      "format": "mp3",
      "order": 1
    },
    {
      "id": "campfire",
      "name": "篝火声",
      "nameEn": "Campfire",
      "category": "nature",
      "icon": "🔥",
      "source": "LOCAL",
      "loopStart": 500,
      "loopEnd": 90000,
      "isSeamless": true,
      "format": "mp3",
      "order": 2
    }
  ]
}
```

### 网络音频清单 (sounds_remote.json)

```json
{
  "version": "1.0.0",
  "categories": [
    {
      "id": "space",
      "name": "太空",
      "nameEn": "Space",
      "icon": "🌌",
      "order": 1
    },
    {
      "id": "ocean",
      "name": "海洋",
      "nameEn": "Ocean",
      "icon": "🌊",
      "order": 2
    }
  ],
  "sounds": [
    {
      "id": "ocean_waves",
      "name": "海浪声",
      "nameEn": "Ocean Waves",
      "category": "ocean",
      "icon": "🌊",
      "source": "REMOTE",
      "remoteUrl": "ocean_waves.ogg",
      "loopStart": 500,
      "loopEnd": 60000,
      "isSeamless": true,
      "format": "ogg",
      "fileSize": 480000,
      "duration": 60000,
      "order": 1
    },
    {
      "id": "space_ambient",
      "name": "太空环境音",
      "nameEn": "Space Ambient",
      "category": "space",
      "icon": "🌌",
      "source": "REMOTE",
      "remoteUrl": "space_ambient.ogg",
      "loopStart": 500,
      "loopEnd": 120000,
      "isSeamless": true,
      "format": "ogg",
      "fileSize": 960000,
      "duration": 120000,
      "order": 2
    }
  ]
}
```

## 使用流程

### 首页（白噪音页面）使用流程

```
1. SoundsScreen 启动
   ↓
2. 调用 AudioResourceManager.getLocalSounds()
   ↓
3. LocalAudioLoader 加载 sounds_local.json
   ↓
4. 显示本地音频列表
   ↓
5. 用户点击播放
   ↓
6. AudioManager.playSound(context, metadata)
   ↓
7. 使用本地资源ID播放
```

### 星空页面使用流程

```
1. StarSkyScreen 启动
   ↓
2. 调用 AudioResourceManager.getRemoteSounds()
   ↓
3. RemoteAudioLoader 从GitHub加载 sounds_remote.json
   ↓
4. 显示网络音频列表
   ↓
5. 用户点击播放
   ↓
6. AudioResourceManager.ensureSoundDownloaded(metadata)
   ↓
7. AudioCacheManager 检查缓存或下载
   ↓
8. AudioManager.playSound(context, metadata)
   ↓
9. 使用缓存文件或网络URL播放
```

## 依赖项

需要在 `app/build.gradle.kts` 中添加：

```kotlin
dependencies {
    // 网络请求
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JSON解析
    implementation("com.google.code.gson:gson:2.10.1")
    
    // 协程（如果还没有）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

## 权限

需要在 `AndroidManifest.xml` 中添加：

```xml
<!-- 网络权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 总结

本架构设计实现了：
1. ✅ 统一的音频资源管理接口
2. ✅ 本地和网络资源的统一处理
3. ✅ 网络音频的下载和缓存管理
4. ✅ 清晰的职责分离
5. ✅ 易于扩展和维护

下一步可以：
1. 实现各个组件的代码
2. 创建音频清单JSON文件
3. 在GitHub上创建音频资源仓库
4. 更新UI以使用新的架构

