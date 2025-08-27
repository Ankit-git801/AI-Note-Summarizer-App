package com.yourname.ainotessummarizer

import androidx.navigation.NavType
import androidx.navigation.navArgument

// Defines the navigation routes and arguments used in the app
object AppDestinations {
    const val SUMMARIZER_ROUTE = "summarizer"
    const val HISTORY_ROUTE = "history"
    const val RESULT_ROUTE = "result"

    // Route for the detail screen, with a placeholder for the summaryId
    const val DETAIL_ROUTE = "detail/{summaryId}"
    const val SUMMARY_ID_ARG = "summaryId"

    // Helper function to create the full route for navigating to a specific summary
    fun navigateToDetail(summaryId: Int): String {
        return "detail/$summaryId"
    }
}
