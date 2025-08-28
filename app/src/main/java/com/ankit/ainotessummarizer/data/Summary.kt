package com.yourname.ainotessummarizer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "summaries")
data class Summary(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val originalText: String,
    val summarizedText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val tags: String = "" // Comma-separated list of tags
)
