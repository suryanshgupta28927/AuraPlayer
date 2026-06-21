package com.aura.player

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.aura.player.ui.library.LibraryScreen
import com.aura.player.ui.components.MiniPlayer
import com.aura.player.ui.player.PlayerScreen
import com.aura.player.ui.player.PlayerViewModel
import com.aura.player.ui.playlist.PlaylistDetailScreen
import com.aura.player.ui.playlist.PlaylistsScreen
import com.aura.player.ui.search.SearchScreen
import com.aura.player.ui.theme.AuraAccent
import com.aura.player.ui.theme.AuraBg
import com.aura.player.ui.theme.AuraMuted
import com.aura.player.ui.theme.AuraPlayerTheme
import com.aura.player.ui.theme.AuraSurface
import androidx.compose.material.icons.filled.Download
import com.aura.player.ui.download.DownloadScreen
import com.aura.player.ui.download.DownloadViewModel
import com.aura.player.ui.video.VideoLibraryScreen
import com.aura.player.ui.video.VideoPlayerScreen
import com.aura.player.ui.video.VideoPlayerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()
    private val videoViewModel: VideoPlayerViewModel by viewModels()
    private val downloadViewModel: DownloadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AuraPlayerTheme {
                AuraApp(viewModel, videoViewModel, downloadViewModel)
            }
        }
    }
}

private data class NavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomNavItems = listOf(
    NavItem("library", "Library", Icons.Filled.LibraryMusic),
    NavItem("videos", "Videos", Icons.Filled.VideoLibrary),
    NavItem("search", "Search", Icons.Filled.Search),
    NavItem("playlists", "Playlists", Icons.Filled.QueueMusic),
    NavItem("download", "Download", Icons.Filled.Download)
)

@Composable
fun AuraApp(viewModel: PlayerViewModel, videoViewModel: VideoPlayerViewModel, downloadViewModel: DownloadViewModel) {
    val navController = rememberNavController()
    var showPlayer by remember { mutableStateOf(false) }
    var showVideoPlayer by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }
    var hasVideoPermission by remember { mutableStateOf(false) }

    val audioPermission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val videoPermission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStoragePermission = granted
        if (granted) viewModel.refreshLocalLibrary()
    }

    val videoPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasVideoPermission = granted
        if (granted) videoViewModel.loadLocalVideos()
    }

    // Also request POST_NOTIFICATIONS on Android 13+ so the playback
    // notification (the Spotify-style controls) can actually display.
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op, optional */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = AuraBg,
        bottomBar = {
            Column {
                MiniPlayer(viewModel = viewModel, onExpand = { showPlayer = true })
                NavigationBar(containerColor = AuraSurface) {
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = backStackEntry?.destination
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AuraAccent,
                                selectedTextColor = AuraAccent,
                                unselectedIconColor = AuraMuted,
                                unselectedTextColor = AuraMuted,
                                indicatorColor = AuraSurface
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(AuraBg)) {
            NavHost(navController = navController, startDestination = "library") {
                composable("library") {
                    LibraryScreen(
                        viewModel = viewModel,
                        hasStoragePermission = hasStoragePermission,
                        onRequestPermission = { permissionLauncher.launch(audioPermission) }
                    )
                }
                composable("videos") {
                    VideoLibraryScreen(
                        viewModel = videoViewModel,
                        hasVideoPermission = hasVideoPermission,
                        onRequestPermission = { videoPermissionLauncher.launch(videoPermission) },
                        onOpenVideo = { videos, index ->
                            videoViewModel.playQueue(videos, index)
                            showVideoPlayer = true
                        }
                    )
                }
                composable("search") {
                    SearchScreen(viewModel = viewModel)
                }
                composable("playlists") {
                    PlaylistsScreen(
                        viewModel = viewModel,
                        onOpenPlaylist = { name -> navController.navigate("playlist/$name") }
                    )
                }
                composable(
                    route = "playlist/{name}",
                    arguments = listOf(navArgument("name") { type = NavType.StringType })
                ) { backStackEntry ->
                    val name = backStackEntry.arguments?.getString("name") ?: ""
                    PlaylistDetailScreen(
                        viewModel = viewModel,
                        playlistName = name,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("download") {
                    DownloadScreen(viewModel = downloadViewModel)
                }
            }
        }
    }

    if (showPlayer) {
        Box(modifier = Modifier.fillMaxSize().background(AuraBg)) {
            PlayerScreen(viewModel = viewModel, onCollapse = { showPlayer = false })
        }
    }

    if (showVideoPlayer) {
        Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
            VideoPlayerScreen(
                viewModel = videoViewModel,
                onBack = { showVideoPlayer = false }
            )
        }
    }

    // Initial permission checks on first composition
    LaunchedEffect(Unit) {
        val audioGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            navController.context, audioPermission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        hasStoragePermission = audioGranted
        if (audioGranted) viewModel.refreshLocalLibrary()

        val videoGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            navController.context, videoPermission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        hasVideoPermission = videoGranted
        if (videoGranted) videoViewModel.loadLocalVideos()
    }
}
