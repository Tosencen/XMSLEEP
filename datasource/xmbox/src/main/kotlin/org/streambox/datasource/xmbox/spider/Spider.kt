package org.streambox.datasource.xmbox.spider

/**
 * CatVod Spider 接口
 * 
 * 基于 CatVodSpider 的标准接口定义
 * 参考：https://github.com/CatVodTVOfficial/CatVodTVSpider
 */
interface Spider {
    
    /**
     * 初始化
     * @param extend 扩展参数（来自配置的ext字段）
     */
    fun init(context: android.content.Context, extend: String?)
    
    /**
     * 获取首页数据
     * @param filter 是否过滤
     * @return JSON字符串，格式: {"class": [...], "list": [...]}
     */
    fun homeContent(filter: Boolean): String?
    
    /**
     * 获取分类数据
     * @param tid 分类ID
     * @param pg 页码
     * @param filter 是否过滤
     * @param extend 扩展参数
     * @return JSON字符串，格式: {"list": [...], "page": ..., "pagecount": ...}
     */
    fun categoryContent(tid: String?, pg: Int, filter: Boolean, extend: HashMap<String, String>?): String?
    
    /**
     * 获取详情数据
     * @param ids 视频ID列表
     * @return JSON字符串，格式: {"list": [{...}]}
     */
    fun detailContent(ids: List<String>?): String?
    
    /**
     * 获取播放地址
     * @param flag 播放源标识
     * @param id 视频ID
     * @param vipFlags 会员标识列表
     * @return JSON字符串，格式: {"parse": 0/1, "url": "..."}
     */
    fun playerContent(flag: String?, id: String?, vipFlags: List<String>?): String?
    
    /**
     * 搜索
     * @param key 搜索关键词
     * @param quick 是否快速搜索
     * @return JSON字符串，格式: {"list": [...]}
     */
    fun searchContent(key: String?, quick: Boolean): String?
}

/**
 * Spider 结果封装
 */
data class SpiderResult(
    val code: Int = 1,
    val msg: String = "",
    val data: Any? = null
)

