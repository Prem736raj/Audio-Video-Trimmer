package com.example.data.repository

import com.example.data.db.MediaFileDao
import com.example.data.model.MediaFile
import kotlinx.coroutines.flow.Flow

class MediaRepository(private val mediaFileDao: MediaFileDao) {
    val allMediaFiles: Flow<List<MediaFile>> = mediaFileDao.getAllMediaFiles()

    suspend fun insertMediaFile(file: MediaFile) = mediaFileDao.insertMediaFile(file)

    suspend fun deleteMediaFileById(id: Int) = mediaFileDao.deleteMediaFileById(id)

    suspend fun getMediaFileById(id: Int): MediaFile? = mediaFileDao.getMediaFileById(id)

    suspend fun getSampleFilesCount(): Int = mediaFileDao.getSampleFilesCount()

    suspend fun deleteSampleFiles() = mediaFileDao.deleteSampleFiles()

    suspend fun renameMediaFile(id: Int, newName: String) = mediaFileDao.renameMediaFile(id, newName)
}
