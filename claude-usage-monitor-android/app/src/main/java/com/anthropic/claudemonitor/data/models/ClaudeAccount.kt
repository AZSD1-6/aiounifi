package com.anthropic.claudemonitor.data.models

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email")    val email: String,
    @SerializedName("password") val password: String,
)

data class LoginResponse(
    @SerializedName("session_token") val sessionToken: String?,
    @SerializedName("account")       val account: ClaudeAccount?,
)

data class ClaudeAccount(
    @SerializedName("uuid")          val uuid: String,
    @SerializedName("email")         val email: String,
    @SerializedName("full_name")     val fullName: String?,
    @SerializedName("display_name")  val displayName: String?,
)

data class OrganizationMembership(
    @SerializedName("organization")  val organization: ClaudeOrganization,
)

data class ClaudeOrganization(
    @SerializedName("uuid")          val uuid: String,
    @SerializedName("name")          val name: String,
    @SerializedName("capabilities")  val capabilities: List<String>?,
)

data class UsageLimits(
    @SerializedName("type")                  val type: String?,
    @SerializedName("resetsAt")              val resetsAt: String?,
    @SerializedName("resets_at")             val resetsAtSnake: String?,
    @SerializedName("used")                  val used: Long?,
    @SerializedName("limit")                 val limit: Long?,
    @SerializedName("usageWarningThreshold") val warningThreshold: Double?,
) {
    val resetTimestamp: String? get() = resetsAt ?: resetsAtSnake
}
