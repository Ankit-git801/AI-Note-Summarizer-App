package com.ankit.snapstudy.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ankit.snapstudy.AppDestinations
import com.ankit.snapstudy.MainViewModel
import com.ankit.snapstudy.UiState
import com.ankit.snapstudy.data.Note
import com.ankit.snapstudy.data.Subject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SubjectsScreen(viewModel: MainViewModel, navController: NavController) {
    val subjects by viewModel.allSubjects.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }
    var subjectToEdit by remember { mutableStateOf<Subject?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Smart Notebook", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Subject")
            }
        }
    ) { padding ->
        if (subjects.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Add your first subject to get started!", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.padding(padding).fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(subjects) { subject ->
                    SubjectCard(
                        subject = subject,
                        onClick = {
                            navController.navigate(AppDestinations.navigateToNotes(subject.id))
                        },
                        onDelete = {
                            subjectToDelete = subject
                        },
                        onEdit = {
                            subjectToEdit = subject
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        var subjectName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Subject") },
            text = {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject Name (e.g. Physics)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (subjectName.isNotBlank()) {
                        viewModel.addSubject(subjectName, 0xFF6200EE.toInt()) // Default purple
                        showAddDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    subjectToEdit?.let { subject ->
        var newName by remember { mutableStateOf(subject.name) }
        AlertDialog(
            onDismissRequest = { subjectToEdit = null },
            title = { Text("Edit Subject") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Subject Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.updateSubject(subject.copy(name = newName))
                        subjectToEdit = null
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { subjectToEdit = null }) { Text("Cancel") }
            }
        )
    }

    subjectToDelete?.let { subject ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Delete Subject?") },
            text = { Text("Are you sure you want to delete '${subject.name}' and all its notes?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSubject(subject)
                    subjectToDelete = null
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SubjectCard(subject: Subject, onClick: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().height(120.dp).combinedClickable(
            onClick = onClick,
            onLongClick = { showMenu = true }
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(subject.color).copy(alpha = 0.1f))
    ) {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            Icon(Icons.Default.Folder, null, tint = Color(subject.color), modifier = Modifier.size(32.dp))
            Text(
                subject.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomStart)
            )
            
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.MoreVert, "More", modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(subjectId: Int, viewModel: MainViewModel, navController: NavController) {
    val notes by viewModel.getNotesForSubject(subjectId).collectAsState(emptyList())
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    val selectedNotes = remember { mutableStateListOf<Note>() }
    var showCombineDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                val bitmaps = uris.mapNotNull { uri ->
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                        } else {
                            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                viewModel.processImages(bitmaps, subjectId)
            }
        }
    )

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            val note = (uiState as UiState.Success).note
            viewModel.resetUiState()
            selectedNotes.clear()
            navController.navigate(AppDestinations.navigateToNoteDetail(note.id))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (selectedNotes.isEmpty()) "Notes" else "${selectedNotes.size} Selected") 
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (selectedNotes.isEmpty()) navController.popBackStack() 
                        else selectedNotes.clear()
                    }) {
                        Icon(if (selectedNotes.isEmpty()) Icons.Default.ArrowBack else Icons.Default.Close, "Back")
                    }
                },
                actions = {
                    if (selectedNotes.size > 1) {
                        IconButton(onClick = { showCombineDialog = true }) {
                            Icon(Icons.Default.AutoAwesomeMotion, "Combine into Chapter")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedNotes.isEmpty()) {
                Column(horizontalAlignment = Alignment.End) {
                    FloatingActionButton(
                        onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, "Gallery")
                    }
                    FloatingActionButton(onClick = { navController.navigate(AppDestinations.navigateToCamera(subjectId)) }) {
                        Icon(Icons.Default.CameraAlt, "Scan Note")
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (notes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No notes scanned yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notes) { note ->
                        val isSelected = selectedNotes.contains(note)
                        NoteItem(
                            note = note,
                            isSelected = isSelected,
                            onSelect = {
                                if (isSelected) selectedNotes.remove(note) else selectedNotes.add(note)
                            },
                            onClick = {
                                if (selectedNotes.isEmpty()) {
                                    navController.navigate(AppDestinations.navigateToNoteDetail(note.id))
                                } else {
                                    if (isSelected) selectedNotes.remove(note) else selectedNotes.add(note)
                                }
                            },
                            onDelete = {
                                noteToDelete = note
                            },
                            onEdit = {
                                noteToEdit = note
                            }
                        )
                    }
                }
            }

            if (uiState is UiState.Loading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(16.dp))
                            Text("AI is processing your images...", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showCombineDialog) {
        var chapterTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCombineDialog = false },
            title = { Text("Combine into Chapter") },
            text = {
                Column {
                    Text("This will merge selected notes into a single cohesive chapter with a fresh AI summary.")
                    Spacer(Modifier.height(8.dp))
                    Text("⚠️ Warning: Original individual notes will be deleted.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = chapterTitle,
                        onValueChange = { chapterTitle = it },
                        label = { Text("Chapter Title (e.g. Chapter 1)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (chapterTitle.isNotBlank()) {
                        viewModel.combineNotes(selectedNotes.toList(), chapterTitle, subjectId)
                        showCombineDialog = false
                    }
                }) { Text("Combine") }
            },
            dismissButton = {
                TextButton(onClick = { showCombineDialog = false }) { Text("Cancel") }
            }
        )
    }

    noteToEdit?.let { note ->
        var newName by remember { mutableStateOf(note.title) }
        AlertDialog(
            onDismissRequest = { noteToEdit = null },
            title = { Text("Edit Note Name") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Note Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.updateNote(note.copy(title = newName))
                        noteToEdit = null
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { noteToEdit = null }) { Text("Cancel") }
            }
        )
    }

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Note?") },
            text = { Text("Are you sure you want to permanently delete this note?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote(note)
                    noteToDelete = null
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    note: Note, 
    isSelected: Boolean = false,
    onSelect: () -> Unit,
    onClick: () -> Unit, 
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onSelect
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelected) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).padding(end = 8.dp))
                }
                Text(
                    note.title.ifBlank { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.timestamp)) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (note.isPinned) Icon(Icons.Default.PushPin, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                
                var showNoteMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showNoteMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, "More", modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = showNoteMenu, onDismissRequest = { showNoteMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit Name") },
                            onClick = { showNoteMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { showNoteMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                note.summarizedText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

