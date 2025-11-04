package org.streambox.datasource.xmbox.spider

import android.content.Context
import android.util.Log
import dalvik.system.DexClassLoader
import java.io.File

/**
 * Spider 动态加载器
 * 使用 DexClassLoader 加载 Spider JAR，并通过反射调用
 */
class SpiderLoader(private val context: Context) {
    
    private val loadedSpiders = mutableMapOf<String, Any>() // className -> spider instance
    private val classLoaders = mutableMapOf<String, DexClassLoader>() // jarPath -> classLoader
    
    /**
     * 加载Spider实例
     * 
     * @param jarFile Spider JAR文件
     * @param className Spider类名（如 csp_DoubanGuard）
     * @param extend 扩展参数
     * @return Spider实例，失败返回null
     */
    fun loadSpider(jarFile: File, className: String, extend: String? = null): Any? {
        try {
            val cacheKey = "${jarFile.absolutePath}#$className"
            
            // 检查是否已加载
            loadedSpiders[cacheKey]?.let {
                Log.d(TAG, "Spider已缓存: $className")
                return it
            }
            
            Log.d(TAG, "开始加载Spider: $className from ${jarFile.name}")
            
            // 获取或创建ClassLoader
            val classLoader = classLoaders.getOrPut(jarFile.absolutePath) {
                val loader = createClassLoader(jarFile)
                // 第一次创建ClassLoader时，先调用Init.init()（对应XMBOX的invokeInit）
                try {
                    val initClass = loader.loadClass("com.github.catvod.spider.Init")
                    val initMethod = initClass.getMethod("init", android.content.Context::class.java)
                    initMethod.invoke(null, context)
                    Log.d(TAG, "成功调用Init.init()")
                } catch (e: ClassNotFoundException) {
                    Log.d(TAG, "未找到Init类，跳过初始化")
                } catch (e: Exception) {
                    Log.w(TAG, "Init.init()失败: ${e.message}")
                }
                loader
            }
            
            // 按照XMBOX的方式：对于csp_开头，直接使用 com.github.catvod.spider.XXX
            val actualClassName = if (className.startsWith("csp_")) {
                val baseClassName = className.removePrefix("csp_")
                "com.github.catvod.spider.$baseClassName"
            } else {
                // 不是csp_开头，尝试多种格式
                className
            }
            
            // 尝试多种可能的类名格式（保持向后兼容）
            val possibleClassNames = buildPossibleClassNames(className)
            Log.d(TAG, "尝试以下类名: ${possibleClassNames.joinToString(", ")}")
            
            var spiderClass: Class<*>? = null
            var lastException: Exception? = null
            
            // 优先尝试XMBOX的标准格式（对应api.split("csp_")[1]）
            if (className.startsWith("csp_") && actualClassName in possibleClassNames) {
                try {
                    spiderClass = classLoader.loadClass(actualClassName)
                    Log.d(TAG, "成功加载类（XMBOX标准格式）: ${spiderClass.name}")
                } catch (e: ClassNotFoundException) {
                    lastException = e
                    Log.w(TAG, "XMBOX标准格式失败: $actualClassName")
                }
            }
            
            // 如果XMBOX格式失败，尝试其他格式
            if (spiderClass == null) {
                for (possibleName in possibleClassNames) {
                    if (possibleName == actualClassName) continue // 已经尝试过了
                    try {
                        spiderClass = classLoader.loadClass(possibleName)
                        Log.d(TAG, "成功加载类: ${spiderClass.name}")
                        break
                    } catch (e: ClassNotFoundException) {
                        lastException = e
                        // 继续尝试下一个
                    }
                }
            }
            
            if (spiderClass == null) {
                throw ClassNotFoundException("无法找到Spider类，尝试了: ${possibleClassNames.joinToString(", ")}", lastException)
            }
            
            // 创建实例
            val spiderInstance = spiderClass.getDeclaredConstructor().newInstance()
            Log.d(TAG, "成功创建实例: ${spiderClass.name}")
            
            // 调用init方法初始化Spider实例
            try {
                val initMethod = spiderClass.getMethod("init", android.content.Context::class.java, String::class.java)
                initMethod.invoke(spiderInstance, context, extend)
                Log.d(TAG, "成功初始化Spider: $className")
            } catch (e: NoSuchMethodException) {
                Log.w(TAG, "Spider没有init方法: $className")
            } catch (e: Exception) {
                Log.w(TAG, "Spider初始化失败: ${e.message}")
            }
            
            // 缓存实例
            loadedSpiders[cacheKey] = spiderInstance
            
            return spiderInstance
        } catch (e: Exception) {
            Log.e(TAG, "加载Spider失败: $className", e)
            return null
        }
    }
    
