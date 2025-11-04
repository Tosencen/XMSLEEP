# XMBOX 代码复用总结

## 已复制的关键代码

### 1. Spider JAR 加载机制
**位置**: `datasource/xmbox/src/main/kotlin/org/streambox/datasource/xmbox/spider/SpiderLoader.kt`

**关键点**:
- ✅ 直接使用JAR文件路径创建`DexClassLoader`（不需要提取DEX）
- ✅ 使用`cache/jar`作为优化目录（类似XMBOX的`Path.jar()`）
- ✅ 确保JAR文件只读（Android 10+要求）
- ✅ 类名格式：`com.github.catvod.spider.{ClassName}`（从`csp_ClassName`转换）

**参考**: XMBOX的`JarLoader.java`第60-61行和第131行

### 2. JSON解析（Result格式）
**位置**: `datasource/xmbox/src/main/kotlin/org/streambox/datasource/xmbox/XmboxVideoSource.kt`

**关键点**:
- ✅ 支持XMBOX的Result格式：`{ code: 1, list: [...], msg: "" }`
- ✅ 支持多种列表字段名：`list`, `data`, `videos`, `items`
- ✅ 支持所有XMBOX的Vod字段：`vod_id`, `vod_name`, `vod_pic`, `vod_remarks`等
- ✅ 兼容多种字段名变体（`name`/`title`, `pic`/`image`/`cover`等）

**参考**: XMBOX的`Result.java`第100-103行，`Vod.java`的字段定义

### 3. 视频字段映射
**已支持的字段**:
- `vod_id` / `id` → `VideoItem.id`
- `vod_name` / `name` / `title` → `VideoItem.name`
- `vod_pic` / `pic` / `image` / `cover` → `VideoItem.pic`
- `vod_remarks` / `note` / `remarks` / `subtitle` → `VideoItem.note`
- `vod_year` / `year` → `VideoItem.year`
- `vod_area` / `area` → `VideoItem.area`
- `vod_actor` / `actor` → `VideoItem.actor`
- `vod_director` / `director` → `VideoItem.director`
- `vod_content` / `des` / `content` / `description` → `VideoItem.des`
- `vod_time` / `last` / `update_time` → `VideoItem.last`

## 可以进一步复制的代码

### 1. 图片URL处理（ImgUtil）
**位置**: `app/src/main/java/com/fongmi/android/tv/utils/ImgUtil.java`

**功能**:
- 处理URL中的特殊参数：`@Headers=`, `@Cookie=`, `@Referer=`, `@User-Agent=`
- URL转换（assets://, file://, proxy://）
- 使用Glide加载图片，支持签名和缓存控制

**建议**: 如果需要在图片加载时添加Headers，可以复制`ImgUtil.getUrl()`方法

### 2. Result类的trans()方法
**位置**: `app/src/main/java/com/fongmi/android/tv/bean/Result.java`第306行

**功能**:
- 对视频列表进行转换（包括类型名称翻译等）

**建议**: 如果需要在显示前转换视频数据，可以参考此方法

### 3. VideoItem的setVodFlags()方法
**位置**: `app/src/main/java/com/fongmi/android/tv/bean/Vod.java`第277行

**功能**:
- 解析播放源和播放列表（`vod_play_from`和`vod_play_url`）

**建议**: 如果需要支持多播放源，可以参考此方法

## 当前状态

✅ **已完成**:
1. Spider JAR加载机制（按照XMBOX方式）
2. JSON解析（支持XMBOX格式）
3. 视频字段映射（完整支持）

⏳ **待优化**:
1. Spider加载失败问题（日志显示类已找到但加载失败，可能需要在运行时确认）
2. 图片URL处理（如果需要支持特殊Headers）
3. 视频播放源解析（如果需要多播放源支持）

## 测试建议

1. 清理所有缓存后重新测试Spider加载
2. 检查日志中的类名格式是否正确
3. 验证JSON解析是否能正确识别所有视频字段

