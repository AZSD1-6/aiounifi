package com.anthropic.claudemonitor.data.models

import com.google.gson.annotations.SerializedName

data class Session(
    @SerializedName("session_id")         val sessionId: String,
    @SerializedName("project")            val project: String?,
    @SerializedName("project_path")       val projectPath: String?,
    @SerializedName("model")              val model: String,
    @SerializedName("start_time")         val startTime: String?,
    @SerializedName("last_activity")      val lastActivity: String?,
    @SerializedName("input_tokens")       val inputTokens: Long,
    @SerializedName("output_tokens")      val outputTokens: Long,
    @SerializedName("cache_write_tokens") val cacheWriteTokens: Long,
    @SerializedName("cache_read_tokens")  val cacheReadTokens: Long,
    @SerializedName("total_tokens")       val totalTokens: Long,
    @SerializedName("estimated_cost_usd") val estimatedCostUsd: Double,
    @SerializedName("git_branch")         val gitBranch: String?,
    @SerializedName("cwd")                val cwd: String?,
    @SerializedName("user_messages")      val userMessages: Int,
    @SerializedName("assistant_messages") val assistantMessages: Int,
)

data class SessionsResponse(
    @SerializedName("sessions") val sessions: List<Session>,
    @SerializedName("total")    val total: Int,
    @SerializedName("limit")    val limit: Int,
    @SerializedName("offset")   val offset: Int,
)
