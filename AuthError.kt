package com.studyhub.app.util

/**
 * Which field a failure should be shown against. The UI uses this to paint the
 * email box red when the address is not registered - exactly the "flag the
 * email" behaviour in the brief.
 */
enum class AuthField { EMAIL, PASSWORD, NAME, DATE_OF_BIRTH, GENERAL }

data class AuthError(
    val field: AuthField,
    val message: String
)