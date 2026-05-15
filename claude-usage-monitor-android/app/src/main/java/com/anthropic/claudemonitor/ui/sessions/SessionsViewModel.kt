package com.anthropic.claudemonitor.ui.sessions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.anthropic.claudemonitor.data.models.Session

class SessionsViewModel(app: Application) : AndroidViewModel(app) {

    private val _sessions = MutableLiveData<List<Session>>(emptyList())
    val sessions: LiveData<List<Session>> = _sessions

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    fun load() {
        // Session history is not available via the Claude.ai web API.
        _sessions.value = emptyList()
    }
}
