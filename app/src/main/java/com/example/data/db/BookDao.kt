package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY title ASC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books")
    suspend fun getAllBooksSync(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    fun getBookById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookByIdSync(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE filePath = :filePath LIMIT 1")
    suspend fun getBookByPath(filePath: String): BookEntity?

    @Query("SELECT * FROM books WHERE lastOpenedTime > 0 ORDER BY lastOpenedTime DESC LIMIT :limit")
    fun getRecentlyOpened(limit: Int = 20): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavorites(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("UPDATE books SET currentPage = :page, progressPercent = :progress, lastOpenedTime = :timestamp WHERE id = :id")
    suspend fun updateProgress(id: Long, page: Int, progress: Float, timestamp: Long)

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE books SET readingMode = :readingMode, readingDirection = :readingDirection WHERE id = :id")
    suspend fun updateReadingSettings(id: Long, readingMode: String, readingDirection: String)

    @Query("UPDATE books SET coverPath = :coverPath, pageCount = :pageCount WHERE id = :id")
    suspend fun updateCoverAndPageCount(id: Long, coverPath: String?, pageCount: Int)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("DELETE FROM books WHERE filePath = :filePath")
    suspend fun deleteBookByPath(filePath: String)

    @Query("DELETE FROM books WHERE filePath NOT IN (:activePaths)")
    suspend fun deleteMissingBooks(activePaths: List<String>)
}
