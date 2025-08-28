@file:OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.yourname.ainotessummarizer

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavType
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.roundToInt

// --- ViewModel (No Changes) ---
class SummarizerViewModel(private val dao: SummaryDao) : ViewModel() {
    private val _uiState = MutableStateFlow<SummarizerUiState>(SummarizerUiState.Initial)
    val uiState = _uiState.asStateFlow()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag = _selectedTag.asStateFlow()
    private val allSummaries = dao.getAllSummaries()
    val filteredHistory: StateFlow<List<Summary>> = combine(allSummaries, searchQuery, selectedTag) { summaries, query, tag ->
        val searched = if (query.isBlank()) summaries else summaries.filter {
            it.originalText.contains(query, ignoreCase = true) || it.summarizedText.contains(query, ignoreCase = true) || it.tags.contains(query, ignoreCase = true)
        }
        if (tag == null) searched else searched.filter { s -> s.tags.split(",").any { it.trim().equals(tag, ignoreCase = true) } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTags: StateFlow<List<String>> = allSummaries.map { summaries ->
        summaries.flatMap { it.tags.split(",") }.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val generativeModel = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)
    private val _latestSummary = MutableStateFlow<Summary?>(null)
    val latestSummary = _latestSummary.asStateFlow()
    fun getSummaryById(id: Int): Flow<Summary?> = dao.getSummaryById(id)
    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onTagSelected(tag: String?) { _selectedTag.value = tag }
    fun summarize(originalText: String, desiredLength: Int) {
        if (originalText.isBlank()) return
        _uiState.value = SummarizerUiState.Loading
        val lengthDescription = when {
            desiredLength < 125 -> "very short, about 1-2 sentences"
            desiredLength < 225 -> "concise, like a short paragraph"
            desiredLength < 325 -> "large, like a medium-sized paragraph"
            else -> "very detailed, a few paragraphs long"
        }
        val prompt = "You are an expert assistant specialized in summarizing text. Summarize the following notes into clear, concise bullet points using the '•' character for each point. The desired summary style is: $lengthDescription.\n\nOriginal Text:\n\"\"\"\n$originalText\n\"\"\""
        viewModelScope.launch {
            try {
                val response = generativeModel.generateContent(prompt)
                response.text?.let { summarizedText ->
                    val summary = Summary(originalText = originalText, summarizedText = summarizedText)
                    dao.insert(summary)
                    _latestSummary.value = summary
                    _uiState.value = SummarizerUiState.Success
                } ?: run { _uiState.value = SummarizerUiState.Error("Failed to get summary. The response was empty.") }
            } catch (e: Exception) {
                Log.e("SummarizerViewModel", "API Error: ${e.message}", e)
                _uiState.value = SummarizerUiState.Error("API call failed. Check connection or API Key.")
            }
        }
    }
    fun togglePin(summary: Summary) { viewModelScope.launch { dao.update(summary.copy(isPinned = !summary.isPinned)) } }
    fun updateSummary(summary: Summary) { viewModelScope.launch { dao.update(summary) } }
    fun delete(summary: Summary) { viewModelScope.launch { dao.delete(summary) } }
    fun resetState() { _uiState.value = SummarizerUiState.Initial }
}

// --- UI State ---
sealed interface SummarizerUiState { object Initial:SummarizerUiState; object Loading:SummarizerUiState; object Success:SummarizerUiState; data class Error(val errorMessage: String):SummarizerUiState }

// --- Main Activity ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val viewModelFactory = ViewModelFactory(database.summaryDao())
        val viewModel: SummarizerViewModel by viewModels { viewModelFactory }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val isSystemDark = isSystemInDarkTheme()
            var darkTheme by remember { mutableStateOf(isSystemDark) }
            val context = LocalContext.current
            var showOnboarding by remember {
                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                mutableStateOf(!prefs.getBoolean("onboarding_complete", false))
            }
            AINotesSummarizerTheme(darkTheme = darkTheme) {
                if (showOnboarding) {
                    OnboardingScreen {
                        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().putBoolean("onboarding_complete", true).apply()
                        showOnboarding = false
                    }
                } else {
                    AppNavigation(viewModel, darkTheme) { darkTheme = !darkTheme }
                }
            }
        }
    }
}