    /**
     * 创建DexClassLoader
     * 按照XMBOX的方式：直接使用JAR文件路径，DexClassLoader会自动处理JAR中的DEX
     */
    private fun createClassLoader(jarFile: File): DexClassLoader {
        // 确保JAR文件是只读的（Android 10+要求，XMBOX也有这个逻辑）
        if (jarFile.canWrite()) {
            jarFile.setReadOnly()
        }
        
        // 使用cache/jar作为优化目录（类似XMBOX的Path.jar()）
        val optimizedDir = File(context.cacheDir, "jar").also { it.mkdirs() }
        
        // 调试：列出JAR中的所有类
        listClassesInJar(jarFile)
        
        // 直接使用JAR文件路径，DexClassLoader会自动提取并优化DEX
        // 注意：parent classLoader必须使用context.classLoader（对应XMBOX的App.get().getClassLoader()）
        val classLoader = DexClassLoader(
            jarFile.absolutePath,
            optimizedDir.absolutePath,
            null,
            context.classLoader
        )
        
        Log.d(TAG, "ClassLoader创建成功，JAR路径: ${jarFile.absolutePath}")
        return classLoader
    }
    
    /**
     * 列出JAR文件中的所有类（用于调试）
     */
    private fun listClassesInJar(jarFile: File) {
        try {
            val zipFile = java.util.zip.ZipFile(jarFile)
            val entries = zipFile.entries()
            val allEntries = mutableListOf<String>()
            val classList = mutableListOf<String>()
            var hasDex = false
            
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name
                allEntries.add(name)
                
                // 检查是否有 DEX 文件
                if (name.endsWith(".dex")) {
                    hasDex = true
                }
                
                // 只列出 .class 文件（排除内部类、资源文件等）
                if (name.endsWith(".class") && !name.contains("$")) {
                    val className = name.replace('/', '.').removeSuffix(".class")
                    classList.add(className)
                }
            }
            
            zipFile.close()
            
            Log.d(TAG, "JAR 文件条目总数: ${allEntries.size}")
            Log.d(TAG, "包含 DEX 文件: $hasDex")
            Log.d(TAG, "JAR 中包含 ${classList.size} 个 .class 文件")
            
            if (hasDex) {
                Log.d(TAG, "检测到 DEX 文件，使用 DexFile 加载类列表")
                listClassesInDex(jarFile)
            } else if (classList.isNotEmpty()) {
                classList.take(20).forEach { className ->
                    Log.d(TAG, "  - $className")
                }
                if (classList.size > 20) {
                    Log.d(TAG, "  ... 还有 ${classList.size - 20} 个类")
                }
            } else {
                Log.w(TAG, "JAR 中没有找到 .class 或 .dex 文件")
                Log.d(TAG, "JAR 文件内容（前10个条目）:")
                allEntries.take(10).forEach { entry ->
                    Log.d(TAG, "  - $entry")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "列出JAR类失败: ${e.message}", e)
        }
    }
    
    /**
     * 从 DEX 文件中列出类
     */
    private fun listClassesInDex(jarFile: File) {
        try {
            val optimizedDir = File(context.cacheDir, "spider_dex_inspect").also { it.mkdirs() }
            val dexFile = dalvik.system.DexFile.loadDex(
                jarFile.absolutePath,
                File(optimizedDir, "inspect.dex").absolutePath,
                0
            )
            
            val classNames = mutableListOf<String>()
            val entries = dexFile.entries()
            while (entries.hasMoreElements()) {
                classNames.add(entries.nextElement())
            }
            
            dexFile.close()
            
            Log.d(TAG, "DEX 中包含 ${classNames.size} 个类:")
            classNames.take(30).forEach { className ->
                Log.d(TAG, "  - $className")
            }
            if (classNames.size > 30) {
                Log.d(TAG, "  ... 还有 ${classNames.size - 30} 个类")
            }
        } catch (e: Exception) {
            Log.e(TAG, "列出 DEX 类失败: ${e.message}", e)
        }
    }
    
