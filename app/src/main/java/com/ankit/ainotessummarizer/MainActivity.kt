package com.ankit.ainotessummarizer

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ankit.ainotessummarizer.data.AppDatabase
import com.ankit.ainotessummarizer.ui.*
import com.ankit.ainotessummarizer.ui.theme.AINotesSummarizerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val viewModelFactory = MainViewModelFactory(database.noteDao(), database.subjectDao())
        val viewModel: MainViewModel by viewModels { viewModelFactory }
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            AINotesSummarizerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    
    NavHost(navController, startDestination = AppDestinations.SUBJECTS_ROUTE) {
        composable(AppDestinations.SUBJECTS_ROUTE) {
            SubjectsScreen(viewModel, navController)
        }
        
        composable(
            AppDestinations.NOTES_ROUTE,
            arguments = listOf(navArgument(AppDestinations.SUBJECT_ID_ARG) { type = NavType.IntType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getInt(AppDestinations.SUBJECT_ID_ARG) ?: -1
            NotesScreen(subjectId, viewModel, navController)
        }
        
        composable(
            AppDestinations.NOTE_DETAIL_ROUTE,
            arguments = listOf(navArgument(AppDestinations.NOTE_ID_ARG) { type = NavType.IntType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getInt(AppDestinations.NOTE_ID_ARG) ?: -1
            NoteDetailScreen(noteId, viewModel, navController)
        }
        
        composable(
            AppDestinations.STUDY_MODE_ROUTE,
            arguments = listOf(navArgument(AppDestinations.NOTE_ID_ARG) { type = NavType.IntType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getInt(AppDestinations.NOTE_ID_ARG) ?: -1
            StudyModeScreen(noteId, viewModel, navController)
        }
        
        composable(
            AppDestinations.CAMERA_ROUTE,
            arguments = listOf(navArgument(AppDestinations.SUBJECT_ID_ARG) { type = NavType.IntType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getInt(AppDestinations.SUBJECT_ID_ARG) ?: -1
            CameraPermissionWrapper {
                CameraScreen(subjectId, viewModel, navController)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionWrapper(content: @Composable () -> Unit) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    if (cameraPermissionState.status.isGranted) {
        content()
    } else {
        // Simple permission denied UI
        Surface(Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                androidx.compose.material3.Text("Camera permission is required to scan notes.")
            }
        }
    }
}