// --- App Navigation ---
@Composable
fun AppNavigation(viewModel: SummarizerViewModel, darkTheme: Boolean, onThemeChange: () -> Unit) {
    val navController = rememberAnimatedNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedNavHost(navController, AppDestinations.SUMMARIZER_ROUTE, enterTransition={fadeIn(tween(300))}, exitTransition={fadeOut(tween(300))}) {
            composable(AppDestinations.SUMMARIZER_ROUTE) { SummarizerScreenWithPermission(viewModel, navController, darkTheme, onThemeChange) }
            composable(AppDestinations.HISTORY_ROUTE) { HistoryScreen(viewModel, navController) }
            composable(AppDestinations.RESULT_ROUTE) { ResultScreen(viewModel, navController, snackbarHostState) }
            composable(AppDestinations.DETAIL_ROUTE, arguments=listOf(navArgument(AppDestinations.SUMMARY_ID_ARG){type=NavType.IntType})) {
                val summaryId = it.arguments?.getInt(AppDestinations.SUMMARY_ID_ARG) ?: -1
                DetailScreen(summaryId, viewModel, navController, snackbarHostState)
            }
        }
    }
}

// --- OnboardingScreen ---
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    Scaffold(Modifier.fillMaxSize().systemBarsPadding()) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(32.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center) {
            Icon(Icons.Default.AutoAwesome, "AI Icon", Modifier.size(100.dp), tint=MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text("Welcome to AI Summarizer", style=MaterialTheme.typography.headlineSmall, textAlign=TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Text("Scan or paste any text, and let AI create a concise summary for you. Save, edit, and manage your notes with ease.", style=MaterialTheme.typography.bodyLarge, textAlign=TextAlign.Center, color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(48.dp))
            Button(onComplete, Modifier.fillMaxWidth()) { Text("Get Started") }
        }
    }
}

// --- Screen Composables ---
@Composable
fun SummarizerScreenWithPermission(viewModel: SummarizerViewModel, navController: NavController, darkTheme: Boolean, onThemeChange: () -> Unit) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    LaunchedEffect(Unit) { if (!cameraPermissionState.status.isGranted) { cameraPermissionState.launchPermissionRequest() } }
    SummarizerScreen(cameraPermissionState.status.isGranted, viewModel, navController, darkTheme, onThemeChange)
}

