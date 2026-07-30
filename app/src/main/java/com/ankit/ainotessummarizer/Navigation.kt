package com.ankit.ainotessummarizer

import androidx.navigation.NavType
import androidx.navigation.navArgument

// Defines the navigation routes and arguments used in the app
object AppDestinations {
    const val SUBJECTS_ROUTE = "subjects"
    const val NOTES_ROUTE = "notes/{subjectId}"
    const val NOTE_DETAIL_ROUTE = "note_detail/{noteId}"
    const val STUDY_MODE_ROUTE = "study/{noteId}"
    const val CAMERA_ROUTE = "camera/{subjectId}"

    const val SUBJECT_ID_ARG = "subjectId"
    const val NOTE_ID_ARG = "noteId"

    fun navigateToNotes(subjectId: Int) = "notes/$subjectId"
    fun navigateToNoteDetail(noteId: Int) = "note_detail/$noteId"
    fun navigateToStudy(noteId: Int) = "study/$noteId"
    fun navigateToCamera(subjectId: Int) = "camera/$subjectId"
}
