package com.anthropic.claudemonitor.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.anthropic.claudemonitor.MainActivity
import com.anthropic.claudemonitor.R
import com.anthropic.claudemonitor.data.api.Result
import com.anthropic.claudemonitor.data.auth.ClaudeAiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class UsageWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_usage)

        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_pct_text, pi)

        views.setTextViewText(R.id.widget_pct_text, "…")
        appWidgetManager.updateAppWidget(widgetId, views)

        CoroutineScope(Dispatchers.IO).launch {
            val repo = ClaudeAiRepository(context)
            val result = repo.getUsage()

            val updatedViews = RemoteViews(context.packageName, R.layout.widget_usage)
            updatedViews.setOnClickPendingIntent(R.id.widget_pct_text, pi)

            when (result) {
                is Result.Success -> {
                    val u = result.data
                    val used  = u.used ?: 0L
                    val limit = u.limit
                    val pct   = if (limit != null && limit > 0) used.toDouble() / limit * 100.0 else null

                    updatedViews.setTextViewText(
                        R.id.widget_pct_text,
                        if (pct != null) "%.0f%%".format(pct) else "${fmtNum(used)}"
                    )
                    updatedViews.setProgressBar(
                        R.id.widget_progress,
                        100,
                        pct?.roundToInt() ?: 0,
                        false
                    )
                    updatedViews.setTextViewText(
                        R.id.widget_days_text,
                        u.resetTimestamp?.let { "Resets: $it" } ?: ""
                    )
                    updatedViews.setTextViewText(
                        R.id.widget_today_text,
                        if (limit != null) "${fmtNum(used)} / ${fmtNum(limit)}" else "${fmtNum(used)} used"
                    )
                }
                is Result.Error -> {
                    updatedViews.setTextViewText(R.id.widget_pct_text, "—")
                    updatedViews.setTextViewText(R.id.widget_days_text, "Tap to open")
                    updatedViews.setTextViewText(R.id.widget_today_text, "")
                }
            }
            appWidgetManager.updateAppWidget(widgetId, updatedViews)
        }
    }

    private fun fmtNum(n: Long): String = when {
        n >= 1_000_000L -> "%.1fM".format(n / 1_000_000.0)
        n >= 1_000L     -> "%.1fK".format(n / 1_000.0)
        else            -> n.toString()
    }
}
