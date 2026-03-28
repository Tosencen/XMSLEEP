package org.xmsleep.app.ui.flipclock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 翻页时钟数字卡片 - 简单单卡片设计
 */
@Composable
fun FlipCard(
    value: Int,
    fontFamily: FontFamily = FontFamily.Monospace,
    verticalOffset: Int = 0,
    fontSize: Int = 240,
    modifier: Modifier = Modifier
) {
    val displayValue = value.toString().padStart(2, '0')

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayValue,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.offset(y = verticalOffset.dp),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 带秒数显示的翻页卡片（用于分钟位置）- 简单单卡片设计
 */
@Composable
fun FlipCardWithSeconds(
    value: Int,
    seconds: Int,
    fontFamily: FontFamily = FontFamily.Monospace,
    verticalOffset: Int = 0,
    fontSize: Int = 240,
    modifier: Modifier = Modifier
) {
    val displayValue = value.toString().padStart(2, '0')

    Box(
        modifier = modifier
    ) {
        // 主数字卡片
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayValue,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.offset(y = verticalOffset.dp),
                textAlign = TextAlign.Center
            )
        }

        // 秒数（右下角）
        Text(
            text = seconds.toString().padStart(2, '0'),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp, end = 16.dp)
        )
    }
}
