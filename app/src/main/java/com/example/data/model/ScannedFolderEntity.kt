package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_folders")
data class ScannedFolderEntity(
    @PrimaryKey
    val uriString: String,
    val displayName: String,
    val addedDate: Long = System.currentTimeMillis()
)
