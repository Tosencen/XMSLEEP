package org.xmsleep.app.weather

/**
 * 和风天气 condition/icon 代码 → WMO 天气代码（与本 App 的 WeatherCodeMapper 对齐）。
 * 参考：https://dev.qweather.com/docs/resource/weather-condition/
 */
object QWeatherMapper {
    fun iconToWmo(icon: String): Int {
        val c = icon.toIntOrNull() ?: return 0
        return when (c) {
            // 晴
            100, 150 -> 0
            // 少云 / 晴间多云
            102, 103, 152, 153 -> 1
            // 多云
            101, 151 -> 2
            // 阴
            104 -> 3
            // 雷阵雨
            302, 303, 372, 373 -> 95
            // 雷阵雨伴冰雹
            304 -> 96
            // 毛毛雨
            309 -> 51
            // 小雨
            305, 399 -> 61
            // 中雨
            306, 314 -> 63
            // 大雨 / 暴雨 / 极端降雨
            307, 308, 310, 311, 312, 315, 316, 317, 318 -> 65
            // 冻雨
            313 -> 66
            // 小雪
            400, 499 -> 71
            // 中雪
            401, 408 -> 73
            // 大雪 / 暴雪
            402, 403, 407, 409, 410 -> 75
            // 雨夹雪 / 阵雪（雨雪）
            404, 405, 406, 456, 457, 903, 905 -> 85
            // 强阵雪
            904 -> 86
            // 雾 / 霾 / 沙尘
            500, 501, 502, 503, 504, 507, 508, 509, 510, 511, 512, 513 -> 45
            // 阵雨
            900, 902, 906, 907 -> 80
            // 强阵雨
            901 -> 82
            else -> 0
        }
    }

    /** 和风夜间图标以 15x 表示（如 150 晴夜），据此判断昼夜 */
    fun isDayFromIcon(icon: String): Boolean {
        val c = icon.toIntOrNull() ?: return true
        return c !in 150..199
    }
}
