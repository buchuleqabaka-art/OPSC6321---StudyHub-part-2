package com.studyhub.app.util

import androidx.annotation.StringRes
import com.studyhub.app.R

/** Supported in-app languages. Persist the BCP-47 tag, never a translated label. */
object AppLanguages {
    const val ENGLISH = "en"
    const val AFRIKAANS = "af"
    const val ISIZULU = "zu"

    data class Option(val tag: String, @StringRes val labelRes: Int)

    val supported = listOf(
        Option(ENGLISH, R.string.language_english),
        Option(AFRIKAANS, R.string.language_afrikaans),
        Option(ISIZULU, R.string.language_isizulu)
    )

    fun normalize(value: String?): String = when (value?.trim()?.lowercase()) {
        "af", "af-za", "afrikaans" -> AFRIKAANS
        "zu", "zu-za", "isizulu", "isi-zulu" -> ISIZULU
        "en", "en-za", "english", null, "" -> ENGLISH
        else -> ENGLISH
    }

    @StringRes
    fun labelFor(tag: String): Int = supported.firstOrNull { it.tag == normalize(tag) }?.labelRes
        ?: R.string.language_english
}
