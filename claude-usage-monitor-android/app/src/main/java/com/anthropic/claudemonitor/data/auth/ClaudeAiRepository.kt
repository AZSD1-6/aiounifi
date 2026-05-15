package com.anthropic.claudemonitor.data.auth

import android.content.Context
import com.anthropic.claudemonitor.data.api.Result
import com.anthropic.claudemonitor.data.models.ClaudeAccount
import com.anthropic.claudemonitor.data.models.LoginRequest
import com.anthropic.claudemonitor.data.models.UsageLimits

class ClaudeAiRepository(private val context: Context) {

    private val session get() = SessionManager.getInstance(context)
    private val api get() = ClaudeAiClientFactory.create(context)

    suspend fun login(email: String, password: String): Result<ClaudeAccount> = safeCall {
        val resp = api.login(LoginRequest(email, password))
        if (!resp.isSuccessful) throw Exception("Login failed (${resp.code()}): check email and password")
        val body = resp.body() ?: throw Exception("Empty login response")

        // Persist session token if returned in body
        body.sessionToken?.let { session.sessionToken = it }
        session.userEmail = email

        // Fetch org ID for subsequent usage calls
        fetchAndStoreOrgId()

        body.account ?: ClaudeAccount(
            uuid = "", email = email, fullName = null, displayName = null
        )
    }

    suspend fun getUsage(): Result<UsageLimits> = safeCall {
        val orgId = session.orgId ?: run {
            fetchAndStoreOrgId()
            session.orgId ?: throw Exception("Could not determine organization ID")
        }
        val resp = api.getUsage(orgId)
        if (resp.code() == 401) throw UnauthorizedException()
        if (!resp.isSuccessful) throw Exception("Usage fetch failed (${resp.code()})")
        resp.body() ?: throw Exception("Empty usage response")
    }

    suspend fun getAccount(): Result<ClaudeAccount> = safeCall {
        val resp = api.getAccount()
        if (resp.code() == 401) throw UnauthorizedException()
        if (!resp.isSuccessful) throw Exception("Account fetch failed (${resp.code()})")
        resp.body() ?: throw Exception("Empty account response")
    }

    private suspend fun fetchAndStoreOrgId() {
        try {
            val resp = api.getOrganizations()
            if (resp.isSuccessful) {
                val orgs = resp.body()
                orgs?.firstOrNull()?.organization?.uuid?.let { session.orgId = it }
            }
        } catch (_: Exception) { /* best-effort */ }
    }

    fun logout() = session.clear()

    val isLoggedIn get() = session.isLoggedIn
    val userEmail get() = session.userEmail

    private inline fun <T> safeCall(block: () -> T): Result<T> = try {
        Result.Success(block())
    } catch (e: UnauthorizedException) {
        Result.Error("Session expired — please log in again")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Unknown error")
    }
}

class UnauthorizedException : Exception()
