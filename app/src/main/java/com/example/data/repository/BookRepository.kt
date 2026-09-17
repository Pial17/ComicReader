package com.example.data.repository

import com.example.data.db.BookDao
import com.example.data.db.ScannedFolderDao
import com.example.data.model.BookEntity
import com.example.data.model.ScannedFolderEntity
import kotlinx.coroutines.flow.Flow

class BookRepository(
    private val bookDao: BookDao,
    private val scannedFolderDao: ScannedFolderDao
) {
    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()
    val recentlyOpened: Flow<List<BookEntity>> = bookDao.getRecentlyOpened(20)
    val favorites: Flow<List<BookEntity>> = bookDao.getFavorites()
    val folders: Flow<List<ScannedFolderEntity>> = scannedFolderDao.getAllFolders()

    suspend fun getAllBooksSync(): List<BookEntity> = bookDao.getAllBooksSync()

    fun getBookById(id: Long): Flow<BookEntity?> = bookDao.getBookById(id)

    suspend fun getBookByIdSync(id: Long): BookEntity? = bookDao.getBookByIdSync(id)

    suspend fun getBookByPath(filePath: String): BookEntity? = bookDao.getBookByPath(filePath)

    suspend fun insertBook(book: BookEntity): Long = bookDao.insertBook(book)

    suspend fun insertBooks(books: List<BookEntity>) = bookDao.insertBooks(books)

    suspend fun updateBook(book: BookEntity) = bookDao.updateBook(book)

    suspend fun updateProgress(id: Long, page: Int, progress: Float) {
        bookDao.updateProgress(id, page, progress, System.currentTimeMillis())
    }

    suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
        bookDao.updateFavorite(id, isFavorite)
    }

    suspend fun updateReadingSettings(id: Long, readingMode: String, readingDirection: String) {
        bookDao.updateReadingSettings(id, readingMode, readingDirection)
    }

    suspend fun updateCoverAndPageCount(id: Long, coverPath: String?, pageCount: Int) {
        bookDao.updateCoverAndPageCount(id, coverPath, pageCount)
    }

    suspend fun deleteBookById(id: Long) = bookDao.deleteBookById(id)

    suspend fun deleteBookByPath(filePath: String) = bookDao.deleteBookByPath(filePath)

    suspend fun insertFolder(folder: ScannedFolderEntity) = scannedFolderDao.insertFolder(folder)

    suspend fun deleteFolder(uriString: String) = scannedFolderDao.deleteFolder(uriString)

    suspend fun getAllFoldersList(): List<ScannedFolderEntity> = scannedFolderDao.getAllFoldersList()
}
