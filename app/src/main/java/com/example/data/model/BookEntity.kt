package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    indices = [Index(value = ["filePath"], unique = true)]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val title: String,
    val fileSize: Long = 0L,
    val lastModified: Long = 0L,
    val pageCount: Int = 0,
    val currentPage: Int = 1,
    val progressPercent: Float = 0f,
    val isFavorite: Boolean = false,
    val lastOpenedTime: Long = 0L,
    val readingMode: String = "DEFAULT", // "VERTICAL", "HORIZONTAL", "DEFAULT"
    val readingDirection: String = "DEFAULT", // "LEFT_TO_RIGHT", "RIGHT_TO_LEFT", "DEFAULT"
    val coverPath: String? = null
)
