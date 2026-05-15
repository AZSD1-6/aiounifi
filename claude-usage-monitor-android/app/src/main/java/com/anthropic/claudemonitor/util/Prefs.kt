package com.anthropic.claudemonitor.util

import android.content.Context
import androidx.preference.PreferenceManager

object Prefs {
    private const val KEY_REFRESH_SECS = "refresh_seconds"

    fun refreshSeconds(ctx: Context): Int =
        PreferenceManager.getDefaultSharedPreferences(ctx)
            .getString(KEY_REFRESH_SECS, "30")?.toIntOrNull() ?: 30
}
