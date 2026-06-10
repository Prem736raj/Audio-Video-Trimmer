package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MediaFile
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MediaViewModel(private val repository: MediaRepository) : ViewModel() {

    val allMediaFiles: StateFlow<List<MediaFile>> = repository.allMediaFiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentPlayingFileId = MutableStateFlow<Int?>(null)
    val currentPlayingFileId: StateFlow<Int?> = _currentPlayingFileId.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // Shared Intent Flow (for incoming external files)
    private val _sharedMediaUri = MutableStateFlow<Pair<String, String>?>(null)
    val sharedMediaUri: StateFlow<Pair<String, String>?> = _sharedMediaUri.asStateFlow()

    fun handleSharedMedia(uri: String, type: String) {
        _sharedMediaUri.value = Pair(uri, type)
    }

    fun consumeSharedMedia() {
        _sharedMediaUri.value = null
    }

    // Audio Trimmer Live States
    private val _audioStart = MutableStateFlow(0.0f)
    val audioStart: StateFlow<Float> = _audioStart.asStateFlow()

    private val _audioEnd = MutableStateFlow(1.0f)
    val audioEnd: StateFlow<Float> = _audioEnd.asStateFlow()

    private val _audioSpeed = MutableStateFlow(1.0f)
    val audioSpeed: StateFlow<Float> = _audioSpeed.asStateFlow()

    private val _audioVolume = MutableStateFlow(1.0f)
    val audioVolume: StateFlow<Float> = _audioVolume.asStateFlow()

    private val _audioFormat = MutableStateFlow("MP3")
    val audioFormat: StateFlow<String> = _audioFormat.asStateFlow()

    private val _audioFadeIn = MutableStateFlow(false)
    val audioFadeIn: StateFlow<Boolean> = _audioFadeIn.asStateFlow()

    private val _audioFadeOut = MutableStateFlow(false)
    val audioFadeOut: StateFlow<Boolean> = _audioFadeOut.asStateFlow()

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

    private val _audioPlaybackProgress = MutableStateFlow(0.0f)
    val audioPlaybackProgress: StateFlow<Float> = _audioPlaybackProgress.asStateFlow()

    // Video Trimmer Live States
    private val _videoStart = MutableStateFlow(0.0f)
    val videoStart: StateFlow<Float> = _videoStart.asStateFlow()

    private val _videoEnd = MutableStateFlow(1.0f)
    val videoEnd: StateFlow<Float> = _videoEnd.asStateFlow()

    private val _videoResolution = MutableStateFlow("1080p")
    val videoResolution: StateFlow<String> = _videoResolution.asStateFlow()

    private val _videoFormat = MutableStateFlow("MP4")
    val videoFormat: StateFlow<String> = _videoFormat.asStateFlow()

    private val _videoQuality = MutableStateFlow("High")
    val videoQuality: StateFlow<String> = _videoQuality.asStateFlow()

    private val _isVideoMuted = MutableStateFlow(false)
    val isVideoMuted: StateFlow<Boolean> = _isVideoMuted.asStateFlow()

    private val _isVideoPlaying = MutableStateFlow(false)
    val isVideoPlaying: StateFlow<Boolean> = _isVideoPlaying.asStateFlow()

    private val _videoPlaybackProgress = MutableStateFlow(0.0f)
    val videoPlaybackProgress: StateFlow<Float> = _videoPlaybackProgress.asStateFlow()

    init {
        prepopulateSamples()
        startPlaybackSimulationLoop()
    }

    private fun prepopulateSamples() {
        viewModelScope.launch {
            // Guarantee a completely clean start by deleting any existing sample files
            repository.deleteSampleFiles()
        }
    }

    private fun startPlaybackSimulationLoop() {
        viewModelScope.launch {
            while (true) {
                delay(100)
                if (_isAudioPlaying.value) {
                    val current = _audioPlaybackProgress.value + 0.01f * _audioSpeed.value
                    if (current > _audioEnd.value) {
                        _audioPlaybackProgress.value = _audioStart.value
                    } else {
                        _audioPlaybackProgress.value = current
                    }
                }
                if (_isVideoPlaying.value) {
                    val current = _videoPlaybackProgress.value + 0.008f
                    if (current > _videoEnd.value) {
                        _videoPlaybackProgress.value = _videoStart.value
                    } else {
                        _videoPlaybackProgress.value = current
                    }
                }
            }
        }
    }

    fun togglePlayRecentFile(fileId: Int) {
        if (_currentPlayingFileId.value == fileId) {
            _currentPlayingFileId.value = null
        } else {
            _currentPlayingFileId.value = fileId
            viewModelScope.launch {
                // Diminish or stop current playing simulator inside components
                delay(12000) // Auto-stop after 12s simulated playback
                if (_currentPlayingFileId.value == fileId) {
                    _currentPlayingFileId.value = null
                }
            }
        }
    }

    fun togglePremium() {
        _isPremium.value = !_isPremium.value
    }

    // Audio State Controllers
    fun setAudioStart(value: Float) {
        if (value < _audioEnd.value - 0.05f) {
            _audioStart.value = value
            if (_audioPlaybackProgress.value < value) {
                _audioPlaybackProgress.value = value
            }
        }
    }

    fun setAudioEnd(value: Float) {
        if (value > _audioStart.value + 0.05f) {
            _audioEnd.value = value
            if (_audioPlaybackProgress.value > value) {
                _audioPlaybackProgress.value = _audioStart.value
            }
        }
    }

    fun setAudioSpeed(value: Float) { _audioSpeed.value = value }
    fun setAudioVolume(value: Float) { _audioVolume.value = value }
    fun setAudioFormat(format: String) { _audioFormat.value = format }
    fun setAudioFadeIn(enabled: Boolean) { _audioFadeIn.value = enabled }
    fun setAudioFadeOut(enabled: Boolean) { _audioFadeOut.value = enabled }
    fun toggleAudioPlaying() { _isAudioPlaying.value = !_isAudioPlaying.value }

    // Video State Controllers
    fun setVideoStart(value: Float) {
        if (value < _videoEnd.value - 0.05f) {
            _videoStart.value = value
            if (_videoPlaybackProgress.value < value) {
                _videoPlaybackProgress.value = value
            }
        }
    }

    fun setVideoEnd(value: Float) {
        if (value > _videoStart.value + 0.05f) {
            _videoEnd.value = value
            if (_videoPlaybackProgress.value > value) {
                _videoPlaybackProgress.value = _videoStart.value
            }
        }
    }

    fun setVideoResolution(res: String) { _videoResolution.value = res }
    fun setVideoFormat(format: String) { _videoFormat.value = format }
    fun setVideoQuality(qty: String) { _videoQuality.value = qty }
    fun setVideoMuted(muted: Boolean) { _isVideoMuted.value = muted }
    fun toggleVideoPlaying() { _isVideoPlaying.value = !_isVideoPlaying.value }
    fun setVideoPlaybackProgress(value: Float) { _videoPlaybackProgress.value = value }

    fun resetAudioTrimmer() {
        _audioStart.value = 0.0f
        _audioEnd.value = 1.0f
        _audioPlaybackProgress.value = 0.0f
        _isAudioPlaying.value = false
    }

    fun resetVideoTrimmer() {
        _videoStart.value = 0.0f
        _videoEnd.value = 1.0f
        _videoPlaybackProgress.value = 0.0f
        _isVideoPlaying.value = false
    }

    // Action execution
    fun trimAudio(originalName: String, originalDurationSec: Int, originalSizeMb: Double, suffix: String, sourceUri: String? = null, startVal: Float? = null, endVal: Float? = null) {
        val span = (_audioEnd.value - _audioStart.value)
        val finalSec = (originalDurationSec * span).toInt()
        val finalMb = (originalSizeMb * span)
        val formattedDuration = String.format("%02d:%02d", finalSec / 60, finalSec % 60)
        val formattedSize = String.format("%.1f MB", finalMb)

        val trimmedName = "trim_$originalName"

        val originalFile = allMediaFiles.value.firstOrNull {
            it.fileName == originalName || (!sourceUri.isNullOrEmpty() && it.sourceUri == sourceUri)
        }
        val hash = if (!sourceUri.isNullOrEmpty()) {
            kotlin.math.abs(sourceUri.hashCode())
        } else {
            kotlin.math.abs(originalName.hashCode())
        }
        val thumbnailId = originalFile?.customThumbnailId ?: ((hash % 5) + 1)

        viewModelScope.launch {
            repository.insertMediaFile(
                MediaFile(
                    fileName = trimmedName,
                    fileType = "audio",
                    format = _audioFormat.value,
                    duration = formattedDuration,
                    fileSize = formattedSize,
                    customThumbnailId = thumbnailId,
                    sourceUri = sourceUri,
                    startValue = 0.0f,
                    endValue = 1.0f
                )
            )
        }
    }

    fun trimVideo(originalName: String, originalDurationSec: Int, originalSizeMb: Double, sourceUri: String? = null, startVal: Float? = null, endVal: Float? = null) {
        val span = (_videoEnd.value - _videoStart.value)
        val finalSec = (originalDurationSec * span).toInt()
        val finalMb = (originalSizeMb * span)
        val formattedDuration = String.format("%02d:%02d", finalSec / 60, finalSec % 60)
        val formattedSize = String.format("%.1f MB", finalMb)

        val trimmedName = "trim_$originalName"

        val originalFile = allMediaFiles.value.firstOrNull {
            it.fileName == originalName || (!sourceUri.isNullOrEmpty() && it.sourceUri == sourceUri)
        }
        val hash = if (!sourceUri.isNullOrEmpty()) {
            kotlin.math.abs(sourceUri.hashCode())
        } else {
            kotlin.math.abs(originalName.hashCode())
        }
        val thumbnailId = originalFile?.customThumbnailId ?: ((hash % 5) + 1)

        viewModelScope.launch {
            repository.insertMediaFile(
                MediaFile(
                    fileName = trimmedName,
                    fileType = "video",
                    format = _videoFormat.value,
                    duration = formattedDuration,
                    fileSize = formattedSize,
                    customThumbnailId = thumbnailId,
                    sourceUri = sourceUri,
                    startValue = 0.0f,
                    endValue = 1.0f
                )
            )
        }
    }

    fun splitVideo(
        originalName: String,
        originalDurationSec: Int,
        originalSizeMb: Double,
        splitRatio: Float,
        part1Uri: String? = null,
        part2Uri: String? = null
    ) {
        val baseName = if (originalName.contains(".")) {
            originalName.substringBeforeLast(".")
        } else {
            originalName
        }
        val extension = if (originalName.contains(".")) {
            originalName.substringAfterLast(".")
        } else {
            "mp4"
        }

        val startRatio = _videoStart.value
        val endRatio = _videoEnd.value

        val firstSpan = (splitRatio - startRatio).coerceAtLeast(0.05f)
        val firstSec = (originalDurationSec * firstSpan).toInt().coerceAtLeast(1)
        val firstMb = originalSizeMb * firstSpan
        val firstDuration = String.format("%02d:%02d", firstSec / 60, firstSec % 60)
        val firstSizeStr = String.format("%.1f MB", firstMb)

        val secondSpan = (endRatio - splitRatio).coerceAtLeast(0.05f)
        val secondSec = (originalDurationSec * secondSpan).toInt().coerceAtLeast(1)
        val secondMb = originalSizeMb * secondSpan
        val secondDuration = String.format("%02d:%02d", secondSec / 60, secondSec % 60)
        val secondSizeStr = String.format("%.1f MB", secondMb)

        val originalFile = allMediaFiles.value.firstOrNull {
            it.fileName == originalName || (!part1Uri.isNullOrEmpty() && it.sourceUri == part1Uri) || (!part2Uri.isNullOrEmpty() && it.sourceUri == part2Uri)
        }
        val hash = if (!part1Uri.isNullOrEmpty()) {
            kotlin.math.abs(part1Uri.hashCode())
        } else {
            kotlin.math.abs(originalName.hashCode())
        }
        val thumbnailId = originalFile?.customThumbnailId ?: ((hash % 5) + 1)

        viewModelScope.launch {
            repository.insertMediaFile(
                MediaFile(
                    fileName = "trim_${baseName}_Part1.$extension",
                    fileType = "video",
                    format = _videoFormat.value,
                    duration = firstDuration,
                    fileSize = firstSizeStr,
                    customThumbnailId = thumbnailId,
                    sourceUri = part1Uri,
                    startValue = startRatio,
                    endValue = splitRatio
                )
            )
            repository.insertMediaFile(
                MediaFile(
                    fileName = "trim_${baseName}_Part2.$extension",
                    fileType = "video",
                    format = _videoFormat.value,
                    duration = secondDuration,
                    fileSize = secondSizeStr,
                    customThumbnailId = thumbnailId,
                    sourceUri = part2Uri,
                    startValue = splitRatio,
                    endValue = endRatio
                )
            )
        }
    }

    fun deleteFile(id: Int) {
        viewModelScope.launch {
            repository.deleteMediaFileById(id)
        }
    }

    fun renameFile(id: Int, newName: String) {
        viewModelScope.launch {
            repository.renameMediaFile(id, newName)
        }
    }

    fun addCustomMedia(name: String, duration: String, size: String, type: String, format: String) {
        viewModelScope.launch {
            repository.insertMediaFile(
                MediaFile(
                    fileName = name,
                    fileType = type,
                    format = format,
                    duration = duration,
                    fileSize = size,
                    customThumbnailId = if (type == "audio") 3 else 2
                )
            )
        }
    }
}
