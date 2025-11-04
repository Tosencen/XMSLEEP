package org.streambox.datasource.xmbox

import android.content.Context
import android.webkit.*
import kotlinx.coroutines.Dispatchers
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * CatVodTV JavaScript 执行器
 * 在 WebView 中执行 CatVodTV 格式的 JavaScript 代码来解析视频源
 */
class CatVodTVJavaScriptExecutor(private val context: Context) {
    
    /**
     * 执行 CatVodTV JavaScript 代码并提取视频 URL
     * 
     * @param pageUrl 要解析的页面 URL
     * @param jsCode CatVodTV JavaScript 代码
     * @param timeout 超时时间（秒）
     * @return 提取的视频 URL 列表
     */
    suspend fun execute(
        pageUrl: String,
        jsCode: String,
        timeout: Long = 30
    ): List<String> = withContext(Dispatchers.Main) {
        executeInWebView(pageUrl, jsCode, timeout)
    }
    
    /**
     * 执行 home() 函数获取首页视频列表
     * 
     * @param baseUrl 视频源基础URL
     * @param jsCode CatVodTV JavaScript 代码
     * @param timeout 超时时间（秒）
     * @return 视频列表JSON字符串
     */
    suspend fun executeHome(
        baseUrl: String,
        jsCode: String,
        timeout: Long = 30
    ): String? = withContext(Dispatchers.Main) {
        executeHomeInWebView(baseUrl, jsCode, timeout)
    }
    
    /**
     * 在 WebView 中执行 JavaScript 代码
     */
    private suspend fun executeInWebView(
        pageUrl: String,
        jsCode: String,
        timeout: Long
    ): List<String> = suspendCancellableCoroutine { continuation ->
        var webView: WebView? = null
        val videoUrls = mutableListOf<String>()
        val latch = CountDownLatch(1)
        var isCompleted = false
        
        try {
            webView = WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36"
                }
                
                // 注入 JavaScript 代码来拦截视频 URL
                val injectedScript = """
                    (function() {
                        var originalFetch = window.fetch;
                        var originalXHR = XMLHttpRequest;
                        var videoUrls = [];
                        
                        // 拦截 fetch 请求
                        window.fetch = function() {
                            var url = arguments[0];
                            if (url && typeof url === 'string') {
                                if (isVideoUrl(url)) {
                                    videoUrls.push(url);
                                    window.videoExtractorCallback(url);
                                }
                            }
                            return originalFetch.apply(this, arguments);
                        };
                        
                        // 拦截 XMLHttpRequest
                        var xhrOpen = XMLHttpRequest.prototype.open;
                        XMLHttpRequest.prototype.open = function(method, url) {
                            if (url && isVideoUrl(url)) {
                                videoUrls.push(url);
                                window.videoExtractorCallback(url);
                            }
                            return xhrOpen.apply(this, arguments);
                        };
                        
                        // 监听视频元素
                        var observer = new MutationObserver(function(mutations) {
                            mutations.forEach(function(mutation) {
                                mutation.addedNodes.forEach(function(node) {
                                    if (node.nodeName === 'VIDEO' || node.nodeName === 'AUDIO') {
                                        var src = node.src || node.getAttribute('src');
                                        if (src && isVideoUrl(src)) {
                                            videoUrls.push(src);
                                            window.videoExtractorCallback(src);
                                        }
                                    }
                                });
                            });
                        });
                        observer.observe(document.body, { childList: true, subtree: true });
                        
                        function isVideoUrl(url) {
                            return /\.(mp4|m3u8|flv|avi|mkv|webm|mov|wmv|ts|m3u)/i.test(url) || 
                                   /video|stream|play|m3u8/i.test(url);
                        }
                        
                        window.videoUrls = videoUrls;
                    })();
                """.trimIndent()
                
                // 设置 WebViewClient 来拦截资源请求
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                        
                        // 检查是否是视频 URL
                        if (isVideoUrl(url)) {
                            synchronized(videoUrls) {
                                if (!videoUrls.contains(url)) {
                                    videoUrls.add(url)
                                }
                            }
                            // 不立即返回结果，继续收集更多可能的视频 URL
                            return null
                        }
                        
                        return super.shouldInterceptRequest(view, request)
                    }
                    
