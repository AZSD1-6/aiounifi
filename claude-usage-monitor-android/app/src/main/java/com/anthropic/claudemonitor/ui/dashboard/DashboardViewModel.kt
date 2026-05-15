package com.anthropic.claudemonitor.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anthropic.claudemonitor.data.api.Result
import com.anthropic.claudemonitor.data.auth.ClaudeAiRepository
import com.anthropic.claudemonitor.data.auth.SessionManager
import com.anthropic.claudemonitor.data.models.UsageLimits
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ClaudeAiRepository(app)

    private val _usage = MutableLiveData<UsageLimits?>()
    val usage: LiveData<UsageLimits?> = _usage

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _sessionExpired = MutableLiveData(false)
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    val userEmail: String?
        get() = SessionManager.getInstance(getApplication()).userEmail

    private var pollJob: Job? = null

    fun startPolling(intervalSeconds: Int = 30) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(intervalSeconds * 1000L)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repo.getUsage()) {
                is Result.Success -> _usage.value = result.data
                is Result.Error   -> {
                    if (result.message.contains("Session expired", ignoreCase = true)) {
                        _sessionExpired.value = true
                    } else {
                        _error.value = result.message
                    }
                }
            }
            _loading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
