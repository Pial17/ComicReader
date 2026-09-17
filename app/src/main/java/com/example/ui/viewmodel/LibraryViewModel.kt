package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cbz.CbzArchiveManager
import com.example.cbz.NaturalOrderComparator
import com.example.data.db.AppDatabase
import com.example.data.model.BookEntity
import com.example.data.model.ScannedFolderEntity
import com.example.data.repository.AppSettingsManager
import com.example.data.repository.BookRepository
import com.example.scanner.CbzScanner
import com.example.ui.model.FilterOption
import com.example.ui.model.LibraryViewMode
import com.example.ui.model.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val repository = BookRepository(database.bookDao(), database.scannedFolderDao())
    val settingsManager = AppSettingsManager(application)
    private val scanner = CbzScanner(application, repository)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterOption = MutableStateFlow(FilterOption.ALL)
    val filterOption: StateFlow<FilterOption> = _filterOption.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanStatusMessage = MutableStateFlow("")
    val scanStatusMessage: StateFlow<String> = _scanStatusMessage.asStateFlow()

    private val _scanFoundCount = MutableStateFlow(0)
    val scanFoundCount: StateFlow<Int> = _scanFoundCount.asStateFlow()

    private val _scanCurrentStep = MutableStateFlow(0)
    val scanCurrentStep: StateFlow<Int> = _scanCurrentStep.asStateFlow()

    private val _scanTotalSteps = MutableStateFlow(0)
    val scanTotalSteps: StateFlow<Int> = _scanTotalSteps.asStateFlow()

    val libraryViewMode: StateFlow<LibraryViewMode> = settingsManager.libraryViewMode
    val sortOrder: StateFlow<SortOrder> = settingsManager.sortOrder

    val folders: StateFlow<List<ScannedFolderEntity>> = repository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All books combined with search, filter, and sort
    val books: StateFlow<List<BookEntity>> = combine(
        repository.allBooks,
        _searchQuery,
        _filterOption,
        sortOrder
    ) { allBooks, query, filter, sort ->
        var list = allBooks

        // 1. Search filtering
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) || it.filePath.lowercase().contains(q)
            }
        }

        // 2. Status filtering
        list = when (filter) {
            FilterOption.ALL -> list
            FilterOption.UNREAD -> list.filter { it.lastOpenedTime == 0L || it.progressPercent == 0f }
            FilterOption.READING -> list.filter { it.progressPercent > 0f && it.progressPercent < 100f }
            FilterOption.COMPLETED -> list.filter { it.progressPercent >= 100f || (it.pageCount > 0 && it.currentPage >= it.pageCount) }
            FilterOption.FAVORITES -> list.filter { it.isFavorite }
        }

        // 3. Sorting
        when (sort) {
            SortOrder.NAME_ASC -> list.sortedWith { a, b -> NaturalOrderComparator.INSTANCE.compare(a.title, b.title) }
            SortOrder.NAME_DESC -> list.sortedWith { a, b -> NaturalOrderComparator.INSTANCE.compare(b.title, a.title) }
            SortOrder.RECENTLY_OPENED -> list.sortedByDescending { it.lastOpenedTime }
            SortOrder.RECENTLY_ADDED -> list.sortedByDescending { it.id }
            SortOrder.PAGE_COUNT -> list.sortedByDescending { it.pageCount }
            SortOrder.FILE_SIZE -> list.sortedByDescending { it.fileSize }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Most recent book for "Continue Reading" banner
    val continueReadingBook: StateFlow<BookEntity?> = repository.recentlyOpened
        .combine(repository.allBooks) { recentlyOpened, all ->
            recentlyOpened.firstOrNull { it.lastOpenedTime > 0L && it.progressPercent > 0f && it.progressPercent < 100f }
                ?: recentlyOpened.firstOrNull { it.lastOpenedTime > 0L }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Automatically scan storage if enabled
        if (settingsManager.autoScanOnStart.value) {
            scanStorage()
        }
    }

    fun scanStorage() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            scanner.scanStorage { progress ->
                _isScanning.value = progress.isScanning
                _scanStatusMessage.value = progress.statusMessage
                _scanFoundCount.value = progress.foundCount
                _scanCurrentStep.value = progress.currentStep
                _scanTotalSteps.value = progress.totalSteps
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterOption(option: FilterOption) {
        _filterOption.value = option
    }

    fun setSortOrder(order: SortOrder) {
        settingsManager.setSortOrder(order)
    }

    fun setLibraryViewMode(mode: LibraryViewMode) {
        settingsManager.setLibraryViewMode(mode)
    }

    fun toggleFavorite(book: BookEntity) {
        viewModelScope.launch {
            repository.updateFavorite(book.id, !book.isFavorite)
        }
    }

    fun importCbz(uri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanStatusMessage.value = "Importing selected CBZ file..."
            val book = scanner.importSingleCbz(uri.toString())
            _isScanning.value = false
            if (book != null) {
                _scanStatusMessage.value = "Imported ${book.title}"
            } else {
                _scanStatusMessage.value = "Failed to import CBZ"
            }
        }
    }

    fun addFolder(treeUri: Uri, displayName: String) {
        viewModelScope.launch {
            repository.insertFolder(
                ScannedFolderEntity(
                    uriString = treeUri.toString(),
                    displayName = displayName
                )
            )
            // Immediately trigger scan on new folder
            scanStorage()
        }
    }

    fun removeFolder(uriString: String) {
        viewModelScope.launch {
            repository.deleteFolder(uriString)
        }
    }

    fun rescanBook(bookId: Long) {
        viewModelScope.launch {
            val book = repository.getBookByIdSync(bookId) ?: return@launch
            val entries = CbzArchiveManager.getPageEntries(getApplication(), book.filePath)
            val cover = CbzArchiveManager.extractThumbnail(getApplication(), book.filePath, entries.firstOrNull()?.entryName)
            repository.updateCoverAndPageCount(bookId, cover, entries.size)
        }
    }

    fun deleteBook(book: BookEntity, deleteFileFromStorage: Boolean) {
        viewModelScope.launch {
            if (deleteFileFromStorage) {
                try {
                    if (book.filePath.startsWith("/")) {
                        val f = File(book.filePath)
                        if (f.exists()) f.delete()
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
            repository.deleteBookById(book.id)
        }
    }

    fun clearThumbnailCache() {
        viewModelScope.launch {
            CbzArchiveManager.clearThumbnailCache(getApplication())
        }
    }

    fun getThumbnailCacheSize(): Long {
        return CbzArchiveManager.getThumbnailCacheSize(getApplication())
    }
}
