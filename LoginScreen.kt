package com.studyhub.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoggedIn: () -> Unit,
    onGoToRegister: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.clearMessages() }
    LaunchedEffect(state.signedIn) {
        if (state.signedIn) {
            state.infoMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
            onLoggedIn()
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
        Spacer(Modifier.height(40.dp))

        StudyHubLogo(Modifier.size(80.dp))

        Spacer(Modifier.height(20.dp))

        Text("Login", style = MaterialTheme.typography.displayLarge, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text("Sign in to continue.", color = InkMuted)

        Spacer(Modifier.height(26.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(36.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 22.dp, vertical = 32.dp)
        ) {
            // onFocusLost triggers the "is this email registered?" lookup, so the
            // student is told the address is unknown before they even submit.
            LabeledField(
                label = "Email",
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                leadingIcon = Icons.Default.Email,
                placeholder = "you@example.com",
                error = state.emailError,
                keyboardType = KeyboardType.Email,
                onFocusLost = viewModel::checkEmailRegistered
            )

            Spacer(Modifier.height(16.dp))

            LabeledField(
                label = "Password",
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                leadingIcon = Icons.Default.Lock,
                placeholder = "Your password",
                error = state.passwordError,
                isPassword = true,
                passwordVisible = state.passwordVisible,
                onTogglePassword = viewModel::togglePasswordVisibility,
                keyboardType = KeyboardType.Password
            )

            Spacer(Modifier.height(24.dp))

            PrimaryButton(text = "Log in", onClick = viewModel::login, loading = state.loading)

            if (state.generalError != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.generalError!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (state.infoMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.infoMessage!!,
                    color = LilacPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Forgot Password?",
                color = Ink,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.sendPasswordReset() },
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Signup !",
                color = LilacPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGoToRegister() },
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            SocialSignInSection(
                onGoogleClick = { viewModel.signInWithGoogle(context) },
                enabled = !state.loading
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}