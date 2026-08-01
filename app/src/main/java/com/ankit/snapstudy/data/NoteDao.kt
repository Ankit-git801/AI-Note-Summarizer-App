package com.ankit.snapstudy.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: Note): Long

    @Query("SELECT * FROM notes WHERE subjectId = :subjectId ORDER BY isPinned DESC, timestamp DESC")
    fun getNotesForSubject(subjectId: Int): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteById(noteId: Int): Flow<Note?>

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)
}

