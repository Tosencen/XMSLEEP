package org.xmsleep.app.ui.settings

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toHct
import org.xmsleep.app.R
import org.xmsleep.app.theme.DarkModeOption
import org.xmsleep.app.ui.icons.Tonality

/**
 * 主题设置屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    darkMode: DarkModeOption,
    selectedColor: Color,
    paletteColors: List<Color>,
    useDynamicColor: Boolean,
    useBlackBackground: Boolean,
    useMonochrome: Boolean,
    onDarkModeChange: (DarkModeOption) -> Unit,
    onColorChange: (Color) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onBlackBackgroundChange: (Boolean) -> Unit,
    onMonochromeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onScrollDetected: () -> Unit = {} // 滚动检测回调
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // 监听滚动事件
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            onScrollDetected()
        }
    }
    
    // 固定 TopAppBar，不随滚动隐藏
    Scaffold(
        containerColor = Color.Transparent, // 透明背景，显示背景动画
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        context.getString(R.string.theme_and_colors),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    // 返回导航按钮
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.offset(x = (-4).dp)
                    ) {
                        Box(Modifier.size(24.dp)) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = context.getString(R.string.go_back),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // 透明背景
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                // TopAppBar 使用系统栏和显示区域切口
                windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 消费 TopAppBar 使用的 insets
                .consumeWindowInsets(
                    WindowInsets.systemBars.union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Top)
                )
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 主题设置卡片
            SettingsCategory(
                items = buildList {
                    // 1. 启用动态主题 (Android 12+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        add(
                            SettingsCategoryItem(
                                icon = Icons.Filled.Palette,
                                title = { Text(context.getString(R.string.dynamic_color)) },
                                description = { 
                                    Text(
                                        context.getString(R.string.use_wallpaper_color_as_theme),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingContent = {
                                    Switch(
                                        checked = useDynamicColor,
                                        onCheckedChange = onDynamicColorChange
                                    )
                                },
                                onClick = { onDynamicColorChange(!useDynamicColor) }
                            )
                        )
                    }
                    
                    // 2. 深色主题
                    add(
                        SettingsCategoryItem(
                            icon = Icons.Filled.DarkMode,
                            title = { Text(context.getString(R.string.dark_mode)) },
                            description = { 
                                Text(
                                    when (darkMode) {
                                        DarkModeOption.LIGHT -> context.getString(R.string.light_mode)
                                        DarkModeOption.DARK -> context.getString(R.string.dark_mode)
                                        DarkModeOption.AUTO -> context.getString(R.string.auto_mode)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                // 循环切换深色模式
                                val nextMode = when (darkMode) {
                                    DarkModeOption.LIGHT -> DarkModeOption.DARK
                                    DarkModeOption.DARK -> DarkModeOption.AUTO
                                    DarkModeOption.AUTO -> DarkModeOption.LIGHT
                                }
                                onDarkModeChange(nextMode)
                            }
                        )
                    )
                    
                    // 3. 黑白模式
                    add(
                        SettingsCategoryItem(
                            icon = Tonality,
                            title = { Text(context.getString(R.string.monochrome_mode)) },
                            description = { 
                                Text(
                                    context.getString(R.string.monochrome_mode_description),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = useMonochrome,
                                    onCheckedChange = onMonochromeChange
                                )
                            },
                            onClick = { onMonochromeChange(!useMonochrome) }
                        )
                    )
                    
                    // 4. 纯黑色
                    add(
                        SettingsCategoryItem(
                            icon = Icons.Filled.Contrast,
                            title = { Text(context.getString(R.string.high_contrast)) },
                            description = { 
                                Text(
                                    context.getString(R.string.use_pure_black_background_in_dark_mode),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = useBlackBackground,
                                    onCheckedChange = onBlackBackgroundChange
                                )
                            },
                            onClick = { onBlackBackgroundChange(!useBlackBackground) }
                        )
                    )
                }
            )
            
            // 主题配色（底部）
            ThemeColorCard(
                paletteColors = paletteColors,
                selectedColor = selectedColor,
                enabled = !useDynamicColor,
                onColorChange = onColorChange
            )
            
            // 底部留出安全空间
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 主题配色分组（参照 XMVISIO）：primary 色标题 + FlowRow 居中色板网格；
 * 动态颜色开启时整组置灰禁用。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeColorCard(
    paletteColors: List<Color>,
    selectedColor: Color,
    enabled: Boolean,
    onColorChange: (Color) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = LocalContext.current.getString(R.string.theme_palette),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                paletteColors.forEach { color ->
                    ColorButton(
                        color = color,
                        selected = color == selectedColor,
                        enabled = enabled,
                        onClick = { onColorChange(color) },
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}

/**
 * 颜色按钮（参照 XMVISIO ColorButton）：圆角方块托底，内嵌三色圆
 * （HCT 生成：主色调 tone80 + 左下浅 tone90 + 右下深 tone60）；
 * 选中时圆心弹出勾选圆（带缩放动画），无描边。
 */
@Composable
private fun ColorButton(
    color: Color,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerSize by animateDpAsState(
        targetValue = if (selected) 28.dp else 0.dp,
        label = "selectedContainer"
    )
    val iconSize by animateDpAsState(
        targetValue = if (selected) 16.dp else 0.dp,
        label = "selectedIcon"
    )

    Surface(
        modifier = modifier
            .size(64.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // HCT 色彩空间生成同色相三色调
            val hct = color.toHct()
            val color1 = Color(Hct.from(hct.hue, 40.0, 80.0).toInt())  // 主色调
            val color2 = Color(Hct.from(hct.hue, 40.0, 90.0).toInt())  // 浅色
            val color3 = Color(Hct.from(hct.hue, 40.0, 60.0).toInt())  // 深色

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color1)
            ) {
                // 左下浅色块
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(24.dp)
                        .background(color2)
                )
                // 右下深色块
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .background(color3)
                )
                // 选中状态：圆心动画容器 + Check 图标
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(containerSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }
        }
    }
}


