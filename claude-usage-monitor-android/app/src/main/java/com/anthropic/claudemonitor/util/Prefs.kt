package com.anthropic.claudemonitor.util

import android.content.Context
import androidx.preference.PreferenceManager

object Prefs {
    private const val KEY_SERVER_URL    = "server_url"
    private const val KEY_BILLING_START = "billing_start"
    private const val KEY_PLAN_TIER     = "plan_tier"
    private const val KEY_PLAN_LIMIT    = "plan_limit"
    private const val KEY_REFRESH_SECS  = "refresh_seconds"

    fun serverUrl(ctx: Context): String =
        PreferenceManager.getDefaultSharedPreferences(ctx)
            .getString(KEY_SERVER_URL, "http://192.168.1.100:8765") ?: "http://192.168.1.100:8765"

    fun billingStart(ctx: Context): String? =
        PreferenceManager.getDefaultSharedPreferences(ctx)
            .getString(KEY_BILLING_START, null)?.takeIf { it.isNotBlank() }

    fun planTier(ctx: Context): String =
        PreferenceManager.getDefaultSharedPreferences(ctx)
            .getString(KEY_PLAN_TIER, "max5x") ?: "max5x"

    fun planLimit(ctx: Context): Long? {
        val custom = PreferenceManager.getDefaultSharedPreferences(ctx)
            .getString(KEY_PLAN_LIMIT, null)?.toLongOrNull()
        if (custom != null && custom > 0) return custom
        return when (planTier(ctx)) {
            "pro"    ->  45_000_000L
            "max5x"  -> 225_000_000L
            "max20x" -> 900_000_000L
            else     -> null
        }
    }

    fun refreshSeconds(ctx: Context): Int =
        PreferenceManager.getDefaultSharedPreferences(ctx)
            .getString(KEY_REFRESH_SECS, "30")?.toIntOrNull() ?: 30
}
