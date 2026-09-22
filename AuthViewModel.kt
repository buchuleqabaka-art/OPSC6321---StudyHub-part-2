package com.studyhub.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val dobError: String? = null,
    val generalError: String? = null,
    val infoMessage: String? = null,
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

    // --- Field updates. Editing a field clears the error shown against it. ---

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

    // --- Register -----------------------------------------------------------

    fun register() {
        val s = _state.value
        var valid = true

        if (!Validators.isValidName(s.name)) {
            _state.update { it.copy(nameError = "Enter your full name (at least 2 characters).") }
            valid = false
        }
        if (!Validators.isValidEmail(s.email)) {
            _state.update { it.copy(emailError = "Enter a valid email address.") }
            valid = false
        }
        if (!Validators.isValidPassword(s.password)) {
            _state.update { it.copy(passwordError = "Use at least 8 characters with a number.") }
            valid = false
        }
        if (s.dateOfBirth.isBlank()) {
            _state.update { it.copy(dobError = "Select your date of birth.") }
            valid = false
        } else if (!Validators.isValidDateOfBirth(s.dateOfBirth)) {
            _state.update { it.copy(dobError = "You must be at least 13 years old.") }
            valid = false
        }
        
        if (!valid) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, generalError = null) }
            repo.register(s.name, s.email, s.password, s.dateOfBirth)
                .onSuccess { profile ->
                    _state.update { it.copy(
                        loading = false,
                        signedIn = true,
                        profile = profile,
                        infoMessage = "Account created successfully!"
                    ) }
                }
                .onFailure { e -> applyError(e) }
        }
    }

    // --- Login --------------------------------------------------------------

    fun login() {
        val s = _state.value
        var valid = true

        if (!Validators.isValidEmail(s.email)) {
            _state.update { it.copy(emailError = "Enter a valid email address, e.g. name@example.com") }
            valid = false
        }
        if (s.password.isBlank()) {
            _state.update { it.copy(passwordError = "Enter your password.") }
            valid = false
        }
        if (!valid) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            repo.login(s.email, s.password)
                .onSuccess { profile ->
                    _state.update { it.copy(
                        loading = false,
                        signedIn = true,
                        profile = profile,
                        infoMessage = "Login successful!"
                    ) }
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
                _state.update {
                    it.copy(emailError = "This email is not registered. Check the address or create an account.")
                }
            }
        }
    }

    // --- Social sign-in -----------------------------------------------------

    fun signInWithGoogle(context: Context) = viewModelScope.launch {
        _state.update { it.copy(loading = true, generalError = null) }
        repo.signInWithGoogle(context)
            .onSuccess { profile ->
                _state.update { it.copy(
                    loading = false,
                    signedIn = true,
                    profile = profile,
                    infoMessage = "Login successful!"
                ) }
            }
            .onFailure { e -> applyError(e) }
    }


    // --- Password reset -----------------------------------------------------

    fun sendPasswordReset() {
        val email = _state.value.email
        if (!Validators.isValidEmail(email)) {
            _state.update { it.copy(emailError = "Enter your email address first.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            repo.sendPasswordReset(email)
                .onSuccess {
                    _state.update {
                        it.copy(loading = false, infoMessage = "Reset link sent. Check your inbox.")
                    }
                }
                .onFailure { e -> applyError(e) }
        }
    }

    fun signOut() {
        repo.signOut()
        _state.value = AuthUiState()
    }

    /** Routes a repository failure onto the field it belongs to. */
    private fun applyError(e: Throwable) {
        val error = (e as? AuthException)?.error
            ?: AuthError(AuthField.GENERAL, e.localizedMessage ?: "Something went wrong.")
        _state.update {
            when (error.field) {
                AuthField.EMAIL -> it.copy(loading = false, emailError = error.message)
                AuthField.PASSWORD -> it.copy(loading = false, passwordError = error.message)
                AuthField.NAME -> it.copy(loading = false, nameError = error.message)
                AuthField.DATE_OF_BIRTH -> it.copy(loading = false, dobError = error.message)
                AuthField.GENERAL -> it.copy(loading = false, generalError = error.message)
            }
        }
    }
}