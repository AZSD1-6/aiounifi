package com.anthropic.claudemonitor.data.api

import com.anthropic.claudemonitor.data.models.ProjectsResponse
import com.anthropic.claudemonitor.data.models.SessionsResponse
import com.anthropic.claudemonitor.data.models.Stats
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ClaudeMonitorApi {

    @GET("stats")
    suspend fun getStats(
        @Query("billing_start") billingStart: String? = null,
        @Query("plan_limit")    planLimit: Long? = null,
    ): Response<Stats>

    @GET("sessions")
    suspend fun getSessions(
        @Query("limit")  limit: Int = 100,
        @Query("offset") offset: Int = 0,
    ): Response<SessionsResponse>

    @GET("projects")
    suspend fun getProjects(): Response<ProjectsResponse>

    @GET("health")
    suspend fun health(): Response<Map<String, String>>
}
