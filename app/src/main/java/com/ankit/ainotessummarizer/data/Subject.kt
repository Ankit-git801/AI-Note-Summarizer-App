package com.ankit.ainotessummarizer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val color: Int, // Hex color for the UI
    val iconName: String = "Folder"
)
