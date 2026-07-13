package org.xmsleep.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WheelPicker(
    options: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemCount: Int = 5,
    itemHeight: Dp = 40.dp,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    unselectedTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectorColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
) {
    val repeatCount = 150
    val repeatedOptions = remember(options) {
        List(repeatCount) { options }.flatten()
    }
    val centerOffset = visibleItemCount / 2
    val initialPosition = (repeatCount / 2) * options.size + selectedIndex

    val listState = rememberLazyListState()
    var scrolling by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        listState.scrollToItem(initialPosition - centerOffset)
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            scrolling = true
        } else if (scrolling) {
            val center = listState.firstVisibleItemIndex + centerOffset
            if (center in 0 until repeatedOptions.size) {
                onItemSelected(center % options.size)
            }
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemCount)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(selectorColor)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            flingBehavior = rememberSnapFlingBehavior(listState)
        ) {
            for (idx in 0 until repeatedOptions.size) {
                item(key = idx) {
                    val firstVisible by remember { derivedStateOf { listState.firstVisibleItemIndex } }
                    val isCenter = idx == firstVisible + centerOffset

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = repeatedOptions[idx],
                            style = TextStyle(
                                fontSize = if (isCenter) 22.sp else 18.sp,
                                fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                color = if (isCenter) textColor else unselectedTextColor.copy(alpha = 0.5f),
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
