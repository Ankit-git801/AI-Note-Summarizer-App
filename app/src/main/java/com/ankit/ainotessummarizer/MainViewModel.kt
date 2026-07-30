package com.ankit.ainotessummarizer

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ankit.ainotessummarizer.data.Note
import com.ankit.ainotessummarizer.data.NoteRepository
import com.ankit.ainotessummarizer.data.Subject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(private val repository: NoteRepository) : ViewModel() {

    val allSubjects = repository.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun getNotesForSubject(subjectId: Int): Flow<List<Note>> = repository.getNotesForSubject(subjectId)
    fun getNoteById(noteId: Int): Flow<Note?> = repository.getNoteById(noteId)

    fun addSubject(name: String, color: Int) {
        viewModelScope.launch {
            repository.insertSubject(name, color)
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
        }
    }

    fun processImages(bitmaps: List<Bitmap>, subjectId: Int) {
        if (bitmaps.isEmpty()) return
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.processNotesFromImages(bitmaps, subjectId)
                .onSuccess {
                    _uiState.value = UiState.Success(it)
                }
                .onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Unknown error")
                }
        }
    }

    fun resetUiState() {
        _uiState.value = UiState.Idle
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }
}

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val note: Note) : UiState
    data class Error(val message: String) : UiState
}
