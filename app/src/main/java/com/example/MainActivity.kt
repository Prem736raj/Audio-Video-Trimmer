package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.room.Room
import com.example.data.db.AppDatabase
import com.example.data.repository.MediaRepository
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.screens.queryFileInfo
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MediaViewModel

class MainActivity : ComponentActivity() {

    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "media_trimmer_database_v3"
        ).fallbackToDestructiveMigration().build()
    }

    private val repository by lazy {
        MediaRepository(database.mediaFileDao())
    }

    private val viewModel by lazy {
        MediaViewModel(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDarkMode) {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: MediaViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            // Display bottom bar ONLY on primary workspace destinations (Home, Archive, Profile)
            val showBottomBar = currentRoute in listOf(
                Screen.Home.route,
                Screen.MyFiles.route,
                Screen.Profile.route
            )
            if (showBottomBar) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(), // layout safe-margin overlay preventions
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    tonalElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tab Item: Home
                        BottomTab(
                            modifier = Modifier.weight(1f),
                            label = "Home",
                            iconEnabled = Icons.Filled.Home,
                            iconDisabled = Icons.Outlined.Home,
                            active = currentRoute == Screen.Home.route,
                            onClick = {
                                if (currentRoute != Screen.Home.route) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Home.route)
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )

                        // Tab Item: My Files
                        BottomTab(
                            modifier = Modifier.weight(1f),
                            label = "My Files",
                            iconEnabled = Icons.Filled.Folder,
                            iconDisabled = Icons.Outlined.FolderOpen,
                            active = currentRoute == Screen.MyFiles.route,
                            onClick = {
                                if (currentRoute != Screen.MyFiles.route) {
                                    navController.navigate(Screen.MyFiles.route) {
                                        popUpTo(Screen.Home.route)
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )

                        // Tab Item: Profile
                        BottomTab(
                            modifier = Modifier.weight(1f),
                            label = "Profile",
                            iconEnabled = Icons.Filled.AccountCircle,
                            iconDisabled = Icons.Outlined.AccountCircle,
                            active = currentRoute == Screen.Profile.route,
                            onClick = {
                                if (currentRoute != Screen.Profile.route) {
                                    navController.navigate(Screen.Profile.route) {
                                        popUpTo(Screen.Home.route)
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Animated transition set-up
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            }
        ) {
            // Route 1: Home View Navigation
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAudio = { name, len, sz, uri ->
                        navController.navigate(Screen.AudioTrimmer.createRoute(name, len, sz, uri))
                    },
                    onNavigateToVideo = { name, len, sz, uri ->
                        navController.navigate(Screen.VideoTrimmer.createRoute(name, len, sz, uri))
                    },
                    onNavigateToMyFiles = { navController.navigate(Screen.MyFiles.route) }
                )
            }

            // Route 2: Audio Trimmer Detail View
            composable(
                route = Screen.AudioTrimmer.route,
                arguments = listOf(
                    navArgument("fileName") { type = NavType.StringType },
                    navArgument("duration") { type = NavType.StringType },
                    navArgument("size") { type = NavType.StringType },
                    navArgument("uri") { type = NavType.StringType; nullable = true; defaultValue = "" }
                )
            ) { backStackEntry ->
                val fName = backStackEntry.arguments?.getString("fileName") ?: "recording.mp3"
                val len = backStackEntry.arguments?.getString("duration") ?: "02:30"
                val sz = backStackEntry.arguments?.getString("size") ?: "4.2 MB"
                val rawUri = backStackEntry.arguments?.getString("uri")
                val decodedUri = if (!rawUri.isNullOrEmpty()) {
                    try {
                        java.net.URLDecoder.decode(rawUri, "UTF-8")
                    } catch (e: Exception) {
                        rawUri
                    }
                } else null

                AudioTrimmerScreen(
                    viewModel = viewModel,
                    fileName = fName,
                    originalDuration = len,
                    originalSize = sz,
                    uriString = decodedUri,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMyFiles = {
                        navController.navigate(Screen.MyFiles.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            // Route 3: Video Trimmer Detail View
            composable(
                route = Screen.VideoTrimmer.route,
                arguments = listOf(
                    navArgument("fileName") { type = NavType.StringType },
                    navArgument("duration") { type = NavType.StringType },
                    navArgument("size") { type = NavType.StringType },
                    navArgument("uri") { type = NavType.StringType; nullable = true; defaultValue = "" }
                )
            ) { backStackEntry ->
                val fName = backStackEntry.arguments?.getString("fileName") ?: "video.mp4"
                val len = backStackEntry.arguments?.getString("duration") ?: "01:15"
                val sz = backStackEntry.arguments?.getString("size") ?: "15.0 MB"
                val rawUri = backStackEntry.arguments?.getString("uri")
                val decodedUri = if (!rawUri.isNullOrEmpty()) {
                    try {
                        java.net.URLDecoder.decode(rawUri, "UTF-8")
                    } catch (e: Exception) {
                        rawUri
                    }
                } else null

                VideoTrimmerScreen(
                    viewModel = viewModel,
                    fileName = fName,
                    originalDuration = len,
                    originalSize = sz,
                    uriString = decodedUri,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMyFiles = {
                        navController.navigate(Screen.MyFiles.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            // Route 4: Export Clips Library Archive View
            composable(Screen.MyFiles.route) {
                MyFilesScreen(
                    viewModel = viewModel,
                    onNavigateToAudio = { name, len, sz, uri ->
                        navController.navigate(Screen.AudioTrimmer.createRoute(name, len, sz, uri))
                    },
                    onNavigateToVideo = { name, len, sz, uri ->
                        navController.navigate(Screen.VideoTrimmer.createRoute(name, len, sz, uri))
                    }
                )
            }

            // Route 5: Profile & Specs View
            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToPrivacyPolicy = {
                        navController.navigate(Screen.PrivacyPolicy.route)
                    }
                )
            }
            
            composable(Screen.PrivacyPolicy.route) {
                com.example.ui.screens.PrivacyPolicyScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Cleaned up bottom sheet modal
    }
}

@Composable
fun BottomTab(
    modifier: Modifier = Modifier,
    label: String,
    iconEnabled: androidx.compose.ui.graphics.vector.ImageVector,
    iconDisabled: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (active) iconEnabled else iconDisabled,
            contentDescription = label,
            tint = if (active) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
