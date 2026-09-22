package com.studyhub.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyhub.app.ui.components.PrimaryButton
import com.studyhub.app.ui.components.StudyHubLogo
import com.studyhub.app.ui.theme.Ink
import com.studyhub.app.ui.theme.LilacPrimary
import com.studyhub.app.ui.theme.StudyHubTheme

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onLogIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(90.dp))

        StudyHubLogo(Modifier.size(140.dp))

        Spacer(Modifier.height(14.dp))

        Text(
            text = "STUDYHUB",
            style = MaterialTheme.typography.headlineLarge,
            color = Ink,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
        Text(
            text = "ORGANIZE.COLLABORATE.\nACHIEVE.TOGETHER",
            style = MaterialTheme.typography.labelSmall,
            color = Ink,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(70.dp))

        Text(
            text = "Organize.Collaborate.Achieve.\nTogether",
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            textAlign = TextAlign.Center,
            fontSize = 20.sp
        )

        Spacer(Modifier.weight(1f))

        PrimaryButton(text = "Get Started", onClick = onGetStarted)

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account?  ", color = Ink, fontSize = 18.sp)
            Text(
                text = "Log In",
                color = LilacPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onLogIn() }
            )
        }

        Spacer(Modifier.height(48.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE3C6E8)
@Composable
private fun WelcomePreview() {
    StudyHubTheme { WelcomeScreen(onGetStarted = {}, onLogIn = {}) }
}

