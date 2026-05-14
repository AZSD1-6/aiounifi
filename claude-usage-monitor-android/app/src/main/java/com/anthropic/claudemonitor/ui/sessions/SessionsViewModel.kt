package com.anthropic.claudemonitor.ui.sessions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anthropic.claudemonitor.data.api.ApiClientFactory
import com.anthropic.claudemonitor.data.api.Result
import com.anthropic.claudemonitor.data.api.UsageRepository
import com.anthropic.claudemonitor.data.models.Session
import com.anthropic.claudemonitor.util.Prefs
import kotlinx.coroutines.launch

class SessionsViewModel(app: Application) : AndroidViewModel(app) {

    private val _sessions = MutableLiveData<List<Session>>()
    val sessions: LiveData<List<Session>> = _sessions

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val ctx = getApplication<Application>()
            val repo = UsageRepository(ApiClientFactory.create(Prefs.serverUrl(ctx)))
            when (val result = repo.getSessions()) {
                is Result.Success -> _sessions.value = result.data
                is Result.Error   -> _error.value = result.message
            }
            _loading.value = false
        }
    }
}
