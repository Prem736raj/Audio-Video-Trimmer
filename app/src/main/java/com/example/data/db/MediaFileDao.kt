package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MediaFile
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaFileDao {
    @Query("SELECT * FROM media_files ORDER BY timestamp DESC")
    fun getAllMediaFiles(): Flow<List<MediaFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaFile(file: MediaFile)

    @Query("DELETE FROM media_files WHERE id = :id")
    suspend fun deleteMediaFileById(id: Int)

    @Query("SELECT * FROM media_files WHERE id = :id LIMIT 1")
    suspend fun getMediaFileById(id: Int): MediaFile?

    @Query("SELECT COUNT(*) FROM media_files WHERE isSample = 1")
    suspend fun getSampleFilesCount(): Int

    @Query("DELETE FROM media_files WHERE isSample = 1")
    suspend fun deleteSampleFiles()

    @Query("UPDATE media_files SET fileName = :newName WHERE id = :id")
    suspend fun renameMediaFile(id: Int, newName: String)
}
