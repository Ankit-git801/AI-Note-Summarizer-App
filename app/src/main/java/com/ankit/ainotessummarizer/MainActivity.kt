@file:OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)

package com.yourname.ainotessummarizer

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween // THIS IS THE MISSING IMPORT
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.ai.client.generativeai.GenerativeModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yourname.ainotessummarizer.data.AppDatabase
import com.yourname.ainotessummarizer.data.Summary
import com.yourname.ainotessummarizer.data.SummaryDao
import com.yourname.ainotessummarizer.ui.theme.AINotesSummarizerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.roundToInt

// --- YOUR API KEY ---
private const val API_KEY = "AIzaSyCQsJaJ6rJCac0b3sfkK-3Jw83q6lEe8bc"

// --- ViewModel ---
class SummarizerViewModel(private val dao: SummaryDao) : ViewModel() {
    private val _uiState = MutableStateFlow<SummarizerUiState>(SummarizerUiState.Initial)
    val uiState = _uiState.asStateFlow()
    val history = dao.getAllSummaries()
    private val generativeModel = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = API_KEY)
    private val _latestSummary = MutableStateFlow<Summary?>(null)
    val latestSummary = _latestSummary.asStateFlow()

    fun summarize(originalText: String, desiredLength: Int) {
        if (originalText.isBlank()) return
        _uiState.value = SummarizerUiState.Loading
        val lengthDescription = when {
            desiredLength < 100 -> "very short, about 1-2 sentences"
            desiredLength < 250 -> "concise, like a short paragraph"
            else -> "detailed, a few paragraphs long"
        }
        val prompt = "Summarize the following notes into clear, concise bullet points. The desired summary style is: $lengthDescription.\n\n$originalText"
        viewModelScope.launch {
            try {
                val response = generativeModel.generateContent(prompt)
                response.text?.let { summarizedText ->
                    val summary = Summary(originalText = originalText, summarizedText = summarizedText)
                    dao.insert(summary)
                    _latestSummary.value = summary
                    _uiState.value = SummarizerUiState.Success
                } ?: run {
                    _uiState.value = SummarizerUiState.Error("Failed to get summary. The response was empty.")
                }
            } catch (e: Exception) {
                Log.e("SummarizerViewModel", "API Error: ${e.message}", e)
                _uiState.value = SummarizerUiState.Error("API call failed. Check your connection or API Key configuration.")
            }
        }
    }

    fun delete(summary: Summary) {
        viewModelScope.launch {
            dao.delete(summary)
        }
    }

    fun resetState() { _uiState.value = SummarizerUiState.Initial }
}

// --- UI State ---
sealed interface SummarizerUiState {
    object Initial : SummarizerUiState
    object Loading : SummarizerUiState
    object Success : SummarizerUiState
    data class Error(val errorMessage: String) : SummarizerUiState
}

