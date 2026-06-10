package com.example.utils

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

object FFmpegTranscoder {

    suspend fun transcodeAndTrim(
        context: Context,
        sourceUri: String,
        outputFilePath: String,
        startTimeSec: Double,
        durationSec: Double,
        isAudioOnly: Boolean
    ): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            val outFile = File(outputFilePath)
            if (outFile.exists()) outFile.delete()

            val startMs = (startTimeSec * 1000).toLong()
            val endMs = ((startTimeSec + durationSec) * 1000).toLong()
            
            val mediaItem = MediaItem.Builder()
                .setUri(sourceUri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build()
                )
                .build()

            val editedMediaItem = EditedMediaItem.Builder(mediaItem).apply {
                if (isAudioOnly) {
                    setRemoveVideo(true)
                }
            }.build()

            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        exportException.printStackTrace()
                        if (continuation.isActive) continuation.resume(false)
                    }
                })
                .build()

            transformer.start(editedMediaItem, outputFilePath)

            continuation.invokeOnCancellation {
                transformer.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (continuation.isActive) continuation.resume(false)
        }
    }
}
