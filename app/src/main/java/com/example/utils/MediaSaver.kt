package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object MediaSaver {

    /**
     * Saves a media file from private app storage (sandboxed) to the device's public 
     * media store directories (Movies/MediaTrimmer or Music/MediaTrimmer) so that other
     * apps like system Gallery or Music Player can immediately scan, list, and play them.
     */
    fun saveToPublicMedia(context: Context, srcFile: File, isVideo: Boolean): Uri? {
        val resolver = context.contentResolver
        val mimeType = if (isVideo) "video/mp4" else "audio/mpeg"
        val displayName = srcFile.name

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, if (isVideo) Environment.DIRECTORY_MOVIES + "/MediaTrimmer" else Environment.DIRECTORY_MUSIC + "/MediaTrimmer")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collectionUri = if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            try {
                val insertedUri = resolver.insert(collectionUri, contentValues)
                if (insertedUri != null) {
                    resolver.openOutputStream(insertedUri)?.use { output ->
                        srcFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(insertedUri, contentValues, null, null)
                    return insertedUri
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Legacy Android
            try {
                val targetDir = Environment.getExternalStoragePublicDirectory(
                    if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC
                )
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                val destFile = File(targetDir, displayName)
                srcFile.inputStream().use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                // Trigger media scanner so it appears immediately in system database
                MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf(mimeType), null)
                return Uri.fromFile(destFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }
}
