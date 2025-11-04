# XMBOX URL识别机制总结

## 核心机制

XMBOX通过以下方式识别和处理各种URL：

### 1. URL识别流程

```
用户输入URL
    ↓
UrlUtil.convert() - URL转换（处理assets://, file://, proxy://等）
    ↓
Decoder.getJson() - 下载内容
    ↓
Decoder.verify() - 自动识别编码类型：
    - JSON格式 → 直接使用
    - Base64编码（包含"**"）→ 解码
    - AES/CBC加密（以"2423"开头）→ 解密
    ↓
Decoder.fix() - 修复相对路径和JavaScript引用
    ↓
Json.parse() - 解析为JSON对象
    ↓
VodConfig.parseConfig() - 解析配置并创建Site列表
```

### 2. 支持的URL类型

- **直接JSON URL**: `https://example.com/api.json`
- **Base64编码URL**: 内容包含`**`标记
- **AES/CBC加密URL**: 以`2423`开头的加密数据
- **BMP隐藏数据**: 图片中包含隐藏配置（已在项目中实现）
- **JavaScript配置**: 需要执行JS代码获取配置
- **HTML页面**: 包含配置链接的网页（已在项目中实现）

### 3. 特殊URL scheme处理

- `assets://` → 转换为本地assets路径
- `file://` → 转换为本地文件路径
- `proxy://` → 通过本地代理服务器访问

### 4. 依赖项

**必需依赖**（已在项目中）:
- ✅ `io.ktor:ktor-client-*` - HTTP客户端
- ✅ `kotlinx-serialization-json` - JSON解析
- ✅ `android.util.Base64` - Base64解码（Android内置）
- ✅ `javax.crypto.*` - AES加密解密（Java内置）
- ✅ `org.jsoup:jsoup` - HTML解析（已添加）

**可选依赖**（XMBOX使用但项目未使用）:
- `com.squareup.okhttp3:okhttp` - OkHttp（XMBOX使用，项目使用Ktor）
- `com.google.code.gson:gson` - Gson（XMBOX使用，项目使用kotlinx-serialization）

## 已实现的工具类

### ✅ UrlDecoder.kt
- Base64解码
- AES/CBC解密
- 相对路径修复
- JavaScript引用修复

### ✅ ImageUrlUtil.kt  
- URL中的Headers解析（@Headers=, @Cookie=, @Referer=等）
- URL scheme转换

### ✅ PlaySourceParser.kt
- 播放源解析（vod_play_from和vod_play_url）
- 剧集列表解析

## 使用方式

### 在XmboxVideoSource中使用UrlDecoder:

```kotlin
// 在identify方法中
val jsonString = UrlDecoder.getJson(configUrl, httpClient)
val json = Json.parseToJsonElement(jsonString)
// 然后解析配置...
```

## 注意事项

1. **httpClient传递**: UrlDecoder需要httpClient参数，从XmboxVideoSource的httpClient传入
2. **异常处理**: 解码可能失败，需要捕获异常并fallback到其他方式
3. **编码识别**: 自动识别编码类型，按顺序尝试：JSON → Base64 → AES/CBC

