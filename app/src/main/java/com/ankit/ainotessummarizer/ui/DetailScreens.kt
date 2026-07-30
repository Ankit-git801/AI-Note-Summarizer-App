package com.ankit.ainotessummarizer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ankit.ainotessummarizer.AppDestinations
import com.ankit.ainotessummarizer.MainViewModel
import com.ankit.ainotessummarizer.data.Flashcard
import com.ankit.ainotessummarizer.data.Note
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteDetailScreen(noteId: Int, viewModel: MainViewModel, navController: NavController) {
    val note by viewModel.getNoteById(noteId).collectAsState(null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(AppDestinations.navigateToStudy(noteId)) }) {
                        Icon(Icons.Default.School, "Study Mode")
                    }
                }
            )
        }
    ) { padding ->
        note?.let { n ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(n.summarizedText, style = MaterialTheme.typography.bodyLarge)

                Spacer(Modifier.height(24.dp))
                Text("Key Concepts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                val concepts = remember(n.keyConcepts) {
                    try { JSONArray(n.keyConcepts).let { arr -> List(arr.length()) { i -> arr.getString(i) } } }
                    catch (e: Exception) { emptyList() }
                }
                
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    concepts.forEach { concept ->
                        AssistChip(onClick = {}, label = { Text(concept) })
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("Original Transcription", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(n.originalText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyModeScreen(noteId: Int, viewModel: MainViewModel, navController: NavController) {
    val note by viewModel.getNoteById(noteId).collectAsState(null)
    
    val flashcards = remember(note) {
        note?.let { n ->
            try {
                val arr = JSONArray(n.flashcards)
                List(arr.length()) { i ->
                    val obj = arr.getJSONObject(i)
                    Flashcard(obj.getString("question"), obj.getString("answer"))
                }
            } catch (e: Exception) { emptyList() }
        } ?: emptyList()
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Mode") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (flashcards.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                Text("No flashcards generated for this note.", modifier = Modifier.padding(16.dp))
            }
        } else {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Card ${currentIndex + 1} of ${flashcards.size}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val card = flashcards[currentIndex]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Let card take available space
                        .clickable { showAnswer = !showAnswer },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (showAnswer) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()) // Make content scrollable
                    ) {
                        Text(
                            text = if (showAnswer) "ANSWER" else "QUESTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (showAnswer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (showAnswer) card.answer else card.question,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        onClick = {
                            if (currentIndex > 0) {
                                currentIndex--
                                showAnswer = false
                            }
                        },
                        enabled = currentIndex > 0
                    ) { Text("Previous") }
                    
                    Button(
                        onClick = {
                            if (currentIndex < flashcards.size - 1) {
                                currentIndex++
                                showAnswer = false
                            }
                        },
                        enabled = currentIndex < flashcards.size - 1
                    ) { Text("Next") }
                }
            }
        }
    }
}
