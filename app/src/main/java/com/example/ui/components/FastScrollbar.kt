package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookEntity
import com.example.ui.theme.CbzDarkSurfaceContainer
import com.example.ui.theme.CbzDarkSurfaceHighlight
import com.example.ui.theme.CbzGoldPrimary
import com.example.ui.theme.CbzTextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * FastScrollbar for LazyVerticalGrid in the LibraryScreen.
 */
@Composable
fun FastScrollbarGrid(
    gridState: LazyGridState,
    books: List<BookEntity>,
    headerItemCount: Int = 0,
    modifier: Modifier = Modifier
) {
    if (books.size < 6) return // Only show fast scrollbar if library has enough items

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    // Natural scroll progress from gridState
    val scrollProgress by remember {
        derivedStateOf {
            val total = gridState.layoutInfo.totalItemsCount
            val visible = gridState.layoutInfo.visibleItemsInfo.size
            if (total > visible && total > 0) {
                (gridState.firstVisibleItemIndex.toFloat() / (total - visible).coerceAtLeast(1))
                    .coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    val activeProgress = if (isDragging) dragProgress else scrollProgress
    val isScrolling by remember { derivedStateOf { gridState.isScrollInProgress } }

    FastScrollbarContent(
        activeProgress = activeProgress,
        isDragging = isDragging,
        isScrolling = isScrolling,
        books = books,
        headerItemCount = headerItemCount,
        onDragStart = { fraction ->
            isDragging = true
            dragProgress = fraction
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val targetBookIndex = (fraction * (books.size - 1)).roundToInt().coerceIn(0, books.size - 1)
            coroutineScope.launch {
                gridState.scrollToItem(headerItemCount + targetBookIndex)
            }
        },
        onDrag = { fraction ->
            dragProgress = fraction
            val targetBookIndex = (fraction * (books.size - 1)).roundToInt().coerceIn(0, books.size - 1)
            coroutineScope.launch {
                gridState.scrollToItem(headerItemCount + targetBookIndex)
            }
        },
        onDragEnd = {
            isDragging = false
        },
        modifier = modifier
    )
}

/**
 * FastScrollbar for LazyColumn in the LibraryScreen.
 */
@Composable
fun FastScrollbarList(
    listState: LazyListState,
    books: List<BookEntity>,
    headerItemCount: Int = 0,
    modifier: Modifier = Modifier
) {
    if (books.size < 6) return

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val scrollProgress by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val visible = listState.layoutInfo.visibleItemsInfo.size
            if (total > visible && total > 0) {
                (listState.firstVisibleItemIndex.toFloat() / (total - visible).coerceAtLeast(1))
                    .coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    val activeProgress = if (isDragging) dragProgress else scrollProgress
    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }

    FastScrollbarContent(
        activeProgress = activeProgress,
        isDragging = isDragging,
        isScrolling = isScrolling,
        books = books,
        headerItemCount = headerItemCount,
        onDragStart = { fraction ->
            isDragging = true
            dragProgress = fraction
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val targetBookIndex = (fraction * (books.size - 1)).roundToInt().coerceIn(0, books.size - 1)
            coroutineScope.launch {
                listState.scrollToItem(headerItemCount + targetBookIndex)
            }
        },
        onDrag = { fraction ->
            dragProgress = fraction
            val targetBookIndex = (fraction * (books.size - 1)).roundToInt().coerceIn(0, books.size - 1)
            coroutineScope.launch {
                listState.scrollToItem(headerItemCount + targetBookIndex)
            }
        },
        onDragEnd = {
            isDragging = false
        },
        modifier = modifier
    )
}

@Composable
private fun FastScrollbarContent(
    activeProgress: Float,
    isDragging: Boolean,
    isScrolling: Boolean,
    books: List<BookEntity>,
    headerItemCount: Int,
    onDragStart: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val thumbHeightDp = 52.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(52.dp) // Accessible 52dp hit area along the right edge
            .testTag("fast_scrollbar_track")
    ) {
        val trackHeightPx = with(density) { maxHeight.toPx() }
        val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
        val availableTrackPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)

        val thumbOffsetPx = (activeProgress * availableTrackPx).coerceIn(0f, availableTrackPx)
        val thumbOffsetDp = with(density) { thumbOffsetPx.toDp() }

        // Determine current book under thumb
        val currentBookIndex = (activeProgress * (books.size - 1)).roundToInt().coerceIn(0, books.size - 1)
        val currentBook = books.getOrNull(currentBookIndex)
        val initialLetter = currentBook?.title?.trimStart()?.firstOrNull()?.uppercaseChar()?.toString() ?: "#"

        // Thumb styling animations
        val thumbWidth by animateDpAsState(
            targetValue = if (isDragging) 9.dp else if (isScrolling) 6.dp else 4.dp,
            label = "thumb_width"
        )
        val thumbAlpha by animateFloatAsState(
            targetValue = if (isDragging) 1f else if (isScrolling) 0.85f else 0.45f,
            label = "thumb_alpha"
        )

        // Gesture detector on the 52dp strip
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(trackHeightPx) {
                    detectTapGestures(
                        onPress = { offset ->
                            val fraction = ((offset.y - thumbHeightPx / 2f) / availableTrackPx).coerceIn(0f, 1f)
                            onDragStart(fraction)
                            tryAwaitRelease()
                            onDragEnd()
                        }
                    )
                }
                .pointerInput(trackHeightPx) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val fraction = ((offset.y - thumbHeightPx / 2f) / availableTrackPx).coerceIn(0f, 1f)
                            onDragStart(fraction)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val fraction = ((change.position.y - thumbHeightPx / 2f) / availableTrackPx).coerceIn(0f, 1f)
                            onDrag(fraction)
                        },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() }
                    )
                }
        ) {
            // Background subtle track line when dragging
            if (isDragging) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(Color(0x33C9840E))
                )
            }

            // Draggable Scrollbar Thumb
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(x = 0, y = thumbOffsetPx.roundToInt()) }
                    .padding(end = 3.dp)
                    .width(thumbWidth)
                    .height(thumbHeightDp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                CbzGoldPrimary.copy(alpha = thumbAlpha),
                                Color(0xFFFFA000).copy(alpha = thumbAlpha)
                            )
                        )
                    )
                    .testTag("fast_scrollbar_thumb")
            )

            // Fast-Scroller Popup Indicator Bubble (Appears on drag)
            AnimatedVisibility(
                visible = isDragging,
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset {
                        // Clamp bubble so it does not overflow top or bottom of screen
                        val bubbleY = (thumbOffsetPx - 16.dp.toPx()).coerceIn(
                            10.dp.toPx(),
                            (trackHeightPx - 90.dp.toPx()).coerceAtLeast(10.dp.toPx())
                        )
                        IntOffset(x = -48.dp.roundToPx(), y = bubbleY.roundToInt())
                    }
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CbzDarkSurfaceContainer,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, CbzGoldPrimary),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .widthIn(min = 120.dp, max = 220.dp)
                        .padding(end = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Section Letter Badge
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CbzDarkSurfaceHighlight)
                                .border(1.dp, CbzGoldPrimary.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initialLetter,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = CbzGoldPrimary,
                                fontSize = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Book Title & Index Counter
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentBook?.title ?: "Comic",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${currentBookIndex + 1} of ${books.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CbzTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
