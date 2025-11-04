package org.streambox.datasource.xmbox.model

/**
 * 播放源信息
 * 从XMBOX的Flag和Episode移植
 */
data class PlaySource(
    val flag: String,              // 播放源名称（如"线路1"、"超清"）
    val episodes: List<Episode> = emptyList()  // 剧集列表
)

/**
 * 单集信息
 */
data class Episode(
    val name: String,      // 集数名称（如"第1集"）
    val url: String        // 播放地址
)

/**
 * 播放源解析工具
 * 从XMBOX的Vod.setVodFlags()移植
 */
object PlaySourceParser {
    
    /**
     * 解析播放源和播放列表
     * 格式：
     * - vod_play_from: "线路1$$$线路2$$$线路3"
     * - vod_play_url: "第1集$第2集$第3集$$$第1集$第2集$$$第1集$第2集$第3集$第4集"
     * 
     * 使用$$$分隔不同的播放源，使用$分隔同一源下的不同集
     */
    fun parsePlaySources(
        vodPlayFrom: String,
        vodPlayUrl: String
    ): List<PlaySource> {
        val sources = mutableListOf<PlaySource>()
        
        if (vodPlayFrom.isBlank() || vodPlayUrl.isBlank()) {
            return sources
        }
        
        val playFlags = vodPlayFrom.split("$$$")
        val playUrls = vodPlayUrl.split("$$$")
        
        for (i in playFlags.indices) {
            val flag = playFlags[i].trim()
            if (flag.isBlank()) continue
            
            if (i < playUrls.size) {
                val urlStr = playUrls[i].trim()
                val episodes = parseEpisodes(urlStr)
                sources.add(PlaySource(flag = flag, episodes = episodes))
            } else {
                sources.add(PlaySource(flag = flag))
            }
        }
        
        return sources
    }
    
    /**
     * 解析剧集列表
     * 格式："第1集$第2集$第3集" 或 "第1集#播放地址1$第2集#播放地址2"
     */
    private fun parseEpisodes(urlStr: String): List<Episode> {
        val episodes = mutableListOf<Episode>()
        
        if (urlStr.isBlank()) {
            return episodes
        }
        
        val episodeStrs = urlStr.split("$")
        for (episodeStr in episodeStrs) {
            val trimmed = episodeStr.trim()
            if (trimmed.isBlank()) continue
            
            // 支持两种格式：
            // 1. "第1集" -> name="第1集", url=""
            // 2. "第1集#播放地址" -> name="第1集", url="播放地址"
            if (trimmed.contains("#")) {
                val parts = trimmed.split("#", limit = 2)
                val name = parts[0].trim()
                val url = if (parts.size > 1) parts[1].trim() else ""
                episodes.add(Episode(name = name, url = url))
            } else {
                episodes.add(Episode(name = trimmed, url = ""))
            }
        }
        
        return episodes
    }
    
    /**
     * 从单个URL字符串解析剧集列表
     * 如果URL字符串中没有剧集分隔符，返回单个Episode
     */
    fun parseSingleUrl(name: String, url: String): List<Episode> {
        if (url.isBlank()) return emptyList()
        
        return if (url.contains("$")) {
            // 包含多个剧集
            parseEpisodes(url)
        } else {
            // 单个播放地址
            listOf(Episode(name = name.ifBlank { "播放" }, url = url))
        }
    }
}

