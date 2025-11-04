package org.streambox.datasource.xmbox.utils

import android.util.Base64
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * URL解码工具
 * 从XMBOX的Decoder类移植
 * 支持多种编码格式：Base64、AES/CBC加密等
 */
object UrlDecoder {
    
    private const val TAG = "UrlDecoder"
    
    // JavaScript URI模式匹配："./" 或 "../"
    private val JS_URI_PATTERN = Pattern.compile("\"(\\.|\\.\\.)/(.?|.+?)\\.js\\?(.?|.+?)\"")
    
    // Base64模式匹配
    private val BASE64_PATTERN = Pattern.compile("[A-Za-z0-9]{8}\\*\\*")
    
    /**
     * 从URL获取并解码JSON字符串
     * 支持多种编码格式自动识别
     * 从XMBOX的Decoder.getJson()移植
     * 
     * @param url 要下载的URL（会自动转换特殊scheme）
     * @param httpClient HTTP客户端（从XmboxVideoSource传入）
     */
    suspend fun getJson(url: String, httpClient: io.ktor.client.HttpClient): String {
        return withContext(Dispatchers.IO) {
            try {
                // 检查是否是本地文件
                val scheme = UrlUtil.scheme(url)
                val rawBytes: ByteArray = when (scheme) {
                    "file" -> {
                        // 直接读取本地文件
                        val filePath = url.removePrefix("file://")
                        val file = java.io.File(filePath)
                        if (file.exists() && file.isFile) {
                            file.readBytes()
                        } else {
                            throw Exception("文件不存在: $filePath")
                        }
                    }
                    "assets" -> {
                        // assets:// 需要从Android Assets读取，这里暂时不支持
                        // 如果需要支持，需要传入Context
                        throw Exception("assets:// 暂不支持，需要Context参数")
                    }
                    else -> {
                        // HTTP/HTTPS URL，先转换（处理proxy://等）
                        val convertedUrl = UrlUtil.convert(url)
                        
                        // 下载内容（先获取原始字节）
                        val response = httpClient.get(convertedUrl) {
                            header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                            header("Accept", "*/*")
                            header("Accept-Language", "zh-CN,zh;q=0.9")
                        }
                        
                        response.body<ByteArray>()
                    }
                }
                
                // 检查BMP文件头
                if (rawBytes.size > 2) {
                    val header = String(rawBytes.take(2).toByteArray(), Charsets.ISO_8859_1)
                    if (header == "BM") {
                        Log.d(TAG, "检测到BMP文件，提取隐写数据")
                        // 提取BMP隐写数据
                        val hiddenData = extractHiddenDataFromBmp(rawBytes)
                        if (hiddenData != null) {
                            Log.d(TAG, "从BMP提取数据成功，长度: ${hiddenData.length}")
                            return@withContext verify(url, hiddenData)
                        } else {
                            throw Exception("BMP隐写数据提取失败")
                        }
                    }
                }
                
                // 如果不是BMP，尝试作为文本解码
                var data: String
                try {
                    data = String(rawBytes, Charsets.UTF_8).trim()
                } catch (e: Exception) {
                    // UTF-8失败，尝试ISO-8859-1
                    Log.w(TAG, "UTF-8解码失败，尝试ISO-8859-1")
                    data = String(rawBytes, Charsets.ISO_8859_1).trim()
                }
                
                // 验证并解码（支持Base64、AES/CBC等）
                data = verify(url, data)
                data
            } catch (e: Exception) {
                Log.e(TAG, "从URL获取JSON失败: $url", e)
                throw e
            }
        }
    }
    
    /**
     * 从BMP文件中提取隐写数据
     * 这是一个简化的实现，完整实现应该在XmboxVideoSource中
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
            
            Log.d(TAG, "BMP: ${width}x${height}, ${bitsPerPixel}bpp, offset=$pixelDataOffset")
            
            // 计算像素数据大小
            val rowSize = ((bitsPerPixel * width + 31) / 32) * 4
            val pixelDataSize = rowSize * kotlin.math.abs(height)
            val totalBmpSize = pixelDataOffset + pixelDataSize
            
            Log.d(TAG, "BMP大小: $totalBmpSize, 文件大小: ${bmpBytes.size}")
            
            // 提取BMP末尾的隐藏数据
            if (bmpBytes.size > totalBmpSize + 10) {
                val hiddenBytes = bmpBytes.sliceArray(totalBmpSize until bmpBytes.size)
                Log.d(TAG, "发现 ${hiddenBytes.size} 字节额外数据")
                
                // UTF-8解码
                try {
                    val hiddenText = String(hiddenBytes, Charsets.UTF_8).trim()
                    if (hiddenText.isNotBlank()) {
                        Log.d(TAG, "UTF-8解码成功，长度: ${hiddenText.length}")
                        return hiddenText
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "UTF-8解码失败，尝试Base64: ${e.message}")
                }
                
                // 尝试Base64解码
                try {
                    val base64Str = String(hiddenBytes, Charsets.ISO_8859_1)
                    val decoded = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
                    val decodedText = String(decoded, Charsets.UTF_8).trim()
                    if (decodedText.isNotBlank()) {
                        Log.d(TAG, "Base64解码成功，长度: ${decodedText.length}")
                        return decodedText
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Base64解码失败: ${e.message}")
                }
                
                // 如果都失败，返回原始字节的ISO-8859-1字符串
                return String(hiddenBytes, Charsets.ISO_8859_1).trim()
            }
            
            return null
        } catch (e: Exception) {
            Log.e(TAG, "BMP提取失败: ${e.message}", e)
            return null
        }
    }
    
    /**
     * 验证并解码数据
     * 自动识别编码类型：JSON、Base64、AES/CBC加密等
     */
    private fun verify(url: String, data: String): String {
        if (data.isEmpty()) {
            throw IllegalArgumentException("数据为空")
        }
        
        // 如果已经是JSON对象，直接返回
        if (isJsonObject(data)) {
            return fix(url, data)
        }
        
        // 如果包含"**"，可能是Base64编码
        if (data.contains("**")) {
            val decoded = base64(data)
            if (decoded.isNotEmpty() && decoded != data) {
                return fix(url, decoded)
            }
        }
        
        // 如果以"2423"开头，可能是AES/CBC加密
        if (data.startsWith("2423")) {
            try {
                val decoded = cbc(data)
                return fix(url, decoded)
            } catch (e: Exception) {
                Log.w(TAG, "AES/CBC解密失败，尝试其他方式", e)
            }
        }
        
        // 默认修复相对路径后返回
        return fix(url, data)
    }
    