                    override fun onLoadResource(view: WebView?, url: String?) {
                        super.onLoadResource(view, url)
                        url?.let {
                            if (isVideoUrl(it)) {
                                synchronized(videoUrls) {
                                    if (!videoUrls.contains(it)) {
                                        videoUrls.add(it)
                                    }
                                }
                            }
                        }
                    }
                    
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        
                        // 页面加载完成后，执行 CatVodTV JavaScript 代码
                        if (jsCode.isNotBlank()) {
                            view?.evaluateJavascript("""
                                (function() {
                                    try {
                                        // 注入 CatVodTV API 支持函数
                                        window.request = function(url, options) {
                                            return fetch(url, options || {}).then(function(res) {
                                                return res.text();
                                            });
                                        };
                                        
                                        window.post = function(url, data) {
                                            return fetch(url, {
                                                method: 'POST',
                                                body: JSON.stringify(data),
                                                headers: { 'Content-Type': 'application/json' }
                                            }).then(function(res) {
                                                return res.text();
                                            });
                                        };
                                        
                                        // PDFH 和 PDFA 函数（用于解析 HTML）
                                        window.pdfh = function(html, rule) {
                                            try {
                                                var parser = new DOMParser();
                                                var doc = parser.parseFromString(html, 'text/html');
                                                var elements = doc.querySelectorAll(rule);
                                                return elements.length > 0 ? elements[0].textContent : '';
                                            } catch (e) {
                                                return '';
                                            }
                                        };
                                        
                                        window.pdfa = function(html, rule) {
                                            try {
                                                var parser = new DOMParser();
                                                var doc = parser.parseFromString(html, 'text/html');
                                                var elements = doc.querySelectorAll(rule);
                                                var result = [];
                                                for (var i = 0; i < elements.length; i++) {
                                                    result.push(elements[i].outerHTML);
                                                }
                                                return JSON.stringify(result);
                                            } catch (e) {
                                                return '[]';
                                            }
                                        };
                                        
                                        // 执行用户提供的 JavaScript 代码
                                        ${jsCode}
                                        
                                        // 尝试调用不同的函数来获取视频 URL
                                        var videoUrl = null;
                                        
                                        // 1. 尝试 play() 函数
                                        if (typeof play === 'function') {
                                            try {
                                                var playResult = play("", "");
                                                if (playResult && typeof playResult === 'string' && playResult.trim()) {
                                                    videoUrl = playResult.trim();
                                                } else if (playResult && playResult.url) {
                                                    videoUrl = playResult.url;
                                                }
                                            } catch (e) {
                                                console.log('play() error:', e);
                                            }
                                        }
                                        
                                        // 2. 如果没有结果，尝试查找页面中的视频元素
                                        if (!videoUrl) {
                                            var videoElement = document.querySelector('video');
                                            if (videoElement && videoElement.src) {
                                                videoUrl = videoElement.src;
                                            }
                                        }
                                        
                                        // 3. 查找 source 标签
                                        if (!videoUrl) {
                                            var sourceElement = document.querySelector('video source, audio source');
                                            if (sourceElement && sourceElement.src) {
                                                videoUrl = sourceElement.src;
                                            }
                                        }
                                        
                                        return videoUrl || null;
                                    } catch (e) {
                                        console.error('CatVodTV script error:', e);
                                        return null;
                                    }
                                })();
                            """.trimIndent()) { result ->
                                // 解析结果
                                if (result != null && result != "null" && result.isNotBlank()) {
                                    val url = result.trim('"', '\'', ' ', '\n', '\r')
                                    if (url.isNotBlank()) {
                                        synchronized(videoUrls) {
                                            if (!videoUrls.contains(url)) {
                                                videoUrls.add(url)
                                            }
                                        }
                                    }
                                }
                                
                                // 延迟一下，等待可能的异步请求
                                Handler(Looper.getMainLooper()).postDelayed({
                                    synchronized(videoUrls) {
                                        if (!isCompleted && videoUrls.isNotEmpty()) {
                                            continuation.resume(videoUrls.toList())
                                            isCompleted = true
                                            latch.countDown()
                                        }
                                    }
                                }, 2000) // 等待 2 秒以收集更多 URL
                            }
                        } else {
                            // 如果没有 JavaScript 代码，只是查找页面中的视频元素
                            view?.evaluateJavascript("""
                                (function() {
                                    var videoUrl = null;
                                    var videoElement = document.querySelector('video');
                                    if (videoElement && videoElement.src) {
                                        videoUrl = videoElement.src;
                                    } else {
                                        var sourceElement = document.querySelector('video source, audio source');
                                        if (sourceElement && sourceElement.src) {
                                            videoUrl = sourceElement.src;
                                        }
                                    }
                                    return videoUrl || null;
                                })();
                            """.trimIndent()) { result ->
                                if (result != null && result != "null" && result.isNotBlank()) {
                                    val url = result.trim('"', '\'', ' ', '\n', '\r')
                                    if (url.isNotBlank()) {
                                        synchronized(videoUrls) {
                                            if (!videoUrls.contains(url)) {
                                                videoUrls.add(url)
                                            }
                                        }
                                    }
                                }
                                
                                Handler(Looper.getMainLooper()).postDelayed({
                                    synchronized(videoUrls) {
                                        if (!isCompleted) {
                                            continuation.resume(videoUrls.toList())
                                            isCompleted = true
                                            latch.countDown()
                                        }
                                    }
                                }, 1000)
                            }
                        }
                    }
                }
                
                // 加载页面
                loadUrl(pageUrl)
            }
            
            // 设置超时
            continuation.invokeOnCancellation {
                webView?.destroy()
            }
            
            // 等待结果（带超时）
            val timeoutReached = !latch.await(timeout, TimeUnit.SECONDS)
            
            if (timeoutReached && !isCompleted) {
                synchronized(videoUrls) {
                    webView?.destroy()
                    if (videoUrls.isNotEmpty()) {
                        continuation.resume(videoUrls.toList())
                    } else {
                        continuation.resume(emptyList())
                    }
                    isCompleted = true
                }
            }
            
        } catch (e: Exception) {
            webView?.destroy()
            if (!isCompleted) {
                continuation.resumeWithException(e)
            }
        }
    }
    
    /**
     * 判断是否是视频 URL
     */
    private fun isVideoUrl(url: String): Boolean {
        val videoExtensions = listOf(".mp4", ".m3u8", ".flv", ".avi", ".mkv", ".webm", ".mov", ".wmv", ".ts", ".m3u")
        val lowerUrl = url.lowercase()
        
        return videoExtensions.any { lowerUrl.contains(it) } ||
               Regex("video|stream|play|m3u8", RegexOption.IGNORE_CASE).containsMatchIn(url)
    }
    
    /**
     * 在 WebView 中执行 home() 函数获取视频列表
     */
    private suspend fun executeHomeInWebView(
        baseUrl: String,
        jsCode: String,
        timeout: Long
    ): String? = suspendCancellableCoroutine { continuation ->
        var webView: WebView? = null
        val latch = CountDownLatch(1)
        var isCompleted = false
        
        try {
            webView = WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36"
                }
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        
                        // 页面加载完成后，执行 JavaScript 代码并调用 home() 函数
                        view?.evaluateJavascript("""
                            (function() {
                                try {
                                    // 注入 CatVodTV API 支持函数
                                    window.request = function(url, options) {
                                        return fetch(url, options || {}).then(function(res) {
                                            return res.text();
                                        });
                                    };
                                    
                                    window.post = function(url, data) {
                                        return fetch(url, {
                                            method: 'POST',
                                            body: JSON.stringify(data),
                                            headers: { 'Content-Type': 'application/json' }
                                        }).then(function(res) {
                                            return res.text();
                                        });
                                    };
                                    
                                    // PDFH 和 PDFA 函数（用于解析 HTML）
                                    window.pdfh = function(html, rule) {
                                        try {
                                            var parser = new DOMParser();
                                            var doc = parser.parseFromString(html, 'text/html');
                                            var elements = doc.querySelectorAll(rule);
                                            return elements.length > 0 ? elements[0].textContent : '';
                                        } catch (e) {
                                            return '';
                                        }
                                    };
                                    
                                    window.pdfa = function(html, rule) {
                                        try {
                                            var parser = new DOMParser();
                                            var doc = parser.parseFromString(html, 'text/html');
                                            var elements = doc.querySelectorAll(rule);
                                            var result = [];
                                            for (var i = 0; i < elements.length; i++) {
                                                result.push(elements[i].outerHTML);
                                            }
                                            return JSON.stringify(result);
                                        } catch (e) {
                                            return '[]';
                                        }
                                    };
                                    
                                    // 执行用户提供的 JavaScript 代码
                                    ${jsCode}
                                    
                                    // 调用 home() 函数获取首页视频列表
                                    if (typeof home === 'function') {
                                        try {
                                            var homeResult = home();
                                            if (homeResult && typeof homeResult === 'string') {
                                                return homeResult;
                                            } else if (homeResult && typeof homeResult === 'object') {
                                                return JSON.stringify(homeResult);
                                            }
                                        } catch (e) {
                                            console.error('home() error:', e);
                                            return JSON.stringify({code: 0, msg: e.message, list: []});
                                        }
                                    } else {
                                        return JSON.stringify({code: 0, msg: 'home() function not found', list: []});
                                    }
                                } catch (e) {
                                    console.error('CatVodTV script error:', e);
                                    return JSON.stringify({code: 0, msg: e.message, list: []});
                                }
                            })();
                        """.trimIndent()) { result ->
                            val jsonString = result?.trim('"', '\'', ' ', '\n', '\r') ?: ""
                            if (!isCompleted) {
                                continuation.resume(if (jsonString.isNotBlank()) jsonString else null)
                                isCompleted = true
                                latch.countDown()
                            }
                        }
                    }
                }
                
                // 加载基础URL（通常是一个空页面或源的主页）
                loadUrl(baseUrl)
            }
            
            // 设置超时
            continuation.invokeOnCancellation {
                webView?.destroy()
            }
            
            // 等待结果（带超时）
            val timeoutReached = !latch.await(timeout, TimeUnit.SECONDS)
            
            if (timeoutReached && !isCompleted) {
                webView?.destroy()
                continuation.resume(null)
                isCompleted = true
            }
            
        } catch (e: Exception) {
            webView?.destroy()
            if (!isCompleted) {
                continuation.resume(null)
                isCompleted = true
            }
        }
    }
}

