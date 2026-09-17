package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.collection.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cbz.CbzArchiveManager
import com.example.cbz.CbzPageEntry
import com.example.data.db.AppDatabase
import com.example.data.model.BookEntity
import com.example.data.repository.AppSettingsManager
import com.example.data.repository.BookRepository
import com.example.ui.model.PageFit
import com.example.ui.model.ReadingDirection
import com.example.ui.model.ReadingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = BookRepository(database.bookDao(), database.scannedFolderDao())
    private val settingsManager = AppSettingsManager(application)

    private val _book = MutableStateFlow<BookEntity?>(null)
    val book: StateFlow<BookEntity?> = _book.asStateFlow()

    private val _pages = MutableStateFlow<List<CbzPageEntry>>(emptyList())
    val pages: StateFlow<List<CbzPageEntry>> = _pages.asStateFlow()

    private val _currentPage = MutableStateFlow(1) // 1-based index
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _readingMode = MutableStateFlow(ReadingMode.VERTICAL)
    val readingMode: StateFlow<ReadingMode> = _readingMode.asStateFlow()

    private val _readingDirection = MutableStateFlow(ReadingDirection.LEFT_TO_RIGHT)
    val readingDirection: StateFlow<ReadingDirection> = _readingDirection.asStateFlow()

    private val _pageFit = MutableStateFlow(PageFit.FIT_WIDTH)
    val pageFit: StateFlow<PageFit> = _pageFit.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showControls = MutableStateFlow(false)
    val showControls: StateFlow<Boolean> = _showControls.asStateFlow()

    // In-memory LRU Bitmap cache for fast, memory-safe reading (keyed by bookId_pageIndex)
    private val maxCacheSize = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt().coerceAtLeast(16 * 1024) // 1/8th of RAM in KB
    private val bitmapCache = object : LruCache<String, Bitmap>(maxCacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    private var loadJob: Job? = null
    private var autoHideControlsJob: Job? = null
    private var saveProgressJob: Job? = null

    /**
     * Resets and closes any open book, clearing cache and stopping background jobs.
     */
    fun closeBook() {
        loadJob?.cancel()
        saveProgressJob?.cancel()
        autoHideControlsJob?.cancel()
        bitmapCache.evictAll()
        _book.value = null
        _pages.value = emptyList()
        _currentPage.value = 1
        _isLoading.value = true
        _errorMessage.value = null
        _showControls.value = false
    }

    fun loadBook(bookId: Long, forceStartPage: Int? = null) {
        // Synchronously reset previous state to avoid any stale page/bitmap leakage
        loadJob?.cancel()
        saveProgressJob?.cancel()
        autoHideControlsJob?.cancel()
        bitmapCache.evictAll()
        _book.value = null
        _pages.value = emptyList()
        _currentPage.value = forceStartPage ?: 1
        _isLoading.value = true
        _errorMessage.value = null
        _showControls.value = false

        loadJob = viewModelScope.launch {
            try {
                val loadedBook = repository.getBookByIdSync(bookId)
                if (loadedBook == null) {
                    _errorMessage.value = "Comic not found in library."
                    _isLoading.value = false
                    return@launch
                }

                // Determine reading mode: check book's specific setting or fallback to global settings
                val mode = when (loadedBook.readingMode) {
                    ReadingMode.VERTICAL.name -> ReadingMode.VERTICAL
                    ReadingMode.HORIZONTAL.name -> ReadingMode.HORIZONTAL
                    else -> settingsManager.readingMode.value
                }
                _readingMode.value = mode

                // Reading direction
                val direction = when (loadedBook.readingDirection) {
                    ReadingDirection.LEFT_TO_RIGHT.name -> ReadingDirection.LEFT_TO_RIGHT
                    ReadingDirection.RIGHT_TO_LEFT.name -> ReadingDirection.RIGHT_TO_LEFT
                    else -> settingsManager.readingDirection.value
                }
                _readingDirection.value = direction
                _pageFit.value = settingsManager.pageFit.value

                // Load pages from CBZ
                val entries = CbzArchiveManager.getPageEntries(getApplication(), loadedBook.filePath)
                if (entries.isEmpty()) {
                    _errorMessage.value = "Unable to read this CBZ file. No supported images found."
                    _isLoading.value = false
                    return@launch
                }

                // Set current page accurately
                val targetPage = forceStartPage ?: loadedBook.currentPage.coerceIn(1, entries.size)
                _currentPage.value = targetPage
                _book.value = loadedBook
                _pages.value = entries

                // Update page count in DB if not set
                if (loadedBook.pageCount != entries.size) {
                    repository.updateCoverAndPageCount(
                        loadedBook.id,
                        loadedBook.coverPath ?: CbzArchiveManager.extractThumbnail(getApplication(), loadedBook.filePath, entries.first().entryName),
                        entries.size
                    )
                }

                _isLoading.value = false

                // Preload current, next, and previous pages for this book
                preloadPages(loadedBook.id, targetPage - 1)

                // Show controls briefly on open so user sees context, then auto-hide
                showControlsWithTimeout()
            } catch (e: Exception) {
                _errorMessage.value = "Error opening comic: ${e.localizedMessage}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Retrieves or decodes the bitmap for a specific 0-based page index of a book.
     */
    suspend fun getPageBitmap(bookId: Long, pageIndex: Int): Bitmap? = withContext(Dispatchers.IO) {
        val currentBook = _book.value
        if (currentBook == null || currentBook.id != bookId) return@withContext null

        val cacheKey = "${bookId}_$pageIndex"
        val cached = bitmapCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            return@withContext cached
        }

        val entries = _pages.value
        if (pageIndex !in entries.indices) return@withContext null
        val bookPath = currentBook.filePath

        val entry = entries[pageIndex]
        val bitmap = CbzArchiveManager.loadPageBitmap(getApplication(), bookPath, entry.entryName)
        if (bitmap != null) {
            bitmapCache.put(cacheKey, bitmap)
        }
        bitmap
    }

    fun onPageChanged(pageOneBased: Int) {
        val currentBook = _book.value ?: return
        if (_isLoading.value) return
        val total = _pages.value.size
        if (total == 0) return
        val clamped = pageOneBased.coerceIn(1, total)
        if (_currentPage.value != clamped) {
            _currentPage.value = clamped
            preloadPages(currentBook.id, clamped - 1)
            scheduleSaveProgress(clamped)
        }
    }

    private fun preloadPages(bookId: Long, centerIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentBook = _book.value
            if (currentBook == null || currentBook.id != bookId) return@launch
            val total = _pages.value.size
            val indicesToPreload = listOf(centerIndex, centerIndex + 1, centerIndex - 1, centerIndex + 2)
            for (idx in indicesToPreload) {
                if (idx in 0 until total && bitmapCache.get("${bookId}_$idx") == null) {
                    getPageBitmap(bookId, idx)
                }
            }
        }
    }

    private fun scheduleSaveProgress(page: Int) {
        saveProgressJob?.cancel()
        saveProgressJob = viewModelScope.launch {
            delay(500) // Debounce rapid scrolling
            val currentBook = _book.value ?: return@launch
            val total = _pages.value.size
            if (total > 0) {
                val progress = (page.toFloat() / total.toFloat()) * 100f
                repository.updateProgress(currentBook.id, page, progress)
            }
        }
    }

    fun toggleControls() {
        if (_showControls.value) {
            hideControls()
        } else {
            showControlsWithTimeout()
        }
    }

    fun showControlsWithTimeout() {
        _showControls.value = true
        autoHideControlsJob?.cancel()
        autoHideControlsJob = viewModelScope.launch {
            delay(4000)
            _showControls.value = false
        }
    }

    fun hideControls() {
        autoHideControlsJob?.cancel()
        _showControls.value = false
    }

    fun setReadingMode(mode: ReadingMode) {
        _readingMode.value = mode
        val currentBook = _book.value ?: return
        viewModelScope.launch {
            repository.updateReadingSettings(currentBook.id, mode.name, _readingDirection.value.name)
        }
    }

    fun setReadingDirection(direction: ReadingDirection) {
        _readingDirection.value = direction
        val currentBook = _book.value ?: return
        viewModelScope.launch {
            repository.updateReadingSettings(currentBook.id, _readingMode.value.name, direction.name)
        }
    }

    fun setPageFit(fit: PageFit) {
        _pageFit.value = fit
        settingsManager.setPageFit(fit)
    }

    override fun onCleared() {
        super.onCleared()
        autoHideControlsJob?.cancel()
        saveProgressJob?.cancel()
        bitmapCache.evictAll()
    }
}