    /**
     * 修复相对路径和JavaScript引用
     */
    private fun fix(url: String, data: String): String {
        var result = data
        
        // 修复JavaScript URI引用
        val jsMatcher = JS_URI_PATTERN.matcher(result)
        while (jsMatcher.find()) {
            val matched = jsMatcher.group()
            result = replace(url, result, matched)
        }
        
        // 修复相对路径
        if (result.contains("../")) {
            result = result.replace("../", resolveUrl(url, "../"))
        }
        if (result.contains("./")) {
            result = result.replace("./", resolveUrl(url, "./"))
        }
        
        // 修复占位符
        if (result.contains("__JS1__")) {
            result = result.replace("__JS1__", "./")
        }
        if (result.contains("__JS2__")) {
            result = result.replace("__JS2__", "../")
        }
        
        return result
    }
    
    /**
     * 替换JavaScript URI引用
     */
    private fun replace(url: String, data: String, ext: String): String {
        var t = ext.replace("\"./", "\"" + resolveUrl(url, "./"))
        t = t.replace("\"../", "\"" + resolveUrl(url, "../"))
        t = t.replace("./", "__JS1__").replace("../", "__JS2__")
        return data.replace(ext, t)
    }
    
    /**
     * Base64解码
     */
    private fun base64(data: String): String {
        val extract = extractBase64(data)
        if (extract.isEmpty()) return data
        
        return try {
            val decoded = Base64.decode(extract, Base64.DEFAULT)
            String(decoded)
        } catch (e: Exception) {
            Log.w(TAG, "Base64解码失败", e)
            data
        }
    }
    
    /**
     * 提取Base64编码的内容
     */
    private fun extractBase64(data: String): String {
        val matcher = BASE64_PATTERN.matcher(data)
        return if (matcher.find()) {
            val startIndex = data.indexOf(matcher.group()) + 10
            if (startIndex < data.length) {
                data.substring(startIndex)
            } else {
                ""
            }
        } else {
            ""
        }
    }
    
    /**
     * AES/CBC解密
     * 格式：2423...2324...（特殊加密格式）
     */
    private fun cbc(data: String): String {
        try {
            // 解码hex字符串
            val decode = hex2String(data).lowercase()
            
            // 提取key和iv
            val keyStart = decode.indexOf("$#") + 2
            val keyEnd = decode.indexOf("#$")
            if (keyStart < 2 || keyEnd <= keyStart) {
                throw IllegalArgumentException("无法找到key")
            }
            
            val keyStr = padEnd(decode.substring(keyStart, keyEnd))
            val ivStr = padEnd(decode.substring(decode.length - 13))
            
            // 创建AES/CBC解密器
            val keySpec = SecretKeySpec(keyStr.toByteArray(), "AES")
            val ivSpec = IvParameterSpec(ivStr.toByteArray())
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            
            // 提取加密数据
            val dataStart = data.indexOf("2324") + 4
            val encryptedHex = data.substring(dataStart, data.length - 26)
            val encryptedBytes = hex2Bytes(encryptedHex)
            
            // 解密
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "AES/CBC解密失败", e)
            throw e
        }
    }
    
    /**
     * Hex字符串转字节数组
     */
    private fun hex2Bytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4)
                    + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
    
    /**
     * Hex字符串转字符串（尝试）
     */
    private fun hex2String(hex: String): String {
        return try {
            val bytes = hex2Bytes(hex)
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            hex
        }
    }
    
    /**
     * 填充key到16字节
     */
    private fun padEnd(key: String): String {
        val pad = "0000000000000000"
        return if (key.length < 16) {
            key + pad.substring(key.length)
        } else {
            key.substring(0, 16)
        }
    }
    
    /**
     * 解析相对URL
     * 使用UrlUtil.resolve()
     */
    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        return UrlUtil.resolve(baseUrl, relativeUrl)
    }
    
    /**
     * 检查字符串是否是JSON对象
     */
    private fun isJsonObject(data: String): Boolean {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val element = json.parseToJsonElement(data)
            element is JsonObject || element is JsonArray
        } catch (e: Exception) {
            false
        }
    }
}

