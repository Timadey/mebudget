package com.mebudget.app.ui

import java.time.LocalDate

fun String.requireNotBlank(message: String): String {
    return trim().ifBlank { throw IllegalArgumentException(message) }
}

fun String.parseAmount(): Long {
    val cleaned = replace(",", "").trim()
    return cleaned.toLongOrNull()?.takeIf { it > 0 }
        ?: throw IllegalArgumentException("Enter a valid amount.")
}

fun String.parseRequiredDate(): Long {
    return runCatching { LocalDate.parse(trim()) }.getOrElse {
        throw IllegalArgumentException("Use date format YYYY-MM-DD.")
    }.toEpochDay()
}

fun String.parseOptionalDate(): Long? {
    if (isBlank()) return null
    return parseRequiredDate()
}
