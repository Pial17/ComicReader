package com.example.ui.model

enum class ReadingMode(val label: String) {
    VERTICAL("Vertical Scroll"),
    HORIZONTAL("Horizontal / Page-by-Page")
}

enum class ReadingDirection(val label: String) {
    LEFT_TO_RIGHT("Left → Right"),
    RIGHT_TO_LEFT("Right → Left (Manga)")
}

enum class PageFit(val label: String) {
    FIT_WIDTH("Fit Width"),
    FIT_HEIGHT("Fit Height"),
    AUTO("Auto"),
    ORIGINAL("Original (100%)")
}

enum class LibraryViewMode {
    GRID,
    LIST
}

enum class SortOrder(val label: String) {
    NAME_ASC("Name (A → Z)"),
    NAME_DESC("Name (Z → A)"),
    RECENTLY_OPENED("Recently Opened"),
    RECENTLY_ADDED("Recently Added"),
    PAGE_COUNT("Page Count"),
    FILE_SIZE("File Size")
}

enum class FilterOption(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    READING("Reading"),
    COMPLETED("Completed"),
    FAVORITES("Favorites")
}

enum class ThemeSetting(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System")
}
