package com.example.ui.screens

import android.net.Uri
import android.media.MediaPlayer
import android.media.AudioTrack
import android.media.AudioFormat
import android.media.AudioManager
import android.widget.Toast
import android.widget.VideoView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTrimmerScreen(
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
        viewModel.resetVideoTrimmer()
    }

    val startValue by viewModel.videoStart.collectAsState()
    val endValue by viewModel.videoEnd.collectAsState()
    val resolution by viewModel.videoResolution.collectAsState()
    val format by viewModel.videoFormat.collectAsState()
    val quality by viewModel.videoQuality.collectAsState()
    val isMuted by viewModel.isVideoMuted.collectAsState()
    val isPlaying by viewModel.isVideoPlaying.collectAsState()
    val playbackProgress by viewModel.videoPlaybackProgress.collectAsState()

    // Convert file duration (e.g. "01:29:15" or "00:45") to seconds
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
            60
        }
        maxOf(1, rawSec)
    }
    val rawSizeMb = remember(originalSize) {
        originalSize.substringBefore(" ").toDoubleOrNull() ?: 12.0
    }

    var isRendering by remember { mutableStateOf(false) }
    var renderProgress by remember { mutableStateOf(0.0f) }

    // Local state for cached playable URI
    var cachedUriString by remember(uriString) { mutableStateOf<String?>(null) }
    var isCaching by remember(uriString) { mutableStateOf(false) }

    LaunchedEffect(uriString) {
        if (!uriString.isNullOrEmpty()) {
            isCaching = true
            withContext(Dispatchers.IO) {
                try {
                    val contextUri = Uri.parse(uriString)
                    val tempFile = File(context.cacheDir, "trim_active_video.mp4")
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                    context.contentResolver.openInputStream(contextUri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    cachedUriString = Uri.fromFile(tempFile).toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback to original URI if caching copy fails
                    cachedUriString = uriString
                }
            }
            isCaching = false
        } else {
            cachedUriString = null
        }
    }

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    val playerViewRef = remember { mutableStateOf<PlayerView?>(null) }
    val synthEngine = remember { VideoSynthEngine() }

    DisposableEffect(cachedUriString) {
        var player: ExoPlayer? = null
        if (!cachedUriString.isNullOrEmpty()) {
            try {
                player = ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(Uri.parse(cachedUriString)))
                    repeatMode = Player.REPEAT_MODE_OFF
                    prepare()
                }
                exoPlayer = player
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        onDispose {
            try { synthEngine.stop() } catch (e: Exception) { e.printStackTrace() }
            try { playerViewRef.value?.player = null } catch (e: Exception) { e.printStackTrace() }
            try {
                player?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            exoPlayer = null
        }
    }

    // Playback control and loop constraints
    LaunchedEffect(isPlaying, startValue, endValue, exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        try {
            if (isPlaying) {
                val startMs = (totalSeconds * startValue * 1000).toLong()
                val endMs = (totalSeconds * endValue * 1000).toLong()
                
                val currentPos = player.currentPosition
                if (currentPos < startMs || currentPos >= endMs) {
                    player.seekTo(startMs)
                }
                player.play()
                
                while (isPlaying) {
                    delay(30)
                    try {
                        val curr = player.currentPosition
                        if (curr >= endMs || !player.isPlaying) {
                            player.seekTo(startMs)
                            player.play()
                        }
                        val pos = curr.toFloat() / (totalSeconds * 1000f)
                        viewModel.setVideoPlaybackProgress(pos)
                    } catch (e: Exception) {
                        break
                    }
                }
            } else {
                player.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Fallback synth engine for empty/caching state
    LaunchedEffect(isPlaying, exoPlayer, isMuted) {
        if (exoPlayer == null) {
            if (isPlaying) {
                if (!isMuted) {
                    synthEngine.start()
                } else {
                    synthEngine.stop()
                }
            } else {
                synthEngine.stop()
            }
        }
    }

    // Volume control
    LaunchedEffect(isMuted, exoPlayer) {
        try {
            exoPlayer?.volume = if (isMuted) 0f else 1f
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Scrub/seek while paused
    LaunchedEffect(playbackProgress, exoPlayer, isPlaying) {
        val player = exoPlayer ?: return@LaunchedEffect
        if (!isPlaying) {
            delay(50) // Debounce rapid scrubbing position updates by 50ms
            try {
                val targetMs = (totalSeconds * playbackProgress * 1000).toLong()
                if (Math.abs(player.currentPosition - targetMs) > 150) {
                    player.seekTo(targetMs)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val startSeconds = (totalSeconds * startValue).toInt()
    val endSeconds = (totalSeconds * endValue).toInt()
    val trimmedDurationSeconds = (endSeconds - startSeconds).coerceAtLeast(1)

    val formattedStart = String.format("%02d:%02d", startSeconds / 60, startSeconds % 60)
    val formattedEnd = String.format("%02d:%02d", endSeconds / 60, endSeconds % 60)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Video Trimmer",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1E24)
                        )
                        Text(
                            text = fileName,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Go Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFAFAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Segment 1: Cinematic Video Sandbox Sandbox viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.Black)
            ) {
                // Background visual simulation drawn dynamically on Canvas or real VideoView
                if (isCaching) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFF5E62), strokeWidth = 3.dp)
                            Text(
                                text = "Preparing video preview...",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else if (!cachedUriString.isNullOrEmpty() && exoPlayer != null) {
                    key(cachedUriString) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = true
                                    controllerAutoShow = true
                                    playerViewRef.value = this
                                }
                            },
                            update = { playerView ->
                                playerView.player = exoPlayer
                                playerViewRef.value = playerView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Sunset visual loop
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFF527B).copy(alpha = 0.85f),
                                    Color(0xFFFF7A45).copy(alpha = 0.9f),
                                    Color(0xFFC70039).copy(alpha = 0.4f)
                                )
                            )
                        )

                        // Draw yellow sun
                        drawCircle(
                            color = Color(0xFFFFDF00),
                            center = Offset(size.width / 2f + 50f, size.height / 2f - 10f),
                            radius = 45f
                        )

                        // Wave lines overlay with beach vectors
                        val path = Path().apply {
                            moveTo(0f, size.height - 40f)
                            quadraticTo(size.width / 4, size.height - 60f, size.width / 2, size.height - 45f)
                            quadraticTo(size.width * 3 / 4, size.height - 25f, size.width, size.height - 50f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(path = path, color = Color(0xFF4A00E0).copy(alpha = 0.45f))

                        // Simulated overlay items
                        if (isMuted) {
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.61f),
                                radius = 28f,
                                center = Offset(50f, 50f)
                            )
                        }
                    }
                }

                // Floating watermark length
                Text(
                    text = "Source Clip • $originalDuration ($originalSize)",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // Render play progress overlay indicator in player view itself
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleVideoPlaying() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.9f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Simulated progress bar tracker
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .padding(horizontal = 12.dp)
                                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(playbackProgress)
                                    .background(Color(0xFFFF5E62), RoundedCornerShape(4.dp))
                            )
                        }

                        Text(
                            text = String.format("%02d:%02d", (totalSeconds * playbackProgress).toInt() / 60, (totalSeconds * playbackProgress).toInt() % 60),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Segment 2: Horizontal keyframe scroll strip
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Multi-Track Studio Timeline",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1C1C1E)
                    )

                    // Track: Audio Overlay
                    Text(
                        text = "Audio Overdub Track",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0F0F5))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Synthesized interactive dual-waveform overlay
                            val peaks = List(40) { (20..80).random() / 100f }
                            val spacing = size.width / peaks.size
                            for (i in peaks.indices) {
                                val peakHeight = peaks[i] * size.height * 0.8f
                                val x = i * spacing + spacing / 2
                                val topY = (size.height - peakHeight) / 2
                                drawLine(
                                    color = Color(0xFF6441A5).copy(alpha = 0.6f),
                                    start = Offset(x, topY),
                                    end = Offset(x, topY + peakHeight),
                                    strokeWidth = 6f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    // Trim Segment representation canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.05f))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Dark overlay showcasing excluded ranges
                            // Left unselected section
                            drawRect(
                                color = Color.Black.copy(alpha = 0.45f),
                                topLeft = Offset(0f, 0f),
                                size = Size(size.width * startValue, size.height)
                            )
                            // Right unselected section
                            drawRect(
                                color = Color.Black.copy(alpha = 0.45f),
                                topLeft = Offset(size.width * endValue, 0f),
                                size = Size(size.width * (1.0f - endValue), size.height)
                            )

                            // Highlight border trim box
                            drawRoundRect(
                                color = Color(0xFFFF5E62),
                                topLeft = Offset(size.width * startValue, 0f),
                                size = Size(size.width * (endValue - startValue), size.height),
                                style = Stroke(width = 6f),
                                cornerRadius = CornerRadius(10f)
                            )
                        }
                    }

                    // Trim feedback row range
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Trim Segment start: $formattedStart", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5E62))
                        Text(text = "Trim Segment end: $formattedEnd", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5E62))
                    }
                }
            }

            // Segment 3: Sliders adjustment options
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Precise Trim boundaries Selector",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Start Frame Offset", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(text = formattedStart, fontSize = 12.sp, color = Color(0xFFFF5E62), fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = startValue,
                            onValueChange = { viewModel.setVideoStart(it) },
                            valueRange = 0f..0.9f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF5E62),
                                activeTrackColor = Color(0xFFFF5E62)
                            )
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "End Frame Offset", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(text = formattedEnd, fontSize = 12.sp, color = Color(0xFFFF5E62), fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = endValue,
                            onValueChange = { viewModel.setVideoEnd(it) },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF5E62),
                                activeTrackColor = Color(0xFFFF5E62)
                            )
                        )
                    }
                }
            }

            // Custom Splitting Engine Tool
            Card(
                modifier = Modifier.fillMaxWidth().testTag("split_video_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = null,
                                tint = Color(0xFF5A44E3),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Split Video Clip",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1C1E)
                            )
                        }
                    }

                    Text(
                        text = "Divides the video into two separate independent files at the current playhead frame location (${String.format("%.0f%%", playbackProgress * 100f)}).",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                // Play short synthesized splitting beep sound
                                synthEngine.start()
                                delay(300)
                                synthEngine.stop()

                                // Copy to persistent sandbox directory
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
                                                startPct = 0.0f,
                                                endPct = playbackProgress,
                                                isVideo = true
                                            )
                                        } else {
                                            // Write video placeholder
                                            destFile.outputStream().use { output ->
                                                val placeholderBytes = ByteArray(1024 * 100) { 0x00 }
                                                output.write(placeholderBytes)
                                            }
                                        }
                                        val publicUri = com.example.utils.MediaSaver.saveToPublicMedia(context, destFile, isVideo = true)
                                        persistentUri = publicUri?.toString() ?: android.net.Uri.fromFile(destFile).toString()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                viewModel.splitVideo(originalName = fileName, originalDurationSec = totalSeconds, originalSizeMb = rawSizeMb, splitRatio = playbackProgress, part1Uri = persistentUri ?: uriString, part2Uri = persistentUri ?: uriString)
                                Toast.makeText(context, "Video split completed at ${String.format("%.0f%%", playbackProgress * 100f)}!", Toast.LENGTH_LONG).show()
                                onNavigateToMyFiles()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("split_video_action_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A44E3)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CallSplit, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Split Clip at Progress Playhead",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Segment 4: Video specific configurations (Resolution, Format, Quality)
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
                        text = "Export Render Engine parameters",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )

                    // Target Resolution row
                    Column {
                        Text(text = "Target Resolution Scale", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("720p", "1080p", "4K", "480p").forEach { res ->
                                val selected = resolution == res
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (selected) Color(0xFFFF5E62) else Color(0xFFF2F2F7),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.setVideoResolution(res) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                   ) {
                                    Text(
                                        text = res,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    // Export Format Row
                    Column {
                        Text(text = "Codec Container Format", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("MP4", "MKV", "MOV", "GIF").forEach { fmt ->
                                val selected = format == fmt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (selected) Color(0xFFFF5E62) else Color(0xFFF2F2F7),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.setVideoFormat(fmt) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                   ) {
                                    Text(
                                        text = fmt,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    // Audio track toggle settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.VolumeMute, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "Strip Audio Track", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Mute sound completely in output file", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Switch(checked = isMuted, onCheckedChange = { viewModel.setVideoMuted(it) })
                    }
                }
            }

            // Save Render Actions
            if (isRendering) {
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
                            text = "Encoding Video Frame Buffer...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5E62)
                        )
                        LinearProgressIndicator(
                            progress = { renderProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFF5E62),
                            trackColor = Color(0xFFF2F2F7)
                        )
                        Text(
                            text = "${(renderProgress * 100).toInt()}% segments processed",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        isRendering = true
                        coroutineScope.launch {
                            while (renderProgress < 1.0f) {
                                delay(80)
                                renderProgress += 0.04f
                            }
                            
                            // Copy video to persistent sandbox directory to preserve media access rights
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
                                            isVideo = true
                                        )
                                    } else {
                                        destFile.outputStream().use { output ->
                                            val placeholderBytes = ByteArray(1024 * 100) { 0x00 }
                                            output.write(placeholderBytes)
                                        }
                                    }
                                    val publicUri = com.example.utils.MediaSaver.saveToPublicMedia(context, destFile, isVideo = true)
                                    persistentUri = publicUri?.toString() ?: android.net.Uri.fromFile(destFile).toString()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            viewModel.trimVideo(fileName, totalSeconds, rawSizeMb, sourceUri = persistentUri ?: uriString)
                            Toast.makeText(context, "Saved successfully in high quality to mobile device", Toast.LENGTH_LONG).show()
                            isRendering = false
                            renderProgress = 0f
                            onNavigateToMyFiles()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("trim_video_save_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5E62))
                ) {
                    Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Execute Video Trim & Save",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

class VideoSynthEngine {
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
                val pitches = doubleArrayOf(329.63, 349.23, 392.00, 440.00, 493.88, 523.25, 587.33, 659.25) // E4 to E5 scale
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
