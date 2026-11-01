package com.studyhub.app.util

import androidx.annotation.StringRes

/** Which field a localized authentication failure belongs to. */
enum class AuthField { EMAIL, PASSWORD, NAME, DATE_OF_BIRTH, GENERAL }

data class AuthError(
    val field: AuthField,
    @StringRes val messageResId: Int
)
