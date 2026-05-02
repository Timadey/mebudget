package com.mebudget.app.ui

import com.mebudget.app.data.BudgetSummary
import com.mebudget.app.data.formatAmount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

fun String.filterNumericInput(): String = filter { it.isDigit() || it == ',' }

fun BudgetSummary.formatDateRange(): String {
    val start = startDateEpochDay?.let(LocalDate::ofEpochDay)?.toString()
    val end = endDateEpochDay?.let(LocalDate::ofEpochDay)?.toString()
    return when {
        start != null && end != null -> "$start to $end"
        start != null -> "Starts $start"
        else -> "No date range"
    }
}

fun String.toPickerMillis(): Long? {
    return parseDateOrNull()?.let { epochDay ->
        LocalDate.ofEpochDay(epochDay).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
}

fun Long.toStoredDate(): String {
    return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate().toString()
}

fun maskedAmount(amount: Long, privacyModeEnabled: Boolean): String {
    if (!privacyModeEnabled) return formatAmount(amount)
    return if (amount < 0L) "-••••" else "••••"
}

private fun String.parseDateOrNull(): Long? {
    if (isBlank()) return null
    return runCatching { LocalDate.parse(trim()).toEpochDay() }.getOrNull()
}
