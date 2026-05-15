package com.anthropic.claudemonitor.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anthropic.claudemonitor.data.api.Result
import com.anthropic.claudemonitor.data.auth.ClaudeAiRepository
import kotlinx.coroutines.launch

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ClaudeAiRepository(app)

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("Email and password are required")
            return
        }
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            when (val result = repo.login(email, password)) {
                is Result.Success -> _loginState.value = LoginState.Success
                is Result.Error   -> _loginState.value = LoginState.Error(result.message)
            }
        }
    }
}

sealed class LoginState {
    object Idle    : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
