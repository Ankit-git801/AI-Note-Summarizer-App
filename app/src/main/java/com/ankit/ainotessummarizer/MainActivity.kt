@file:OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)

package com.yourname.ainotessummarizer

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.roundToInt

// --- ViewModel ---
class SummarizerViewModel(private val dao: SummaryDao) : ViewModel() {
    private val _uiState = MutableStateFlow<SummarizerUiState>(SummarizerUiState.Initial)
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val allSummaries = dao.getAllSummaries()
    val filteredHistory: Flow<List<Summary>> = _searchQuery.combine(allSummaries) { query, summaries ->
        if (query.isBlank()) {
            summaries
        } else {
            summaries.filter {
                it.originalText.contains(query, ignoreCase = true) ||
                        it.summarizedText.contains(query, ignoreCase = true)
            }
        }
    }

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val _latestSummary = MutableStateFlow<Summary?>(null)
    val latestSummary = _latestSummary.asStateFlow()

    fun getSummaryById(id: Int): Flow<Summary?> = dao.getSummaryById(id)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun summarize(originalText: String, desiredLength: Int) {
        if (originalText.isBlank()) return
        _uiState.value = SummarizerUiState.Loading

        val lengthDescription = when {
            desiredLength < 100 -> "very short, about 1-2 sentences"
            desiredLength < 250 -> "concise, like a short paragraph"
            else -> "detailed, a few paragraphs long"
        }
        val prompt = "You are an expert assistant specialized in summarizing text. " +
                "Summarize the following notes into clear, concise bullet points. " +
                "The desired summary style is: $lengthDescription.\n\n" +
                "Original Text:\n\"\"\"\n$originalText\n\"\"\""

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
                _uiState.value = SummarizerUiState.Error("API call failed. Check connection or API Key.")
            }
        }
    }

    fun updateSummary(summary: Summary) {
        viewModelScope.launch {
            dao.update(summary)
        }
    }

    fun delete(summary: Summary) {
        viewModelScope.launch {
            dao.delete(summary)
        }
    }

    fun resetState() {
        _uiState.value = SummarizerUiState.Initial
    }
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
@Composable
fun AppNavigation(viewModel: SummarizerViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestinations.SUMMARIZER_ROUTE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestinations.SUMMARIZER_ROUTE) {
                SummarizerScreenWithPermission(viewModel = viewModel, navController = navController)
            }
            composable(AppDestinations.HISTORY_ROUTE) {
                HistoryScreen(viewModel = viewModel, navController = navController)
            }
            composable(AppDestinations.RESULT_ROUTE) {
                ResultScreen(viewModel = viewModel, navController = navController, snackbarHostState = snackbarHostState)
            }
            composable(
                route = AppDestinations.DETAIL_ROUTE,
                arguments = listOf(navArgument(AppDestinations.SUMMARY_ID_ARG) { type = NavType.IntType })
            ) { backStackEntry ->
                val summaryId = backStackEntry.arguments?.getInt(AppDestinations.SUMMARY_ID_ARG) ?: -1
                DetailScreen(
                    summaryId = summaryId,
                    viewModel = viewModel,
                    navController = navController,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

// --- Composables ---
@Composable
fun SummarizerScreenWithPermission(viewModel: SummarizerViewModel, navController: NavController) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    SummarizerScreen(hasPermission = cameraPermissionState.status.isGranted, viewModel = viewModel, navController = navController)
}

@Composable
fun SummarizerScreen(hasPermission: Boolean, viewModel: SummarizerViewModel, navController: NavController) {
    var text by remember { mutableStateOf("") }
    var showCamera by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    var summaryLength by remember { mutableFloatStateOf(150f) }

    LaunchedEffect(uiState) {
        if (uiState is SummarizerUiState.Success) {
            navController.navigate(AppDestinations.RESULT_ROUTE)
        }
    }

    if (showCamera && hasPermission) {
        CameraPreview(
            onTextRecognized = { recognizedText ->
                text = recognizedText
                showCamera = false
            },
            onClose = { showCamera = false }
        )
    } else {
        Scaffold(
            topBar = { TopAppBar(title = { Text("AI Notes Summarizer") }, actions = { IconButton(onClick = { navController.navigate(AppDestinations.HISTORY_ROUTE) }) { Icon(Icons.Default.History, "History") } }) },
            floatingActionButton = {
                FloatingActionButton(onClick = { if (hasPermission) showCamera = true }) {
                    Icon(Icons.Default.CameraAlt, "Scan Notes")
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
                Column(
                    modifier = Modifier.padding(16.dp).weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Paste or scan notes here...") },
                        modifier = Modifier.fillMaxWidth().height(250.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Summary Length", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = summaryLength,
                            onValueChange = { summaryLength = it },
                            valueRange = 50f..350f,
                            steps = 2
                        )
                        Text(
                            when (summaryLength.roundToInt()) {
                                in 50..125 -> "Short"
                                in 126..275 -> "Medium"
                                else -> "Detailed"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    AnimatedContent(
                        targetState = uiState is SummarizerUiState.Loading,
                        label = "SummarizeButtonAnimation"
                    ) { isLoading ->
                        if (isLoading) {
                            CircularProgressIndicator()
                        } else {
                            Button(
                                onClick = { if (text.isNotBlank()) viewModel.summarize(text, summaryLength.roundToInt()) },
                                enabled = text.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Summarize Text") }
                        }
                    }
                    if (uiState is SummarizerUiState.Error) {
                        Text(
                            text = (uiState as SummarizerUiState.Error).errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResultScreen(viewModel: SummarizerViewModel, navController: NavController, snackbarHostState: SnackbarHostState) {
    val summary by viewModel.latestSummary.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Summary Result") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetState()
                        navController.popBackStack()
                    }) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    summary?.let {
                        IconButton(onClick = {
                            copyToClipboard(context, it.summarizedText)
                            scope.launch { snackbarHostState.showSnackbar("Summary copied!") }
                        }) { Icon(Icons.Default.ContentCopy, "Copy") }
                        IconButton(onClick = { shareText(context, it.summarizedText) }) { Icon(Icons.Default.Share, "Share") }
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

@Composable
fun HistoryScreen(viewModel: SummarizerViewModel, navController: NavController) {
    val history by viewModel.filteredHistory.collectAsState(initial = null)
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf<Summary?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                label = { Text("Search History") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            when {
                history == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                history.isNullOrEmpty() -> {
                    EmptyState(
                        title = if (searchQuery.isBlank()) "No Summaries Yet" else "No Results Found",
                        subtitle = if (searchQuery.isBlank()) "Create your first summary by scanning or pasting text." else "Try a different search term.",
                        icon = if (searchQuery.isBlank()) Icons.Outlined.Info else Icons.Default.SearchOff
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(history!!, key = { it.id }) { summary ->
                            HistoryItem(
                                summary = summary,
                                onClick = { navController.navigate(AppDestinations.navigateToDetail(summary.id)) },
                                onDelete = { showDeleteConfirmation = summary }
                            )
                        }
                    }
                }
            }
        }
    }

    showDeleteConfirmation?.let { summaryToDelete ->
        ConfirmationDialog(
            title = "Delete Summary?",
            text = "Are you sure you want to permanently delete this summary?",
            onConfirm = {
                viewModel.delete(summaryToDelete)
                showDeleteConfirmation = null
            },
            onDismiss = { showDeleteConfirmation = null }
        )
    }
}

@Composable
fun DetailScreen(summaryId: Int, viewModel: SummarizerViewModel, navController: NavController, snackbarHostState: SnackbarHostState) {
    val summaryState by viewModel.getSummaryById(summaryId).collectAsState(initial = null)
    var editedText by remember(summaryState) { mutableStateOf(summaryState?.summarizedText ?: "") }
    var isEditing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Summary" else "Summary Detail") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    summaryState?.let { summary ->
                        if (isEditing) {
                            IconButton(onClick = {
                                viewModel.updateSummary(summary.copy(summarizedText = editedText))
                                isEditing = false
                                scope.launch { snackbarHostState.showSnackbar("Summary saved!") }
                            }) { Icon(Icons.Default.Done, "Save") }
                        } else {
                            IconButton(onClick = { isEditing = true }) { Icon(Icons.Outlined.Edit, "Edit") }
                            IconButton(onClick = { copyToClipboard(context, summary.summarizedText); scope.launch { snackbarHostState.showSnackbar("Summary copied!") } }) { Icon(Icons.Default.ContentCopy, "Copy") }
                            IconButton(onClick = { shareText(context, summary.summarizedText) }) { Icon(Icons.Default.Share, "Share") }
                        }
                    }
                }
            )
        }
    ) { padding ->
        summaryState?.let { summary ->
            Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier.fillMaxSize(),
                        label = { Text("Edit your summary") }
                    )
                } else {
                    Text(summary.summarizedText, style = MaterialTheme.typography.bodyLarge)
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryItem(summary: Summary, onClick: () -> Unit, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
        positionalThreshold = { it * .25f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                label = "SwipeToDeleteBackground"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(summary.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = summary.summarizedText, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}


@Composable
fun ConfirmationDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EmptyState(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- Utility Functions ---
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Copied Summary", text)
    clipboard.setPrimaryClip(clip)
}

private fun shareText(context: Context, text: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

// --- Camera Components ---
@Composable
fun CameraPreview(onTextRecognized: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageAnalysis: ImageAnalysis? by remember { mutableStateOf(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
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
            },
            modifier = Modifier.fillMaxSize()
        )

        Button(
            onClick = { imageAnalysis?.setAnalyzer(executor, TextRecognitionAnalyzer(onTextRecognized)) },
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        ) { Text(text = "Scan Text") }

        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close Camera", tint = Color.White)
        }
    }
}

private class TextRecognitionAnalyzer(private val onTextRecognized: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var isBusy = false

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: androidx.camera.core.ImageProxy) {
        if (isBusy) {
            imageProxy.close()
            return
        }
        isBusy = true
        imageProxy.image?.let { mediaImage ->
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            recognizer.process(image)
                .addOnSuccessListener {
                    onTextRecognized(it.text)
                }
                .addOnFailureListener { e -> Log.e("TextAnalyzer", "Text recognition failed", e) }
                .addOnCompleteListener {
                    isBusy = false
                    imageProxy.close()
                }
        } ?: run {
            isBusy = false
            imageProxy.close()
        }
    }
}
