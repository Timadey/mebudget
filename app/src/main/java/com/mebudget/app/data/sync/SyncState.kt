package com.mebudget.app.data.sync

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Error(
        val message: String,
        val retryable: Boolean = true
    ) : SyncState()
    data class Pending(val count: Int) : SyncState()
    data class Paused(val reason: String) : SyncState()

    companion object {
        fun friendlyMessage(raw: String): String {
            val lower = raw.lowercase()
            return when {
                lower.contains("unable to resolve host") ||
                lower.contains("unknownhost") ||
                lower.contains("could not resolve host") ||
                lower.contains("nodename nor hints") ->
                    "No internet connection. Please check your network."

                lower.contains("connectexception") ||
                lower.contains("connection refused") ||
                lower.contains("connection timed out") ->
                    "Cannot connect to server. Please try again."

                lower.contains("timeout") ->
                    "Connection timed out. Please try again."

                lower.contains("socket closed") ||
                lower.contains("interruptedexception") ->
                    "Sync was interrupted. Tap retry."

                lower.contains("job cancelled") ->
                    "Sync was interrupted. Tap retry."

                lower.contains("500") || lower.contains("internal server error") ->
                    "Server is temporarily unavailable. Please try again."

                lower.contains("400") || lower.contains("bad request") ->
                    "Something went wrong. Please try again."

                lower.contains("401") || lower.contains("unauthorized") ->
                    "Session expired. Please sign in again."

                lower.contains("403") || lower.contains("forbidden") ->
                    "Access denied. Please sign in again."

                lower.contains("404") || lower.contains("not found") ->
                    "Server error. Please try again."

                lower.contains("429") || lower.contains("too many requests") ->
                    "Too many requests. Please wait a moment and try again."

                lower.contains("502") || lower.contains("503") || lower.contains("504") ->
                    "Server is temporarily unavailable. Please try again."

                lower.contains("ssl") || lower.contains("certificate") ->
                    "Secure connection failed. Please try again."

                lower.contains("not signed in") ->
                    "Not signed in. Please sign in to sync."

                else -> raw.ifBlank { "Sync failed. Please try again." }
            }
        }
    }
}
