package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CbzPageItem
import com.example.ui.components.ZoomableBox
import com.example.ui.model.PageFit
import com.example.ui.model.ReadingDirection
import com.example.ui.model.ReadingMode
import com.example.ui.theme.CbzGoldPrimary
import com.example.ui.theme.CbzTextSecondary
import com.example.ui.viewmodel.ReaderViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    bookId: Long,
    initialPage: Int = 1,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val book by viewModel.book.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val readingMode by viewModel.readingMode.collectAsState()
    val readingDirection by viewModel.readingDirection.collectAsState()
    val pageFit by viewModel.pageFit.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showControls by viewModel.showControls.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val totalPages = pages.size
    val isReady = !isLoading && book != null && book?.id == bookId && totalPages > 0

    // State for Vertical Mode initialized strictly to requested initial page
    val initialItemIndex = (initialPage - 1).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialItemIndex)

    // State for Horizontal Mode
    val isRtl = (readingDirection == ReadingDirection.RIGHT_TO_LEFT)
    val pagerState = rememberPagerState(
        initialPage = initialItemIndex.coerceIn(0, (totalPages - 1).coerceAtLeast(0)),
        pageCount = { totalPages }
    )

    // Ensure list/pager is positioned at initialPage once book is loaded
    LaunchedEffect(book?.id) {
        if (book?.id == bookId && totalPages > 0) {
            val targetIdx = (initialPage - 1).coerceIn(0, (totalPages - 1).coerceAtLeast(0))
            if (readingMode == ReadingMode.VERTICAL) {
                listState.scrollToItem(targetIdx)
            } else {
                pagerState.scrollToPage(targetIdx)
            }
        }
    }

    // Sync vertical scroll position with current page only during active user scrolling
    LaunchedEffect(listState, isReady) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstIndex ->
                if (isReady && readingMode == ReadingMode.VERTICAL && listState.isScrollInProgress) {
                    viewModel.onPageChanged(firstIndex + 1)
                }
            }
    }

    // Sync horizontal pager position with current page
    LaunchedEffect(pagerState, isReady) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { pageIdx ->
                if (isReady && readingMode == ReadingMode.HORIZONTAL && (pagerState.isScrollInProgress || !pagerState.canScrollForward && !pagerState.canScrollBackward)) {
                    viewModel.onPageChanged(pageIdx + 1)
                }
            }
    }

    // When mode switches, sync position
    LaunchedEffect(readingMode) {
        if (isReady) {
            val targetIdx = (currentPage - 1).coerceIn(0, (totalPages - 1).coerceAtLeast(0))
            if (readingMode == ReadingMode.VERTICAL) {
                listState.scrollToItem(targetIdx)
            } else {
                pagerState.scrollToPage(targetIdx)
            }
        }
    }

    // Fit mode menu state
    var showFitMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            isLoading || book == null || book?.id != bookId -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = CbzGoldPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading comic...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error Opening Comic",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            totalPages > 0 -> {
                // Reading Canvas
                if (readingMode == ReadingMode.VERTICAL) {
                    // Vertical continuous scroll (Long strip / Webtoon)
                    ZoomableBox(
                        onSingleTap = { viewModel.toggleControls() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("reader_vertical_list")
                        ) {
                            items(totalPages, key = { "${bookId}_$it" }) { idx ->
                                CbzPageItem(
                                    bookId = bookId,
                                    pageIndex = idx,
                                    viewModel = viewModel,
                                    pageFit = pageFit
                                )
                            }
                        }
                    }
                } else {
                    // Horizontal Page-by-Page Mode
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val screenWidth = maxWidth

                        HorizontalPager(
                            state = pagerState,
                            reverseLayout = isRtl,
                            key = { "${bookId}_$it" },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("reader_horizontal_pager")
                        ) { pageIdx ->
                            ZoomableBox(
                                onSingleTap = { viewModel.toggleControls() },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                CbzPageItem(
                                    bookId = bookId,
                                    pageIndex = pageIdx,
                                    viewModel = viewModel,
                                    pageFit = pageFit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Left and Right tap navigation zones when controls are hidden
                        if (!showControls) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                // Previous page zone (Left side in LTR, Right side in RTL)
                                Box(
                                    modifier = Modifier
                                        .weight(0.3f)
                                        .fillMaxSize()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            coroutineScope.launch {
                                                val prev = if (!isRtl) pagerState.currentPage - 1 else pagerState.currentPage + 1
                                                if (prev in 0 until totalPages) pagerState.animateScrollToPage(prev)
                                            }
                                        }
                                )

                                // Center zone toggles controls
                                Box(
                                    modifier = Modifier
                                        .weight(0.4f)
                                        .fillMaxSize()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            viewModel.toggleControls()
                                        }
                                )

                                // Next page zone (Right side in LTR, Left side in RTL)
                                Box(
                                    modifier = Modifier
                                        .weight(0.3f)
                                        .fillMaxSize()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            coroutineScope.launch {
                                                val next = if (!isRtl) pagerState.currentPage + 1 else pagerState.currentPage - 1
                                                if (next in 0 until totalPages) pagerState.animateScrollToPage(next)
                                            }
                                        }
                                )
                            }
                        }
                    }
                }

                // Top Bar Overlay
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it },
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xEE111216),
                                        Color(0xAA111216),
                                        Color.Transparent
                                    )
                                )
                            )
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Back button & Title
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onNavigateBack,
                                    modifier = Modifier.testTag("reader_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(
                                    text = book?.title ?: "CBZ Reader",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Quick Action Icons
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Reading Mode Toggle (Vertical <-> Horizontal)
                                IconButton(
                                    onClick = {
                                        val newMode = if (readingMode == ReadingMode.VERTICAL) ReadingMode.HORIZONTAL else ReadingMode.VERTICAL
                                        viewModel.setReadingMode(newMode)
                                    },
                                    modifier = Modifier.testTag("toggle_reading_mode")
                                ) {
                                    Icon(
                                        imageVector = if (readingMode == ReadingMode.VERTICAL) Icons.Default.ViewDay else Icons.Default.ViewCarousel,
                                        contentDescription = "Toggle Reading Mode",
                                        tint = CbzGoldPrimary
                                    )
                                }

                                // Direction Toggle (LTR <-> RTL) for horizontal mode
                                if (readingMode == ReadingMode.HORIZONTAL) {
                                    IconButton(
                                        onClick = {
                                            val newDir = if (readingDirection == ReadingDirection.LEFT_TO_RIGHT) ReadingDirection.RIGHT_TO_LEFT else ReadingDirection.LEFT_TO_RIGHT
                                            viewModel.setReadingDirection(newDir)
                                        },
                                        modifier = Modifier.testTag("toggle_reading_direction")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = "Toggle Direction",
                                            tint = if (isRtl) CbzGoldPrimary else Color.White
                                        )
                                    }
                                }

                                // Page Fit Dropdown
                                Box {
                                    IconButton(
                                        onClick = { showFitMenu = true },
                                        modifier = Modifier.testTag("page_fit_menu_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AspectRatio,
                                            contentDescription = "Page Fit",
                                            tint = Color.White
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showFitMenu,
                                        onDismissRequest = { showFitMenu = false }
                                    ) {
                                        PageFit.values().forEach { fit ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = fit.label,
                                                        fontWeight = if (pageFit == fit) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (pageFit == fit) CbzGoldPrimary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.setPageFit(fit)
                                                    showFitMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Bar Overlay
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0xCC111216),
                                        Color(0xEE111216)
                                    )
                                )
                            )
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Page indicator text
                            val progressPercent = if (totalPages > 0) ((currentPage.toFloat() / totalPages) * 100f).toInt() else 0
                            Row(
                                modifier = Modifier
                                    .background(Color(0x88000000), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Page $currentPage / $totalPages",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "•",
                                    color = CbzTextSecondary,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$progressPercent%",
                                    color = CbzGoldPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Page scrubber slider & Prev/Next buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous page button
                                IconButton(
                                    onClick = {
                                        val prevPage = (currentPage - 1).coerceAtLeast(1)
                                        viewModel.onPageChanged(prevPage)
                                        coroutineScope.launch {
                                            if (readingMode == ReadingMode.VERTICAL) {
                                                listState.scrollToItem(prevPage - 1)
                                            } else {
                                                pagerState.scrollToPage(prevPage - 1)
                                            }
                                        }
                                    },
                                    enabled = currentPage > 1,
                                    modifier = Modifier.testTag("reader_prev_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                        contentDescription = "Previous Page",
                                        tint = if (currentPage > 1) Color.White else Color.Gray
                                    )
                                }

                                // Slider
                                var sliderValue by remember(currentPage) { mutableFloatStateOf(currentPage.toFloat()) }

                                Slider(
                                    value = sliderValue,
                                    onValueChange = { newValue ->
                                        sliderValue = newValue
                                    },
                                    onValueChangeFinished = {
                                        val target = sliderValue.toInt().coerceIn(1, totalPages)
                                        viewModel.onPageChanged(target)
                                        coroutineScope.launch {
                                            if (readingMode == ReadingMode.VERTICAL) {
                                                listState.scrollToItem(target - 1)
                                            } else {
                                                pagerState.scrollToPage(target - 1)
                                            }
                                        }
                                    },
                                    valueRange = 1f..totalPages.toFloat().coerceAtLeast(1f),
                                    steps = (totalPages - 2).coerceAtLeast(0),
                                    colors = SliderDefaults.colors(
                                        thumbColor = CbzGoldPrimary,
                                        activeTrackColor = CbzGoldPrimary,
                                        inactiveTrackColor = Color(0x44FFFFFF)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reader_page_slider")
                                )

                                // Next page button
                                IconButton(
                                    onClick = {
                                        val nextPage = (currentPage + 1).coerceAtMost(totalPages)
                                        viewModel.onPageChanged(nextPage)
                                        coroutineScope.launch {
                                            if (readingMode == ReadingMode.VERTICAL) {
                                                listState.scrollToItem(nextPage - 1)
                                            } else {
                                                pagerState.scrollToPage(nextPage - 1)
                                            }
                                        }
                                    },
                                    enabled = currentPage < totalPages,
                                    modifier = Modifier.testTag("reader_next_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                        contentDescription = "Next Page",
                                        tint = if (currentPage < totalPages) Color.White else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
