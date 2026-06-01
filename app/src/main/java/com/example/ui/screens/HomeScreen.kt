package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaFile
import com.example.ui.navigation.Screen
import com.example.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MediaViewModel,
    onNavigateToAudio: (fileName: String, duration: String, size: String, uri: String?) -> Unit,
    onNavigateToVideo: (fileName: String, duration: String, size: String, uri: String?) -> Unit,
    onNavigateToMyFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allFiles by viewModel.allMediaFiles.collectAsState()
    val playingFileId by viewModel.currentPlayingFileId.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    var showAudioOptionsSheet by remember { mutableStateOf(false) }
    var showVideoOptionsSheet by remember { mutableStateOf(false) }

    // File picker launchers
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        uri?.let {
            val (name, size) = queryFileInfo(context, it)
            val duration = queryFileDuration(context, it, isVideo = false)
            onNavigateToAudio(name, duration, size, it.toString()) // Load picker callback
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        uri?.let {
            val (name, size) = queryFileInfo(context, it)
            val duration = queryFileDuration(context, it, isVideo = true)
            onNavigateToVideo(name, duration, size, it.toString()) // Load picker callback
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFFAFAFC), // Off-white clean layout
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFECF0), // faint rose gradient top
                            Color(0xFFECEFFC), // faint lavender/blue
                            Color(0xFFFAFAFC)  // solid white bottom
                        ),
                        endY = 500f
                    )
                ),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Top Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Audio & Video",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1E24)
                            )
                        }
                        Text(
                            text = "Trimmer",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            style = TextStyle(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF8E2DE2), // Premium violet
                                        Color(0xFFFF007F)  // Coral dynamic pink
                                    )
                                )
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Trim. Cut. Save.\nEasy & Fast.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF7F7F89),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // High Fidelity 3D Media Canvas Illustration Top-Banner Overlay Accent
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF2A0845), Color(0xFF6441A5)) // Deep beautiful purple gradient
                            )
                        )
                        .padding(20.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Ambient smooth waves
                        val wavePath = Path().apply {
                            moveTo(0f, size.height)
                            quadraticBezierTo(size.width * 0.25f, size.height * 0.4f, size.width * 0.5f, size.height * 0.7f)
                            quadraticBezierTo(size.width * 0.75f, size.height * 1f, size.width, size.height * 0.2f)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(wavePath, color = Color.White.copy(alpha = 0.05f))
                        
                        // Floating accents
                        drawCircle(color = Color(0xFFFF5E62).copy(alpha = 0.8f), radius = 24f, center = Offset(size.width - 40f, 30f))
                        drawCircle(color = Color(0xFFFF80D0).copy(alpha = 0.4f), radius = 12f, center = Offset(size.width - 90f, 80f))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Badge
                            Text(
                                text = "⚡ Pro Features Focus",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFA5D8),
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Smart Waveform Alignment",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Precision cuts with zero quality loss.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        
                        // Icon on the right
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // Segment 1: Main Audio & Video Dual Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Audio Trimmer Card
                    Card(
                        onClick = {
                            showAudioOptionsSheet = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(180.dp),
                        shape = RoundedCornerShape(26.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF8E54E9), // Purple violet
                                            Color(0xFF4776E6)  // Tech blue
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Audio\nTrimmer",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        lineHeight = 24.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Trim audio files and save your favorite part",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.82f),
                                        lineHeight = 14.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Custom arrow play circle button
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Go",
                                            tint = Color(0xFF4776E6),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Floating mini note icon
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.28f),
                                        modifier = Modifier.size(52.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Video Trimmer Card
                    Card(
                        onClick = {
                            showVideoOptionsSheet = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(180.dp),
                        shape = RoundedCornerShape(26.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFF9966), // Lively Orange
                                            Color(0xFFFF5E62)  // Watermelon coral
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Video\nTrimmer",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        lineHeight = 24.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Trim video clips and save highlights",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.82f),
                                        lineHeight = 14.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Play button circle accent
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Go",
                                            tint = Color(0xFFFF5E62),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Floating mini clapper board icon
                                    Icon(
                                        imageVector = Icons.Default.MovieFilter,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.25f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

        }

        if (showAudioOptionsSheet) {
            Dialog(
                onDismissRequest = { showAudioOptionsSheet = false }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color(0xFF8E54E9),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Import Audio",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { showAudioOptionsSheet = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close"
                                )
                            }
                        }

                        Text(
                            text = "Select how you would like to import audio. Choose files, use system selectors, or try a studio sample.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        OptionButton(
                            title = "System File Explorer",
                            description = "Import any audio form from your storage or downloads",
                            icon = Icons.Default.FolderOpen,
                            color = Color(0xFF8E54E9),
                            onClick = {
                                showAudioOptionsSheet = false
                                try {
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "audio/*"
                                    }
                                    audioPicker.launch(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "audio/*" }
                                    audioPicker.launch(Intent.createChooser(intent, "Browse Audio"))
                                }
                            }
                        )

                        OptionButton(
                            title = "Photos & Gallery Selector",
                            description = "Browse or search your local system music files",
                            icon = Icons.Default.Audiotrack,
                            color = Color(0xFF4776E6),
                            onClick = {
                                showAudioOptionsSheet = false
                                try {
                                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "audio/*" }
                                    audioPicker.launch(Intent.createChooser(intent, "Select Audio"))
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "System selector not found", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        OptionButton(
                            title = "Try Studio Demo Beat",
                            description = "Instantly load a preloaded 02:15 virtual sample",
                            icon = Icons.Default.PlayArrow,
                            color = Color(0xFF4CAF50),
                            onClick = {
                                showAudioOptionsSheet = false
                                onNavigateToAudio("studio_groove_sample.mp3", "02:15", "3.4 MB", null)
                            }
                        )
                    }
                }
            }
        }

        if (showVideoOptionsSheet) {
            Dialog(
                onDismissRequest = { showVideoOptionsSheet = false }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MovieFilter,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5E62),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Import Video",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { showVideoOptionsSheet = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close"
                                )
                            }
                        }

                        Text(
                            text = "Select how you would like to import videos. Choose files, record a live clip, or try a cinematic sample.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        OptionButton(
                            title = "System File Explorer",
                            description = "Import any video from storage, Downloads, or Drive",
                            icon = Icons.Default.FolderOpen,
                            color = Color(0xFFFF5E62),
                            onClick = {
                                showVideoOptionsSheet = false
                                try {
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "video/*"
                                    }
                                    videoPicker.launch(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "video/*" }
                                    videoPicker.launch(Intent.createChooser(intent, "Browse Video"))
                                }
                            }
                        )

                        OptionButton(
                            title = "Photos & Gallery Selector",
                            description = "Browse or select using standard media libraries",
                            icon = Icons.Default.Movie,
                            color = Color(0xFFFF9966),
                            onClick = {
                                showVideoOptionsSheet = false
                                try {
                                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "video/*" }
                                    videoPicker.launch(Intent.createChooser(intent, "Select Video"))
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "System selector not found", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        OptionButton(
                            title = "Record Video Live",
                            description = "Launch camera app to capture a split second video",
                            icon = Icons.Default.MovieFilter,
                            color = Color(0xFFFF2E93),
                            onClick = {
                                showVideoOptionsSheet = false
                                try {
                                    val intent = Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE)
                                    videoPicker.launch(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "No camera app found on this device", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        )

                        OptionButton(
                            title = "Try Cinematic Demo",
                            description = "Instantly load a gorgeous 01:05 nature clip",
                            icon = Icons.Default.PlayArrow,
                            color = Color(0xFF4CAF50),
                            onClick = {
                                showVideoOptionsSheet = false
                                onNavigateToVideo("cinematic_nature_sample.mp4", "01:05", "8.4 MB", null)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentFileRow(
    file: MediaFile,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onDeleteClick: () -> Unit,
    onUseFileInTrimmer: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                // Play Action Circle button
                IconButton(
                    onClick = onPlayToggle,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF2F2F7), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Simulate Play File",
                        tint = Color(0xFF5A44E3),
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


@Composable
fun OptionButton(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Utility helper to query URI name and sizes
fun queryFileInfo(context: android.content.Context, uri: Uri): Pair<String, String> {
    var name = "device_media"
    var size = "Unknown"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    val displayName = cursor.getString(nameIndex)
                    if (!displayName.isNullOrEmpty()) {
                        name = displayName
                    }
                }
                if (sizeIndex != -1) {
                    val sizeBytes = cursor.getLong(sizeIndex)
                    size = String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        uri.lastPathSegment?.let { name = it }
    }
    return Pair(name, size)
}

// Helper to query dynamic duration of media files using MediaMetadataRetriever
fun queryFileDuration(context: android.content.Context, uri: Uri, isVideo: Boolean): String {
    val retriever = android.media.MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
        if (durationStr != null) {
            val durationMs = durationStr.toLong()
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return if (isVideo) "01:00" else "01:00"
}
