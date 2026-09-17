package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.ui.components.FastScrollbarGrid
import com.example.ui.components.FastScrollbarList
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookEntity
import com.example.ui.components.BookDetailsDialog
import com.example.ui.components.CbzCard
import com.example.ui.components.CbzListItem
import com.example.ui.components.ContinueReadingCard
import com.example.ui.components.EmptyLibraryView
import com.example.ui.components.ResumeReadingDialog
import com.example.ui.components.SortDialog
import com.example.ui.model.FilterOption
import com.example.ui.model.LibraryViewMode
import com.example.ui.theme.CbzDarkBackground
import com.example.ui.theme.CbzDarkSurfaceContainer
import com.example.ui.theme.CbzDarkSurfaceVariant
import com.example.ui.theme.CbzGoldPrimary
import com.example.ui.theme.CbzTextSecondary
import com.example.ui.viewmodel.LibraryViewModel
import com.example.util.StoragePermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenBook: (bookId: Long, startPage: Int?) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val books by viewModel.books.collectAsState()
    val continueBook by viewModel.continueReadingBook.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanMessage by viewModel.scanStatusMessage.collectAsState()
    val scanCurrentStep by viewModel.scanCurrentStep.collectAsState()
    val scanTotalSteps by viewModel.scanTotalSteps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterOption by viewModel.filterOption.collectAsState()
    val viewMode by viewModel.libraryViewMode.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var selectedBookForResume by remember { mutableStateOf<BookEntity?>(null) }
    var selectedBookForDetails by remember { mutableStateOf<BookEntity?>(null) }
    var hasFullPermission by remember {
        mutableStateOf(StoragePermissionHelper.hasFullStoragePermission(context))
    }
    var permissionBannerDismissed by remember { mutableStateOf(false) }

    // Launcher for All Files Access / Storage Permission
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val granted = StoragePermissionHelper.hasFullStoragePermission(context)
        hasFullPermission = granted
        if (granted) {
            viewModel.scanStorage()
        }
    }

    // Check permission on resume or when screen appears
    LaunchedEffect(Unit) {
        hasFullPermission = StoragePermissionHelper.hasFullStoragePermission(context)
    }

    // Folder Picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // ignore
            }
            val displayName = uri.lastPathSegment?.substringAfterLast(':') ?: "Comics Folder"
            viewModel.addFolder(uri, displayName)
        }
    }

    // Single File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // ignore
            }
            viewModel.importCbz(uri)
        }
    }

    // Dialogs
    if (showSortDialog) {
        SortDialog(
            currentSort = sortOrder,
            onSortSelected = { viewModel.setSortOrder(it) },
            onDismiss = { showSortDialog = false }
        )
    }

    selectedBookForResume?.let { book ->
        ResumeReadingDialog(
            book = book,
            onResume = {
                val target = selectedBookForResume
                selectedBookForResume = null
                if (target != null) {
                    onOpenBook(target.id, target.currentPage)
                }
            },
            onStartBeginning = {
                val target = selectedBookForResume
                selectedBookForResume = null
                if (target != null) {
                    onOpenBook(target.id, 1)
                }
            },
            onDismiss = { selectedBookForResume = null }
        )
    }

    selectedBookForDetails?.let { book ->
        BookDetailsDialog(
            book = book,
            onRead = {
                val target = selectedBookForDetails
                selectedBookForDetails = null
                if (target != null) {
                    onOpenBook(target.id, if (target.currentPage > 1) target.currentPage else 1)
                }
            },
            onStartBeginning = {
                val target = selectedBookForDetails
                selectedBookForDetails = null
                if (target != null) {
                    onOpenBook(target.id, 1)
                }
            },
            onToggleFavorite = { viewModel.toggleFavorite(book) },
            onRescan = {
                viewModel.rescanBook(book.id)
                selectedBookForDetails = null
            },
            onDelete = { deleteFile ->
                viewModel.deleteBook(book, deleteFile)
                selectedBookForDetails = null
            },
            onDismiss = { selectedBookForDetails = null }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CbzDarkBackground,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        Text(
                            text = "CBZ Reader",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        // Scan Whole Device Button
                        IconButton(
                            onClick = {
                                if (!StoragePermissionHelper.hasFullStoragePermission(context)) {
                                    storagePermissionLauncher.launch(StoragePermissionHelper.createPermissionIntent(context))
                                } else {
                                    viewModel.scanStorage()
                                }
                            },
                            modifier = Modifier.testTag("scan_whole_device_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan Whole Device",
                                tint = if (isScanning) CbzGoldPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Search Button
                        IconButton(
                            onClick = {
                                isSearchExpanded = !isSearchExpanded
                                if (!isSearchExpanded) viewModel.setSearchQuery("")
                            },
                            modifier = Modifier.testTag("search_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearchExpanded) CbzGoldPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Add folder / document button
                        IconButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier.testTag("add_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "Add Folder",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Grid / List View Toggle Button
                        IconButton(
                            onClick = {
                                val nextMode = if (viewMode == LibraryViewMode.GRID) LibraryViewMode.LIST else LibraryViewMode.GRID
                                viewModel.setLibraryViewMode(nextMode)
                            },
                            modifier = Modifier.testTag("top_bar_view_mode_toggle")
                        ) {
                            Icon(
                                imageVector = if (viewMode == LibraryViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = if (viewMode == LibraryViewMode.GRID) "Switch to list view" else "Switch to grid view",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Sort Button
                        IconButton(
                            onClick = { showSortDialog = true },
                            modifier = Modifier.testTag("sort_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Settings Button
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("settings_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )

                // Search Bar Expandable
                AnimatedVisibility(visible = isSearchExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search by comic or file name...") },
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CbzGoldPrimary,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedContainerColor = CbzDarkSurfaceContainer,
                                unfocusedContainerColor = CbzDarkSurfaceContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_text_field")
                        )
                    }
                }

                // Scanning Progress Bar (Thin Top Header Line)
                if (isScanning) {
                    if (scanTotalSteps > 0 && scanCurrentStep > 0) {
                        LinearProgressIndicator(
                            progress = { (scanCurrentStep.toFloat() / scanTotalSteps.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = CbzGoldPrimary,
                            trackColor = Color.Transparent
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = CbzGoldPrimary,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission Banner if All Files Access is not yet granted
            if (!hasFullPermission && !permissionBannerDismissed) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = CbzDarkSurfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = null,
                            tint = CbzGoldPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scan Whole Device",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Grant storage access to search all folders & SD cards for comic archives.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CbzTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                storagePermissionLauncher.launch(StoragePermissionHelper.createPermissionIntent(context))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CbzGoldPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FilterOption.values()) { option ->
                    val selected = (filterOption == option)
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setFilterOption(option) },
                        label = {
                            Text(
                                text = option.label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CbzGoldPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = CbzDarkSurfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = if (selected) CbzGoldPrimary else Color(0x22FFFFFF)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // Scanning Status Card with Detailed Feedback
            if (scanMessage.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = CbzDarkSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = CbzGoldPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(
                            text = scanMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = CbzGoldPrimary,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Empty State
            if (books.isEmpty()) {
                EmptyLibraryView(
                    onScanNow = {
                        if (!StoragePermissionHelper.hasFullStoragePermission(context)) {
                            storagePermissionLauncher.launch(StoragePermissionHelper.createPermissionIntent(context))
                        } else {
                            viewModel.scanStorage()
                        }
                    },
                    onChooseFolder = { folderPickerLauncher.launch(null) },
                    onOpenFile = { filePickerLauncher.launch(arrayOf("*/*", "application/x-cbz", "application/zip")) },
                    hasStoragePermission = hasFullPermission,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Content with Continue Reading banner + Books Grid or List
                val hasContinue = continueBook != null && searchQuery.isBlank() && filterOption == FilterOption.ALL
                val headerCount = if (hasContinue) 2 else 1

                if (viewMode == LibraryViewMode.GRID) {
                    val gridState = rememberLazyGridState()
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(minSize = 140.dp),
                            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 20.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("library_grid")
                        ) {
                            // Continue Reading Banner if present
                            if (continueBook != null && searchQuery.isBlank() && filterOption == FilterOption.ALL) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Column {
                                        ContinueReadingCard(
                                            book = continueBook!!,
                                            onContinueClick = {
                                                onOpenBook(continueBook!!.id, continueBook!!.currentPage)
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(18.dp))
                                    }
                                }
                            }

                            // Section Header: My Library & View Toggle
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                LibrarySectionHeader(
                                    totalBooks = books.size,
                                    isScanning = isScanning,
                                    viewMode = viewMode,
                                    onToggleView = { viewModel.setLibraryViewMode(LibraryViewMode.LIST) },
                                    onScanNow = { viewModel.scanStorage() }
                                )
                            }

                            // Book Cards
                            items(books, key = { it.id }) { book ->
                                CbzCard(
                                    book = book,
                                    onClick = {
                                        if (book.currentPage > 1) {
                                            selectedBookForResume = book
                                        } else {
                                            onOpenBook(book.id, 1)
                                        }
                                    },
                                    onLongClick = { selectedBookForDetails = book },
                                    onFavoriteToggle = { viewModel.toggleFavorite(book) }
                                )
                            }
                        }

                        // Fast scrollbar overlay for jumping quickly across library
                        FastScrollbarGrid(
                            gridState = gridState,
                            books = books,
                            headerItemCount = headerCount,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                } else {
                    // List View
                    val listState = rememberLazyListState()
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 20.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("library_list")
                        ) {
                            // Continue Reading Banner if present
                            if (continueBook != null && searchQuery.isBlank() && filterOption == FilterOption.ALL) {
                                item {
                                    ContinueReadingCard(
                                        book = continueBook!!,
                                        onContinueClick = {
                                            onOpenBook(continueBook!!.id, continueBook!!.currentPage)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            }

                            // Section Header
                            item {
                                LibrarySectionHeader(
                                    totalBooks = books.size,
                                    isScanning = isScanning,
                                    viewMode = viewMode,
                                    onToggleView = { viewModel.setLibraryViewMode(LibraryViewMode.GRID) },
                                    onScanNow = { viewModel.scanStorage() }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // List Items
                            items(books, key = { it.id }) { book ->
                                CbzListItem(
                                    book = book,
                                    onClick = {
                                        if (book.currentPage > 1) {
                                            selectedBookForResume = book
                                        } else {
                                            onOpenBook(book.id, 1)
                                        }
                                    },
                                    onLongClick = { selectedBookForDetails = book },
                                    onFavoriteToggle = { viewModel.toggleFavorite(book) }
                                )
                            }
                        }

                        // Fast scrollbar overlay for jumping quickly across library
                        FastScrollbarList(
                            listState = listState,
                            books = books,
                            headerItemCount = headerCount,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySectionHeader(
    totalBooks: Int,
    isScanning: Boolean,
    viewMode: LibraryViewMode,
    onToggleView: () -> Unit,
    onScanNow: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "My Library",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = CbzDarkSurfaceVariant
            ) {
                Text(
                    text = "$totalBooks",
                    style = MaterialTheme.typography.labelSmall,
                    color = CbzGoldPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Scan now button
            TextButton(
                onClick = onScanNow,
                enabled = !isScanning,
                modifier = Modifier.testTag("scan_now_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isScanning) CbzTextSecondary else CbzGoldPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Scan Now",
                    fontSize = 12.sp,
                    color = if (isScanning) CbzTextSecondary else CbzGoldPrimary
                )
            }

            // Grid / List View Toggle
            IconButton(
                onClick = onToggleView,
                modifier = Modifier.testTag("view_mode_toggle")
            ) {
                Icon(
                    imageVector = if (viewMode == LibraryViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = "Toggle view",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
