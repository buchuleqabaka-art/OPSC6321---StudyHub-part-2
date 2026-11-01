package com.studyhub.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguagesTest {
    @Test
    fun supportedLanguagesIncludeEnglishAndTwoSouthAfricanLanguages() {
        assertEquals(listOf("en", "af", "zu"), AppLanguages.supported.map { it.tag })
    }

    @Test
    fun storedLanguageNamesAndRegionalTagsNormalizeToSupportedTags() {
        assertEquals(AppLanguages.ENGLISH, AppLanguages.normalize("English"))
        assertEquals(AppLanguages.AFRIKAANS, AppLanguages.normalize("Afrikaans"))
        assertEquals(AppLanguages.AFRIKAANS, AppLanguages.normalize("af-ZA"))
        assertEquals(AppLanguages.ISIZULU, AppLanguages.normalize("isiZulu"))
        assertEquals(AppLanguages.ISIZULU, AppLanguages.normalize("zu-ZA"))
        assertEquals(AppLanguages.ENGLISH, AppLanguages.normalize("unsupported"))
    }

    @Test
    fun eachLanguageHasALabelResource() {
        assertTrue(AppLanguages.supported.all { it.labelRes != 0 })
    }
}
