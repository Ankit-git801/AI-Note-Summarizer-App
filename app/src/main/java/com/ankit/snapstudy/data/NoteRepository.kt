package com.ankit.snapstudy.data

import android.graphics.Bitmap
import android.util.Log
import com.ankit.snapstudy.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class NoteRepository(
    private val noteDao: NoteDao,
    private val subjectDao: SubjectDao
) {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.1f // Lower temperature for faster, more deterministic output
            topP = 0.8f
            topK = 20
            responseMimeType = "application/json"
        }
    )

    fun getNotesForSubject(subjectId: Int): Flow<List<Note>> = noteDao.getNotesForSubject(subjectId)
    fun getAllSubjects(): Flow<List<Subject>> = subjectDao.getAllSubjects()
    fun getNoteById(noteId: Int): Flow<Note?> = noteDao.getNoteById(noteId)

    suspend fun insertSubject(name: String, color: Int) {
        subjectDao.insert(Subject(name = name, color = color))
    }

    suspend fun processNotesFromImages(bitmaps: List<Bitmap>, subjectId: Int): Result<Note> {
        val resizedBitmaps = bitmaps.map { resizeAndCompressBitmap(it) }
        val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
        val defaultTitle = "Scan - $date"

        val prompt = """
            Analyze these handwritten or printed lecture notes provided across ${bitmaps.size} images. 
            Perform the following tasks:
            1. Transcribe the full text from ALL images accurately and combine them into a single coherent document.
            2. Summarize the content into clear bullet points.
            3. Extract 3-5 'Key Concepts' from the combined notes.
            4. Generate 3-5 study 'Flashcards' (Question and Answer pairs) based on the overall content.

            Return the result in the following JSON format:
            {
              "transcription": "...",
              "summary": "...",
              "key_concepts": ["concept1", "concept2", ...],
              "flashcards": [
                {"question": "...", "answer": "..."},
                ...
              ]
            }
        """.trimIndent()

        return try {
            val inputContent = content {
                resizedBitmaps.forEach { image(it) }
                text(prompt)
            }

            val response = generativeModel.generateContent(inputContent)
            val jsonResponse = response.text ?: throw Exception("Empty AI response")
            val jsonObject = JSONObject(jsonResponse)

            val note = Note(
                subjectId = subjectId,
                title = defaultTitle,
                originalText = jsonObject.getString("transcription"),
                summarizedText = jsonObject.getString("summary"),
                keyConcepts = jsonObject.getJSONArray("key_concepts").toString(),
                flashcards = jsonObject.getJSONArray("flashcards").toString()
            )

            val id = noteDao.insert(note)
            Result.success(note.copy(id = id.toInt()))
        } catch (e: Exception) {
            Log.e("NoteRepository", "AI Error", e)
            Result.failure(e)
        }
    }

    suspend fun combineNotesIntoChapter(notes: List<Note>, chapterTitle: String, subjectId: Int): Result<Note> {
        val sortedNotes = notes.sortedBy { it.timestamp }
        val combinedOriginalText = sortedNotes.joinToString("\n\n---\n\n") { it.originalText }
        
        val prompt = """
            You are an expert academic assistant. I am providing you with multiple sets of lecture notes that form a single chapter.
            Please perform the following:
            1. Combine all the notes into one cohesive, high-level summary using clear bullet points.
            2. Extract the most important 'Key Concepts' across all the notes.
            3. Generate a comprehensive set of 5-10 study 'Flashcards' that cover the entire chapter.

            Return the result in the following JSON format:
            {
              "summary": "...",
              "key_concepts": ["concept1", "concept2", ...],
              "flashcards": [
                {"question": "...", "answer": "..."},
                ...
              ]
            }

            Combined Notes Content:
            $combinedOriginalText
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt)
            val jsonResponse = response.text ?: throw Exception("Empty AI response")
            val jsonObject = JSONObject(jsonResponse)

            val chapterNote = Note(
                subjectId = subjectId,
                title = chapterTitle,
                originalText = combinedOriginalText,
                summarizedText = jsonObject.getString("summary"),
                keyConcepts = jsonObject.getJSONArray("key_concepts").toString(),
                flashcards = jsonObject.getJSONArray("flashcards").toString()
            )

            val id = noteDao.insert(chapterNote)
            
            // Delete original notes as requested
            notes.forEach { noteDao.delete(it) }
            
            Result.success(chapterNote.copy(id = id.toInt()))
        } catch (e: Exception) {
            Log.e("NoteRepository", "Combine Error", e)
            Result.failure(e)
        }
    }

    private fun resizeAndCompressBitmap(bitmap: Bitmap): Bitmap {
        val maxWidth = 1024
        val maxHeight = 1024
        val width = bitmap.width
        val height = bitmap.height

        val ratio = width.toFloat() / height.toFloat()
        var newWidth = maxWidth
        var newHeight = (maxWidth / ratio).toInt()

        if (newHeight > maxHeight) {
            newHeight = maxHeight
            newWidth = (maxHeight * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    suspend fun updateNote(note: Note) = noteDao.update(note)
    suspend fun deleteNote(note: Note) = noteDao.delete(note)
    suspend fun deleteSubject(subject: Subject) = subjectDao.delete(subject)
    suspend fun updateSubject(subject: Subject) = subjectDao.update(subject)
}

