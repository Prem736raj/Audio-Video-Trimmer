package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_files")
data class MediaFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val fileType: String, // "audio" or "video"
    val format: String, // "MP3", "MP4", "M4A", etc.
    val duration: String, // e.g. "03:45"
    val fileSize: String, // e.g. "3.6 MB"
    val timestamp: Long = System.currentTimeMillis(),
    val isSample: Boolean = false,
    val customThumbnailId: Int = 0, // Reference for custom visual thumbnails seen in the UI
    val sourceUri: String? = null,
    val startValue: Float = 0.0f,
    val endValue: Float = 1.0f
)
