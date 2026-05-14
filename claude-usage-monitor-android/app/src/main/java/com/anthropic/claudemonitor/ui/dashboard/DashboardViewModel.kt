package com.anthropic.claudemonitor.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anthropic.claudemonitor.data.api.ApiClientFactory
import com.anthropic.claudemonitor.data.api.Result
import com.anthropic.claudemonitor.data.api.UsageRepository
import com.anthropic.claudemonitor.data.models.Stats
import com.anthropic.claudemonitor.util.Prefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val _stats = MutableLiveData<Stats?>()
    val stats: LiveData<Stats?> = _stats

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private var pollJob: Job? = null

    fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                refresh()
                val interval = Prefs.refreshSeconds(getApplication()) * 1000L
                delay(interval)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val ctx = getApplication<Application>()
            val repo = UsageRepository(ApiClientFactory.create(Prefs.serverUrl(ctx)))
            when (val result = repo.getStats(Prefs.billingStart(ctx), Prefs.planLimit(ctx))) {
                is Result.Success -> _stats.value = result.data
                is Result.Error   -> _error.value = result.message
            }
            _loading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