// --- Main Activity ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val viewModelFactory = ViewModelFactory(database.summaryDao())
        val viewModel: SummarizerViewModel by viewModels { viewModelFactory }
        setContent {
            AINotesSummarizerTheme {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

// --- App Navigation ---
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigation(viewModel: SummarizerViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "summarizer") {
        composable("summarizer") { SummarizerScreenWithPermission(viewModel = viewModel, navController = navController) }
        composable("history") { HistoryScreen(viewModel = viewModel, navController = navController) }
        composable("result") { ResultScreen(viewModel = viewModel, navController = navController) }
    }
}

// --- Composables ---
@Composable
fun SummarizerScreenWithPermission(viewModel: SummarizerViewModel, navController: NavController) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    LaunchedEffect(key1 = true) { if (!cameraPermissionState.status.isGranted) { cameraPermissionState.launchPermissionRequest() } }
    SummarizerScreen(hasPermission = cameraPermissionState.status.isGranted, viewModel = viewModel, navController = navController)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SummarizerScreen(hasPermission: Boolean, viewModel: SummarizerViewModel, navController: NavController) {
    var text by remember { mutableStateOf("") }
    var showCamera by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    var summaryLength by remember { mutableFloatStateOf(150f) }

    LaunchedEffect(uiState) {
        if (uiState is SummarizerUiState.Success) {
            navController.navigate("result")
        }
    }

    if (showCamera && hasPermission) {
        CameraPreview(onTextRecognized = { recognizedText ->
            text = recognizedText
            showCamera = false
        }, onClose = { showCamera = false })
    } else {
        Scaffold(
            topBar = { TopAppBar(title = { Text("AI Notes Summarizer") }, actions = { IconButton(onClick = { navController.navigate("history") }) { Icon(Icons.Default.History, "History") } }) },
            floatingActionButton = { FloatingActionButton(onClick = { if (hasPermission) showCamera = true }) { Icon(Icons.Default.CameraAlt, "Scan Notes") } }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize().imePadding()) {
                Column(modifier = Modifier.padding(16.dp).weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Paste or scan notes here...") }, modifier = Modifier.fillMaxWidth().weight(1f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Summary Length", style = MaterialTheme.typography.labelLarge)
                        Slider(value = summaryLength, onValueChange = { summaryLength = it }, valueRange = 50f..350f, steps = 2)
                        Text(when (summaryLength.roundToInt()) { in 50..125 -> "Short"; in 126..275 -> "Medium"; else -> "Detailed" }, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimatedContent(
                        targetState = uiState is SummarizerUiState.Loading,
                        transitionSpec = {
                            if (targetState) { fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300)) }
                            else { fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300)) }
                        }
                    ) { isLoading ->
                        if (isLoading) { CircularProgressIndicator() }
                        else { Button(onClick = { if (text.isNotBlank()) viewModel.summarize(text, summaryLength.roundToInt()) }, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Summarize Text") } }
                    }
                    if (uiState is SummarizerUiState.Error) {
                        Text(text = (uiState as SummarizerUiState.Error).errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

// --- Simplified and Stable Camera ---
@Composable
fun CameraPreview(onTextRecognized: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageAnalysis: ImageAnalysis? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProvider = cameraProviderFuture.get()
            val preview = CameraXPreview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            imageAnalysis = analysis
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analysis)
            } catch (e: Exception) {
                Log.e("CameraPreview", "Binding failed", e)
            }
            previewView
        }, modifier = Modifier.fillMaxSize())

        Button(
            onClick = { imageAnalysis?.setAnalyzer(Executors.newSingleThreadExecutor(), TextRecognitionAnalyzer(onTextRecognized)) },
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        ) {
            Text(text = "Scan Text")
        }

        Button(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Text("Close")
        }
    }
}

private class TextRecognitionAnalyzer(private val onTextRecognized: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: androidx.camera.core.ImageProxy) {
        imageProxy.image?.let { mediaImage ->
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            recognizer.process(image)
                .addOnSuccessListener { onTextRecognized(it.text) }
                .addOnFailureListener { e -> Log.e("TextAnalyzer", "Text recognition failed", e) }
                .addOnCompleteListener { imageProxy.close() }
        }
    }
}


@Composable
fun ResultScreen(viewModel: SummarizerViewModel, navController: NavController) {
    val summary by viewModel.latestSummary.collectAsState()
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Summary Result") },
                navigationIcon = { IconButton(onClick = { viewModel.resetState(); navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    summary?.let {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Copied Summary", it.summarizedText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Summary copied!", Toast.LENGTH_SHORT).show()
                        }) { Icon(Icons.Default.ContentCopy, "Copy") }
                        IconButton(onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, it.summarizedText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        }) { Icon(Icons.Default.Share, "Share") }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
            summary?.let { Text(it.summarizedText, style = MaterialTheme.typography.bodyLarge) }
                ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
}

// HistoryScreen now calls the updated HistoryItem
@Composable
fun HistoryScreen(viewModel: SummarizerViewModel, navController: NavController) {
    val history by viewModel.history.collectAsState(initial = emptyList())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (history.isEmpty()) {
                item { Text("No summaries yet.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center) }
            } else {
                items(history, key = { it.id }) { summary ->
                    HistoryItem(
                        summary = summary,
                        onDelete = { viewModel.delete(summary) }
                    )
                }
            }
        }
    }
}

// HistoryItem now includes the Delete button
@Composable
fun HistoryItem(summary: Summary, onDelete: () -> Unit) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(summary.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = summary.summarizedText, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Copied Summary", summary.summarizedText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Summary copied!", Toast.LENGTH_SHORT).show()
                }) { Text("Copy") }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, summary.summarizedText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }) { Text("Share") }
            }
        }
    }
}
