package com.studyhub.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyhub.app.R
import com.studyhub.app.data.model.UserProfile
import com.studyhub.app.data.repository.AuthException
import com.studyhub.app.data.repository.AuthRepository
import com.studyhub.app.util.AuthError
import com.studyhub.app.util.AuthField
import com.studyhub.app.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val dateOfBirth: String = "",
    val passwordVisible: Boolean = false,
    val loading: Boolean = false,
    val nameError: Int? = null,
    val emailError: Int? = null,
    val passwordError: Int? = null,
    val dobError: Int? = null,
    val generalError: Int? = null,
    val infoMessage: Int? = null,
    val signedIn: Boolean = false,
    val profile: UserProfile? = null
) {
    val passwordStrength: Int get() = Validators.passwordStrength(password)
}

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState(signedIn = repo.isLoggedIn()))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update {
        it.copy(name = value, nameError = null, generalError = null)
    }

    fun onEmailChange(value: String) = _state.update {
        it.copy(email = value, emailError = null, generalError = null)
    }

    fun onPasswordChange(value: String) = _state.update {
        it.copy(password = value, passwordError = null, generalError = null)
    }

    fun onDobChange(value: String) = _state.update {
        it.copy(dateOfBirth = value, dobError = null)
    }

    fun togglePasswordVisibility() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun clearMessages() = _state.update {
        it.copy(
            generalError = null,
            infoMessage = null,
            emailError = null,
            passwordError = null,
            nameError = null,
            dobError = null
        )
    }

    fun register() {
        val s = _state.value
        var valid = true

        if (!Validators.isValidName(s.name)) {
            _state.update { it.copy(nameError = R.string.error_name_invalid) }
            valid = false
        }
        if (!Validators.isValidEmail(s.email)) {
            _state.update { it.copy(emailError = R.string.error_email_invalid) }
            valid = false
        }
        if (!Validators.isValidPassword(s.password)) {
            _state.update { it.copy(passwordError = R.string.error_password_invalid) }
            valid = false
        }
        if (s.dateOfBirth.isBlank()) {
            _state.update { it.copy(dobError = R.string.error_dob_required) }
            valid = false
        } else if (!Validators.isValidDateOfBirth(s.dateOfBirth)) {
            _state.update { it.copy(dobError = R.string.error_minimum_age) }
            valid = false
        }
        if (!valid) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, generalError = null) }
            repo.register(s.name, s.email, s.password, s.dateOfBirth)
                .onSuccess { profile ->
                    _state.update {
                        it.copy(
                            loading = false,
                            signedIn = true,
                            profile = profile,
                            infoMessage = R.string.auth_account_created
                        )
                    }
                }
                .onFailure { e -> applyError(e) }
        }
    }

    fun login() {
        val s = _state.value
        var valid = true

        if (!Validators.isValidEmail(s.email)) {
            _state.update { it.copy(emailError = R.string.error_login_email_format) }
            valid = false
        }
        if (s.password.isBlank()) {
            _state.update { it.copy(passwordError = R.string.error_password_required) }
            valid = false
        }
        if (!valid) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            repo.login(s.email, s.password)
                .onSuccess { profile ->
                    _state.update {
                        it.copy(
                            loading = false,
                            signedIn = true,
                            profile = profile,
                            infoMessage = R.string.auth_login_success
                        )
                    }
                }
                .onFailure { e -> applyError(e) }
        }
    }

    /** Called when the email field loses focus on the login screen. */
    fun checkEmailRegistered() {
        val email = _state.value.email
        if (!Validators.isValidEmail(email)) return
        viewModelScope.launch {
            if (!repo.emailExists(email)) {
                _state.update { it.copy(emailError = R.string.error_email_unregistered) }
            }
        }
    }

    fun signInWithGoogle(context: Context) = viewModelScope.launch {
        _state.update { it.copy(loading = true, generalError = null) }
        repo.signInWithGoogle(context)
            .onSuccess { profile ->
                _state.update {
                    it.copy(
                        loading = false,
                        signedIn = true,
                        profile = profile,
                        infoMessage = R.string.auth_login_success
                    )
                }
            }
            .onFailure { e -> applyError(e) }
    }

    fun sendPasswordReset() {
        val email = _state.value.email
        if (!Validators.isValidEmail(email)) {
            _state.update { it.copy(emailError = R.string.error_email_required_first) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            repo.sendPasswordReset(email)
                .onSuccess {
                    _state.update {
                        it.copy(loading = false, infoMessage = R.string.auth_reset_sent)
                    }
                }
                .onFailure { e -> applyError(e) }
        }
    }

    fun signOut() {
        repo.signOut()
        _state.value = AuthUiState()
    }

    /** Keeps a resource id in state so errors are rendered in the current app language. */
    private fun applyError(e: Throwable) {
        val error = (e as? AuthException)?.error
            ?: AuthError(AuthField.GENERAL, R.string.error_generic)
        _state.update {
            when (error.field) {
                AuthField.EMAIL -> it.copy(loading = false, emailError = error.messageResId)
                AuthField.PASSWORD -> it.copy(loading = false, passwordError = error.messageResId)
                AuthField.NAME -> it.copy(loading = false, nameError = error.messageResId)
                AuthField.DATE_OF_BIRTH -> it.copy(loading = false, dobError = error.messageResId)
                AuthField.GENERAL -> it.copy(loading = false, generalError = error.messageResId)
            }
        }
    }
}
