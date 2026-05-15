package com.anthropic.claudemonitor.data.api

import com.anthropic.claudemonitor.data.models.ClaudeAccount
import com.anthropic.claudemonitor.data.models.LoginRequest
import com.anthropic.claudemonitor.data.models.LoginResponse
import com.anthropic.claudemonitor.data.models.OrganizationMembership
import com.anthropic.claudemonitor.data.models.UsageLimits
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ClaudeAiApi {

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("account")
    suspend fun getAccount(): Response<ClaudeAccount>

    @GET("organizations")
    suspend fun getOrganizations(): Response<List<OrganizationMembership>>

    @GET("organizations/{orgId}/usage")
    suspend fun getUsage(
        @Path("orgId") orgId: String,
    ): Response<UsageLimits>
}
