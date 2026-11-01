package com.studyhub.app.util

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Password handling for StudyHub.
 *
 * Firebase Authentication never receives a plain password over the wire in
 * storage terms - it hashes with scrypt server-side and we never keep the raw
 * value. This object covers the local offline-verification check by deriving a
 * salted PBKDF2 hash; password values and derived hashes must never be logged.
 */
object PasswordSecurity {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    /** Salted PBKDF2-SHA256 hash. Same salt + same password always gives the same output. */
    fun hash(password: String, salt: String): String {
        val spec = PBEKeySpec(password.toCharArray(), salt.hexToBytes(), ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded.toHex()
    }

    /** Constant-time comparison so we do not leak timing information. */
    fun verify(password: String, salt: String, expectedHash: String): Boolean {
        val actual = hash(password, salt)
        if (actual.length != expectedHash.length) return false
        var diff = 0
        for (i in actual.indices) diff = diff or (actual[i].code xor expectedHash[i].code)
        return diff == 0
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}