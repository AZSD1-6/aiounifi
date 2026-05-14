package com.anthropic.claudemonitor.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val dtFmt   = SimpleDateFormat("MMM d, HH:mm", Locale.US)
private val dateFmt = SimpleDateFormat("MMM d", Locale.US)
private val isoFmt  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).also {
    it.timeZone = TimeZone.getTimeZone("UTC")
}

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
        val clean = this.substringBefore("+").replace("Z", "").trimEnd()
        dtFmt.format(isoFmt.parse(clean) ?: return this)
    } catch (_: Exception) { this }
}

fun String?.formatDate(): String {
    if (this.isNullOrBlank()) return "—"
    return try {
        val clean = this.substringBefore("+").replace("Z", "").trimEnd()
        dateFmt.format(isoFmt.parse(clean) ?: return this)
    } catch (_: Exception) { this }
}

fun String?.shortModel(): String {
    if (this.isNullOrBlank()) return "unknown"
    return this
        .removePrefix("claude-")
        .replace("-20251001", "")
        .replace("-", " ")
}
