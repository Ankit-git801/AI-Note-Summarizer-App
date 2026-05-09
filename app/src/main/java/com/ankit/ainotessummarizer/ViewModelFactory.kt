package com.ankit.ainotessummarizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ankit.ainotessummarizer.data.SummaryDao

class ViewModelFactory(private val dao: SummaryDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SummarizerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SummarizerViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
