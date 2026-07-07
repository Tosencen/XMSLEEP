package org.xmsleep.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.xmsleep.app.R
import org.xmsleep.app.i18n.LanguageManager
import org.xmsleep.app.ui.BackgroundSelection

data class BackgroundSettings(
    val selection: BackgroundSelection,
    val color: Color,
    val opacity: Float,
    val blurRadius: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundSettingsSheet(
    show: Boolean,
    currentSelection: BackgroundSelection,
    paletteColors: List<Color>,
    currentColor: Color,
    currentOpacity: Float,
    currentBlurRadius: Float,
    customBackgroundUri: String?,
    customBackgroundThumbnail: String?,
    pendingCustomBgUri: String?,
    pendingCustomBgThumbnail: String?,
    pendingCustomBgColor: Color?,
    hasPendingCustomBg: Boolean = false,
    onSelectionChange: (BackgroundSelection) -> Unit,
    onColorChange: (Color) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onBlurRadiusChange: (Float) -> Unit,
    onCustomBackgroundClick: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (BackgroundSettings) -> Unit,
    currentLanguage: LanguageManager.Language? = null
) {
    if (!show) return

    val context = LocalContext.current
    val localizedContext = remember(currentLanguage) {
        if (currentLanguage != null) LanguageManager.createLocalizedContext(context, currentLanguage) else context
    }

    var localSelection by remember(currentSelection, hasPendingCustomBg) {
        mutableStateOf(
            if (hasPendingCustomBg && pendingCustomBgUri != null) BackgroundSelection.Custom
            else currentSelection
        )
    }
    var localColor by remember(currentColor, pendingCustomBgColor, hasPendingCustomBg) {
        mutableStateOf(
            if (hasPendingCustomBg && pendingCustomBgColor != null) pendingCustomBgColor
            else currentColor
        )
    }
    var localOpacity by remember(currentOpacity) { mutableStateOf(currentOpacity) }
    var localBlurRadius by remember(currentBlurRadius) { mutableStateOf(currentBlurRadius) }

    val previewUri = if (localSelection == BackgroundSelection.Custom) {
        val currentUri = pendingCustomBgUri ?: customBackgroundUri
        if (currentUri?.endsWith(".mp4", ignoreCase = true) == true) {
            pendingCustomBgThumbnail
        } else {
            currentUri
        }
    } else null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // === Radio Preview ===
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                RadioPreview(
                    backgroundSelection = localSelection,
                    customBackgroundUri = pendingCustomBgUri ?: customBackgroundUri,
                    customBackgroundThumbnail = pendingCustomBgThumbnail ?: customBackgroundThumbnail,
                    customBackgroundColor = localColor,
                    backgroundOpacity = localOpacity,
                    backgroundBlurRadius = localBlurRadius
                )
            }

            Spacer(Modifier.height(14.dp))

            // === No Background + Change Background buttons ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { localSelection = BackgroundSelection.None },
                    modifier = Modifier.weight(1f),
                    border = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (localSelection == BackgroundSelection.None)
                            MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        Icons.Default.HideImage,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(localizedContext.getString(R.string.no_background), style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = onCustomBackgroundClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(localizedContext.getString(R.string.change_background), style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(20.dp))

            // === Theme Color ===
            Text(
                text = localizedContext.getString(R.string.theme_color),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
            )
            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                paletteColors.forEach { color ->
                    val isSelected = localColor == color
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .let { mod ->
                                if (isSelected) mod.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else mod
                            }
                            .clickable {
                                localColor = color
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = if (color.luminance() > 0.35f) Color.Black.copy(alpha = 0.7f) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // === Built-in backgrounds ===
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                WebPBackgroundCard(
                    drawableResId = R.drawable.bg_animation_1,
                    isSelected = localSelection == BackgroundSelection.Background1,
                    label = localizedContext.getString(R.string.built_in_background),
                    onClick = {
                        localSelection = if (localSelection == BackgroundSelection.Background1)
                            BackgroundSelection.None else BackgroundSelection.Background1
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                )
                WebPBackgroundCard(
                    drawableResId = R.drawable.bg_animation_6,
                    isSelected = localSelection == BackgroundSelection.Background6,
                    label = localizedContext.getString(R.string.built_in_background),
                    onClick = {
                        localSelection = if (localSelection == BackgroundSelection.Background6)
                            BackgroundSelection.None else BackgroundSelection.Background6
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            // === Opacity & Blur side by side ===
            val hasBg = localSelection != BackgroundSelection.None
            val disabledAlpha = 0.38f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Opacity
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = localizedContext.getString(R.string.opacity),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = if (hasBg) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = disabledAlpha)
                        )
                        Text(
                            text = if (hasBg) "${(localOpacity * 100).toInt()}%" else "--",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (hasBg) 0.6f else disabledAlpha)
                        )
                    }
                    Slider(
                        value = localOpacity,
                        onValueChange = { localOpacity = it },
                        valueRange = 0.05f..1f,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hasBg,
                        colors = SliderDefaults.colors(
                            disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = disabledAlpha),
                            disabledActiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = disabledAlpha),
                            disabledInactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = disabledAlpha)
                        )
                    )
                }
                // Blur
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = localizedContext.getString(R.string.blur_amount),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = if (hasBg) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = disabledAlpha)
                        )
                        Text(
                            text = if (hasBg) (if (localBlurRadius <= 0) localizedContext.getString(R.string.off) else "${localBlurRadius.toInt()}dp") else "--",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (hasBg) 0.6f else disabledAlpha)
                        )
                    }
                    Slider(
                        value = localBlurRadius,
                        onValueChange = { localBlurRadius = it },
                        valueRange = 0f..25f,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hasBg,
                        colors = SliderDefaults.colors(
                            disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = disabledAlpha),
                            disabledActiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = disabledAlpha),
                            disabledInactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = disabledAlpha)
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // === Action buttons ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(localizedContext.getString(android.R.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = {
                    onConfirm(
                        BackgroundSettings(
                            selection = localSelection,
                            color = localColor,
                            opacity = localOpacity,
                            blurRadius = localBlurRadius
                        )
                    )
                }) {
                    Text(localizedContext.getString(android.R.string.ok))
                }
            }
        }
    }
}

@Composable
private fun WebPBackgroundCard(
    drawableResId: Int,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedWebPImage(
                drawableResId = drawableResId,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                isPlaying = false
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
            )
            if (isSelected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
