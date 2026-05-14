package com.anthropic.claudemonitor.util

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val numberFormat = NumberFormat.getNumberInstance(Locale.US)
private val dtFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault())
private val dateFormatter = DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault())

fun Long.formatTokens(): String = when {
    this >= 1_000_000L -> "%.1fM".format(this / 1_000_000.0)
    this >= 1_000L     -> "%.1fK".format(this / 1_000.0)
    else               -> this.toString()
}

fun Double.formatCost(): String = "$%.4f".format(this)

fun Double.formatCostShort(): String = when {
    this >= 1.0  -> "$%.2f".format(this)
    this >= 0.01 -> "$%.3f".format(this)
    else         -> "$%.4f".format(this)
}

fun String?.formatDateTime(): String {
    if (this.isNullOrBlank()) return "—"
    return try {
        dtFormatter.format(Instant.parse(this))
    } catch (_: Exception) { this }
}

fun String?.formatDate(): String {
    if (this.isNullOrBlank()) return "—"
    return try {
        dateFormatter.format(Instant.parse(this))
    } catch (_: Exception) { this }
}

fun String?.shortModel(): String {
    if (this.isNullOrBlank()) return "unknown"
    return this
        .removePrefix("claude-")
        .replace("-20251001", "")
        .replace("-", " ")
}
