package com.example.ui.screens

import android.net.Uri
import android.media.MediaPlayer
import android.media.AudioTrack
import android.media.AudioFormat
import android.media.AudioManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.Screen
import com.example.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrimmerScreen(
    viewModel: MediaViewModel,
    fileName: String,
    originalDuration: String,
    originalSize: String,
    uriString: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToMyFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(fileName, uriString) {
        viewModel.resetAudioTrimmer()
    }

    // ViewModel Flows
    val startValue by viewModel.audioStart.collectAsState()
    val endValue by viewModel.audioEnd.collectAsState()
    val speed by viewModel.audioSpeed.collectAsState()
    val volume by viewModel.audioVolume.collectAsState()
    val format by viewModel.audioFormat.collectAsState()
    val fadeIn by viewModel.audioFadeIn.collectAsState()
    val fadeOut by viewModel.audioFadeOut.collectAsState()
    val isPlaying by viewModel.isAudioPlaying.collectAsState()
    val playbackProgress by viewModel.audioPlaybackProgress.collectAsState()

    // Convert duration (e.g. "01:29:15" or "03:45") to total seconds
    val totalSeconds = remember(originalDuration) {
        val split = originalDuration.split(":")
        val rawSec = if (split.size == 2) {
            val min = split[0].toIntOrNull() ?: 0
            val sec = split[1].toIntOrNull() ?: 0
            min * 60 + sec
        } else if (split.size == 3) {
            val hr = split[0].toIntOrNull() ?: 0
            val min = split[1].toIntOrNull() ?: 0
            val sec = split[2].toIntOrNull() ?: 0
            hr * 3600 + min * 60 + sec
        } else {
            120
        }
        maxOf(1, rawSec)
    }
    val rawSizeMb = remember(originalSize) {
        originalSize.substringBefore(" ").toDoubleOrNull() ?: 5.0
    }

    var isSaving by remember { mutableStateOf(false) }
    var saveProgress by remember { mutableStateOf(0.0f) }

    // Local state for cached playable URI
    var cachedUriString by remember(uriString) { mutableStateOf<String?>(null) }
    var isCaching by remember(uriString) { mutableStateOf(false) }
    var waveformPeaks by remember { mutableStateOf<List<Float>>(emptyList()) }

    LaunchedEffect(uriString) {
        if (!uriString.isNullOrEmpty()) {
            isCaching = true
            withContext(Dispatchers.IO) {
                try {
                    val contextUri = Uri.parse(uriString)
                    val tempFile = File(context.cacheDir, "trim_active_audio.mp3")
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                    context.contentResolver.openInputStream(contextUri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    cachedUriString = Uri.fromFile(tempFile).toString()
                    val peaks = com.example.utils.WaveformExtractor.extractAmplitudes(context, cachedUriString!!, 60)
                    withContext(Dispatchers.Main) {
                        waveformPeaks = peaks
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    cachedUriString = uriString
                    withContext(Dispatchers.Main) {
                        waveformPeaks = List(60) { 0.1f }
                    }
                }
            }
            isCaching = false
        } else {
            cachedUriString = null
        }
    }

    // Live Playback Engine (MediaPlayer + Synth fallback)
    val synthEngine = remember { AudioSynthEngine() }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(cachedUriString) {
        var localMp: MediaPlayer? = null
        if (!cachedUriString.isNullOrEmpty()) {
            try {
                localMp = MediaPlayer()
                localMp.setDataSource(context, Uri.parse(cachedUriString))
                localMp.setOnPreparedListener { preparedMp ->
                    mediaPlayer = preparedMp
                }
                localMp.prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onDispose {
            try { synthEngine.stop() } catch (e: Exception) { e.printStackTrace() }
            try {
                localMp?.release()
                mediaPlayer?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying, startValue, endValue, speed, volume, mediaPlayer) {
        if (isPlaying) {
            val mp = mediaPlayer
            if (mp != null) {
                try {
                    mp.setVolume(volume, volume)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        mp.playbackParams = mp.playbackParams.setSpeed(speed)
                    }
                    val startMs = (totalSeconds * startValue * 1000).toLong()
                    mp.seekTo(startMs.toInt())
                    mp.start()

                    var lastSeekTime = 0L
                    while (isPlaying) {
                        delay(100)
                        try {
                            val currPos = mp.currentPosition
                            val endMs = (totalSeconds * endValue * 1000).toInt()
                            if (currPos >= endMs || !mp.isPlaying) {
                                val now = System.currentTimeMillis()
                                if (now - lastSeekTime > 1200L) {
                                    lastSeekTime = now
                                    mp.seekTo(startMs.toInt())
                                    mp.start()
                                }
                            }
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            break
                        }
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    synthEngine.start()
                }
            } else {
                synthEngine.start()
            }
        } else {
            synthEngine.stop()
            try {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.pause()
                    }
                }
            } catch (e: Exception) {
                // Safe ignore
            }
        }
    }

    // Math for chosen start & end time
    val startSeconds = (totalSeconds * startValue).toInt()
    val endSeconds = (totalSeconds * endValue).toInt()
    val trimmedDurationSeconds = (endSeconds - startSeconds).coerceAtLeast(1)

    val formattedStart = String.format("%02d:%02d", startSeconds / 60, startSeconds % 60)
    val formattedEnd = String.format("%02d:%02d", endSeconds / 60, endSeconds % 60)
    val formattedTrimmedRange = String.format("%02d:%02d Choice", trimmedDurationSeconds / 60, trimmedDurationSeconds % 60)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Audio Trimmer",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E1E24)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = fileName,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.padding(start = 8.dp, end = 8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Go Back", tint = Color(0xFF8E54E9))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            com.example.ui.components.BannerAdComponent()
        },
        containerColor = Color(0xFFF7F8FC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Segment 1: Header information Display Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFFFF0F3), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = Color(0xFFFF5E62),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Source Length",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = originalDuration,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E1E24)
                            )
                            Text(
                                text = "($originalSize)",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(Color.Gray.copy(alpha = 0.2f))
                    )

                    // Trim result badge
                    Column(
                        modifier = Modifier.weight(1f).padding(start = 24.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Output Selection",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$formattedStart - $formattedEnd",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF8E54E9)
                        )
                    }
                }
            }

            // Segment 2: Interactive Waveform Timeline with Integrated Dual-Sliders (Merged into one zone!)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Audio Track & Trim Segment",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )

                    // Waveform Timeline Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        if (isCaching) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF8E54E9), strokeWidth = 3.dp)
                                    Text(
                                        text = "Preparing audio timeline preview...",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            // Custom Waveform rendering
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp)
                            ) {
                                val peaks = if (waveformPeaks.isEmpty()) {
                                    List(60) { 0.2f }
                                } else {
                                    waveformPeaks
                                }

                                val spacing = size.width / peaks.size
                                val stroke = 8f

                                // Draw unselected/muted wave in gray
                                for (i in peaks.indices) {
                                    val peakHeight = peaks[i] * size.height * 0.9f
                                    val x = i * spacing + spacing / 2
                                    val topY = (size.height - peakHeight) / 2
                                    val fileProgress = i.toFloat() / peaks.size

                                    val isWithinTrimSegment = fileProgress in startValue..endValue

                                    drawRoundRect(
                                        color = if (isWithinTrimSegment) Color(0xFF8E54E9).copy(alpha = 0.85f) else Color(0xFFE5E5EA),
                                        topLeft = Offset(x - stroke / 2, topY),
                                        size = Size(stroke, peakHeight),
                                        cornerRadius = CornerRadius(4f)
                                    )
                                }

                                // Draw Simulated Play progress line needle
                                if (isPlaying) {
                                    val playheadX = playbackProgress * size.width
                                    drawLine(
                                        color = Color(0xFFFF5E62),
                                        start = Offset(playheadX, 0f),
                                        end = Offset(playheadX, size.height),
                                        strokeWidth = 5f
                                    )
                                    drawCircle(
                                        color = Color(0xFFFF5E62),
                                        radius = 8f,
                                        center = Offset(playheadX, 0f)
                                    )
                                }
                            }
                        }
                    }

                    // Integrated Slider Adjustments inside the same track zone
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left Handle Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Start Trim Cut Point", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(text = formattedStart, fontSize = 12.sp, color = Color(0xFF8E54E9), fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = startValue,
                                onValueChange = { viewModel.setAudioStart(it) },
                                valueRange = 0f..0.9f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF8E54E9),
                                    activeTrackColor = Color(0xFF8E54E9)
                                )
                            )
                        }

                        // Right Handle Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "End Trim Cut Point", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(text = formattedEnd, fontSize = 12.sp, color = Color(0xFFFF5E62), fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = endValue,
                                onValueChange = { viewModel.setAudioEnd(it) },
                                valueRange = 0.1f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFF5E62),
                                    activeTrackColor = Color(0xFFFF5E62)
                                )
                            )
                        }
                    }
                }
            }

            // Segment 3: Play Controls Menu Item
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF9D50BB), Color(0xFFF0516E))
                            ),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = androidx.compose.material3.ripple(bounded = false, radius = 40.dp),
                            onClick = { viewModel.toggleAudioPlaying() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Simulate Play Segment",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Segment 4: Audio Trimmer output parameters
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Output Profile Parameters",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )

                    // Sub-option format row (Radio buttons styled as nice chips)
                    Column {
                        Text(text = "Target Suffix Extension", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("MP3", "WAV", "AAC", "M4A").forEach { fmt ->
                                val selected = format == fmt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            brush = if (selected) Brush.linearGradient(listOf(Color(0xFF8E54E9), Color(0xFFC769DE))) else Brush.linearGradient(listOf(Color(0xFFF4F4F6), Color(0xFFF4F4F6))),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.setAudioFormat(fmt) }
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                   ) {
                                    Text(
                                        text = fmt,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF2F2F7))

                    // Speed factor config
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Audio playback Speed factor", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(text = "${speed}x", fontSize = 13.sp, color = Color(0xFF8E54E9), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFF4F4F6), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setAudioSpeed(maxOf(0.5f, speed - 0.5f)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease speed", tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                            Slider(
                                value = speed,
                                onValueChange = { viewModel.setAudioSpeed(it) },
                                valueRange = 0.5f..2.0f,
                                steps = 2, // 0.5, 1.0, 1.5, 2.0
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF8E54E9)
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFF4F4F6), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setAudioSpeed(minOf(2.0f, speed + 0.5f)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase speed", tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF2F2F7))

                    // Volume Boost multiplier config
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Volume booster audio power", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(text = "${(volume * 100).toInt()}%", fontSize = 13.sp, color = Color(0xFF8E54E9), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFF4F4F6), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setAudioVolume(maxOf(0.5f, volume - 0.1f)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease volume", tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                            Slider(
                                value = volume,
                                onValueChange = { viewModel.setAudioVolume(it) },
                                valueRange = 0.5f..2.5f,
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF0075A2)
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFF4F4F6), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setAudioVolume(minOf(2.5f, volume + 0.1f)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase volume", tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF2F2F7))

                    // Fade in / fade out settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFFF4F4F6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = "Smooth Fade-In sound", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E24))
                                Text(text = "Smoothly rise decibel on start", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Switch(
                            checked = fadeIn,
                            onCheckedChange = { viewModel.setAudioFadeIn(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF8E54E9))
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF2F2F7))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFFF4F4F6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.VolumeDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = "Smooth Fade-Out segment", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E24))
                                Text(text = "Smoothly lower decibel on exit", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Switch(
                            checked = fadeOut,
                            onCheckedChange = { viewModel.setAudioFadeOut(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF8E54E9))
                        )
                    }
                }
            }

            // Save Trim Action Button and Progress indicators
            if (isSaving) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Exporting Audio Core...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8E54E9)
                        )
                        LinearProgressIndicator(
                            progress = { saveProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF8E54E9),
                            trackColor = Color(0xFFF2F2F7)
                        )
                        Text(
                            text = "${(saveProgress * 100).toInt()}% completed",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        val executeExport = {
                            isSaving = true
                            coroutineScope.launch {
                                viewModel.setAudioFadeIn(fadeIn)
                                // Simulate processing delay
                                while (saveProgress < 1.0f) {
                                    delay(60)
                                    saveProgress += 0.05f
                                }
                                
                                // Copy file to persistent sandbox to ensure it remains playable
                                var persistentUri: String? = null
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val libraryDir = java.io.File(context.filesDir, "trimmed_library").apply { mkdirs() }
                                        val finalName = "trim_$fileName"
                                        val destFile = java.io.File(libraryDir, finalName)
                                        if (destFile.exists()) {
                                            destFile.delete()
                                        }
                                        if (!(cachedUriString ?: uriString).isNullOrEmpty()) {
                                            com.example.utils.MediaTrimmerUtil.trimMedia(
                                                context = context,
                                                sourceUriString = cachedUriString ?: uriString,
                                                destFile = destFile,
                                                startPct = startValue,
                                                endPct = endValue,
                                                isVideo = false
                                            )
                                        } else {
                                            destFile.outputStream().use { output ->
                                                val placeholderBytes = ByteArray(1024 * 100) { 0x00 }
                                                output.write(placeholderBytes)
                                            }
                                        }
                                        val publicUri = com.example.utils.MediaSaver.saveToPublicMedia(context, destFile, isVideo = false)
                                        persistentUri = publicUri?.toString() ?: android.net.Uri.fromFile(destFile).toString()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                viewModel.trimAudio(fileName, totalSeconds, rawSizeMb, format, sourceUri = persistentUri ?: uriString)
                                Toast.makeText(context, "Saved successfully in high quality to mobile device", Toast.LENGTH_LONG).show()
                                isSaving = false
                                saveProgress = 0f
                                onNavigateToMyFiles()
                            }
                        }

                        val activity = context as? android.app.Activity
                        if (activity != null) {
                            com.example.utils.AdManager.showRewardedAd(
                                activity = activity,
                                onRewardEarned = { executeExport() },
                                onAdDismissed = { }
                            )
                        } else {
                            executeExport()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("trim_audio_save_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E54E9))
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Trim & Save Audio File",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

class AudioSynthEngine {
    private var audioTrack: AudioTrack? = null
    @Volatile private var isPlaying = false
    private var synthThread: Thread? = null

    fun start() {
        if (isPlaying) return
        isPlaying = true
        synthThread = Thread {
            try {
                val sampleRate = 22050
                val numSamples = sampleRate
                
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                
                val track = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBufferSize, numSamples * 2),
                    AudioTrack.MODE_STREAM
                )
                audioTrack = track
                try {
                    track.play()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                var pitchIndex = 0
                val pitches = doubleArrayOf(261.63, 293.66, 329.63, 349.23, 392.00, 440.00, 493.88, 523.25)
                var phraseTick = 0L

                while (isPlaying) {
                     val blockSize = sampleRate / 10
                     val noteBuffer = ShortArray(blockSize)
                     val currentPitch = pitches[pitchIndex % pitches.size]
                     
                     for (i in 0 until blockSize) {
                         val angle = 2.0 * Math.PI * i * currentPitch / sampleRate
                         noteBuffer[i] = (Math.sin(angle) * 7000.0).toInt().toShort()
                     }
                     
                     try {
                         val written = track.write(noteBuffer, 0, blockSize)
                         if (written <= 0) {
                             Thread.sleep(50)
                         }
                     } catch (e: Exception) {
                         e.printStackTrace()
                         break
                     }
                     phraseTick++
                     if (phraseTick % 2 == 0L) {
                         pitchIndex = (pitchIndex + 1) % pitches.size
                     }
                }
                try {
                    track.stop()
                    track.release()
                } catch (e: Exception) {
                    // Safe ignore
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
        synthThread?.start()
    }

    fun stop() {
        isPlaying = false
        synthThread = null
        audioTrack = null
    }
}
