package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.model.LibraryViewMode
import com.example.ui.model.PageFit
import com.example.ui.model.ReadingDirection
import com.example.ui.model.ReadingMode
import com.example.ui.model.SortOrder
import com.example.ui.model.ThemeSetting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cbz_reader_prefs", Context.MODE_PRIVATE)

    private val _readingMode = MutableStateFlow(loadReadingMode())
    val readingMode: StateFlow<ReadingMode> = _readingMode.asStateFlow()

    private val _readingDirection = MutableStateFlow(loadReadingDirection())
    val readingDirection: StateFlow<ReadingDirection> = _readingDirection.asStateFlow()

    private val _pageFit = MutableStateFlow(loadPageFit())
    val pageFit: StateFlow<PageFit> = _pageFit.asStateFlow()

    private val _libraryViewMode = MutableStateFlow(loadLibraryViewMode())
    val libraryViewMode: StateFlow<LibraryViewMode> = _libraryViewMode.asStateFlow()

    private val _sortOrder = MutableStateFlow(loadSortOrder())
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _autoScanOnStart = MutableStateFlow(prefs.getBoolean(KEY_AUTO_SCAN, true))
    val autoScanOnStart: StateFlow<Boolean> = _autoScanOnStart.asStateFlow()

    private val _fullscreenReading = MutableStateFlow(prefs.getBoolean(KEY_FULLSCREEN, true))
    val fullscreenReading: StateFlow<Boolean> = _fullscreenReading.asStateFlow()

    private val _themeSetting = MutableStateFlow(loadThemeSetting())
    val themeSetting: StateFlow<ThemeSetting> = _themeSetting.asStateFlow()

    fun setReadingMode(mode: ReadingMode) {
        prefs.edit().putString(KEY_READING_MODE, mode.name).apply()
        _readingMode.value = mode
    }

    fun setReadingDirection(direction: ReadingDirection) {
        prefs.edit().putString(KEY_READING_DIRECTION, direction.name).apply()
        _readingDirection.value = direction
    }

    fun setPageFit(fit: PageFit) {
        prefs.edit().putString(KEY_PAGE_FIT, fit.name).apply()
        _pageFit.value = fit
    }

    fun setLibraryViewMode(mode: LibraryViewMode) {
        prefs.edit().putString(KEY_LIBRARY_VIEW, mode.name).apply()
        _libraryViewMode.value = mode
    }

    fun setSortOrder(order: SortOrder) {
        prefs.edit().putString(KEY_SORT_ORDER, order.name).apply()
        _sortOrder.value = order
    }

    fun setAutoScanOnStart(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SCAN, enabled).apply()
        _autoScanOnStart.value = enabled
    }

    fun setFullscreenReading(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FULLSCREEN, enabled).apply()
        _fullscreenReading.value = enabled
    }

    fun setThemeSetting(theme: ThemeSetting) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        _themeSetting.value = theme
    }

    private fun loadReadingMode(): ReadingMode {
        val name = prefs.getString(KEY_READING_MODE, ReadingMode.VERTICAL.name)
        return try {
            ReadingMode.valueOf(name ?: ReadingMode.VERTICAL.name)
        } catch (e: Exception) {
            ReadingMode.VERTICAL
        }
    }

    private fun loadReadingDirection(): ReadingDirection {
        val name = prefs.getString(KEY_READING_DIRECTION, ReadingDirection.LEFT_TO_RIGHT.name)
        return try {
            ReadingDirection.valueOf(name ?: ReadingDirection.LEFT_TO_RIGHT.name)
        } catch (e: Exception) {
            ReadingDirection.LEFT_TO_RIGHT
        }
    }

    private fun loadPageFit(): PageFit {
        val name = prefs.getString(KEY_PAGE_FIT, PageFit.FIT_WIDTH.name)
        return try {
            PageFit.valueOf(name ?: PageFit.FIT_WIDTH.name)
        } catch (e: Exception) {
            PageFit.FIT_WIDTH
        }
    }

    private fun loadLibraryViewMode(): LibraryViewMode {
        val name = prefs.getString(KEY_LIBRARY_VIEW, LibraryViewMode.GRID.name)
        return try {
            LibraryViewMode.valueOf(name ?: LibraryViewMode.GRID.name)
        } catch (e: Exception) {
            LibraryViewMode.GRID
        }
    }

    private fun loadSortOrder(): SortOrder {
        val name = prefs.getString(KEY_SORT_ORDER, SortOrder.RECENTLY_OPENED.name)
        return try {
            SortOrder.valueOf(name ?: SortOrder.RECENTLY_OPENED.name)
        } catch (e: Exception) {
            SortOrder.RECENTLY_OPENED
        }
    }

    private fun loadThemeSetting(): ThemeSetting {
        val name = prefs.getString(KEY_THEME, ThemeSetting.DARK.name)
        return try {
            ThemeSetting.valueOf(name ?: ThemeSetting.DARK.name)
        } catch (e: Exception) {
            ThemeSetting.DARK
        }
    }

    companion object {
        private const val KEY_READING_MODE = "pref_reading_mode"
        private const val KEY_READING_DIRECTION = "pref_reading_direction"
        private const val KEY_PAGE_FIT = "pref_page_fit"
        private const val KEY_LIBRARY_VIEW = "pref_library_view"
        private const val KEY_SORT_ORDER = "pref_sort_order"
        private const val KEY_AUTO_SCAN = "pref_auto_scan"
        private const val KEY_FULLSCREEN = "pref_fullscreen"
        private const val KEY_THEME = "pref_theme"
    }
}
