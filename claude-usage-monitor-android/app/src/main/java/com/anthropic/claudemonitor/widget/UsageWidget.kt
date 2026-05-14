package com.anthropic.claudemonitor.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.anthropic.claudemonitor.MainActivity
import com.anthropic.claudemonitor.R
import com.anthropic.claudemonitor.data.api.ApiClientFactory
import com.anthropic.claudemonitor.data.api.Result
import com.anthropic.claudemonitor.data.api.UsageRepository
import com.anthropic.claudemonitor.util.Prefs
import com.anthropic.claudemonitor.util.formatTokens
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

        // Tap to open app
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_pct_text, pi)

        // Show loading state immediately
        views.setTextViewText(R.id.widget_pct_text, "…")
        appWidgetManager.updateAppWidget(widgetId, views)

        // Fetch data in background
        CoroutineScope(Dispatchers.IO).launch {
            val repo = UsageRepository(ApiClientFactory.create(Prefs.serverUrl(context)))
            val result = repo.getStats(Prefs.billingStart(context), Prefs.planLimit(context))

            val updatedViews = RemoteViews(context.packageName, R.layout.widget_usage)
            updatedViews.setOnClickPendingIntent(R.id.widget_pct_text, pi)

            when (result) {
                is Result.Success -> {
                    val s = result.data
                    val pct = s.periodUsagePct
                    val days = s.daysRemainingInPeriod

                    updatedViews.setTextViewText(
                        R.id.widget_pct_text,
                        if (pct != null) "%.0f%%".format(pct) else "${s.periodTokens.formatTokens()}"
                    )
                    updatedViews.setProgressBar(
                        R.id.widget_progress,
                        100,
                        pct?.roundToInt() ?: 0,
                        false
                    )
                    updatedViews.setTextViewText(
                        R.id.widget_days_text,
                        if (days != null) "%.1f days left".format(days) else "No period set"
                    )
                    updatedViews.setTextViewText(
                        R.id.widget_today_text,
                        "Today: ${s.todayTokens.formatTokens()}"
                    )
                }
                is Result.Error -> {
                    updatedViews.setTextViewText(R.id.widget_pct_text, "ERR")
                    updatedViews.setTextViewText(R.id.widget_days_text, "Tap to retry")
                    updatedViews.setTextViewText(R.id.widget_today_text, "")
                }
            }
            appWidgetManager.updateAppWidget(widgetId, updatedViews)
        }
    }
}
