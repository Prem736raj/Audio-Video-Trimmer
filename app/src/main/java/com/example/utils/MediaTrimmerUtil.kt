package com.example.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream

object MediaTrimmerUtil {

    /**
     * Trims any source URI string by start/end percentage and writes the output directly to destFile.
     * Uses MediaExtractor and MediaMuxer natively for video (MP4) files, and falls back to byte-range
     * trimming for audio or when muxer is not supported, ensuring incredibly fast and high-quality files.
     */
    fun trimMedia(
        context: Context,
        sourceUriString: String?,
        destFile: File,
        startPct: Float,
        endPct: Float,
        isVideo: Boolean
    ): Boolean {
        if (sourceUriString.isNullOrEmpty()) {
            return false
        }

        val uri = Uri.parse(sourceUriString)
        
        if (isVideo) {
            val success = trimWithMediaMuxer(context, uri, destFile, startPct, endPct)
            if (success) {
                return true
            }
        }

        return trimWithByteRange(context, uri, destFile, startPct, endPct)
    }

    private fun trimWithMediaMuxer(
        context: Context,
        uri: Uri,
        destFile: File,
        startPct: Float,
        endPct: Float
    ): Boolean {
        var extractor: android.media.MediaExtractor? = null
        var muxer: android.media.MediaMuxer? = null
        try {
            extractor = android.media.MediaExtractor()
            if (uri.scheme == null || uri.scheme == "file") {
                extractor.setDataSource(uri.path ?: uri.toString())
            } else {
                extractor.setDataSource(context, uri, null)
            }

            var durationUs: Long = 0
            val trackCount = extractor.trackCount
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                if (format.containsKey(android.media.MediaFormat.KEY_DURATION)) {
                    val trackDuration = format.getLong(android.media.MediaFormat.KEY_DURATION)
                    if (trackDuration > durationUs) {
                        durationUs = trackDuration
                    }
                }
            }

            if (durationUs <= 0) {
                durationUs = 60_000_000L
            }

            val startTimeUs = (durationUs * startPct).toLong()
            val endTimeUs = (durationUs * endPct).toLong()

            muxer = android.media.MediaMuxer(destFile.absolutePath, android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackIndices = IntArray(trackCount) { -1 }
            var hasTracks = false

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    trackIndices[i] = muxer.addTrack(format)
                    hasTracks = true
                }
            }

            if (!hasTracks) {
                return false
            }

            muxer.start()

            // Seek to closest sync frame near start position
            extractor.seekTo(startTimeUs, android.media.MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val maxBufferSize = 2 * 1024 * 1024
            val buffer = java.nio.ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = android.media.MediaCodec.BufferInfo()

            val lastPtsMap = HashMap<Int, Long>()
            val offsetPtsMap = HashMap<Int, Long>()
            var sampleCount = 0

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }

                val presentationTimeUs = extractor.sampleTime
                if (presentationTimeUs > endTimeUs) {
                    break
                }

                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex >= 0 && trackIndices[trackIndex] >= 0) {
                    val originalPts = extractor.sampleTime
                    val offsetPts = offsetPtsMap.getOrPut(trackIndex) { originalPts }
                    var adjustedPts = originalPts - offsetPts
                    
                    val lastPts = lastPtsMap[trackIndex]
                    if (lastPts != null && adjustedPts <= lastPts) {
                        adjustedPts = lastPts + 1000L // Ensure 1ms increment
                    }
                    lastPtsMap[trackIndex] = adjustedPts

                    bufferInfo.presentationTimeUs = adjustedPts
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(trackIndices[trackIndex], buffer, bufferInfo)
                    sampleCount++
                }
                extractor.advance()
            }

            if (sampleCount > 0) {
                muxer.stop()
                return true
            } else {
                return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try {
                extractor?.release()
            } catch (ignored: Exception) {}
            try {
                muxer?.release()
            } catch (ignored: Exception) {}
        }
    }

    private fun trimWithByteRange(
        context: Context,
        uri: Uri,
        destFile: File,
        startPct: Float,
        endPct: Float
    ): Boolean {
        try {
            val totalLength: Long
            var input: InputStream? = null
            
            if (uri.scheme == "file" || uri.scheme.isNullOrEmpty()) {
                val file = File(uri.path ?: uri.toString())
                if (file.exists()) {
                    totalLength = file.length()
                    input = file.inputStream()
                } else {
                    return false
                }
            } else {
                var queriedLength = -1L
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (sizeIndex != -1 && cursor.moveToFirst()) {
                            queriedLength = cursor.getLong(sizeIndex)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (queriedLength > 0) {
                    totalLength = queriedLength
                    input = context.contentResolver.openInputStream(uri)
                } else {
                    val temp = File(context.cacheDir, "measuring_temp_${System.currentTimeMillis()}")
                    context.contentResolver.openInputStream(uri)?.use { inStream ->
                        temp.outputStream().use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    totalLength = temp.length()
                    input = temp.inputStream()
                }
            }

            if (input == null) {
                return false
            }

            val startByte = (totalLength * startPct.coerceIn(0f, 1f)).toLong()
            val endByte = (totalLength * endPct.coerceIn(0f, 1f)).toLong()
            val targetBytesToCopy = (endByte - startByte).coerceAtLeast(0)

            destFile.outputStream().use { output ->
                input.use { inStream ->
                    var skipped = 0L
                    while (skipped < startByte) {
                        val currentSkipped = inStream.skip(startByte - skipped)
                        if (currentSkipped <= 0) {
                            break
                        }
                        skipped += currentSkipped
                    }

                    val buffer = ByteArray(8192)
                    var bytesRemaining = targetBytesToCopy
                    while (bytesRemaining > 0) {
                        val toRead = java.lang.Math.min(buffer.size.toLong(), bytesRemaining).toInt()
                        val read = inStream.read(buffer, 0, toRead)
                        if (read == -1) {
                            break
                        }
                        output.write(buffer, 0, read)
                        bytesRemaining -= read
                    }
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
