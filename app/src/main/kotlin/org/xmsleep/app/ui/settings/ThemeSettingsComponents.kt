package org.xmsleep.app.ui.settings

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.hct.Hct
import org.xmsleep.app.R
import org.xmsleep.app.theme.BottomRightDiagonalShape
import org.xmsleep.app.theme.DarkModeOption
import org.xmsleep.app.theme.TopLeftDiagonalShape
import org.xmsleep.app.ui.components.ColorButton
import org.xmsleep.app.ui.components.SwitchItem

/**
 * 主题设置屏幕
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsScreen(
    darkMode: DarkModeOption,
    selectedColor: Color,
    useDynamicColor: Boolean,
    useBlackBackground: Boolean,
    onDarkModeChange: (DarkModeOption) -> Unit,
    onColorChange: (Color) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onBlackBackgroundChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // 固定 TopAppBar，不随滚动隐藏
    Scaffold(
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
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                // TopAppBar 使用系统栏和显示区域切口
                windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 消费 TopAppBar 使用的 insets
                .consumeWindowInsets(
                    WindowInsets.systemBars.union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Top)
                )
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 外观模式选择面板
            DarkModeSelectPanel(
                currentMode = darkMode,
                onModeSelected = onDarkModeChange
            )
            
            // 动态颜色开关（Android 12+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchItem(
                    checked = useDynamicColor,
                    onCheckedChange = onDynamicColorChange,
                    title = context.getString(R.string.dynamic_color),
                    description = context.getString(R.string.use_wallpaper_color_as_theme)
                )
            }
            
            // 高对比度开关
            SwitchItem(
                checked = useBlackBackground,
                onCheckedChange = onBlackBackgroundChange,
                title = context.getString(R.string.high_contrast),
                description = context.getString(R.string.use_pure_black_background_in_dark_mode)
            )
            
            // 调色板选择
            Box(
                modifier = Modifier.alpha(if (useDynamicColor) 0.5f else 1f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(context.getString(R.string.color_palette), style = MaterialTheme.typography.titleMedium)
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 使用 HCT 色彩空间生成均匀分布的颜色
                        val colors = listOf(
                            Color(Hct.from(140.0, 40.0, 40.0).toInt()),  // base=4: 绿色偏青
                            Color(Hct.from(175.0, 40.0, 40.0).toInt()),  // base=5: 青绿
                            Color(Hct.from(210.0, 40.0, 40.0).toInt()),  // base=6: 青蓝
                            Color(Hct.from(245.0, 40.0, 40.0).toInt()),  // base=7: 蓝紫
                            Color(Hct.from(280.0, 40.0, 40.0).toInt()),  // base=8: 紫红
                            Color(0xFF4F378B),  // 默认紫色
                            Color(Hct.from(315.0, 40.0, 40.0).toInt()),  // base=9: 粉红
                            Color(Hct.from(350.0, 40.0, 40.0).toInt()),  // base=10: 红
                            Color(Hct.from(35.0, 40.0, 40.0).toInt()),   // base=1: 橙黄
                            Color(Hct.from(70.0, 40.0, 40.0).toInt()),   // base=2: 黄
                            Color(Hct.from(105.0, 40.0, 40.0).toInt()),  // base=3: 黄绿
                        )
                        
                        colors.forEach { color ->
                            ColorButton(
                                baseColor = color,
                                selected = selectedColor == color && !useDynamicColor,
                                onClick = {
                                    onColorChange(color)
                                    onDynamicColorChange(false)
                                }
                            )
                        }
                    }
                }
            }
            
            // 底部留出安全空间
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 深色模式选择面板
 */
@Composable
fun DarkModeSelectPanel(
    currentMode: DarkModeOption,
    onModeSelected: (DarkModeOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val panelModifier = Modifier.size(96.dp, 146.dp)
    
    val themePanelItem: @Composable (DarkModeOption) -> Unit = { mode ->
        ColorSchemePreviewItem(
            onClick = { onModeSelected(mode) },
            panel = {
                if (mode != DarkModeOption.AUTO) {
                    ThemePreviewPanel(
                        isDark = mode == DarkModeOption.DARK,
                        modifier = panelModifier,
                    )
                } else {
                    DiagonalMixedThemePreviewPanel(
                        modifier = panelModifier,
                    )
                }
            },
            text = {
                Text(
                    when (mode) {
                        DarkModeOption.LIGHT -> context.getString(R.string.light_mode)
                        DarkModeOption.DARK -> context.getString(R.string.dark_mode)
                        DarkModeOption.AUTO -> context.getString(R.string.auto_mode)
                    }
                )
            },
            selected = currentMode == mode,
        )
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        themePanelItem(DarkModeOption.LIGHT)
        themePanelItem(DarkModeOption.DARK)
        themePanelItem(DarkModeOption.AUTO)
    }
}

/**
 * 颜色方案预览项
 */
@Composable
fun ColorSchemePreviewItem(
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    panel: @Composable () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.Start,
    ) {
        panel()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                RadioButton(
                    selected = selected,
                    interactionSource = interactionSource,
                    onClick = null,
                )
            }
            text()
        }
    }
}

/**
 * 主题预览面板
 */
@Composable
fun ThemePreviewPanel(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    // 使用更柔和的紫色（降低饱和度和亮度）
    val previewColor = Color(Hct.from(280.0, 36.0, 50.0).toInt())
    val colorScheme = dynamicColorScheme(
        primary = previewColor,
        isDark = isDark,
        isAmoled = false,
        style = PaletteStyle.TonalSpot,
    )
    
    Box(modifier) {
        Row(
            modifier = Modifier.fillMaxSize()
                .background(
                    color = colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = colorScheme.background, shape = RoundedCornerShape(9.dp)),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(32.dp)
                                .background(
                                    color = colorScheme.primary,
                                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp),
                                ),
                        )
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(32.dp)
                                .background(colorScheme.tertiary),
                        )
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(32.dp)
                                .background(
                                    color = colorScheme.secondary,
                                    shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                                ),
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(8.dp)
                                .background(color = colorScheme.primaryContainer, shape = CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(8.dp)
                                .background(color = colorScheme.secondaryContainer, shape = CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(8.dp)
                                .background(color = colorScheme.tertiaryContainer, shape = CircleShape),
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .height(height = 26.dp)
                        .background(
                            color = colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                        )
                        .padding(horizontal = 5.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(color = colorScheme.primary, shape = CircleShape),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .background(color = colorScheme.primaryContainer, shape = CircleShape),
                    )
                }
            }
        }
    }
}

/**
 * 对角线混合主题预览面板（用于"跟随系统"选项）
 */
@Composable
fun DiagonalMixedThemePreviewPanel(
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        ThemePreviewPanel(
            isDark = false,
            modifier = Modifier
                .fillMaxSize()
                .clip(TopLeftDiagonalShape),
        )
        ThemePreviewPanel(
            isDark = true,
            modifier = Modifier
                .fillMaxSize()
                .clip(BottomRightDiagonalShape),
        )
    }
}
