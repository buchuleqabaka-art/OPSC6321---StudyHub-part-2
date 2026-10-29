package com.studyhub.app.ui.screens

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.snackbar.Snackbar
import com.studyhub.app.R
import com.studyhub.app.databinding.FragmentLoginBinding
import com.studyhub.app.ui.navigation.Routes

class LoginFragment : HubFragment(R.layout.fragment_login) {
    private var lastMessage: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentLoginBinding.bind(view)
        val initial = authViewModel.state.value
        binding.emailInput.setText(initial.email)
        binding.passwordInput.setText(initial.password)

        binding.buttonBack.setOnClickListener { navigator.navigate(Routes.WELCOME, clearBackStack = true) }
        binding.textCreateAccount.setOnClickListener { navigator.navigate(Routes.REGISTER) }
        binding.textForgotPassword.setOnClickListener {
            authViewModel.onEmailChange(binding.emailInput.text?.toString().orEmpty())
            authViewModel.sendPasswordReset()
        }
        binding.buttonLogin.setOnClickListener {
            authViewModel.onEmailChange(binding.emailInput.text?.toString().orEmpty())
            authViewModel.onPasswordChange(binding.passwordInput.text?.toString().orEmpty())
            authViewModel.login()
        }
        binding.buttonGoogle.setOnClickListener { authViewModel.signInWithGoogle(requireContext()) }
        binding.emailInput.doAfterTextChanged {
            authViewModel.onEmailChange(it?.toString().orEmpty())
        }
        binding.passwordInput.doAfterTextChanged {
            authViewModel.onPasswordChange(it?.toString().orEmpty())
        }
        binding.emailInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) authViewModel.checkEmailRegistered()
        }

        observe(authViewModel.state) { state ->
            binding.emailLayout.error = state.emailError
            binding.passwordLayout.error = state.passwordError
            binding.loginProgress.visibility = if (state.loading) View.VISIBLE else View.GONE
            binding.buttonLogin.isEnabled = !state.loading
            binding.buttonGoogle.isEnabled = !state.loading
            binding.textGeneralError.visibility = if (state.generalError.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.textGeneralError.text = state.generalError.orEmpty()
            val message = state.infoMessage ?: state.generalError
            if (!message.isNullOrBlank() && message != lastMessage) {
                lastMessage = message
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
