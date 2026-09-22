package com.studyhub.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.studyhub.app.ui.theme.CardGrey
import com.studyhub.app.ui.theme.Ink
import com.studyhub.app.ui.theme.InkMuted
import com.studyhub.app.ui.theme.LilacPrimary

/** Graduation cap + open book mark, drawn rather than shipped as a bitmap. */
@Composable
fun StudyHubLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Open book
        val leftPage = Path().apply {
            moveTo(w * 0.08f, h * 0.60f)
            cubicTo(w * 0.22f, h * 0.50f, w * 0.38f, h * 0.52f, w * 0.48f, h * 0.62f)
            lineTo(w * 0.48f, h * 0.92f)
            cubicTo(w * 0.38f, h * 0.82f, w * 0.22f, h * 0.80f, w * 0.08f, h * 0.90f)
            close()
        }
        val rightPage = Path().apply {
            moveTo(w * 0.92f, h * 0.60f)
            cubicTo(w * 0.78f, h * 0.50f, w * 0.62f, h * 0.52f, w * 0.52f, h * 0.62f)
            lineTo(w * 0.52f, h * 0.92f)
            cubicTo(w * 0.62f, h * 0.82f, w * 0.78f, h * 0.80f, w * 0.92f, h * 0.90f)
            close()
        }
        drawPath(leftPage, LilacPrimary)
        drawPath(rightPage, LilacPrimary)

        // Student figure
        drawCircle(LilacPrimary, radius = w * 0.075f, center = Offset(w * 0.62f, h * 0.30f))

        // Graduation cap
        val cap = Path().apply {
            moveTo(w * 0.30f, h * 0.24f)
            lineTo(w * 0.05f, h * 0.34f)
            lineTo(w * 0.30f, h * 0.44f)
            lineTo(w * 0.55f, h * 0.34f)
            close()
        }
        drawPath(cap, Ink)
        drawLine(
            color = Ink,
            start = androidx.compose.ui.geometry.Offset(w * 0.09f, h * 0.36f),
            end = androidx.compose.ui.geometry.Offset(w * 0.09f, h * 0.52f),
            strokeWidth = w * 0.02f
        )
    }
}

/** Rounded pill button used for Get Started / Sign up / Log in. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (enabled) LilacPrimary else CardGrey)
            .clickable(enabled = enabled && !loading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Labelled input with a leading icon, matching the mock-ups. When [error] is
 * non-null the box turns red and the message shows underneath - this is how a
 * non-existent email gets flagged.
 */
@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    onFocusLost: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = InkMuted,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState -> if (!focusState.isFocused) onFocusLost?.invoke() },
            placeholder = { Text(placeholder, color = InkMuted) },
            singleLine = true,
            readOnly = readOnly,
            isError = error != null,
            shape = RoundedCornerShape(26.dp),
            leadingIcon = {
                Icon(leadingIcon, contentDescription = null, tint = LilacPrimary)
            },
            trailingIcon = {
                if (isPassword && onTogglePassword != null) {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = InkMuted
                        )
                    }
                }
            },
            visualTransformation = if (isPassword && !passwordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CardGrey.copy(alpha = 0.55f),
                unfocusedContainerColor = CardGrey.copy(alpha = 0.55f),
                errorContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                focusedIndicatorColor = LilacPrimary,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Ink,
                unfocusedTextColor = Ink
            )
        )
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp)
            )
        }
    }
}

/** Back arrow + centered title header used across the inner screens. */
@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Ink,
                    modifier = Modifier.size(30.dp)
                )
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = Ink,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            trailing?.invoke()
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = InkMuted,
        modifier = modifier.padding(start = 24.dp, top = 18.dp, bottom = 8.dp)
    )
}

@Composable
fun FilterChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(if (isSelected) LilacPrimary else Color.White.copy(alpha = 0.55f))
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    color = if (isSelected) Color.White else Ink,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}