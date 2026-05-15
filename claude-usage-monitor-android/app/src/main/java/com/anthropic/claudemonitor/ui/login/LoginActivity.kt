package com.anthropic.claudemonitor.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.anthropic.claudemonitor.MainActivity
import com.anthropic.claudemonitor.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            viewModel.login(
                email    = binding.emailInput.text.toString().trim(),
                password = binding.passwordInput.text.toString(),
            )
        }

        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Idle    -> setLoading(false)
                is LoginState.Loading -> setLoading(true)
                is LoginState.Success -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is LoginState.Error   -> {
                    setLoading(false)
                    binding.errorText.visibility = View.VISIBLE
                    binding.errorText.text = state.message
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.loginButton.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.errorText.visibility = View.GONE
    }
}
