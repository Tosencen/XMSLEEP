package org.xmsleep.app.ui.flipclock

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.xmsleep.app.R

/**
 * 翻页时钟字体枚举
 */
enum class ClockFont(
    val displayName: String,
    val fontFamily: FontFamily,
    val description: String,
    val verticalOffset: Int = 0, // 垂直偏移量（dp）
    val fontSize: Int = 240 // 主数字字号（sp）
) {
    BEBAS_NEUE(
        displayName = "Bebas Neue",
        fontFamily = FontFamily(Font(R.font.bebas_neue, FontWeight.Normal)),
        description = "机场航班显示屏风格，极粗",
        verticalOffset = 12, // 向下偏移12dp
        fontSize = 260 // 字号放大40sp
    ),
    OSWALD(
        displayName = "Oswald",
        fontFamily = FontFamily(Font(R.font.oswald_bold, FontWeight.Bold)),
        description = "工业复古风",
        verticalOffset = -6, // 向上偏移6dp
        fontSize = 230 // 字号稍小
    );

    companion object {
        fun fromOrdinal(ordinal: Int): ClockFont {
            return entries.getOrElse(ordinal) { OSWALD }
        }
    }
}
