package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ScannedFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedFolderDao {
    @Query("SELECT * FROM scanned_folders ORDER BY addedDate ASC")
    fun getAllFolders(): Flow<List<ScannedFolderEntity>>

    @Query("SELECT * FROM scanned_folders")
    suspend fun getAllFoldersList(): List<ScannedFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: ScannedFolderEntity)

    @Query("DELETE FROM scanned_folders WHERE uriString = :uriString")
    suspend fun deleteFolder(uriString: String)
}
