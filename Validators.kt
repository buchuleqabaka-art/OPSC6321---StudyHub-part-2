package com.studyhub.app.util

import java.util.Calendar

/**
 * Pure input validation - no Android imports, so it runs in plain JVM unit tests
 * and inside GitHub Actions without an emulator.
 */
object Validators {

    // Deliberately stricter than Patterns.EMAIL_ADDRESS: exactly one @, a dot in
    // the domain, no spaces, and a TLD of at least two characters.
    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$"
    )

    fun isValidEmail(email: String?): Boolean {
        val value = email?.trim().orEmpty()
        if (value.isEmpty() || value.length > 254) return false
        return EMAIL_REGEX.matches(value)
    }

    fun isValidName(name: String?): Boolean {
        val value = name?.trim().orEmpty()
        return value.length in 2..60 && value.any { it.isLetter() }
    }

    /** At least 8 chars, one letter and one digit. */
    fun isValidPassword(password: String?): Boolean {
        val value = password.orEmpty()
        return value.length >= 8 && value.any { it.isLetter() } && value.any { it.isDigit() }
    }

    fun passwordStrength(password: String?): Int {
        val value = password.orEmpty()
        if (value.isEmpty()) return 0
        var score = 0
        if (value.length >= 8) score++
        if (value.length >= 12) score++
        if (value.any { it.isDigit() }) score++
        if (value.any { it.isUpperCase() }) score++
        if (value.any { !it.isLetterOrDigit() }) score++
        return score.coerceAtMost(4)
    }

    /** Expects yyyy-MM-dd and rejects anyone under 13 or clearly impossible dates. */
    fun isValidDateOfBirth(dob: String?): Boolean {
        val value = dob?.trim().orEmpty()
        val match = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").find(value) ?: return false
        val (y, m, d) = match.destructured
        val year = y.toInt(); val month = m.toInt(); val day = d.toInt()
        
        if (month !in 1..12 || day !in 1..31) return false
        
        val today = Calendar.getInstance()
        val todayYear = today.get(Calendar.YEAR)
        val todayMonth = today.get(Calendar.MONTH) + 1
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        
        var age = todayYear - year
        if (month > todayMonth || (month == todayMonth && day > todayDay)) {
            age--
        }
        
        return age in 13..120
    }

    fun isNotBlank(text: String?): Boolean = !text.isNullOrBlank()
}