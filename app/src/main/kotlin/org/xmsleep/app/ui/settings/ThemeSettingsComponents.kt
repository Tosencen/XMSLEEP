package org.xmsleep.app.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
            
            // 底部留出安全空间
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


