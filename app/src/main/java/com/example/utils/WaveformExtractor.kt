package com.example.utils

import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.abs

object WaveformExtractor {
    
    suspend fun extractAmplitudes(context: android.content.Context, uriString: String, numBars: Int = 100): List<Float> {
        return withContext(Dispatchers.IO) {
            val amplitudes = mutableListOf<Float>()
            val extractor = MediaExtractor()
            try {
                val uri = Uri.parse(uriString)
                if (uri.scheme == null) {
                    extractor.setDataSource(uriString)
                } else {
                    extractor.setDataSource(context, uri, null)
                }
                var audioTrackIndex = -1
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    if (mime?.startsWith("audio/") == true) {
                        audioTrackIndex = i
                        break
                    }
                }
                
                if (audioTrackIndex < 0) {
                    return@withContext List(numBars) { 0.1f } // fallback
                }
                
                extractor.selectTrack(audioTrackIndex)
                val format = extractor.getTrackFormat(audioTrackIndex)
                val durationUs = format.getLong(MediaFormat.KEY_DURATION)
                val stepUs = durationUs / numBars
                
                val buffer = ByteBuffer.allocate(8192 * 4) // Read chunks
                for (i in 0 until numBars) {
                    extractor.seekTo(i * stepUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize > 0) {
                        var sum = 0f
                        var count = 0
                        buffer.position(0)
                        val limit = minOf(sampleSize, buffer.capacity())
                        // Rough amplitude estimation from bytes
                        for (j in 0 until limit step 2) {
                            if (j + 1 < limit) {
                                val sample = (buffer.get(j).toInt() and 0xFF) or (buffer.get(j + 1).toInt() shl 8)
                                val shortSample = sample.toShort()
                                sum += abs(shortSample.toFloat())
                                count++
                            }
                        }
                        val avgAmp = if (count > 0) sum / count else 0f
                        // normalize somewhat against max Short (32768)
                        amplitudes.add((avgAmp / 32768f).coerceIn(0.01f, 1f))
                    } else {
                        amplitudes.add(0.01f)
                    }
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext List(numBars) { 0.1f }
            } finally {
                extractor.release()
            }
            
            // Normalize the list so the highest peak is 1.0
            val maxAmp = amplitudes.maxOrNull() ?: 1f
            val safeMax = if (maxAmp > 0f) maxAmp else 1f
            amplitudes.map { (it / safeMax).coerceIn(0.05f, 1f) }
        }
    }
}
