package com.ankit.ainotessummarizer.data

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = Summary::class)
@Entity(tableName = "summaries_fts")
data class SummaryFts(
    val originalText: String,
    val summarizedText: String,
    val tags: String
)
