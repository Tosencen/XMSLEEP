package org.xmsleep.app.quote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import org.xmsleep.app.R
import org.xmsleep.app.utils.Logger
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
     * 纵向布局，背景图填满整个图片，文字和二维码覆盖在上面
     */
    fun generateQuoteImage(
        context: Context,
        quote: Quote,
        isDarkTheme: Boolean = false
    ): Bitmap {
        Logger.d("ImageGenerator", "开始生成图片（覆盖式布局）")
        
        // 图片尺寸
        val width = 1080
        val height = 1500
        val padding = 60f
        
        // 创建 Bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 绘制背景图（CenterCrop 方式）
        try {
            val backgroundBitmap = android.graphics.BitmapFactory.decodeResource(
                context.resources,
                R.drawable.bg
            )
            if (backgroundBitmap != null) {
                val scaledBitmap = Bitmap.createScaledBitmap(backgroundBitmap, width, height, true)
                canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
                scaledBitmap.recycle()
                backgroundBitmap.recycle()
            } else {
                canvas.drawColor(android.graphics.Color.parseColor("#1C1B1F"))
            }
        } catch (e: Exception) {
            Logger.e("ImageGenerator", "绘制背景图失败", e)
            canvas.drawColor(android.graphics.Color.parseColor("#1C1B1F"))
        }
        
        // 创建 Paint 对象（白色文字，带阴影增加可读性）
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 42f
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(6f, 0f, 2f, android.graphics.Color.argb(200, 0, 0, 0))
        }
        
        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 68f
            textAlign = Paint.Align.LEFT
            setShadowLayer(8f, 0f, 3f, android.graphics.Color.argb(200, 0, 0, 0))
        }
        
        val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 36f
            textAlign = Paint.Align.RIGHT
            setShadowLayer(6f, 0f, 2f, android.graphics.Color.argb(200, 0, 0, 0))
        }
        
        // 开始绘制文字
        var y = padding + 120f
        
        // 绘制日期
        val dateText = LocalDate.now().format(
            DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE", Locale.CHINA)
        )
        canvas.drawText(dateText, padding, y, titlePaint)
        y += 60f
        
        // 绘制名句（多行）
        val quoteLines = wrapText(quote.text, quotePaint, width - padding * 2)
        for (line in quoteLines) {
            y += 95f
            canvas.drawText(line, padding, y, quotePaint)
        }
        y += 50f
        
        // 绘制作者
        canvas.drawText("— ${quote.author}", width - padding, y, authorPaint)
        
        // 绘制来源
        if (quote.from != null) {
            val authorText = "— ${quote.author}"
            val authorWidth = authorPaint.measureText(authorText)
            val fromText = "《${quote.from}》  "
            val fromPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 30f
                textAlign = Paint.Align.RIGHT
                setShadowLayer(6f, 0f, 2f, android.graphics.Color.argb(200, 0, 0, 0))
            }
            canvas.drawText(fromText, width - padding - authorWidth - 20f, y, fromPaint)
        }
        
        // 底部信息
        val qrSize = 150f
        val qrBottom = height - padding - 60f
        val qrRight = width - padding
        val qrTop = qrBottom - qrSize
        val qrLeft = qrRight - qrSize
        
        // 绘制二维码白色背景
        val qrWhiteBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
        }
        canvas.drawRect(qrLeft - 8f, qrTop - 8f, qrRight + 8f, qrBottom + 8f, qrWhiteBgPaint)
        
        // 绘制二维码
        try {
            val qrBitmap = android.graphics.BitmapFactory.decodeResource(
                context.resources,
                R.drawable.download_qr_code
            )
            if (qrBitmap != null) {
                val srcRect = android.graphics.Rect(0, 0, qrBitmap.width, qrBitmap.height)
                val dstRect = android.graphics.Rect(
                    qrLeft.toInt(), 
                    qrTop.toInt(), 
                    qrRight.toInt(), 
                    qrBottom.toInt()
                )
                canvas.drawBitmap(qrBitmap, srcRect, dstRect, null)
                qrBitmap.recycle()
            }
        } catch (e: Exception) {
            Logger.e("ImageGenerator", "绘制二维码失败", e)
        }
        
        // 绘制左侧应用信息
        val appNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 42f
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(6f, 0f, 2f, android.graphics.Color.argb(200, 0, 0, 0))
        }
        
        val appDescPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 34f
            textAlign = Paint.Align.LEFT
            setShadowLayer(6f, 0f, 2f, android.graphics.Color.argb(200, 0, 0, 0))
        }
        
        // 计算文本块垂直中心，与二维码垂直中心对齐
        val qrCenterY = qrTop + qrSize / 2f
        val fm1 = appNamePaint.fontMetrics
        val fm2 = appDescPaint.fontMetrics
        val textBlockCenter = fm1.ascent + 48f + fm2.descent
        val appNameY = qrCenterY - textBlockCenter / 2f
        canvas.drawText("XMSLEEP", padding, appNameY, appNamePaint)
        canvas.drawText("白噪音助眠应用", padding, appNameY + 48f, appDescPaint)
        
        Logger.d("ImageGenerator", "图片生成完成: ${bitmap.width}x${bitmap.height}")
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
