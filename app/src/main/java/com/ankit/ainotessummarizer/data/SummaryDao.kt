package com.ankit.ainotessummarizer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    @Insert
    suspend fun insert(summary: Summary): Long

    // Updated query to sort by pinned status first, then timestamp
    @Query("SELECT * FROM summaries ORDER BY isPinned DESC, timestamp DESC")
    fun getAllSummaries(): Flow<List<Summary>>

    @Query("SELECT * FROM summaries WHERE id = :summaryId")
    fun getSummaryById(summaryId: Int): Flow<Summary?>

    @Query("""
        SELECT * FROM summaries 
        JOIN summaries_fts ON summaries.id = summaries_fts.rowid 
        WHERE summaries_fts MATCH :query
        ORDER BY isPinned DESC, timestamp DESC
    """)
    fun searchSummaries(query: String): Flow<List<Summary>>

    @Update
    suspend fun update(summary: Summary)

    @Delete
    suspend fun delete(summary: Summary)
}
