package org.streambox.datasource.xmbox

import android.content.Context
import org.streambox.datasource.xmbox.config.XmboxConfig
import org.streambox.datasource.xmbox.config.XmboxConfigParser
import org.streambox.datasource.xmbox.model.VideoItem
import org.streambox.datasource.xmbox.spider.SpiderManager
import org.streambox.datasource.xmbox.spider.SpiderLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import java.util.Base64
import android.util.Log

/**
 * XMBOX 视频源识别器
 * 负责识别和处理 XMBOX 格式的视频源配置
 */
class XmboxVideoSource(
    private val context: Context,
    private val extractor: VideoUrlExtractor = WebViewVideoUrlExtractor(context)
) {
    private val jsExecutor = CatVodTVJavaScriptExecutor(context)
    private val spiderManager = SpiderManager.getInstance(context)
    private val spiderLoader = SpiderLoader(context)
    private val httpClient = HttpClient(Android) {
        followRedirects = true
        
        // 设置超时 - 增加超时时间以应对慢速网络
        engine {
            connectTimeout = 60_000  // 60秒连接超时
            socketTimeout = 60_000   // 60秒读取超时
        }
        
        // 允许非 2xx 状态码
        expectSuccess = false
    }
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
           /**
            * 识别并解析视频源配置
            * 
            * @param configString 配置字符串（JSON、JavaScript 或 URL）
            * @param name 源名称（可选）
            * @return 解析后的配置，如果解析失败则返回 null
            */
           suspend fun identify(configString: String, name: String = ""): XmboxConfig? {
               return withContext(Dispatchers.Default) {
                   Log.d("XmboxVideoSource", "开始识别，输入: ${configString.take(100)}")
                   
                   val trimmedInput = configString.trim()
                   
                   // 1. 检查是否是 URL - 先处理URL以便后续可以下载配置内容
                   val isUrl = trimmedInput.startsWith("http://") || trimmedInput.startsWith("https://")
                   
                   // 2. 先尝试直接解析（JSON 或 JavaScript）
                   var config = XmboxConfigParser.parse(configString, name)
                   Log.d("XmboxVideoSource", "直接解析结果: ${config?.let { it::class.simpleName } ?: "null"}")
                   
                   // 如果直接解析成功，检查是否需要设置URL
                   if (config != null) {
                       // 如果输入是URL，更新config的url字段
                       if (isUrl) {
                           config = when (config) {
                               is XmboxConfig.VodConfig -> config.copy(url = trimmedInput)
                               is XmboxConfig.LiveConfig -> config.copy(url = trimmedInput)
                               is XmboxConfig.JavaScriptConfig -> config.copy(url = trimmedInput)
                           }
                       }
                       
                       // 验证配置是否有效
                       if (XmboxConfigParser.isValid(config)) {
                       Log.d("XmboxVideoSource", "直接解析成功，返回配置")
                       return@withContext config
                       }
                   }
                   
                   // 3. 如果输入是URL，完全按照XMBOX的VodConfig.loadConfig()流程
                   if (isUrl) {
                       Log.d("XmboxVideoSource", "输入是URL，使用XMBOX流程下载配置: $trimmedInput")
                       
                       try {
                           // XMBOX流程：Decoder.getJson(UrlUtil.convert(config.getUrl()), "vod")
                           val convertedUrl = org.streambox.datasource.xmbox.utils.UrlUtil.convert(trimmedInput)
                           val jsonString = org.streambox.datasource.xmbox.utils.UrlDecoder.getJson(convertedUrl, httpClient)
                           Log.d("XmboxVideoSource", "UrlDecoder获取配置成功，长度: ${jsonString.length}, 前100字符: ${jsonString.take(100)}")
                           
                           // 检查是否是HTML页面（必须在JSON解析之前）
                           if (jsonString.trim().startsWith("<!DOCTYPE") || 
                               jsonString.trim().startsWith("<html") ||
                               jsonString.contains("</html>")) {
                               Log.d("XmboxVideoSource", "检测到HTML页面，使用extractConfigFromHtml处理")
                               // 从HTML中提取配置链接
                               val configLinks = extractConfigFromHtml(jsonString, trimmedInput)
                               if (configLinks.isNotEmpty()) {
                                   Log.d("XmboxVideoSource", "HTML页面包含 ${configLinks.size} 个配置链接，依次尝试...")
                                   // 依次尝试提取的链接，直到找到有效配置
                                   // 限制最多尝试前10个链接，避免无限递归
                                   val linksToTry = configLinks.take(10)
                                   for ((index, linkUrl) in linksToTry.withIndex()) {
                                       try {
                                           Log.d("XmboxVideoSource", "尝试解析配置链接 [${index + 1}/${linksToTry.size}]: $linkUrl")
                                           // 先快速检查链接返回的内容类型
                                           try {
                                               val testResponse = httpClient.get(linkUrl) {
                                                   header("User-Agent", "Mozilla/5.0")
                                               }
                                               
                                               val contentType = testResponse.headers["Content-Type"] ?: ""
                                               val bodyText = testResponse.bodyAsText()
                                               
                                               // 如果返回JavaScript重定向，提取其中的URL
                                               if (bodyText.contains("document.location") || bodyText.contains("location =")) {
                                                   val redirectPattern = Regex("""(?:location|document\.location)\s*[=:]\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                                                   val redirectMatch = redirectPattern.find(bodyText)
                                                   if (redirectMatch != null) {
                                                       val redirectUrl = redirectMatch.groupValues[1]
                                                       val absoluteRedirectUrl = org.streambox.datasource.xmbox.utils.UrlUtil.resolve(linkUrl, redirectUrl)
                                                       Log.d("XmboxVideoSource", "检测到JavaScript重定向: $absoluteRedirectUrl")
                                                       try {
                                                           val redirectConfig = identify(absoluteRedirectUrl, name)
                                                           if (redirectConfig != null) {
                                                               Log.d("XmboxVideoSource", "成功从重定向URL解析配置: $absoluteRedirectUrl")
                                                               return@withContext redirectConfig
                                   }
                               } catch (e: Exception) {
                                                           Log.w("XmboxVideoSource", "重定向URL解析失败: ${e.message}")
                                                       }
                                                   }
                                                   continue // 跳过重定向链接，继续下一个
                                               }
                                               
                                               // 如果看起来像配置文件，尝试解析
                                               if (contentType.contains("json") || contentType.contains("text") ||
                                                   bodyText.trim().startsWith("{") || bodyText.trim().startsWith("[")) {
                                                   val linkConfig = identify(linkUrl, name)
                                                   if (linkConfig != null) {
                                                       Log.d("XmboxVideoSource", "成功从HTML链接解析配置: $linkUrl")
                                                       return@withContext linkConfig
                                                   }
                                       }
                                   } catch (e: Exception) {
                                               Log.w("XmboxVideoSource", "快速检查链接失败: ${e.message}")
                                               // 仍然尝试完整识别
                                               val linkConfig = identify(linkUrl, name)
                                               if (linkConfig != null) {
                                                   Log.d("XmboxVideoSource", "成功从HTML链接解析配置: $linkUrl")
                                                   return@withContext linkConfig
                                               }
                                           }
                                       } catch (e: Exception) {
                                           Log.w("XmboxVideoSource", "解析链接失败: $linkUrl, ${e.message}")
                                           // 继续尝试下一个链接
                                       }
                                   }
                                   Log.w("XmboxVideoSource", "所有HTML链接都解析失败")
                               } else {
                                   Log.w("XmboxVideoSource", "HTML页面中未找到配置链接")
                               }
                               // HTML处理失败，继续fallback
                           }
                           
                           // XMBOX流程：checkJson -> parseConfig
                           val jsonElement = json.parseToJsonElement(jsonString)
                           if (jsonElement is JsonObject) {
                               // 检查是否有msg字段（错误消息）
                               if (jsonElement["msg"] != null) {
                                   val msg = jsonElement["msg"]?.jsonPrimitive?.content
                                   Log.e("XmboxVideoSource", "配置返回错误消息: $msg")
                                   return@withContext null
                               }
                               
                               // 检查是否有urls字段（仓库模式）
                               if (jsonElement["urls"] != null) {
                                   // TODO: 处理仓库模式
                                   Log.w("XmboxVideoSource", "检测到urls字段（仓库模式），暂不支持")
                               }
                               
                               // 解析配置（XMBOX: parseConfig）
                               val configUrl = trimmedInput
                               val configName = name.ifBlank { jsonElement["name"]?.jsonPrimitive?.content ?: "视频源" }
                               
                               // 检查是否有sites数组（多源配置）
                                           val sitesArray = jsonElement["sites"]?.jsonArray
                                           if (sitesArray != null && sitesArray.isNotEmpty()) {
                                   Log.d("XmboxVideoSource", "检测到多源配置，包含 ${sitesArray.size} 个子源")
                                   // 这是多源配置，创建JavaScriptConfig
                                               val firstSite = sitesArray.firstOrNull()?.jsonObject
                                               val siteType = firstSite?.get("type")?.jsonPrimitive?.intOrNull ?: 0
                                               val sourceType = if (siteType == 2) {
                                                   org.streambox.datasource.xmbox.config.SourceType.LIVE
                                               } else {
                                                   org.streambox.datasource.xmbox.config.SourceType.VOD
                                               }
                                               
                                               return@withContext XmboxConfig.JavaScriptConfig(
                                       url = configUrl,
                                                   name = configName,
                                       jsCode = jsonString, // 存储整个JSON配置
                                                   type = sourceType,
                                                   timeout = 30
                                               )
                               } else {
                                   // 单个源配置，尝试标准解析
                                   val parsedConfig = XmboxConfigParser.parse(jsonString, configName)
                                   if (parsedConfig != null && XmboxConfigParser.isValid(parsedConfig)) {
                                       val finalConfig = when (parsedConfig) {
                                           is XmboxConfig.VodConfig -> parsedConfig.copy(url = configUrl)
                                           is XmboxConfig.LiveConfig -> parsedConfig.copy(url = configUrl)
                                           is XmboxConfig.JavaScriptConfig -> parsedConfig.copy(url = configUrl)
                                       }
                                       Log.d("XmboxVideoSource", "配置解析成功: ${finalConfig::class.simpleName}")
                                       return@withContext finalConfig
                                   }
                               }
                           }
                       } catch (e: Exception) {
                           Log.e("XmboxVideoSource", "XMBOX流程加载配置失败: ${e.message}", e)
                           // 如果失败，继续尝试原始的fallback方式
                       }
                       
                       // Fallback: 如果XMBOX流程失败，尝试原始方式（处理BMP等特殊情况）
                       Log.d("XmboxVideoSource", "尝试fallback方式")
                       try {
                           val response = httpClient.get(trimmedInput) {
                               header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                               header("Accept", "*/*")
                           }
                           val rawBytes = response.body<ByteArray>()
                           
                           var content: String? = null
                           
                           // 检查是否是BMP文件
                           if (rawBytes.size > 2) {
                               val header = String(rawBytes.take(2).toByteArray(), Charsets.ISO_8859_1)
                               if (header == "BM") {
                                   content = extractHiddenDataFromBmp(rawBytes)
                               } else if (header == "GI") {
                                   // GIF图片，跳过fallback
                                   content = null
                               }
                           }
                           
                           // 如果不是BMP或BMP提取失败，尝试文本解码
                           if (content == null) {
                               content = String(rawBytes, Charsets.UTF_8).trim()
                           }
                           
                           // 如果获取到内容且不是GIF，尝试解析
                           if (content != null && !content.isNullOrBlank()) {
                               var finalContent = content.trim()
                               
                               // 移除BOM
                               if (finalContent.startsWith("\uFEFF")) {
                                   finalContent = finalContent.removePrefix("\uFEFF").trim()
                               }
                               
                               Log.d("XmboxVideoSource", "Fallback提取的内容长度: ${finalContent.length}, 前200字符: ${finalContent.take(200)}")
                               
                               // 检查是否是HTML页面
                               if (finalContent.trim().startsWith("<!DOCTYPE") || 
                                   finalContent.trim().startsWith("<html") ||
                                   finalContent.contains("</html>")) {
                                   Log.d("XmboxVideoSource", "检测到HTML页面，尝试提取配置链接")
                                   val configLinks = extractConfigFromHtml(finalContent, trimmedInput)
                                   if (configLinks.isNotEmpty()) {
                                       try {
                                           val linkContent = org.streambox.datasource.xmbox.utils.UrlDecoder.getJson(configLinks[0], httpClient)
                                           if (!linkContent.isNullOrBlank()) {
                                               Log.d("XmboxVideoSource", "从HTML链接获取到内容，长度=${linkContent.length}")
                                               finalContent = linkContent.trim()
                                           }
                                       } catch (e: Exception) {
                                           Log.w("XmboxVideoSource", "下载HTML链接失败: ${e.message}")
                                       }
                                   }
                               }
                               
                               // 尝试解析配置
                               try {
                                   val parsedConfig = XmboxConfigParser.parse(finalContent, name)
                                   if (parsedConfig != null && XmboxConfigParser.isValid(parsedConfig)) {
                                       val finalConfig = when (parsedConfig) {
                                           is XmboxConfig.VodConfig -> parsedConfig.copy(url = trimmedInput)
                                           is XmboxConfig.LiveConfig -> parsedConfig.copy(url = trimmedInput)
                                           is XmboxConfig.JavaScriptConfig -> parsedConfig.copy(url = trimmedInput)
                                       }
                                       Log.d("XmboxVideoSource", "Fallback解析成功: ${finalConfig::class.simpleName}")
                                       return@withContext finalConfig
                                   } else {
                                       Log.w("XmboxVideoSource", "Fallback解析失败：配置无效")
                                   }
                               } catch (e: Exception) {
                                   Log.w("XmboxVideoSource", "Fallback配置解析异常: ${e.message}")
                                   // 如果解析失败，可能是直接的视频列表JSON或配置JSON，尝试作为JavaScriptConfig
                                   if (finalContent.startsWith("{") && finalContent.contains("\"sites\"")) {
                                       Log.d("XmboxVideoSource", "Fallback: 检测到sites配置，创建JavaScriptConfig")
                                   return@withContext XmboxConfig.JavaScriptConfig(
                                       url = trimmedInput,
                                       name = name.ifBlank { "视频源" },
                                       jsCode = finalContent,
                                       type = org.streambox.datasource.xmbox.config.SourceType.VOD,
                                       timeout = 30
                                   )
                               }
                               }
                           } else {
                               Log.w("XmboxVideoSource", "Fallback: 内容为空")
                           }
                       } catch (e: Exception) {
                           Log.w("XmboxVideoSource", "Fallback方式也失败: ${e.message}")
                       }
                       
                       // 检查是否是视频直链
                       if (isDirectVideoUrl(trimmedInput)) {
                           Log.d("XmboxVideoSource", "URL看起来是视频直链，创建默认 VodConfig")
                       return@withContext XmboxConfig.VodConfig(
                           url = trimmedInput,
                           name = name.ifBlank { "视频源" },
                           timeout = 15
                       )
                       }
                       
                       Log.w("XmboxVideoSource", "无法识别URL: $trimmedInput")
                       return@withContext null
            }
            
            // 4. 如果都不是，返回 null
            null
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
     * 完全按照XMBOX的SiteViewModel.homeContent()逻辑
     * 
     * @param config 视频源配置
     * @param spiderUrl Spider JAR的URL（可选，从配置中提取）
     * @param spiderClassName Spider类名（可选，从api字段提取）
     * @return 视频列表，如果获取失败则返回空列表
     */
    suspend fun getHomeVideoList(
        config: XmboxConfig,
        spiderUrl: String? = null,
        spiderClassName: String? = null
    ): List<VideoItem> {
        return withContext(Dispatchers.IO) {
            try {
                when (config) {
                    is XmboxConfig.JavaScriptConfig -> {
                        Log.d("XmboxVideoSource", "处理JavaScriptConfig: url=${config.url}, jsCode.length=${config.jsCode.length}, jsCode前100字符=${config.jsCode.take(100)}")
                        
                        // 解析站点信息（从配置或从传入的参数）
                        val site = buildSiteFromConfig(config, spiderUrl, spiderClassName)
                        
                        if (site == null) {
                            // 如果无法构建Site，可能是直接的视频列表JSON或Spider类名，尝试处理
                            Log.d("XmboxVideoSource", "无法构建Site，尝试其他方式")
                            val trimmedJsCode = config.jsCode.trim()
                            
                            // 检查是否是Spider类名（csp_开头）
                            if (spiderClassName != null && (trimmedJsCode == spiderClassName || trimmedJsCode.startsWith("csp_"))) {
                                Log.d("XmboxVideoSource", "检测到Spider类名，直接使用Spider模式")
                                // 直接使用Spider模式
                                val spiderSite = org.streambox.datasource.xmbox.model.Site(
                                    key = "main",
                                    name = config.name,
                                    api = spiderClassName,
                                    jar = spiderUrl ?: "",
                                    type = 3,
                                    timeout = config.timeout
                                )
                                return@withContext getHomeContentFromSpider(spiderSite, spiderUrl, spiderClassName)
                            }
                            
                            // 检查是否是直接的视频列表JSON
                            if (trimmedJsCode.startsWith("{") || trimmedJsCode.startsWith("[")) {
                                try {
                                    Log.d("XmboxVideoSource", "尝试直接解析jsCode为视频列表JSON")
                                    val result = org.streambox.datasource.xmbox.utils.ResultParser.fromJson(trimmedJsCode)
                                    Log.d("XmboxVideoSource", "直接解析成功，获取到 ${result.size} 个视频")
                                    return@withContext result
                                } catch (e: Exception) {
                                    Log.e("XmboxVideoSource", "直接解析JSON失败: ${e.message}", e)
                                }
                            }
                            
                            Log.w("XmboxVideoSource", "无法解析站点信息且jsCode不是有效格式")
                            return@withContext emptyList()
                        }
                        
                        Log.d("XmboxVideoSource", "成功构建Site: key=${site.key}, name=${site.name}, type=${site.type}, api=${site.api.take(50)}")
                        
                        // 按照XMBOX的SiteViewModel.homeContent()逻辑
                        when {
                            // type == 3: 使用Spider
                            site.type == 3 || site.isSpider() -> {
                                Log.d("XmboxVideoSource", "使用Spider模式（type=${site.type}）")
                                getHomeContentFromSpider(site, spiderUrl, spiderClassName)
                            }
                            
                            // type == 4: 使用Ext URL
                            site.isExt() -> {
                                Log.d("XmboxVideoSource", "使用Ext模式（type=4）")
                                getHomeContentFromExt(site)
                            }
                            
                            // type == 0, 1, 2: 直接HTTP请求API
                            site.isDirectApi() -> {
                                Log.d("XmboxVideoSource", "使用直接API模式（type=${site.type}）")
                                getHomeContentFromApi(site)
                            }
                            
                            else -> {
                                Log.w("XmboxVideoSource", "未知的站点类型: ${site.type}")
                                emptyList()
                            }
                        }
                    }
                    
                    is XmboxConfig.VodConfig -> {
                        // 直接API配置，type默认为1（JSON）
                        val apiSite = org.streambox.datasource.xmbox.model.Site(
                            key = "main",
                            name = config.name,
                            api = config.url,
                            type = 1
                        )
                        getHomeContentFromApi(apiSite)
                    }
                    
                    is XmboxConfig.LiveConfig -> {
                        // 直播源不需要首页列表
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("XmboxVideoSource", "获取首页视频列表失败", e)
                emptyList()
            }
        }
    }
    
    /**
     * 从配置构建Site对象
     * 优先使用传入的spider信息（如果提供），否则从配置中解析
     */
    private fun buildSiteFromConfig(
        config: XmboxConfig.JavaScriptConfig,
        spiderUrl: String?,
        spiderClassName: String?
    ): org.streambox.datasource.xmbox.model.Site? {
        return try {
            val trimmedJsCode = config.jsCode.trim()
            
            // 如果jsCode就是Spider类名（csp_开头），直接构建Site
            if (trimmedJsCode.startsWith("csp_") || 
                (spiderClassName != null && trimmedJsCode == spiderClassName) ||
                (trimmedJsCode.length < 100 && trimmedJsCode.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")))) {
                Log.d("XmboxVideoSource", "jsCode是Spider类名，直接构建Site")
                return org.streambox.datasource.xmbox.model.Site(
                    key = "main",
                    name = config.name,
                    api = trimmedJsCode,
                    jar = spiderUrl ?: "",
                    type = 3,
                    timeout = config.timeout
                )
            }
            
            // 如果传入了spiderClassName，说明已经选择了特定子源，优先使用传入的信息
            if (spiderClassName != null) {
                Log.d("XmboxVideoSource", "使用传入的spiderClassName: $spiderClassName")
                // 尝试从配置中找到匹配的subSource
                try {
                    val jsonElement = json.parseToJsonElement(trimmedJsCode)
                    if (jsonElement is JsonObject) {
                        val sitesArray = jsonElement["sites"]?.jsonArray
                        if (sitesArray != null) {
                            // 查找匹配api=spiderClassName的子源
                            val matchedSite = sitesArray.firstOrNull { siteElement ->
                                siteElement is JsonObject && 
                                (siteElement["api"]?.jsonPrimitive?.content == spiderClassName)
                            }?.jsonObject
                            
                            if (matchedSite != null) {
                                Log.d("XmboxVideoSource", "在sites数组中找到匹配的子源")
                                val key = matchedSite["key"]?.jsonPrimitive?.content ?: matchedSite["name"]?.jsonPrimitive?.content ?: ""
                                val name = matchedSite["name"]?.jsonPrimitive?.content ?: matchedSite["key"]?.jsonPrimitive?.content ?: ""
                                val api = matchedSite["api"]?.jsonPrimitive?.content ?: spiderClassName
                                val ext = matchedSite["ext"]?.jsonPrimitive?.content ?: ""
                                val jar = matchedSite["jar"]?.jsonPrimitive?.content ?: (spiderUrl ?: "")
                                val type = matchedSite["type"]?.jsonPrimitive?.intOrNull ?: 3 // Spider类型默认为3
                                val timeout = matchedSite["timeout"]?.jsonPrimitive?.intOrNull ?: config.timeout
                                
                                return org.streambox.datasource.xmbox.model.Site(
                                    key = key,
                                    name = name,
                                    api = org.streambox.datasource.xmbox.utils.UrlUtil.convert(api),
                                    ext = org.streambox.datasource.xmbox.utils.UrlUtil.convert(ext),
                                    jar = jar.ifBlank { spiderUrl ?: "" },
                                    type = type,
                                    timeout = timeout
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("XmboxVideoSource", "解析配置JSON失败，使用传入信息: ${e.message}")
                }
                
                // 如果没找到匹配的子源，但传入了spider信息，使用传入信息构建Site
                Log.d("XmboxVideoSource", "未找到匹配子源，使用传入的spider信息构建Site")
                return org.streambox.datasource.xmbox.model.Site(
                    key = "main",
                    name = config.name,
                    api = spiderClassName,
                    jar = spiderUrl ?: "",
                    type = 3,
                    timeout = config.timeout
                )
            }
            
            // 如果没有传入spider信息，从配置中解析
            val jsonElement = try {
                json.parseToJsonElement(trimmedJsCode)
            } catch (e: Exception) {
                Log.w("XmboxVideoSource", "JSON解析失败，jsCode可能不是JSON: ${e.message}")
                // 如果解析失败，可能是直接的Spider类名或其他格式
                return null
            }
            if (jsonElement is JsonObject) {
                val sitesArray = jsonElement["sites"]?.jsonArray
                if (sitesArray != null && sitesArray.isNotEmpty()) {
                    // 取第一个子源
                    val firstSite = sitesArray.firstOrNull()?.jsonObject
                    if (firstSite != null) {
                        val key = firstSite["key"]?.jsonPrimitive?.content ?: firstSite["name"]?.jsonPrimitive?.content ?: ""
                        val name = firstSite["name"]?.jsonPrimitive?.content ?: firstSite["key"]?.jsonPrimitive?.content ?: ""
                        val api = firstSite["api"]?.jsonPrimitive?.content ?: ""
                        val ext = firstSite["ext"]?.jsonPrimitive?.content ?: ""
                        val jar = firstSite["jar"]?.jsonPrimitive?.content ?: (spiderUrl ?: "")
                        val type = firstSite["type"]?.jsonPrimitive?.intOrNull ?: determineSiteType(api, jar, spiderClassName)
                        val timeout = firstSite["timeout"]?.jsonPrimitive?.intOrNull ?: config.timeout
                        
                        org.streambox.datasource.xmbox.model.Site(
                            key = key,
                            name = name,
                            api = org.streambox.datasource.xmbox.utils.UrlUtil.convert(api),
                            ext = org.streambox.datasource.xmbox.utils.UrlUtil.convert(ext),
                            jar = jar,
                            type = type,
                            timeout = timeout
                        )
                        } else {
                            null
                        }
                    } else {
                        // 没有sites数组，可能是单个源配置
                        // 检查config.jsCode是否是直接的API响应（视频列表JSON）
                        val isVideoListJson = (trimmedJsCode.startsWith("{") && 
                                              (trimmedJsCode.contains("\"code\"") || trimmedJsCode.contains("\"list\""))) ||
                                             trimmedJsCode.startsWith("[")
                        
                        if (isVideoListJson && !trimmedJsCode.contains("\"sites\"")) {
                            // 这是直接的视频列表JSON，不是配置，返回null让调用者处理
                            Log.d("XmboxVideoSource", "检测到视频列表JSON，返回null让调用者处理")
                            return null
                        }
                        
                        // 根据api字段判断类型
                        val api = jsonElement["api"]?.jsonPrimitive?.content ?: config.url
                        val type = determineSiteType(api, spiderUrl, spiderClassName)
                        
                        Log.d("XmboxVideoSource", "构建单个源Site: api=$api, type=$type")
                        org.streambox.datasource.xmbox.model.Site(
                            key = "main",
                            name = config.name,
                            api = org.streambox.datasource.xmbox.utils.UrlUtil.convert(api),
                            jar = spiderUrl ?: "",
                            type = type,
                            timeout = config.timeout
                        )
                    }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("XmboxVideoSource", "构建Site失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 根据api、jar、spiderClassName判断站点类型
     */
    private fun determineSiteType(api: String, jar: String?, spiderClassName: String?): Int {
        return when {
            // 如果api是csp_开头或是Spider类名，type=3
            api.startsWith("csp_") || spiderClassName != null -> 3
            // 如果api包含.js，type=3（JavaScript Spider）
            api.contains(".js") -> 3
            // 如果jar不为空，type=3
            !jar.isNullOrBlank() -> 3
            // 如果api包含.py，type=3（Python Spider，暂不支持但类型是3）
            api.contains(".py") -> 3
            // 如果api是HTTP URL，默认type=1（JSON）
            api.startsWith("http://") || api.startsWith("https://") -> 1
            // 其他情况，默认type=1
            else -> 1
        }
    }
    
    /**
     * 使用Spider获取首页视频列表
     */
    private suspend fun getHomeVideoListFromSpider(spiderUrl: String, className: String): List<VideoItem> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("XmboxVideoSource", "开始下载Spider: $spiderUrl")
                
                // 1. 下载Spider JAR
                val jarFile = spiderManager.downloadSpider(spiderUrl)
                if (jarFile == null) {
                    Log.e("XmboxVideoSource", "下载Spider失败")
                    return@withContext emptyList()
                }
                
                Log.d("XmboxVideoSource", "Spider下载成功: ${jarFile.absolutePath}")
                
                // 2. 加载Spider
                val spider = spiderLoader.loadSpider(jarFile, className)
                if (spider == null) {
                    Log.e("XmboxVideoSource", "加载Spider失败: $className")
                    return@withContext emptyList()
                }
                
                Log.d("XmboxVideoSource", "Spider加载成功，调用homeContent")
                
                // 3. 调用homeContent方法（对应XMBOX: spider.homeContent(true)）
                var jsonString = spiderLoader.callHomeContent(spider, true)
                if (jsonString == null) {
                    Log.w("XmboxVideoSource", "Spider.homeContent返回null，尝试homeVideoContent")
                    // 如果homeContent返回空，尝试homeVideoContent（对应XMBOX逻辑）
                    jsonString = spiderLoader.callHomeVideoContent(spider)
                }
                
                if (jsonString == null) {
                    Log.w("XmboxVideoSource", "Spider返回null")
                    return@withContext emptyList()
                }
                
                Log.d("XmboxVideoSource", "Spider返回数据: ${jsonString.take(200)}")
                
                // 4. 使用ResultParser解析（对应XMBOX: Result.fromJson()）
                val result = org.streambox.datasource.xmbox.utils.ResultParser.fromJson(jsonString)
                
                // 如果第一次解析为空且使用的是homeContent，尝试homeVideoContent（对应XMBOX逻辑）
                if (result.isEmpty()) {
                    val homeVideoContent = spiderLoader.callHomeVideoContent(spider)
                    if (homeVideoContent != null && homeVideoContent != jsonString) {
                        Log.d("XmboxVideoSource", "homeContent返回空列表，使用homeVideoContent结果")
                        org.streambox.datasource.xmbox.utils.ResultParser.fromJson(homeVideoContent)
                    } else {
                        result
                    }
                } else {
                    result
                }
            } catch (e: Exception) {
                Log.e("XmboxVideoSource", "Spider执行失败", e)
                emptyList()
            }
        }
    }
    
    /**
     * 使用Spider获取首页内容（type == 3）
     * 完全按照XMBOX的BaseLoader.getSpider()判断逻辑：
     * - api.contains(".py") -> Python Spider（暂不支持）
     * - api.contains(".js") -> JavaScript Spider（使用jsExecutor）
     * - api.startsWith("csp_") -> JAR Spider（使用jarLoader）
     * - else -> SpiderNull
     * 对应XMBOX的：site.recent().spider().homeContent(true)
     */
    private suspend fun getHomeContentFromSpider(
        site: org.streambox.datasource.xmbox.model.Site,
        mainSpiderUrl: String?,
        mainSpiderClassName: String?
    ): List<VideoItem> {
        return try {
            val api = site.api
            val js = api.contains(".js")
            val py = api.contains(".py")
            val csp = api.startsWith("csp_")
            
            when {
                // Python Spider（暂不支持，返回空）
                py -> {
                    Log.w("XmboxVideoSource", "Python Spider暂不支持: $api")
                    emptyList()
                }
                
                // JavaScript Spider：使用jsExecutor执行
                js -> {
                    Log.d("XmboxVideoSource", "使用JavaScript Spider: $api")
                    // api可能是.js文件的URL，需要先下载
                    val jsCode = if (api.startsWith("http://") || api.startsWith("https://")) {
                        try {
                            val response = httpClient.get(api)
                            response.bodyAsText()
                        } catch (e: Exception) {
                            Log.e("XmboxVideoSource", "下载JS文件失败: ${e.message}")
                            api // fallback: 使用api作为代码
                        }
                    } else {
                        api // 直接是JS代码
                    }
                    
                    val jsonString = jsExecutor.executeHome(site.key, jsCode, site.timeout.toLong())
                    if (jsonString != null) {
                        val result = org.streambox.datasource.xmbox.utils.ResultParser.fromJson(jsonString)
                        // 如果homeContent返回空列表，尝试homeVideoContent（对应XMBOX逻辑）
                        if (result.isEmpty()) {
                            val homeVideoContent = jsExecutor.executeHomeVideo(site.key, jsCode, site.timeout.toLong())
                            homeVideoContent?.let { org.streambox.datasource.xmbox.utils.ResultParser.fromJson(it) } ?: result
                        } else {
                            result
                        }
                    } else {
                        emptyList()
                    }
                }
                
                // JAR Spider：使用jarLoader
                csp || site.getSpiderClassName() != null -> {
                    val spiderClassName = site.getSpiderClassName() ?: mainSpiderClassName
                    val spiderUrl = site.getSpiderUrl(mainSpiderUrl)
                    
                    if (spiderClassName != null && spiderUrl != null) {
                        Log.d("XmboxVideoSource", "使用JAR Spider: className=$spiderClassName, url=$spiderUrl")
                        getHomeVideoListFromSpider(spiderUrl, spiderClassName)
            } else {
                        Log.w("XmboxVideoSource", "JAR Spider信息不完整: className=$spiderClassName, url=$spiderUrl")
                        emptyList()
                    }
                }
                
                // 其他情况：SpiderNull（空实现）
                else -> {
                    Log.w("XmboxVideoSource", "无法识别Spider类型: api=$api")
                emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e("XmboxVideoSource", "Spider获取首页内容失败: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * 使用Ext URL获取首页内容（type == 4）
     * 对应XMBOX的：call(site.fetchExt(), params) -> Result.fromJson()
     */
    private suspend fun getHomeContentFromExt(site: org.streambox.datasource.xmbox.model.Site): List<VideoItem> {
        return try {
            val extUrl = site.ext.ifBlank { site.api }
            if (extUrl.isBlank()) {
                Log.w("XmboxVideoSource", "Ext URL为空")
                return emptyList()
            }
            
            Log.d("XmboxVideoSource", "调用Ext URL: $extUrl")
            val response = httpClient.get(extUrl) {
                parameter("filter", "true")
            }
            val content = response.bodyAsText()
            org.streambox.datasource.xmbox.utils.ResultParser.fromJson(content)
        } catch (e: Exception) {
            Log.e("XmboxVideoSource", "Ext获取首页内容失败: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * 直接HTTP请求API获取首页内容（type == 0, 1, 2）
     * 对应XMBOX的：OkHttp.newCall(site.getApi(), site.getHeaders()).execute() -> Result.fromType(site.getType(), content)
     */
    private suspend fun getHomeContentFromApi(site: org.streambox.datasource.xmbox.model.Site): List<VideoItem> {
        return try {
            val apiUrl = site.api.ifBlank { return emptyList() }
            
            Log.d("XmboxVideoSource", "调用API: $apiUrl, type=${site.type}")
            val response = httpClient.get(apiUrl) {
                header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                header("Accept", "*/*")
            }
            val content = response.bodyAsText()
            
            // 使用Result.fromType解析
            org.streambox.datasource.xmbox.utils.ResultParser.fromType(site.type, content)
        } catch (e: Exception) {
            Log.e("XmboxVideoSource", "API获取首页内容失败: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * 解析视频列表 JSON（保持向后兼容）
     * 现在委托给ResultParser
     */
    private fun parseVideoListJson(jsonString: String): List<VideoItem> {
        return org.streambox.datasource.xmbox.utils.ResultParser.fromJson(jsonString)
    }
    
    /**
     * 从BMP图片中提取隐藏的配置数据
     * 使用CatVodSpider的方法来解析BMP隐写数据
     * 
     * TODO: 需要集成CatVodSpider的JAR文件
     * 下载地址: https://github.com/FongMi/CatVodSpider/tree/main/jar
     * 
     * 当前实现：简单的BMP末尾数据提取
     */
    private fun extractHiddenDataFromBmp(bmpBytes: ByteArray): String? {
        try {
            if (bmpBytes.size < 54) return null
            
            // BMP文件头解析
            val pixelDataOffset = (bmpBytes[10].toInt() and 0xFF) or
                                ((bmpBytes[11].toInt() and 0xFF) shl 8) or
                                ((bmpBytes[12].toInt() and 0xFF) shl 16) or
                                ((bmpBytes[13].toInt() and 0xFF) shl 24)
            
            val width = (bmpBytes[18].toInt() and 0xFF) or
                       ((bmpBytes[19].toInt() and 0xFF) shl 8) or
                       ((bmpBytes[20].toInt() and 0xFF) shl 16) or
                       ((bmpBytes[21].toInt() and 0xFF) shl 24)
            
            val height = (bmpBytes[22].toInt() and 0xFF) or
                        ((bmpBytes[23].toInt() and 0xFF) shl 8) or
                        ((bmpBytes[24].toInt() and 0xFF) shl 16) or
                        ((bmpBytes[25].toInt() and 0xFF) shl 24)
            
            val bitsPerPixel = (bmpBytes[28].toInt() and 0xFF) or
                              ((bmpBytes[29].toInt() and 0xFF) shl 8)
            
            Log.d("XmboxVideoSource", "BMP: ${width}x${height}, ${bitsPerPixel}bpp, offset=$pixelDataOffset")
            
            // 计算像素数据大小
            val rowSize = ((bitsPerPixel * width + 31) / 32) * 4
            val pixelDataSize = rowSize * kotlin.math.abs(height)
            val totalBmpSize = pixelDataOffset + pixelDataSize
            
            Log.d("XmboxVideoSource", "BMP大小: $totalBmpSize, 文件大小: ${bmpBytes.size}")
            
            // 提取BMP末尾的隐藏数据
            if (bmpBytes.size > totalBmpSize + 10) {
                val hiddenBytes = bmpBytes.sliceArray(totalBmpSize until bmpBytes.size)
                Log.d("XmboxVideoSource", "发现 ${hiddenBytes.size} 字节额外数据")
                
                // UTF-8解码
                val hiddenText = String(hiddenBytes, Charsets.UTF_8).trim()
                if (hiddenText.startsWith("{") || hiddenText.startsWith("[")) {
                    return hiddenText
                }
                
                // Base64解码
                val clean = hiddenText.replace(Regex("\\s+"), "")
                if (clean.matches(Regex("^[A-Za-z0-9+/=]+$")) && clean.length > 20) {
                    try {
                        val decoded = String(Base64.getDecoder().decode(clean), Charsets.UTF_8).trim()
                        if (decoded.startsWith("{") || decoded.startsWith("[")) return decoded
                    } catch (e: Exception) {}
                }
                
                // 查找JSON标记
                val jsonStart = hiddenBytes.indexOf('{'.code.toByte())
                if (jsonStart >= 0) {
                    return String(hiddenBytes.sliceArray(jsonStart until hiddenBytes.size), Charsets.UTF_8).trim()
                }
                
                return hiddenText
            }
            
            return null
        } catch (e: Exception) {
            Log.e("XmboxVideoSource", "BMP提取失败: ${e.message}", e)
            return null
        }
    }
    
    /**
     * 判断URL是否是视频直链
     */
    private fun isDirectVideoUrl(url: String): Boolean {
        if (url.isBlank()) return false
        
        val videoExtensions = listOf(
            ".mp4", ".mkv", ".m3u8", ".flv", ".avi",
            ".webm", ".mov", ".wmv", ".ts", ".m3u"
        )
        val lowerUrl = url.lowercase()
        
        return videoExtensions.any { lowerUrl.contains(it) } ||
               Regex("""(m3u8|mp4|flv|avi|mkv|webm|mov|wmv|ts)""", RegexOption.IGNORE_CASE)
                   .containsMatchIn(url)
    }
    
    /**
     * 从HTML页面中提取配置链接
     * 处理像 http://www.饭太硬.com/tv 这样的网页
     * 完全按照XMBOX的逻辑：智能识别配置链接
     */
    private suspend fun extractConfigFromHtml(htmlContent: String, baseUrl: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val doc = org.jsoup.Jsoup.parse(htmlContent, baseUrl)
                val configLinks = mutableListOf<String>()
                
                Log.d("XmboxVideoSource", "开始解析HTML，baseUrl=$baseUrl")
                
                // 1. 提取所有 <a> 标签的链接，按优先级排序
                val links = doc.select("a[href]")
                val candidateLinks = mutableListOf<Pair<String, String>>() // href, text
                
                for (link in links) {
                    var href = link.attr("abs:href") // 获取绝对URL
                    if (href.isBlank()) {
                        href = link.attr("href")
                    }
                    
                    // 跳过空链接、锚点链接、javascript链接、mailto等
                    if (href.isBlank() || href.startsWith("#") || href.startsWith("javascript:")) {
                        continue
                    }
                    
                    // 检查是否是可能的配置链接
                    // CatVod配置通常是 .json、.txt 或者看起来像API的URL
                    if (href.matches(Regex(".*\\.(json|txt|php|jsp|asp|cfg|conf).*")) ||
                        href.contains("/api") ||
                        href.contains("config") ||
                        href.contains("spider") ||
                        href.contains(".png") || // kk.png 伪装
                        href.contains(".jpg") ||
                        href.contains(".gif") ||
                        href.matches(Regex(".*[a-zA-Z0-9]+\\.(top|com|cn|net)/[a-zA-Z0-9]+/?$"))) {
                        
                        configLinks.add(href)
                        Log.d("XmboxVideoSource", "找到可能的配置链接: $href, 文本=${link.text().take(50)}")
                    }
                }
                
                // 2. 提取 <script> 标签中的JSON配置
                val scripts = doc.select("script")
                for (script in scripts) {
                    val scriptContent = script.data()
                    if (scriptContent.isBlank()) continue
                    
                    // 查找JSON对象或数组
                    val jsonPattern = Regex("""(\{[\s\S]*?"sites"[\s\S]*?\})""")
                    val match = jsonPattern.find(scriptContent)
                    if (match != null) {
                        val jsonStr = match.groupValues[1]
                        Log.d("XmboxVideoSource", "在<script>中找到JSON配置，长度=${jsonStr.length}")
                        // 这里可以直接返回JSON字符串，或者继续搜索URL
                    }
                }
                
                // 3. 查找可能的Base64编码的配置
                val base64Pattern = Regex("""["']([A-Za-z0-9+/]{100,}={0,2})["']""")
                val base64Matches = base64Pattern.findAll(htmlContent)
                for (match in base64Matches) {
                    val base64Str = match.groupValues[1]
                    try {
                        val decoded = String(android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT), Charsets.UTF_8)
                        if (decoded.contains("sites") || decoded.startsWith("{") || decoded.startsWith("[")) {
                            Log.d("XmboxVideoSource", "在HTML中找到Base64编码的配置，解码后长度=${decoded.length}")
                        }
                    } catch (e: Exception) {
                        // 不是有效的Base64，跳过
                    }
                }
                
                Log.d("XmboxVideoSource", "从HTML中提取到 ${configLinks.size} 个潜在配置链接")
                configLinks
            } catch (e: Exception) {
                Log.e("XmboxVideoSource", "HTML解析失败: ${e.message}", e)
                emptyList()
            }
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

