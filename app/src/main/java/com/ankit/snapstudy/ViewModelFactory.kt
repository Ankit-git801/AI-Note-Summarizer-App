package com.ankit.snapstudy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ankit.snapstudy.data.NoteDao
import com.ankit.snapstudy.data.NoteRepository
import com.ankit.snapstudy.data.SubjectDao

class MainViewModelFactory(
    private val noteDao: NoteDao,
    private val subjectDao: SubjectDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val repository = NoteRepository(noteDao, subjectDao)
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

