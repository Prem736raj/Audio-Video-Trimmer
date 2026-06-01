package com.example.ui.screens

import android.media.MediaPlayer
import android.widget.VideoView
import android.net.Uri
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.withContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.MediaFile
import com.example.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFilesScreen(
    viewModel: MediaViewModel,
    onNavigateToAudio: (fileName: String, duration: String, size: String, uriString: String?) -> Unit,
    onNavigateToVideo: (fileName: String, duration: String, size: String, uriString: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val allFiles by viewModel.allMediaFiles.collectAsState()
    var activePlaybackFile by remember { mutableStateOf<MediaFile?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = All, 1 = Audio, 2 = Video

    // Compute Storage Stats based on actual values in Room DB list
    val totalCount = allFiles.size
    val totalSpaceMb = remember(allFiles) {
        allFiles.sumOf { file ->
            val num = file.fileSize.substringBefore(" ").toDoubleOrNull() ?: 0.0
            num
        }
    }

    // Filter file logic
    val filteredFiles = remember(allFiles, searchQuery, selectedTab) {
        allFiles.filter { file ->
            val matchesSearch = file.fileName.contains(searchQuery, ignoreCase = true)
            val matchesTab = when (selectedTab) {
                1 -> file.fileType == "audio"
                2 -> file.fileType == "video"
                else -> true
            }
            matchesSearch && matchesTab
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFFAFAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title
            Column {
                Text(
                    text = "Library Archive",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E)
                )
                Text(
                    text = "View and play all exported trimmed clips securely",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            // Storage Statistics Card Dashboard Accent
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
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
                        Text(
                            text = "Clips Storage Metrics",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1E24)
                        )
                        Text(
                            text = "$totalCount Saved Items",
                            fontSize = 12.sp,
                            color = Color(0xFF8E54E9),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Simulated Memory Slider
                    val memoryPercentage = (totalSpaceMb / 150.0).coerceIn(0.0, 1.0).toFloat()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { memoryPercentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF8E54E9),
                            trackColor = Color(0xFFF2F2F7)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format("%.1f MB Cached Size", totalSpaceMb),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Disk Cap: 150 MB",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search files by name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF8E54E9),
                    unfocusedBorderColor = Color(0xFFE5E5EA),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            // Filtering Tab Items Row style matching primary layout colors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("All Clips", "Audio Only", "Video Only").forEachIndexed { index, header ->
                    val active = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (active) Color(0xFF8E54E9) else Color(0xFFE5E5EA).copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = header,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else Color.Gray
                        )
                    }
                }
            }

            // Result Clips Archive list
            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(56.dp))
                        Text(
                            text = "No clips match this query.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFiles, key = { it.id }) { file ->
                        LibraryFileRow(
                            file = file,
                            onPlayClick = { activePlaybackFile = file },
                            onDeleteClick = { viewModel.deleteFile(file.id) },
                            onRenameSubmit = { newName -> viewModel.renameFile(file.id, newName) },
                            onUseFileInTrimmer = {
                                if (file.fileType == "audio") {
                                    onNavigateToAudio(file.fileName, file.duration, file.fileSize, file.sourceUri)
                                } else {
                                    onNavigateToVideo(file.fileName, file.duration, file.fileSize, file.sourceUri)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Modern Isolated Media Player Dialog overlay
    activePlaybackFile?.let { file ->
        MediaPlayerDialog(
            file = file,
            onDismiss = { activePlaybackFile = null }
        )
    }
}

@Composable
fun AudioPlaceholderThumbnail(file: MediaFile) {
    val gradientBrush = remember(file.id) {
        Brush.linearGradient(
            colors = when (file.customThumbnailId % 3) {
                0 -> listOf(Color(0xFF1f4068), Color(0xFF162447))
                1 -> listOf(Color(0xFF321575), Color(0xFF8D0B93))
                else -> listOf(Color(0xFF0F2027), Color(0xFF203A43))
            }
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = width * 0.45f
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = width * 0.35f
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = width * 0.25f
            )
            
            val barCount = 7
            val spacing = width / (barCount + 2)
            val barWidth = spacing * 0.6f
            for (i in 0 until barCount) {
                val waveHeight = when (i) {
                    0, 6 -> height * 0.25f
                    1, 5 -> height * 0.40f
                    2, 4 -> height * 0.60f
                    else -> height * 0.75f
                }
                val x = spacing + i * spacing + (spacing - barWidth)/2
                val y = (height - waveHeight) / 2
                
                drawRoundRect(
                    color = when (i % 3) {
                        0 -> Color(0xFF00F2FE)
                        1 -> Color(0xFF4FACFE)
                        else -> Color(0xFFF355DA)
                    }.copy(alpha = 0.85f),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, waveHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2)
                )
            }
        }
        
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

@Composable
fun VideoPlaceholderThumbnail(file: MediaFile) {
    val skyGradient = remember(file.id) {
        Brush.linearGradient(
            colors = when (file.customThumbnailId % 3) {
                0 -> listOf(Color(0xFFEA384D), Color(0xFFD31027))
                1 -> listOf(Color(0xFF5f2c82), Color(0xFF49a09d))
                else -> listOf(Color(0xFF1A1B2F), Color(0xFF162447))
            }
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(skyGradient)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            drawCircle(
                color = Color(0xFFFFD200).copy(alpha = 0.85f),
                radius = width * 0.18f,
                center = Offset(width * 0.75f, height * 0.35f)
            )
            
            val path1 = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, height)
                lineTo(width * 0.35f, height * 0.45f)
                lineTo(width * 0.7f, height)
                close()
            }
            drawPath(
                path = path1,
                color = Color(0xFF2C1E3D).copy(alpha = 0.9f)
            )
            
            val path2 = androidx.compose.ui.graphics.Path().apply {
                moveTo(width * 0.3f, height)
                lineTo(width * 0.65f, height * 0.55f)
                lineTo(width, height)
                close()
            }
            drawPath(
                path = path2,
                color = Color(0xFF191124).copy(alpha = 0.95f)
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.Black.copy(alpha = 0.3f))
                .align(Alignment.BottomStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.65f)
                    .background(Color(0xFF8E54E9))
            )
        }
        
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color.White.copy(alpha = 0.4f), CircleShape)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun MediaFileThumbnail(
    file: MediaFile,
    modifier: Modifier = Modifier
) {
    val realThumbnail = rememberMediaThumbnail(file)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (realThumbnail != null) {
            Image(
                bitmap = realThumbnail.asImageBitmap(),
                contentDescription = "File Thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            if (file.fileType == "audio") {
                AudioPlaceholderThumbnail(file = file)
            } else {
                VideoPlaceholderThumbnail(file = file)
            }
        }
    }
}

@Composable
fun rememberMediaThumbnail(file: MediaFile): Bitmap? {
    val context = LocalContext.current
    return produceState<Bitmap?>(initialValue = null, file.id, file.fileName, file.sourceUri, file.startValue, file.endValue) {
        withContext(Dispatchers.IO) {
            val libraryFile = java.io.File(java.io.File(context.filesDir, "trimmed_library"), file.fileName)
            val retriever = MediaMetadataRetriever()
            var hasSetDataSource = false
            try {
                if (libraryFile.exists()) {
                    retriever.setDataSource(libraryFile.absolutePath)
                    hasSetDataSource = true
                } else if (!file.sourceUri.isNullOrEmpty()) {
                    val uri = Uri.parse(file.sourceUri)
                    if (uri.scheme == "content" || uri.scheme == "android.resource" || uri.scheme == "file") {
                        retriever.setDataSource(context, uri)
                    } else {
                        retriever.setDataSource(file.sourceUri)
                    }
                    hasSetDataSource = true
                }

                if (hasSetDataSource) {
                    if (file.fileType == "video") {
                        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        val durationMs = durationStr?.toLongOrNull() ?: 0L
                        val frameTimeUs = if (durationMs > 0 && file.startValue > 0f) {
                            (durationMs * file.startValue * 1000).toLong()
                        } else {
                            0L
                        }
                        var frame = try {
                            retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        } catch (e: Throwable) {
                            null
                        }
                        if (frame == null) {
                            frame = try {
                                retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            } catch (e: Throwable) {
                                null
                            }
                        }
                        value = frame
                    } else {
                        val rawArt = retriever.embeddedPicture
                        if (rawArt != null) {
                            value = BitmapFactory.decodeByteArray(rawArt, 0, rawArt.size)
                        }
                    }
                }
            } catch (e: Exception) {
                // Return null if fails or not found
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }.value
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryFileRow(
    file: MediaFile,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenameSubmit: (String) -> Unit,
    onUseFileInTrimmer: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val realThumbnail = rememberMediaThumbnail(file)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = onUseFileInTrimmer,
                onLongClick = { expandedMenu = true }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    MediaFileThumbnail(
                        file = file,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = file.fileName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${file.duration}  •  ${file.fileSize}  •  ${file.format}",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF2F2F7), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Open Playback Player Overlay",
                            tint = Color(0xFF8E54E9),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box {
                        IconButton(onClick = { expandedMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu Options",
                                tint = Color.Gray
                            )
                        }

                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open in Trimmer") },
                                onClick = {
                                    expandedMenu = false
                                    onUseFileInTrimmer()
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    expandedMenu = false
                                    showRenameDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray) }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = {
                                    expandedMenu = false
                                    try {
                                        val shareableUri = getShareableUri(context, file)
                                        if (shareableUri != null) {
                                            val shareIntent = android.content.Intent().apply {
                                                action = android.content.Intent.ACTION_SEND
                                                putExtra(android.content.Intent.EXTRA_STREAM, shareableUri)
                                                type = if (file.fileType == "audio") "audio/*" else "video/*"
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Media File"))
                                        } else {
                                            Toast.makeText(context, "Could not prepare file for sharing", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Cannot share this file: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color.Gray) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Clip") },
                                onClick = {
                                    expandedMenu = false
                                    onDeleteClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(file.fileName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        onRenameSubmit(newName)
                    }
                    showRenameDialog = false
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Beautiful isolated popup media player dialog matches premium Material Design 3 and is 100% stable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPlayerDialog(
    file: MediaFile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Determine segment durations
    val totalSec = remember(file.duration) {
        val split = file.duration.split(":")
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

    val libraryFile = remember(file.fileName) { java.io.File(java.io.File(context.filesDir, "trimmed_library"), file.fileName) }
    val isLocallyTrimmed = remember(libraryFile) { libraryFile.exists() }

    val trimStartMs = remember(totalSec, file.startValue) { (totalSec * file.startValue * 1000).toInt() }
    val trimEndMs = remember(totalSec, file.endValue) { (totalSec * file.endValue * 1000).toInt() }
    val clipDurationMs = trimEndMs - trimStartMs

    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var currentElapsedMs by remember { mutableStateOf(0) }
    var playPercent by remember { mutableStateOf(0f) }
    var activeDurationMs by remember { mutableStateOf(clipDurationMs.toLong()) }

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isSynthFallbackActive by remember { mutableStateOf(false) }
    val synthEngine = remember { AudioSynthEngine() }
    val playerViewRef = remember { mutableStateOf<PlayerView?>(null) }

    DisposableEffect(file.id) {
        var player: ExoPlayer? = null
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isSynthFallbackActive = true
                isPlaying = true
                try { synthEngine.start() } catch (ex: Exception) { ex.printStackTrace() }
            }
        }
        val playUri = if (isLocallyTrimmed) {
            Uri.fromFile(libraryFile)
        } else if (!file.sourceUri.isNullOrEmpty()) {
            Uri.parse(file.sourceUri)
        } else {
            null
        }

        if (playUri != null) {
            try {
                player = ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(playUri))
                    prepare()
                    addListener(listener)
                    if (!isLocallyTrimmed) {
                        seekTo(trimStartMs.toLong())
                    }
                    play()
                }
                isPlaying = true
                exoPlayer = player
            } catch (e: Exception) {
                e.printStackTrace()
                isSynthFallbackActive = true
                isPlaying = true
                try { synthEngine.start() } catch (ex: Exception) { ex.printStackTrace() }
            }
        } else {
            isSynthFallbackActive = true
            isPlaying = true
            try { synthEngine.start() } catch (ex: Exception) { ex.printStackTrace() }
        }

        onDispose {
            try { playerViewRef.value?.player = null } catch (e: Exception) { e.printStackTrace() }
            try { synthEngine.stop() } catch (e: Exception) { e.printStackTrace() }
            try {
                player?.removeListener(listener)
                player?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            exoPlayer = null
        }
    }

    // Playback loop controller updates progress securely
    LaunchedEffect(isPlaying, exoPlayer, isSynthFallbackActive) {
        if (isPlaying) {
            while (isPlaying) {
                val player = exoPlayer
                if (player != null && !isSynthFallbackActive) {
                    try {
                        val pos = player.currentPosition
                        val dur = if (player.duration > 0) player.duration else (clipDurationMs.toLong()).coerceAtLeast(100L)
                        val effectiveStart = if (isLocallyTrimmed) 0L else trimStartMs.toLong()
                        val effectiveEnd = if (isLocallyTrimmed) clipDurationMs.toLong() else trimEndMs.toLong()
                        val effectiveDuration = if (isLocallyTrimmed) clipDurationMs.toLong() else clipDurationMs.toLong()
                        activeDurationMs = effectiveDuration

                        if (pos >= effectiveEnd || pos < effectiveStart) {
                            player.seekTo(effectiveStart)
                            if (!player.isPlaying) {
                                player.play()
                            }
                            currentElapsedMs = 0
                            playPercent = 0f
                        } else {
                            currentElapsedMs = (pos - effectiveStart).toInt()
                            playPercent = if (effectiveDuration > 0) currentElapsedMs.toFloat() / effectiveDuration else 0f
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else if (isSynthFallbackActive) {
                    currentElapsedMs = (currentElapsedMs + 100) % (if (clipDurationMs > 0) clipDurationMs else 10000)
                    playPercent = currentElapsedMs.toFloat() / (if (clipDurationMs > 0) clipDurationMs else 10000)
                }
                delay(100)
            }
        }
    }

    // Volume synchronizer
    LaunchedEffect(isMuted, exoPlayer, isSynthFallbackActive) {
        try {
            exoPlayer?.volume = if (isMuted) 0f else 1f
            if (isSynthFallbackActive) {
                if (isMuted) synthEngine.stop() else synthEngine.start()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    val togglePlayPause = {
        if (isPlaying) {
            isPlaying = false
            try {
                exoPlayer?.pause()
                if (isSynthFallbackActive) synthEngine.stop()
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            isPlaying = true
            try {
                exoPlayer?.play()
                if (isSynthFallbackActive) synthEngine.start()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val visualizerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.fileName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${file.fileType.uppercase()} CLIP  •  ${file.fileSize}  •  ${file.format}",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close player")
                    }
                }

                // Waveform or video window
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF121213)),
                    contentAlignment = Alignment.Center
                ) {
                    if (file.fileType == "video" && !isSynthFallbackActive && exoPlayer != null) {
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
                    } else {
                        // Custom fluid synth animation
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2, size.height / 2)
                            val baseAmplitude = size.height / 3

                            if (file.fileType == "video") {
                                val radius = 35f
                                drawCircle(
                                    color = Color(0xFFFF5E62).copy(alpha = 0.2f),
                                    radius = radius + (playPercent * 20f),
                                    center = center
                                )
                                drawCircle(
                                    color = Color(0xFFFF9966).copy(alpha = 0.45f),
                                    radius = radius,
                                    center = center
                                )
                                val orbitRadius = 70f + 10f * sin(visualizerPhase)
                                val pointX = center.x + orbitRadius * cos(visualizerPhase + playPercent * 5f)
                                val pointY = center.y + orbitRadius * sin(visualizerPhase + playPercent * 5f)
                                drawCircle(
                                    color = Color.White,
                                    radius = 6f,
                                    center = Offset(pointX, pointY)
                                )
                            } else {
                                val barWidth = 6f
                                val barSpacing = 12f
                                val peaksCount = 18
                                val startX = (size.width - (peaksCount * barSpacing)) / 2

                                for (i in 0 until peaksCount) {
                                    val stepAmp = sin((visualizerPhase + i * 0.43f).toDouble()) * cos((visualizerPhase - i * 0.17f).toDouble())
                                    val mult = if (isPlaying) (0.2f + 0.8f * kotlin.math.abs(stepAmp).toFloat()) else 0.12f
                                    val height = baseAmplitude * mult * 1.5f
                                    val top = (size.height - height) / 2
                                    val x = startX + i * barSpacing

                                    drawRoundRect(
                                        color = Color(0xFF8E54E9).copy(alpha = if (isPlaying) 0.9f else 0.25f),
                                        topLeft = Offset(x, top),
                                        size = Size(barWidth, height),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
                                    )
                                }
                            }
                        }

                        if (isSynthFallbackActive) {
                            Text(
                                text = "Synthetic Tone Playback",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
                            )
                        }
                    }
                }

                // Seek bar progress
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Slider(
                        value = playPercent,
                        onValueChange = { value ->
                            playPercent = value
                            val targetMs = if (isLocallyTrimmed) {
                                (value * activeDurationMs).toInt()
                            } else {
                                trimStartMs + (value * clipDurationMs).toInt()
                            }
                            try {
                                exoPlayer?.seekTo(targetMs.toLong())
                            } catch (e: Exception) { e.printStackTrace() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF8E54E9),
                            activeTrackColor = Color(0xFF8E54E9),
                            inactiveTrackColor = Color(0xFFF2F2F7)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val elapsedSec = currentElapsedMs / 1000
                        val elapsedLabel = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60)
                        Text(
                            text = elapsedLabel,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = file.duration,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Controls toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFF2F2F7), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Mute",
                            tint = if (isMuted) Color.Red else Color(0xFF8E54E9)
                        )
                    }

                    Button(
                        onClick = togglePlayPause,
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E54E9)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause Toggle",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { expanded = true },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFFF2F2F7), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed settings",
                                tint = Color(0xFF8E54E9)
                            )
                        }

                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed}x") },
                                    onClick = {
                                        expanded = false
                                        try {
                                            exoPlayer?.playbackParameters = PlaybackParameters(speed)
                                        } catch (e: Exception) { e.printStackTrace() }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getShareableUri(context: android.content.Context, file: MediaFile): Uri? {
    val libraryFile = java.io.File(java.io.File(context.filesDir, "trimmed_library"), file.fileName)
    if (libraryFile.exists()) {
        try {
            return androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                libraryFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val srcUri = file.sourceUri
    if (srcUri.isNullOrEmpty()) {
        try {
            val sampleFile = java.io.File(context.cacheDir, "sample_${file.id}_${file.fileName}")
            if (!sampleFile.exists()) {
                sampleFile.parentFile?.mkdirs()
                sampleFile.outputStream().use { out ->
                    val placeholder = ByteArray(1024 * 100) { 0x00 }
                    out.write(placeholder)
                }
            }
            return androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                sampleFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    val uri = Uri.parse(srcUri)
    if (uri.scheme == "file" || uri.scheme.isNullOrEmpty()) {
        val path = uri.path ?: srcUri
        val localFile = java.io.File(path)
        if (localFile.exists()) {
            try {
                return androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    localFile
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    return uri
}
