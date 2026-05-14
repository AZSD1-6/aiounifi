package com.anthropic.claudemonitor.data.api

import com.anthropic.claudemonitor.data.models.Project
import com.anthropic.claudemonitor.data.models.Session
import com.anthropic.claudemonitor.data.models.Stats

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

class UsageRepository(private val api: ClaudeMonitorApi) {

    suspend fun getStats(billingStart: String?, planLimit: Long?): Result<Stats> = safeCall {
        val resp = api.getStats(billingStart, planLimit)
        if (resp.isSuccessful) resp.body()!! else throw Exception("HTTP ${resp.code()}")
    }

    suspend fun getSessions(limit: Int = 100, offset: Int = 0): Result<List<Session>> = safeCall {
        val resp = api.getSessions(limit, offset)
        if (resp.isSuccessful) resp.body()!!.sessions else throw Exception("HTTP ${resp.code()}")
    }

    suspend fun getProjects(): Result<List<Project>> = safeCall {
        val resp = api.getProjects()
        if (resp.isSuccessful) resp.body()!!.projects else throw Exception("HTTP ${resp.code()}")
    }

    suspend fun checkHealth(): Result<String> = safeCall {
        val resp = api.health()
        if (resp.isSuccessful) "ok" else throw Exception("HTTP ${resp.code()}")
    }

    private inline fun <T> safeCall(block: () -> T): Result<T> = try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(e.message ?: "Unknown error")
    }
}
