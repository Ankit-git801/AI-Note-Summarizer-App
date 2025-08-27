package com.yourname.ainotessummarizer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    @Insert
    suspend fun insert(summary: Summary)

    @Query("SELECT * FROM summaries ORDER BY timestamp DESC")
    fun getAllSummaries(): Flow<List<Summary>>

    @Query("SELECT * FROM summaries WHERE id = :summaryId")
    fun getSummaryById(summaryId: Int): Flow<Summary?>

    @Update
    suspend fun update(summary: Summary)

    @Delete
    suspend fun delete(summary: Summary)
}
