package com.studyhub.app.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyhub.app.ui.components.LabeledField
import com.studyhub.app.ui.components.PrimaryButton
import com.studyhub.app.ui.components.SocialSignInSection
import com.studyhub.app.ui.components.StudyHubLogo
import com.studyhub.app.ui.theme.Ink
import com.studyhub.app.ui.theme.InkMuted
import com.studyhub.app.ui.theme.LilacPrimary
import com.studyhub.app.viewmodel.AuthViewModel
import java.util.Calendar

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegistered: () -> Unit,
    onGoToLogin: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.clearMessages() }
    LaunchedEffect(state.signedIn) {
        if (state.signedIn) {
            state.infoMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
            onRegistered()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(30.dp))

        StudyHubLogo(Modifier.size(80.dp))

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Create new\nAccount",
            style = MaterialTheme.typography.displayLarge,
            color = Ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Already Registered? Log in here.",
            color = InkMuted,
            modifier = Modifier.clickable { onGoToLogin() }
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(36.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 22.dp, vertical = 30.dp)
        ) {
            LabeledField(
                label = "Name",
                value = state.name,
                onValueChange = viewModel::onNameChange,
                leadingIcon = Icons.Default.Person,
                placeholder = "Your full name",
                error = state.nameError
            )
            Spacer(Modifier.height(14.dp))

            LabeledField(
                label = "Email",
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                leadingIcon = Icons.Default.Email,
                placeholder = "you@example.com",
                error = state.emailError,
                keyboardType = KeyboardType.Email
            )
            Spacer(Modifier.height(14.dp))

            LabeledField(
                label = "Password",
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                leadingIcon = Icons.Default.Lock,
                placeholder = "At least 8 characters",
                error = state.passwordError,
                isPassword = true,
                passwordVisible = state.passwordVisible,
                onTogglePassword = viewModel::togglePasswordVisibility,
                keyboardType = KeyboardType.Password
            )
            if (state.password.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                PasswordStrengthBar(state.passwordStrength)
            }
            Spacer(Modifier.height(14.dp))

            // Date picker writes back in yyyy-MM-dd so validation stays simple
            Box {
                LabeledField(
                    label = "Date of Birth",
                    value = state.dateOfBirth,
                    onValueChange = {},
                    leadingIcon = Icons.Default.CalendarMonth,
                    placeholder = "Select",
                    error = state.dobError,
                    readOnly = true
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    viewModel.onDobChange(String.format("%04d-%02d-%02d", y, m + 1, d))
                                },
                                cal.get(Calendar.YEAR) - 20,
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                )
            }

            Spacer(Modifier.height(22.dp))

            PrimaryButton(
                text = "Sign up",
                onClick = viewModel::register,
                loading = state.loading
            )

            if (state.generalError != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.generalError!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(22.dp))

            SocialSignInSection(
                onGoogleClick = { viewModel.signInWithGoogle(context) },
                enabled = !state.loading
            )
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun PasswordStrengthBar(strength: Int) {
    val label = when (strength) {
        0, 1 -> "Weak"
        2 -> "Fair"
        3 -> "Good"
        else -> "Strong"
    }
    Column(modifier = Modifier.padding(start = 16.dp)) {
        androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .padding(end = 4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (index < strength) LilacPrimary else InkMuted.copy(alpha = 0.25f))
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(text = "Password strength: $label", color = InkMuted, fontWeight = FontWeight.Medium)
    }
}