package org.xmsleep.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.xmsleep.app.R
import org.xmsleep.app.i18n.LanguageManager
import org.xmsleep.app.ui.BackgroundSelection
import coil.compose.AsyncImage

@Composable
fun BackgroundSelectionDialog(
    currentSelection: BackgroundSelection,
    paletteColors: List<Color>,
    currentColor: Color,
    onSelectionChange: (BackgroundSelection) -> Unit,
    onColorChange: (Color) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    currentLanguage: LanguageManager.Language? = null,
    customBackgroundUri: String? = null,
    onCustomBackgroundClick: () -> Unit = {},
    pendingCustomBgUri: String? = null,
    pendingCustomBgThumbnail: String? = null,
    pendingCustomBgColor: Color? = null,
    onCustomColorChange: (Color) -> Unit = {}
) {
    val context = LocalContext.current
    val localizedContext = remember(currentLanguage) {
        if (currentLanguage != null) LanguageManager.createLocalizedContext(context, currentLanguage) else context
    }

    val backgroundOptions = remember {
        listOf(
            BackgroundSelection.None,
            BackgroundSelection.Custom,
            BackgroundSelection.Background1,
            BackgroundSelection.Background6
        )
    }

    val dialogTitle = remember(currentLanguage) { localizedContext.getString(R.string.select_background) }
    val confirmText = remember(currentLanguage) { localizedContext.getString(android.R.string.ok) }
    val cancelText = remember(currentLanguage) { localizedContext.getString(android.R.string.cancel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = dialogTitle) },
        text = {
            val showColorSection = currentSelection == BackgroundSelection.None ||
                    currentSelection == BackgroundSelection.Custom
            val colorSectionTitle = remember(currentLanguage) { localizedContext.getString(R.string.theme_color) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(backgroundOptions) { option ->
                        val previewUri = if (option == BackgroundSelection.Custom) {
                            val currentUri = pendingCustomBgUri ?: customBackgroundUri
                            if (currentUri?.endsWith(".mp4", ignoreCase = true) == true) {
                                pendingCustomBgThumbnail
                            } else {
                                currentUri
                            }
                        } else null
                        BackgroundOptionItem(
                            option = option,
                            isSelected = option == currentSelection,
                            customPreviewUri = previewUri,
                            onClick = {
                                if (option == BackgroundSelection.Custom) {
                                    onCustomBackgroundClick()
                                } else {
                                    onSelectionChange(option)
                                }
                            },
                            currentLanguage = currentLanguage
                        )
                    }
                }

                if (showColorSection) {
                    Spacer(Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = colorSectionTitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(paletteColors) { color ->
                                val isSelected = currentColor == color
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    3.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                    CircleShape
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable {
                                            if (currentSelection == BackgroundSelection.Custom) {
                                                onCustomColorChange(color)
                                            } else {
                                                onColorChange(color)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (color.luminance() > 0.35f) {
                                                Color.Black.copy(alpha = 0.7f)
                                            } else {
                                                Color.White
                                            },
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = cancelText)
            }
        }
    )
}


@Composable
private fun BackgroundOptionItem(
    option: BackgroundSelection,
    isSelected: Boolean,
    customPreviewUri: String? = null,
    onClick: () -> Unit,
    currentLanguage: LanguageManager.Language? = null
) {
    val localContext = LocalContext.current
    val localizedContext = remember(currentLanguage) {
        if (currentLanguage != null) LanguageManager.createLocalizedContext(localContext, currentLanguage) else localContext
    }

    val selectedText = remember(currentLanguage) { localizedContext.getString(R.string.selected) }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                option == BackgroundSelection.Custom -> {
                    if (customPreviewUri != null) {
                        AsyncImage(
                            model = customPreviewUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // 半透明遮罩
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                        )
                    }
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(12.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                option != BackgroundSelection.None && option.resourceId != null -> {
                    AnimatedWebPImage(
                        drawableResId = option.resourceId,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        isPlaying = false
                    )
                }
                else -> {
                    Icon(
                        Icons.Default.HideImage,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            if (isSelected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = selectedText,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}