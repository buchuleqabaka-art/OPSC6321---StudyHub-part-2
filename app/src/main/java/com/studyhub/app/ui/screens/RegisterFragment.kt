package com.studyhub.app.ui.screens

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.studyhub.app.R
import com.studyhub.app.databinding.FragmentRegisterBinding
import com.studyhub.app.ui.navigation.Routes
import java.util.Calendar
import java.util.Locale

class RegisterFragment : HubFragment(R.layout.fragment_register) {
    private var lastMessage: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentRegisterBinding.bind(view)
        val initial = authViewModel.state.value
        binding.nameInput.setText(initial.name)
        binding.emailInput.setText(initial.email)
        binding.passwordInput.setText(initial.password)
        if (initial.dateOfBirth.isNotBlank()) binding.buttonDob.text = getString(R.string.date_of_birth_format, initial.dateOfBirth)

        binding.buttonBack.setOnClickListener { navigator.navigate(Routes.WELCOME, clearBackStack = true) }
        binding.textGoLogin.setOnClickListener { navigator.navigate(Routes.LOGIN) }
        binding.buttonGoogle.setOnClickListener { authViewModel.signInWithGoogle(requireContext()) }
        binding.nameInput.doAfterTextChanged { authViewModel.onNameChange(it?.toString().orEmpty()) }
        binding.emailInput.doAfterTextChanged { authViewModel.onEmailChange(it?.toString().orEmpty()) }
        binding.passwordInput.doAfterTextChanged { authViewModel.onPasswordChange(it?.toString().orEmpty()) }
        binding.buttonDob.setOnClickListener {
            val current = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val value = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                    authViewModel.onDobChange(value)
                    binding.buttonDob.text = getString(R.string.date_of_birth_format, value)
                },
                current.get(Calendar.YEAR) - 20,
                current.get(Calendar.MONTH),
                current.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        binding.buttonRegister.setOnClickListener {
            authViewModel.onNameChange(binding.nameInput.text?.toString().orEmpty())
            authViewModel.onEmailChange(binding.emailInput.text?.toString().orEmpty())
            authViewModel.onPasswordChange(binding.passwordInput.text?.toString().orEmpty())
            authViewModel.register()
        }

        observe(authViewModel.state) { state ->
            binding.nameLayout.error = state.nameError?.let(::getString)
            binding.emailLayout.error = state.emailError?.let(::getString)
            binding.passwordLayout.error = state.passwordError?.let(::getString)
            binding.textDobError.text = state.dobError?.let(::getString).orEmpty()
            binding.textDobError.visibility = if (state.dobError == null) View.GONE else View.VISIBLE
            binding.passwordStrength.progress = state.passwordStrength
            binding.registerProgress.visibility = if (state.loading) View.VISIBLE else View.GONE
            binding.buttonRegister.isEnabled = !state.loading
            binding.buttonGoogle.isEnabled = !state.loading
            binding.textGeneralError.visibility = if (state.generalError == null) View.GONE else View.VISIBLE
            binding.textGeneralError.text = state.generalError?.let(::getString).orEmpty()
            val message = (state.infoMessage ?: state.generalError)?.let(::getString)
            if (!message.isNullOrBlank() && message != lastMessage) {
                lastMessage = message
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
