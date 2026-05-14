package com.anthropic.claudemonitor.data.models

import com.google.gson.annotations.SerializedName

data class Project(
    @SerializedName("name")                 val name: String,
    @SerializedName("path")                 val path: String,
    @SerializedName("sessions")             val sessions: Int,
    @SerializedName("input_tokens")         val inputTokens: Long,
    @SerializedName("output_tokens")        val outputTokens: Long,
    @SerializedName("cache_write_tokens")   val cacheWriteTokens: Long,
    @SerializedName("cache_read_tokens")    val cacheReadTokens: Long,
    @SerializedName("total_tokens")         val totalTokens: Long,
    @SerializedName("estimated_cost_usd")   val estimatedCostUsd: Double,
)

data class ProjectsResponse(
    @SerializedName("projects") val projects: List<Project>,
)
