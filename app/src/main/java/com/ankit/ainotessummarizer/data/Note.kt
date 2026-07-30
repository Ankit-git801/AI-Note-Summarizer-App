package com.ankit.ainotessummarizer.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subjectId: Int,
    val originalText: String,
    val summarizedText: String,
    val keyConcepts: String = "", // Comma-separated or JSON list
    val flashcards: String = "", // JSON representation of List<Flashcard>
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val tags: String = ""
)

data class Flashcard(
    val question: String,
    val answer: String
)
