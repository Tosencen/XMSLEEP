package org.xmsleep.app.quote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import org.xmsleep.app.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 图片生成器
 * 使用 Canvas API 直接绘制，避免 Compose 的 windowRecomposer 问题
 */
object ImageGenerator {
    
    /**
     * 生成名句分享图片
     * 使用原生 Canvas 绘制，不依赖 Compose
     */
    fun generateQuoteImage(
        context: Context,
        quote: Quote,
        isDarkTheme: Boolean = false
    ): Bitmap {
        android.util.Log.d("ImageGenerator", "开始生成图片，isDarkTheme=$isDarkTheme")
        
        // 图片尺寸
        val width = 1080
        val padding = 80f
        
        // 颜色方案
        val backgroundColor = if (isDarkTheme) Color(0xFF1C1B1F) else Color(0xFFFFFBFE)
        val surfaceColor = if (isDarkTheme) Color(0xFF1C1B1F) else Color(0xFFFFFBFE)
        val primaryColor = if (isDarkTheme) Color(0xFFD0BCFF) else Color(0xFF6750A4)
        val onSurfaceColor = if (isDarkTheme) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)
        val onSurfaceVariantColor = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF49454F)
        val surfaceVariantColor = if (isDarkTheme) Color(0xFF49454F) else Color(0xFFE7E0EC)
        
        // 创建 Paint 对象
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onSurfaceColor.toArgb()
            textAlign = Paint.Align.LEFT
        }
        
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor.toArgb()
            textSize = 48f
            textAlign = Paint.Align.LEFT
        }
        
        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onSurfaceColor.toArgb()
            textSize = 90f
            textAlign = Paint.Align.LEFT
        }
        
        val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onSurfaceVariantColor.toArgb()
            textSize = 42f
            textAlign = Paint.Align.RIGHT
        }
        
        val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onSurfaceVariantColor.toArgb()
            textSize = 36f
            textAlign = Paint.Align.RIGHT
        }
        
        val appNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onSurfaceColor.toArgb()
            textSize = 52f
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        val appDescPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onSurfaceVariantColor.toArgb()
            textSize = 38f
            textAlign = Paint.Align.LEFT
        }
        
        // 计算所需高度
        var currentY = padding
        
        // 日期高度
        currentY += 60f
        currentY += 48f // spacing
        
        // 名句高度（多行文本）
        val quoteLines = wrapText(quote.text, quotePaint, width - padding * 2)
        currentY += quoteLines.size * 120f
        currentY += 32f // spacing
        
        // 作者高度（作者和来源在同一行，不需要额外高度）
        currentY += 50f
        
        currentY += 80f // spacing
        
        // 底部区域高度（应用名称 + 二维码）
        currentY += 200f
        
        currentY += padding
        
        val height = currentY.toInt()
        
        android.util.Log.d("ImageGenerator", "计算高度: $height")
        
        // 创建 Bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 绘制背景
        canvas.drawColor(backgroundColor.toArgb())
        
        // 绘制卡片背景
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = surfaceColor.toArgb()
        }
        val cardRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint)
        
        // 开始绘制内容
        var y = padding + 60f
        
        // 绘制日期（左对齐）
        val dateText = LocalDate.now().format(
            DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE", Locale.CHINA)
        )
        canvas.drawText(dateText, padding, y, titlePaint)
        y += 48f
        
        // 绘制名句（多行，左对齐）
        for (line in quoteLines) {
            y += 120f
            canvas.drawText(line, padding, y, quotePaint)
        }
        y += 32f
        
        // 绘制作者（右对齐）
        y += 50f
        val authorY = y
        canvas.drawText("— ${quote.author}", width - padding, authorY, authorPaint)
        
        // 绘制来源（右对齐，同一水平线）
        if (quote.from != null) {
            // 计算作者文字的宽度，让书名显示在作者左边
            val authorText = "— ${quote.author}"
            val authorWidth = authorPaint.measureText(authorText)
            val fromText = "《${quote.from}》  "
            val fromX = width - padding - authorWidth - 24f // 24f 是间距
            
            // 使用左对齐绘制书名
            val fromPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = onSurfaceVariantColor.toArgb()
                textSize = 36f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(fromText, fromX, authorY, fromPaint)
        }
        
        y += 80f
        
        // 底部区域：左边文字，右边二维码
        val qrSize = 200f
        val qrRight = width - padding
        val qrLeft = qrRight - qrSize
        val bottomY = y + 200f // 底部基线
        
        // 绘制二维码（右侧，底部对齐）
        val qrTop = bottomY - qrSize
        val qrWhiteBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
        }
        val qrWhiteBgRect = RectF(qrLeft, qrTop, qrRight, bottomY)
        canvas.drawRoundRect(qrWhiteBgRect, 16f, 16f, qrWhiteBgPaint)
        
        // 绘制二维码图片
        try {
            val qrBitmap = android.graphics.BitmapFactory.decodeResource(
                context.resources,
                R.drawable.download_qr_code
            )
            val qrRect = Rect(
                (qrLeft + 12f).toInt(),
                (qrTop + 12f).toInt(),
                (qrRight - 12f).toInt(),
                (bottomY - 12f).toInt()
            )
            canvas.drawBitmap(qrBitmap, null, qrRect, null)
        } catch (e: Exception) {
            android.util.Log.e("ImageGenerator", "绘制二维码失败", e)
        }
        
        // 绘制左侧文字（底部对齐）
        // 先绘制下面的"白噪音助眠应用"
        val descY = bottomY
        canvas.drawText("白噪音助眠应用", padding, descY, appDescPaint)
        
        // 再绘制上面的"XMSLEEP"
        val appNameY = descY - 60f // 向上偏移
        canvas.drawText("XMSLEEP", padding, appNameY, appNamePaint)
        
        android.util.Log.d("ImageGenerator", "图片生成完成: ${bitmap.width}x${bitmap.height}")
        return bitmap
    }
    
    /**
     * 文本换行处理
     */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        var currentLine = ""
        
        for (char in text) {
            val testLine = currentLine + char
            val width = paint.measureText(testLine)
            
            if (width > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = char.toString()
            } else {
                currentLine = testLine
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        
        return lines
    }
}
