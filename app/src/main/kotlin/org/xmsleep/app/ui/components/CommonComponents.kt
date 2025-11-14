package org.xmsleep.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toHct
import org.xmsleep.app.R
import org.xmsleep.app.theme.DarkModeOption

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
