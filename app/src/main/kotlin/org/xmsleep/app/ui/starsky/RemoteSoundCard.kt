package org.xmsleep.app.ui.starsky

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.xmsleep.app.R
import org.xmsleep.app.ui.AudioVisualizer
import org.xmsleep.app.utils.ToastUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RemoteSoundCard(
    sound: org.xmsleep.app.audio.model.SoundMetadata,
    displayName: String,
    isPlaying: Boolean,
    downloadProgress: Float?,
    isDownloadingButNoProgress: Boolean = false,
    columnsCount: Int = 3,
    isPinned: Boolean = false,
    onPinnedChange: (Boolean) -> Unit = {},
    onCardClick: () -> Unit,
    onVolumeClick: () -> Unit,
    cardHeight: Dp? = null,
    isEditMode: Boolean = false,
    onRemove: () -> Unit = {},
    isInPresetDialog: Boolean = false,
) {
    val context = LocalContext.current
    val cacheManager = remember {
        org.xmsleep.app.audio.AudioCacheManager.getInstance(context)
    }
    var isCached by remember(sound.id) {
        mutableStateOf(cacheManager.getCachedFile(sound.id) != null)
    }

    LaunchedEffect(downloadProgress, sound.id) {
        if (downloadProgress == null || downloadProgress >= 1.0f) {
            delay(200)
            val newCached = cacheManager.getCachedFile(sound.id) != null
            if (newCached != isCached) {
                isCached = newCached
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        val cached = cacheManager.getCachedFile(sound.id) != null
        if (cached != isCached) {
            isCached = cached
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.6f,
        label = "alpha"
    )

    var showTitleMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

        val finalCardHeight = cardHeight ?: if (columnsCount == 3) 110.dp else 120.dp

    if (isInPresetDialog) {
        val cardBackgroundColor = if (isCached) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(finalCardHeight)
                .then(
                    if (!isEditMode) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onCardClick() },
                                onLongPress = {
                                    scope.launch {
                                        showTitleMenu = true
                                    }
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            colors = CardDefaults.cardColors(
                containerColor = cardBackgroundColor
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val padding = if (finalCardHeight == 80.dp) 12.dp else 16.dp
                val textStyle = if (finalCardHeight == 80.dp) {
                    MaterialTheme.typography.titleSmall
                } else {
                    MaterialTheme.typography.titleMedium
                }
                val maxLines = if (finalCardHeight == 80.dp) 1 else 2

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Box(modifier = Modifier.align(Alignment.TopStart)) {
                        Text(
                            text = displayName,
                            style = textStyle,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .alpha(alpha)
                                .padding(end = 32.dp),
                            maxLines = maxLines,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!isEditMode) {
                        Box(modifier = Modifier.align(Alignment.TopStart)) {
                            DropdownMenu(
                                expanded = showTitleMenu,
                                onDismissRequest = { showTitleMenu = false },
                                modifier = Modifier.width(120.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = if (isPinned) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                            Text(
                                                text = if (isPinned) {
                                                    context.getString(R.string.cancel_default)
                                                } else {
                                                    context.getString(R.string.set_as_default)
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isPinned) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        }
                                    },
                                    onClick = {
                                        val newPinnedState = !isPinned
                                        if (newPinnedState && !isCached) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.must_download_before_pin),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            onPinnedChange(newPinnedState)
                                            showTitleMenu = false
                                            val toastMessage = if (newPinnedState) {
                                                context.getString(R.string.pinned_success)
                                            } else {
                                                context.getString(R.string.unpinned_success)
                                            }
                                            ToastUtils.showToast(context, toastMessage)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (isPlaying) {
                        AudioVisualizer(
                            isPlaying = isPlaying,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .size(24.dp, 16.dp)
                                .alpha(alpha),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isEditMode) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(y = 8.dp)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = context.getString(R.string.remove),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (isPlaying && cardHeight == null && !isEditMode) {
                        IconButton(
                            onClick = onVolumeClick,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 10.dp, y = 12.dp)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = context.getString(R.string.adjust_volume),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (isDownloadingButNoProgress && (downloadProgress == null || downloadProgress == 0f)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 12.dp,
                                    bottomEnd = 0.dp,
                                    bottomStart = 12.dp
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (downloadProgress != null && downloadProgress > 0f && downloadProgress < 1f) {
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .background(
                                color = if (isCached) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                shape = RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 12.dp,
                                    bottomEnd = 0.dp,
                                    bottomStart = 12.dp
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCached) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = context.getString(R.string.downloaded),
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = context.getString(R.string.cloud_audio),
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                if (downloadProgress != null && downloadProgress > 0f) {
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                }
            }
        }
    } else {
        val cardPadding = if (finalCardHeight <= 96.dp) 10.dp else 12.dp
        val circleSize = if (columnsCount == 3) 60.dp else 72.dp
        val iconSize = if (columnsCount == 3) 36.dp else 45.dp

        val circleAlpha by animateFloatAsState(
            targetValue = if (isPlaying) 1f else 0.4f,
            label = "circleAlpha"
        )

        val titleAndIconAlpha by animateFloatAsState(
            targetValue = if (isPlaying) 1f else 0.4f,
            label = "titleAndIconAlpha"
        )

        val pulseInfiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseGlowAlpha by pulseInfiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseGlowAlpha"
        )
        val isDownloading = isDownloadingButNoProgress ||
                (downloadProgress != null && downloadProgress > 0f && downloadProgress < 1f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(finalCardHeight)
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (!isEditMode) {
                        Modifier.combinedClickable(
                            onClick = onCardClick,
                            onLongClick = { showTitleMenu = true }
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = cardPadding)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Glow ring wrapper
                    Box(
                        modifier = Modifier.size(circleSize),
                        contentAlignment = Alignment.Center
                    ) {
                        // Pulse glow (when playing)
                        if (isPlaying) {
                            val glowColor = MaterialTheme.colorScheme.primary
                            Box(
                                modifier = Modifier
                                    .size(circleSize)
                                    .graphicsLayer {
                                        this.alpha = pulseGlowAlpha
                                    }
                                    .drawBehind {
                                        val radius = size.minDimension / 2f
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    glowColor.copy(alpha = 0.8f),
                                                    glowColor.copy(alpha = 0.0f)
                                                ),
                                                center = center,
                                                radius = radius
                                            ),
                                            radius = radius,
                                            center = center
                                        )
                                    }
                            )
                        }

                        // Main circle
                        Box(
                            modifier = Modifier
                                .size(circleSize)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        if (isPlaying) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                                        }
                                    )
                                    .graphicsLayer(alpha = circleAlpha)
                            )

                            if (isDownloading) {
                                if (isDownloadingButNoProgress && (downloadProgress == null || downloadProgress <= 0f)) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.fillMaxSize(),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else if (downloadProgress != null && downloadProgress > 0f && downloadProgress < 1f) {
                                    CircularProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier.fillMaxSize(),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val pulseIconAlpha by pulseInfiniteTransition.animateFloat(
                                    initialValue = 0.6f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1500, easing = EaseInOutCubic),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulseIconAlpha"
                                )
                                val pulseIconScale by pulseInfiniteTransition.animateFloat(
                                    initialValue = 0.9f,
                                    targetValue = 1.05f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1500, easing = EaseInOutCubic),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulseIconScale"
                                )
                                Icon(
                                    imageVector = getSoundIcon(sound),
                                    contentDescription = displayName,
                                    modifier = Modifier
                                        .size(iconSize)
                                        .graphicsLayer {
                                            if (isPlaying) {
                                                this.alpha = pulseIconAlpha
                                                this.scaleX = pulseIconScale
                                                this.scaleY = pulseIconScale
                                            }
                                        },
                                    tint = if (isPlaying) Color.White
                                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .alpha(titleAndIconAlpha)
                            .clickable { onVolumeClick() },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isEditMode) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(y = 4.dp)
                            .size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = context.getString(R.string.remove),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (!isEditMode) {
                    Box(modifier = Modifier.align(Alignment.TopStart)) {
                        DropdownMenu(
                            expanded = showTitleMenu,
                            onDismissRequest = { showTitleMenu = false },
                            modifier = Modifier.width(120.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (isPinned) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                        Text(
                                            text = if (isPinned) {
                                                context.getString(R.string.cancel_default)
                                            } else {
                                                context.getString(R.string.set_as_default)
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isPinned) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                },
                                onClick = {
                                    val newPinnedState = !isPinned
                                    if (newPinnedState && !isCached) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.must_download_before_pin),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        onPinnedChange(newPinnedState)
                                        showTitleMenu = false
                                        val toastMessage = if (newPinnedState) {
                                            context.getString(R.string.pinned_success)
                                        } else {
                                            context.getString(R.string.unpinned_success)
                                        }
                                        ToastUtils.showToast(context, toastMessage)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
