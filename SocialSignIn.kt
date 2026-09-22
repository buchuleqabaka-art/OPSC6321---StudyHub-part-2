package com.studyhub.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyhub.app.R
import com.studyhub.app.ui.theme.Ink
import com.studyhub.app.ui.theme.InkMuted

/**
 * "or continue with" block. Google uses Credential Manager; Apple/iCloud uses
 * the Firebase OAuth provider. Both land in the same Firebase user record.
 */
@Composable
fun SocialSignInSection(
    onGoogleClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = InkMuted.copy(alpha = 0.4f))
            Text(
                text = "or continue with",
                color = InkMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = InkMuted.copy(alpha = 0.4f))
        }

        Spacer(Modifier.height(16.dp))

        SocialButton(
            text = "Continue with Google",
            background = Color.White,
            contentColor = Ink,
            glyph = { GoogleGlyph() },
            enabled = enabled,
            onClick = onGoogleClick
        )
    }
}

@Composable
private fun SocialButton(
    text: String,
    background: Color,
    contentColor: Color,
    glyph: @Composable () -> Unit,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(background)
            .border(BorderStroke(1.dp, InkMuted.copy(alpha = 0.25f)), RoundedCornerShape(26.dp))
            .clickable(enabled = enabled) { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        glyph()
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// Using official vector assets for brand compliance
@Composable
private fun GoogleGlyph() {
    Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_google),
        contentDescription = null,
        modifier = Modifier.size(22.dp),
        tint = Color.Unspecified
    )
}