    /**
     * 调用Spider的homeContent方法
     * 对应XMBOX的：spider.homeContent(filter)
     */
    fun callHomeContent(spider: Any, filter: Boolean = true): String? {
        return try {
            val method = spider.javaClass.getMethod("homeContent", Boolean::class.java)
            method.invoke(spider, filter) as? String
        } catch (e: Exception) {
            Log.e(TAG, "调用homeContent失败", e)
            null
        }
    }
    
    /**
     * 调用Spider的homeVideoContent方法
     * 对应XMBOX的：spider.homeVideoContent()
     */
    fun callHomeVideoContent(spider: Any): String? {
        return try {
            val method = spider.javaClass.getMethod("homeVideoContent")
            method.invoke(spider) as? String
        } catch (e: Exception) {
            Log.e(TAG, "调用homeVideoContent失败", e)
            null
        }
    }
    
    /**
     * 调用Spider的categoryContent方法
     */
    fun callCategoryContent(
        spider: Any,
        tid: String?,
        page: Int,
        filter: Boolean,
        extend: HashMap<String, String>?
    ): String? {
        return try {
            val method = spider.javaClass.getMethod(
                "categoryContent",
                String::class.java,
                Int::class.java,
                Boolean::class.java,
                HashMap::class.java
            )
            method.invoke(spider, tid, page, filter, extend) as? String
        } catch (e: Exception) {
            Log.e(TAG, "调用categoryContent失败", e)
            null
        }
    }
    
    /**
     * 调用Spider的detailContent方法
     */
    fun callDetailContent(spider: Any, ids: List<String>): String? {
        return try {
            val method = spider.javaClass.getMethod("detailContent", List::class.java)
            method.invoke(spider, ids) as? String
        } catch (e: Exception) {
            Log.e(TAG, "调用detailContent失败", e)
            null
        }
    }
    
    /**
     * 调用Spider的playerContent方法
     */
    fun callPlayerContent(
        spider: Any,
        flag: String?,
        id: String?,
        vipFlags: List<String>?
    ): String? {
        return try {
            val method = spider.javaClass.getMethod(
                "playerContent",
                String::class.java,
                String::class.java,
                List::class.java
            )
            method.invoke(spider, flag, id, vipFlags) as? String
        } catch (e: Exception) {
            Log.e(TAG, "调用playerContent失败", e)
            null
        }
    }
    
    /**
     * 调用Spider的searchContent方法
     */
    fun callSearchContent(spider: Any, keyword: String, quick: Boolean = false): String? {
        return try {
            val method = spider.javaClass.getMethod(
                "searchContent",
                String::class.java,
                Boolean::class.java
            )
            method.invoke(spider, keyword, quick) as? String
        } catch (e: Exception) {
            Log.e(TAG, "调用searchContent失败", e)
            null
        }
    }
    
    /**
     * 构建可能的类名列表
     * 
     * 按照XMBOX的实现方式：
     * - JarLoader.java 第131行：`"com.github.catvod.spider." + api.split("csp_")[1]`
     * - 对于 csp_DoubanGuard，类名是：com.github.catvod.spider.DoubanGuard
     * 
     * 但需要兼容其他可能格式
     */
    private fun buildPossibleClassNames(className: String): List<String> {
        val names = mutableListOf<String>()
        
        // 如果以 csp_ 开头，按照XMBOX方式处理
        if (className.startsWith("csp_")) {
            val baseClassName = className.removePrefix("csp_")
            
            // XMBOX标准格式（最高优先级）
            names.add("com.github.catvod.spider.$baseClassName")
            
            // 其他可能的格式（兼容性）
            names.add("com.github.catvod.$baseClassName")
            names.add(baseClassName)
            names.add(className) // 原始类名
            names.add("com.github.catvod.spider.impl.$baseClassName")
            names.add("com.catvod.spider.$baseClassName")
        } else {
            // 不以csp_开头，尝试所有格式
            names.add(className)
            names.add("com.github.catvod.spider.$className")
            names.add("com.github.catvod.$className")
            names.add("com.github.catvod.spider.impl.$className")
            names.add("com.catvod.spider.$className")
        }
        
        return names
    }
    
    /**
     * 清理所有加载的Spider
     */
    fun clear() {
        loadedSpiders.clear()
        classLoaders.clear()
        Log.d(TAG, "已清理所有Spider")
    }
    
    companion object {
        private const val TAG = "SpiderLoader"
    }
}

