package com.anthropic.claudemonitor.data.models

import com.google.gson.annotations.SerializedName

data class Stats(
    @SerializedName("total_input_tokens")       val totalInputTokens: Long,
    @SerializedName("total_output_tokens")      val totalOutputTokens: Long,
    @SerializedName("total_cache_write_tokens") val totalCacheWriteTokens: Long,
    @SerializedName("total_cache_read_tokens")  val totalCacheReadTokens: Long,
    @SerializedName("total_tokens")             val totalTokens: Long,
    @SerializedName("total_estimated_cost_usd") val totalEstimatedCostUsd: Double,
    @SerializedName("total_sessions")           val totalSessions: Int,
    @SerializedName("total_projects")           val totalProjects: Int,

    @SerializedName("today_input_tokens")       val todayInputTokens: Long,
    @SerializedName("today_output_tokens")      val todayOutputTokens: Long,
    @SerializedName("today_cache_write_tokens") val todayCacheWriteTokens: Long,
    @SerializedName("today_cache_read_tokens")  val todayCacheReadTokens: Long,
    @SerializedName("today_tokens")             val todayTokens: Long,
    @SerializedName("today_estimated_cost_usd") val todayEstimatedCostUsd: Double,

    @SerializedName("period_input_tokens")      val periodInputTokens: Long,
    @SerializedName("period_output_tokens")     val periodOutputTokens: Long,
    @SerializedName("period_tokens")            val periodTokens: Long,
    @SerializedName("period_estimated_cost_usd") val periodEstimatedCostUsd: Double,
    @SerializedName("period_usage_pct")         val periodUsagePct: Double?,
    @SerializedName("plan_token_limit")         val planTokenLimit: Long?,
    @SerializedName("billing_start")            val billingStart: String?,
    @SerializedName("billing_end")              val billingEnd: String?,
    @SerializedName("days_remaining_in_period") val daysRemainingInPeriod: Double?,
    @SerializedName("models_used")              val modelsUsed: List<String>,
    @SerializedName("last_updated")             val lastUpdated: String,
)
