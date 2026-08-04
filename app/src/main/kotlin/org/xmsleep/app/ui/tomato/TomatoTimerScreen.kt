package org.xmsleep.app.ui.tomato

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.xmsleep.app.R
import org.xmsleep.app.ui.settings.TomatoTimerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TomatoTimerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current

    // 获取系统实际屏幕圆角
    val screenCornerRadius = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets = view.rootWindowInsets
            val cornerRadiusPx = insets?.getRoundedCorner(0)?.radius ?: 0
            if (cornerRadiusPx > 0) {
                with(density) { cornerRadiusPx.toFloat().toDp() }
            } else {
                16.dp
            }
        } else {
            16.dp
        }
    }

    var showPulse by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val pulseAlpha = remember { Animatable(0f) }
    var pulseColor by remember { mutableStateOf(Color.Unspecified) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val breakColor = MaterialTheme.colorScheme.tertiary

    LaunchedEffect(showPulse) {
        if (showPulse) {
            repeat(4) {
                pulseAlpha.animateTo(0.4f, tween(400, easing = FastOutSlowInEasing))
                pulseAlpha.animateTo(0.15f, tween(400, easing = FastOutSlowInEasing))
            }
            showPulse = false
            pulseAlpha.snapTo(0f)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (showPulse || pulseAlpha.value > 0f) {
            val shape = RoundedCornerShape(screenCornerRadius)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(3.dp, pulseColor.copy(alpha = pulseAlpha.value), shape)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(pulseAlpha.value * 0.03f)
                    .padding(3.dp)
                    .clip(shape)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = pulseColor)
                }
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.tomato_timer_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = context.getString(R.string.flip_clock_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = context.getString(R.string.tomato_settings_title)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            TomatoTimerView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onPulseStart = { isBreak ->
                    pulseColor = if (isBreak) breakColor else primaryColor
                    showPulse = true
                }
            )
        }
    }

    if (showSettings) {
        TomatoTimerSettingsSheet(
            onDismiss = { showSettings = false }
        )
    }
}
