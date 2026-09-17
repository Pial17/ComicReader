package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.cbz.NaturalOrderComparator
import com.example.data.db.AppDatabase
import com.example.data.model.BookEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CbzReaderRobolectricTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testNaturalOrderFilenameSorting() {
        val filenames = listOf(
            "page10.jpg",
            "page1.jpg",
            "page2.jpg",
            "page20.jpg",
            "page11.jpg",
            "page0.jpg"
        )
        val sorted = filenames.sortedWith(NaturalOrderComparator.INSTANCE)

        assertEquals(
            listOf(
                "page0.jpg",
                "page1.jpg",
                "page2.jpg",
                "page10.jpg",
                "page11.jpg",
                "page20.jpg"
            ),
            sorted
        )
    }

    @Test
    fun testChapterPageSorting() {
        val pages = listOf(
            "ch1_p10.png",
            "ch1_p1.png",
            "ch1_p2.png",
            "ch2_p1.png",
            "ch10_p1.png"
        )
        val sorted = pages.sortedWith(NaturalOrderComparator.INSTANCE)

        assertEquals(
            listOf(
                "ch1_p1.png",
                "ch1_p2.png",
                "ch1_p10.png",
                "ch2_p1.png",
                "ch10_p1.png"
            ),
            sorted
        )
    }

    @Test
    fun testRoomDatabaseInsertAndUpdateProgress() = runBlocking {
        val bookDao = database.bookDao()
        val book = BookEntity(
            filePath = "/storage/emulated/0/Comics/MangaVol1.cbz",
            title = "Manga Volume 1",
            fileSize = 1024 * 1024 * 45,
            pageCount = 200,
            currentPage = 1,
            progressPercent = 0.5f,
            isFavorite = false
        )

        val id = bookDao.insertBook(book)
        assertTrue(id > 0)

        val loaded = bookDao.getBookByIdSync(id)
        assertEquals("Manga Volume 1", loaded?.title)
        assertEquals(200, loaded?.pageCount)
        assertEquals(1, loaded?.currentPage)

        // Update progress
        bookDao.updateProgress(id, 87, 43.5f, System.currentTimeMillis())
        val updated = bookDao.getBookByIdSync(id)
        assertEquals(87, updated?.currentPage)
        assertEquals(43.5f, updated?.progressPercent ?: 0f, 0.01f)

        // Update favorite
        bookDao.updateFavorite(id, true)
        val favUpdated = bookDao.getBookByIdSync(id)
        assertTrue(favUpdated?.isFavorite == true)

        // Update reading settings
        bookDao.updateReadingSettings(id, "HORIZONTAL", "RIGHT_TO_LEFT")
        val settingsUpdated = bookDao.getBookByIdSync(id)
        assertEquals("HORIZONTAL", settingsUpdated?.readingMode)
        assertEquals("RIGHT_TO_LEFT", settingsUpdated?.readingDirection)

        // Test getAllBooksSync
        val allBooks = bookDao.getAllBooksSync()
        assertEquals(1, allBooks.size)
        assertEquals(id, allBooks[0].id)
    }
}
