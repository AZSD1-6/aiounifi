package com.anthropic.claudemonitor.data.auth

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "claude_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var sessionToken: String?
        get() = prefs.getString(KEY_SESSION_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_SESSION_TOKEN, value) }

    var sessionCookie: String?
        get() = prefs.getString(KEY_SESSION_COOKIE, null)
        set(value) = prefs.edit { putString(KEY_SESSION_COOKIE, value) }

    var orgId: String?
        get() = prefs.getString(KEY_ORG_ID, null)
        set(value) = prefs.edit { putString(KEY_ORG_ID, value) }

    var userEmail: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit { putString(KEY_EMAIL, value) }

    val isLoggedIn: Boolean
        get() = sessionToken != null || sessionCookie != null

    fun clear() = prefs.edit { clear() }

    companion object {
        private const val KEY_SESSION_TOKEN  = "session_token"
        private const val KEY_SESSION_COOKIE = "session_cookie"
        private const val KEY_ORG_ID         = "org_id"
        private const val KEY_EMAIL          = "user_email"

        @Volatile private var instance: SessionManager? = null
        fun getInstance(context: Context): SessionManager =
            instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
    }
}