// --- FINAL, DEFINITIVE SummarizerScreen ---
@Composable
fun SummarizerScreen(hasPermission: Boolean, viewModel: SummarizerViewModel, navController: NavController, darkTheme: Boolean, onThemeChange: () -> Unit) {
    val view = LocalView.current
    var text by remember { mutableStateOf("") }
    var showCamera by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    var summaryLength by remember { mutableFloatStateOf(150f) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState() // State for the scrollable column

    LaunchedEffect(uiState) {
        if (uiState is SummarizerUiState.Success) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            navController.navigate(AppDestinations.RESULT_ROUTE)
        }
    }

    if (showCamera && hasPermission) {
        CameraPreview(
            onTextRecognized = { recognizedText -> text = recognizedText; showCamera = false },
            onClose = { showCamera = false }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(), // Apply padding for the keyboard here
            topBar = {
                TopAppBar(
                    title = { Text("AI Notes Summarizer") },
                    actions = {
                        IconButton(onClick = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onThemeChange() }) {
                            AnimatedContent(darkTheme, label = "ThemeIcon") { isDark ->
                                Icon(if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode, "Toggle Theme")
                            }
                        }
                        IconButton(onClick = { navController.navigate(AppDestinations.HISTORY_ROUTE) }) { Icon(Icons.Default.History, "History") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(Color.Transparent)
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Summary Length", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = summaryLength,
                        onValueChange = { summaryLength = it },
                        valueRange = 50f..400f,
                        steps = 2
                    )
                    Text(
                        text = when (summaryLength.roundToInt()) {
                            in 50..149 -> "Short"
                            in 150..249 -> "Medium"
                            in 250..349 -> "Large"
                            else -> "Detailed"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                    AnimatedContent(
                        targetState = uiState is SummarizerUiState.Loading,
                        label = "SummarizeBtnAnim",
                        modifier = Modifier.fillMaxWidth()
                    ) { isLoading ->
                        if (isLoading) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                                CircularProgressIndicator()
                            }
                        } else {
                            Button(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    if (text.isNotBlank()) viewModel.summarize(text, summaryLength.roundToInt())
                                },
                                enabled = text.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Summarize Text") }
                        }
                    }
                    if (uiState is SummarizerUiState.Error) {
                        Text(
                            (uiState as SummarizerUiState.Error).errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); if (hasPermission) showCamera = true }) {
                    Icon(Icons.Default.CameraAlt, "Scan Notes")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(scrollState) // This makes the column scrollable
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        // **THE KEY FIX**: On every text change, scroll to the bottom.
                        coroutineScope.launch {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    },
                    label = { Text("Paste or scan notes here...") },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun ResultScreen(viewModel: SummarizerViewModel, navController: NavController, snackbarHostState: SnackbarHostState) {
    val summary by viewModel.latestSummary.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    Scaffold(modifier = Modifier.systemBarsPadding(), topBar = {
        TopAppBar(
            title = { Text("Summary Result") },
            navigationIcon = { IconButton(onClick = { viewModel.resetState(); navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = {
                summary?.let {
                    IconButton(onClick = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); copyToClipboard(context, it.summarizedText); scope.launch { snackbarHostState.showSnackbar("Summary copied!") } }) { Icon(Icons.Default.ContentCopy, "Copy") }
                    IconButton(onClick = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); shareText(context, it.summarizedText) }) { Icon(Icons.Default.Share, "Share") }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }) { padding ->
        summary?.let {
            val bulletPoints = remember(it.summarizedText) {
                it.summarizedText.split("\n").mapNotNull { point ->
                    point.trim().replaceFirst(Regex("^\\s*[*•]\\s*"), "").takeIf { it.isNotBlank() }
                }
            }
            if (bulletPoints.isNotEmpty()) {
                LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    items(bulletPoints) { point ->
                        BulletedListItem(text = point)
                    }
                }
            } else {
                Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text(it.summarizedText, style = MaterialTheme.typography.bodyLarge)
                }
            }
        } ?: Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

@Composable
fun HistoryScreen(viewModel: SummarizerViewModel, navController: NavController) {
    val history by viewModel.filteredHistory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf<Summary?>(null) }
    val view = LocalView.current

    Scaffold(modifier = Modifier.systemBarsPadding(), topBar = {
        TopAppBar(title = { Text("History") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
    }) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            OutlinedTextField(value = searchQuery, onValueChange = { viewModel.onSearchQueryChange(it) }, label = { Text("Search history or tags...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
            AnimatedVisibility(allTags.isNotEmpty()) {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allTags) { tag ->
                        FilterChip(
                            selected = tag == selectedTag,
                            onClick = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); viewModel.onTagSelected(if (tag == selectedTag) null else tag) },
                            label = { Text(tag) },
                            leadingIcon = if (tag == selectedTag) { { Icon(Icons.Default.Done, "Selected") } } else null
                        )
                    }
                }
            }
            if (history.isEmpty()) {
                EmptyState(title = if (searchQuery.isBlank() && selectedTag == null) "No Summaries Yet" else "No Results Found", subtitle = if (searchQuery.isBlank() && selectedTag == null) "Create your first summary to see it here." else "Try a different search or filter.", icon = if (searchQuery.isBlank()) Icons.Default.HistoryEdu else Icons.Default.SearchOff, isIllustration = true)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(history, key = { it.id }) { summary ->
                        HistoryItem(summary = summary, onClick = { navController.navigate(AppDestinations.navigateToDetail(summary.id)) }, onDelete = { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); showDeleteConfirmation = summary })
                    }
                }
            }
        }
    }
    showDeleteConfirmation?.let { summaryToDelete -> ConfirmationDialog("Delete Summary?", "Are you sure you want to permanently delete this summary?", { viewModel.delete(summaryToDelete); showDeleteConfirmation = null }, { showDeleteConfirmation = null }) }
}

@Composable
fun DetailScreen(summaryId: Int, viewModel: SummarizerViewModel, navController: NavController, snackbarHostState: SnackbarHostState) {
    val summaryState by viewModel.getSummaryById(summaryId).collectAsState(initial = null)
    var editedText by remember(summaryState) { mutableStateOf(summaryState?.summarizedText ?: "") }
    var editedTags by remember(summaryState) { mutableStateOf(summaryState?.tags ?: "") }
    var isEditing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    Scaffold(modifier = Modifier.systemBarsPadding(), topBar = {
        TopAppBar(
            title = { Text(if (isEditing) "Edit Summary" else "Summary Detail") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = {
                summaryState?.let { summary ->
                    if (isEditing) {
                        IconButton(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.updateSummary(summary.copy(summarizedText = editedText, tags = editedTags))
                            isEditing = false
                            scope.launch { snackbarHostState.showSnackbar("Summary saved!") }
                        }) { Icon(Icons.Default.Done, "Save") }
                    } else {
                        IconButton(onClick = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); viewModel.togglePin(summary) }) {
                            Icon(if (summary.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, "Pin Summary")
                        }
                        IconButton(onClick = { isEditing = true }) { Icon(Icons.Outlined.Edit, "Edit") }
                        IconButton(onClick = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); copyToClipboard(context, summary.summarizedText); scope.launch { snackbarHostState.showSnackbar("Summary copied!") } }) { Icon(Icons.Default.ContentCopy, "Copy") }
                        IconButton(onClick = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); shareText(context, summary.summarizedText) }) { Icon(Icons.Default.Share, "Share") }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }) { padding ->
        summaryState?.let { summary ->
            Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize()) {
                if (isEditing) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = editedText, onValueChange = { editedText = it }, modifier = Modifier.fillMaxWidth().height(300.dp), label = { Text("Edit your summary") })
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(value = editedTags, onValueChange = { editedTags = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Tags (e.g., work, ideas, urgent)") })
                        Spacer(Modifier.height(16.dp))
                    }
                } else {
                    val bulletPoints = remember(summary.summarizedText) {
                        summary.summarizedText.split("\n").mapNotNull { point ->
                            point.trim().replaceFirst(Regex("^\\s*[*•]\\s*"), "").takeIf { it.isNotBlank() }
                        }
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp)) {
                        if (summary.tags.isNotBlank()) {
                            item {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                                    summary.tags.split(",").forEach { tag ->
                                        if (tag.isNotBlank()) AssistChip(onClick = {}, label = { Text(tag.trim()) })
                                    }
                                }
                            }
                        }
                        if (bulletPoints.isNotEmpty()) {
                            items(bulletPoints) { point ->
                                BulletedListItem(text = point)
                            }
                        } else {
                            item { Text(summary.summarizedText, style = MaterialTheme.typography.bodyLarge) }
                        }
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}


// --- Other Composables ---
@Composable
fun BulletedListItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun HistoryItem(summary: Summary, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(summary.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (summary.isPinned) {
                        Icon(Icons.Filled.PushPin, "Pinned", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(summary.summarizedText, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (summary.tags.isNotBlank()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        summary.tags.split(",").take(4).forEach { tag ->
                            if (tag.isNotBlank()) AssistChip(onClick = {}, label = { Text(tag.trim(), style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete Summary", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun ConfirmationDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest=onDismiss, title={Text(title)}, text={Text(text)}, confirmButton={TextButton(onConfirm){Text("Confirm")}}, dismissButton={TextButton(onDismiss){Text("Cancel")}})
}

@Composable
fun EmptyState(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isIllustration: Boolean = false) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, modifier = Modifier.size(if (isIllustration) 120.dp else 64.dp), tint = if (isIllustration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- Utility & Camera Functions ---
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Copied Summary", text)
    clipboard.setPrimaryClip(clip)
}

private fun shareText(context: Context, text: String) {
    val sendIntent: Intent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, text); type = "text/plain" }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

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
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                imageAnalysis = analysis
                try { cameraProvider.unbindAll(); cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analysis) }
                catch (e: Exception) { Log.e("CameraPreview", "Binding failed", e) }
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        Button(onClick = { imageAnalysis?.setAnalyzer(executor, TextRecognitionAnalyzer(onTextRecognized)) }, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) { Text("Scan Text") }
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { Icon(Icons.Default.Close, "Close Camera", tint = Color.White) }
    }
}

private class TextRecognitionAnalyzer(private val onTextRecognized: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var isBusy = false
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: androidx.camera.core.ImageProxy) {
        if (isBusy) { imageProxy.close(); return }
        isBusy = true
        imageProxy.image?.let { mediaImage ->
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            recognizer.process(image)
                .addOnSuccessListener { onTextRecognized(it.text) }
                .addOnFailureListener { e -> Log.e("TextAnalyzer", "Text recognition failed", e) }
                .addOnCompleteListener { isBusy = false; imageProxy.close() }
        } ?: run { isBusy = false; imageProxy.close() }
    }
}
