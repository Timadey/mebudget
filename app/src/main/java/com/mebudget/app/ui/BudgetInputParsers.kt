package com.mebudget.app.ui

import com.mebudget.app.data.TransactionType
import java.time.LocalDate

fun String.requireNotBlank(message: String): String {
    return trim().ifBlank { throw IllegalArgumentException(message) }
}

fun String.parseAmount(): Long {
    val cleaned = replace(",", "").trim()
    return cleaned.toLongOrNull()?.takeIf { it > 0 }
        ?: throw IllegalArgumentException("Enter a valid amount.")
}

fun String.parseSignedAmount(): Long {
    val cleaned = replace(",", "").trim()
    return cleaned.toLongOrNull()?.takeIf { it != 0L }
        ?: throw IllegalArgumentException("Enter a valid signed amount.")
}

fun String.parseAmountAllowSigned(type: TransactionType): Long {
    return when (type) {
        TransactionType.ADJUSTMENT -> parseSignedAmount()
        else -> parseAmount()
    }
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
