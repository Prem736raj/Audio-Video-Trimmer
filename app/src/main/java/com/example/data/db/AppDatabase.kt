package com.example.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.model.MediaFile

@Database(entities = [MediaFile::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaFileDao(): MediaFileDao
}
