package org.streambox.datasource.xmbox.model

/**
 * Site数据类
 * 从XMBOX的Site.java移植
 * 表示一个视频站点（子源）
 */
data class Site(
    val key: String = "",
    val name: String = "",
    val api: String = "", // API URL或JavaScript代码或Spider类名（csp_开头）
    val ext: String = "", // 扩展配置URL
    val jar: String = "", // Spider JAR URL
    val type: Int = 0, // 0=XML, 1=JSON, 2=直播, 3=Spider, 4=Ext
    val hide: Int = 0, // 是否隐藏
    val indexs: Int = 0, // 是否索引
    val timeout: Int = 30 // 超时时间（秒）
) {
    /**
     * 判断是否为Spider类型
     */
    fun isSpider(): Boolean {
        return type == 3 || api.startsWith("csp_") || api.contains(".js") || jar.isNotBlank()
    }
    
    /**
     * 判断是否为Ext类型
     */
    fun isExt(): Boolean {
        return type == 4
    }
    
    /**
     * 判断是否为直接API类型（type 0, 1, 2）
     */
    fun isDirectApi(): Boolean {
        return type in 0..2
    }
    
    /**
     * 获取Spider类名（如果是csp_开头）
     */
    fun getSpiderClassName(): String? {
        return if (api.startsWith("csp_")) {
            api
        } else if (jar.isNotBlank()) {
            // 如果jar不为空，尝试从api提取类名
            api.takeIf { it.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")) && it.length < 100 }
        } else {
            null
        }
    }
    
    /**
     * 获取Spider JAR URL（优先使用site.jar，否则从主配置的spider字段）
     */
    fun getSpiderUrl(mainSpiderUrl: String? = null): String? {
        return if (jar.isNotBlank()) {
            jar
        } else {
            mainSpiderUrl
        }
    }
}

