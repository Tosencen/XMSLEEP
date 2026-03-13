package org.xmsleep.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toHct
import org.xmsleep.app.R
import org.xmsleep.app.theme.DarkModeOption
import org.xmsleep.app.utils.Logger

/**
 * 开关设置项
 */
@Composable
fun SwitchItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

/**
 * 颜色按钮（新版）
 */
@Composable
fun ColorButton(
    baseColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerSize by animateDpAsState(targetValue = if (selected) 28.dp else 0.dp, label = "")
    val iconSize by animateDpAsState(targetValue = if (selected) 16.dp else 0.dp, label = "")
    
    Surface(
        modifier = modifier
            .sizeIn(maxHeight = 80.dp, maxWidth = 80.dp, minHeight = 64.dp, minWidth = 64.dp)
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxSize()) {
            val hct = baseColor.toHct()
            val color1 = Color(Hct.from(hct.hue, 40.0, 80.0).toInt())
            val color2 = Color(Hct.from(hct.hue, 40.0, 90.0).toInt())
            val color3 = Color(Hct.from(hct.hue, 40.0, 60.0).toInt())
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .drawBehind { drawCircle(color1) }
                    .align(Alignment.Center),
            ) {
                Surface(
                    color = color2,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(24.dp),
                ) {}
                Surface(
                    color = color3,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp),
                ) {}
                if (selected) {
                    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .size(containerSize)
                            .drawBehind { drawCircle(primaryContainer) },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize).align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 主题模式卡片
 */
@Composable
fun ThemeModeCard(
    option: DarkModeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (selected) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(0.7f)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (option) {
                        DarkModeOption.LIGHT -> Icons.Default.LightMode
                        DarkModeOption.DARK -> Icons.Default.DarkMode
                        DarkModeOption.AUTO -> Icons.Default.Brightness4
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Text(
                when (option) {
                    DarkModeOption.LIGHT -> context.getString(R.string.light_mode)
                    DarkModeOption.DARK -> context.getString(R.string.dark_mode)
                    DarkModeOption.AUTO -> context.getString(R.string.auto_mode)
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * 增强型颜色按钮
 */
@Composable
fun EnhancedColorButton(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val containerSize by animateDpAsState(targetValue = if (selected) 24.dp else 0.dp, label = "")
    val iconSize by animateDpAsState(targetValue = if (selected) 16.dp else 0.dp, label = "")
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .drawBehind { drawCircle(color) },
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Surface(
                        modifier = Modifier.size(containerSize),
                        shape = CircleShape,
                        color = primaryContainer
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = context.getString(R.string.selected),
                                tint = onPrimaryContainer,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 颜色选项（简单版）
 */
@Composable
fun ColorOption(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .drawBehind { drawCircle(color) },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = context.getString(R.string.selected),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 动画 WebP 图片组件
 * 
 * 显示 WebP 动画并应用主题色适配
 * 
 * @param drawableResId WebP 资源 ID
 * @param modifier 修饰符
 * @param contentScale 内容缩放方式
 * @param isPlaying 是否播放动画
 */
@Composable
fun AnimatedWebPImage(
    drawableResId: Int,
    modifier: Modifier = Modifier,
    contentScale: androidx.compose.ui.layout.ContentScale = androidx.compose.ui.layout.ContentScale.Fit,
    isPlaying: Boolean = false,
    applyThemeColor: Boolean = true // 新增参数：是否应用主题色滤镜
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    // 使用用户选择的主题色（primary），所有动画都跟随这个颜色
    val themeColor = colorScheme.primary
    
    // 检测当前是否为深色模式
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    // 基于主题色生成深色和浅色版本（同色系）
    val themeHct = remember(themeColor) { themeColor.toHct() }
    
    // 降低整体亮度，让背景更暗，同时增加色块对比度
    // - 浅色模式：使用深色调（tone 8-40），对比度更大
    // - 深色模式：使用更暗的色调（tone 5-30），对比度更大
    val darkColor = remember(themeHct, isDarkTheme) {
        if (isDarkTheme) {
            // 深色模式：使用非常暗的颜色作为"深色"（tone 5）
            Color(Hct.from(themeHct.hue, themeHct.chroma, 5.0).toInt())
        } else {
            // 浅色模式：使用很暗的颜色作为"深色"（tone 8）
            Color(Hct.from(themeHct.hue, themeHct.chroma, 8.0).toInt())
        }
    }
    val lightColor = remember(themeHct, isDarkTheme) {
        if (isDarkTheme) {
            // 深色模式：使用中等偏暗的颜色作为"浅色"（tone 30），增加对比度
            Color(Hct.from(themeHct.hue, themeHct.chroma, 30.0).toInt())
        } else {
            // 浅色模式：使用中等偏暗的颜色作为"浅色"（tone 40），增加对比度
            Color(Hct.from(themeHct.hue, themeHct.chroma, 40.0).toInt())
        }
    }
    
    // 记住drawable引用（用于状态追踪）
    var animatedDrawable by remember { mutableStateOf<android.graphics.drawable.AnimatedImageDrawable?>(null) }
    
    // 创建ColorMatrix来应用双色效果（基于亮度映射到深色和浅色）
    // 只有在 applyThemeColor 为 true 时才创建颜色滤镜
    val colorMatrix = remember(darkColor, lightColor, applyThemeColor) {
        if (!applyThemeColor) {
            null // 不应用颜色滤镜
        } else {
            val darkArgb = darkColor.toArgb()
            val lightArgb = lightColor.toArgb()
            
            // 提取RGB值（0-255范围）
            val darkR = ((darkArgb shr 16) and 0xFF) / 255f
            val darkG = ((darkArgb shr 8) and 0xFF) / 255f
            val darkB = (darkArgb and 0xFF) / 255f
            
            val lightR = ((lightArgb shr 16) and 0xFF) / 255f
            val lightG = ((lightArgb shr 8) and 0xFF) / 255f
            val lightB = (lightArgb and 0xFF) / 255f
            
            // 计算RGB差值
            val deltaR = lightR - darkR
            val deltaG = lightG - darkG
            val deltaB = lightB - darkB
            
            // 创建ColorMatrix：将原图的亮度映射到深色和浅色之间
            // 公式：output = dark + (light - dark) * brightness
            // 其中brightness是原图的亮度（0-1）
            android.graphics.ColorMatrix().apply {
                val matrix = FloatArray(20)
                
                // 红色通道：R' = darkR + deltaR * brightness
                // brightness = (R*0.299 + G*0.587 + B*0.114) / 255
                // 简化：R' = darkR + deltaR * (R*0.299 + G*0.587 + B*0.114)
                matrix[0] = deltaR * 0.299f  // R对R的贡献
                matrix[1] = deltaR * 0.587f  // G对R的贡献
                matrix[2] = deltaR * 0.114f  // B对R的贡献
                matrix[3] = 0f
                matrix[4] = darkR * 255f     // 偏移量
                
                // 绿色通道
                matrix[5] = deltaG * 0.299f
                matrix[6] = deltaG * 0.587f
                matrix[7] = deltaG * 0.114f
                matrix[8] = 0f
                matrix[9] = darkG * 255f
                
                // 蓝色通道
                matrix[10] = deltaB * 0.299f
                matrix[11] = deltaB * 0.587f
                matrix[12] = deltaB * 0.114f
                matrix[13] = 0f
                matrix[14] = darkB * 255f
                
                // Alpha通道保持不变
                matrix[15] = 0f
                matrix[16] = 0f
                matrix[17] = 0f
                matrix[18] = 1f
                matrix[19] = 0f
                
                set(matrix)
            }
        }
    }
    
    // 使用AndroidView来显示动画WebP
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            android.widget.ImageView(ctx).apply {
                scaleType = when (contentScale) {
                    androidx.compose.ui.layout.ContentScale.Fit -> android.widget.ImageView.ScaleType.FIT_CENTER
                    androidx.compose.ui.layout.ContentScale.Crop -> android.widget.ImageView.ScaleType.CENTER_CROP
                    androidx.compose.ui.layout.ContentScale.FillBounds -> android.widget.ImageView.ScaleType.FIT_XY
                    androidx.compose.ui.layout.ContentScale.FillWidth -> android.widget.ImageView.ScaleType.FIT_CENTER
                    androidx.compose.ui.layout.ContentScale.FillHeight -> android.widget.ImageView.ScaleType.FIT_CENTER
                    androidx.compose.ui.layout.ContentScale.Inside -> android.widget.ImageView.ScaleType.CENTER_INSIDE
                    else -> android.widget.ImageView.ScaleType.FIT_CENTER
                }
            }
        },
        update = { view ->
            // 每次 drawableResId 或 colorMatrix 改变时重新加载图片
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                // API 28+ 使用ImageDecoder加载动画WebP
                val source = android.graphics.ImageDecoder.createSource(context.resources, drawableResId)
                val decodedDrawable = android.graphics.ImageDecoder.decodeDrawable(source)
                val animatedDrawableInstance = decodedDrawable as? android.graphics.drawable.AnimatedImageDrawable
                animatedDrawable = animatedDrawableInstance
                
                // 应用ColorMatrix滤镜来实现双色效果（如果启用）
                if (animatedDrawableInstance != null) {
                    animatedDrawableInstance.colorFilter = if (colorMatrix != null) {
                        android.graphics.ColorMatrixColorFilter(colorMatrix)
                    } else {
                        null // 不应用滤镜，保持原始颜色
                    }
                    // 如果需要播放，启动动画
                    if (isPlaying && !animatedDrawableInstance.isRunning) {
                        animatedDrawableInstance.repeatCount = android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE
                        animatedDrawableInstance.start()
                    } else if (!isPlaying && animatedDrawableInstance.isRunning) {
                        animatedDrawableInstance.stop()
                    }
                } else {
                    // 如果是静态图片，也应用滤镜（如果启用）
                    decodedDrawable.colorFilter = if (colorMatrix != null) {
                        android.graphics.ColorMatrixColorFilter(colorMatrix)
                    } else {
                        null
                    }
                }
                view.setImageDrawable(decodedDrawable)
            } else {
                // API 28以下使用传统方式加载（不支持动画）
                view.setImageResource(drawableResId)
                // 应用ColorMatrix滤镜（如果启用）
                view.colorFilter = if (colorMatrix != null) {
                    android.graphics.ColorMatrixColorFilter(colorMatrix)
                } else {
                    null
                }
            }
        },
        modifier = modifier
    )
}


/**
 * 可拉伸的拉环组件
 * 
 * 用于控制应用内容的显示/隐藏
 * 
 * @param isContentHidden 当前内容是否隐藏
 * @param onToggle 切换显示/隐藏的回调
 * @param animationProgress 动画进度（0f-1f），用于掉落动画
 * @param modifier Modifier
 */
@Composable
fun PullRingControl(
    isContentHidden: Boolean,
    onToggle: () -> Unit,
    animationProgress: Float = 1f,
    modifier: Modifier = Modifier
) {
    var dragOffset by remember { mutableStateOf(0f) }
    val threshold = 100f // 拖拽阈值（像素）
    
    // 默认长度为40dp，最长可拉伸到120dp
    val defaultStrapHeight = 40.dp
    val maxStrapHeight = 120.dp
    
    // 圆环尺寸
    val ringDiameter = 30.dp
    val ringStrokeWidth = 5.dp
    
    // 计算实际高度（考虑动画进度）
    val targetHeight = ((defaultStrapHeight + dragOffset.dp).coerceIn(defaultStrapHeight, maxStrapHeight)) * animationProgress
    
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "strap_height"
    )
    
    // 呼吸动画：透明度在0.6到1.0之间循环
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_alpha"
    )
    
    val strapColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    
    // 计算总高度：拉条高度 + 圆环直径 + 描边宽度（确保圆环不被裁剪）
    val totalHeight = animatedHeight + ringDiameter + ringStrokeWidth
    
    Box(
        modifier = modifier
            .width(40.dp)
            .height(totalHeight) // 明确指定高度，确保圆环不被裁剪
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        Logger.d("PullRingControl", "拖拽结束，偏移量: $dragOffset")
                        if (kotlin.math.abs(dragOffset) > threshold) {
                            Logger.d("PullRingControl", "超过阈值，触发切换")
                            onToggle()
                        }
                        dragOffset = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragOffset += dragAmount
                        Logger.d("PullRingControl", "拖拽中，当前偏移: $dragOffset")
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // 使用Canvas绘制拉环条和圆环，应用呼吸动画到颜色透明度
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val strapWidth = with(density) { 8.dp.toPx() }
            val strapHeightPx = with(density) { animatedHeight.toPx() }
            val ringRadius = with(density) { (ringDiameter / 2).toPx() }
            val ringStrokeWidthPx = with(density) { ringStrokeWidth.toPx() }
            
            val centerX = size.width / 2
            
            // 将呼吸动画应用到颜色的透明度上，而不是整个 Canvas
            val animatedColor = strapColor.copy(alpha = breathingAlpha)
            
            // 绘制拉伸条（从顶部到圆环顶部位置）
            drawRect(
                color = animatedColor,
                topLeft = Offset(centerX - strapWidth / 2, 0f),
                size = Size(strapWidth, strapHeightPx)
            )
            
            // 绘制圆环（直径30dp，位置在拉伸条底部）
            drawCircle(
                color = animatedColor,
                radius = ringRadius,
                center = Offset(centerX, strapHeightPx + ringRadius),
                style = Stroke(width = ringStrokeWidthPx)
            )
        }
    }
}
