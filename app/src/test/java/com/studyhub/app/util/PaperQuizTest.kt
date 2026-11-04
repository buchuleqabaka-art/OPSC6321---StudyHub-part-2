package com.studyhub.app.util

import com.studyhub.app.quiz.*
import org.junit.Assert.*
import org.junit.Test

class PaperQuizTest {
    private val questions = listOf(
        QuizQuestion("First", listOf("a", "b", "c", "d"), 1, "Reason", "Q1"),
        QuizQuestion("Second", listOf("a", "b", "c", "d"), 2, "Reason", "Q2"))
    @Test fun scoresAndRetriesOnlyMistakes() {
        val q = PaperQuiz("test", "Paper", questions, listOf(1, 0)).checked()
        assertTrue(q.complete); assertEquals(1, q.score)
        val retry = q.retryMistakes("retry").checked()
        assertEquals(listOf(questions[1]), retry.questions); assertTrue(retry.answers.isEmpty())
    }
    @Test fun incompleteQuizScoresOnlyAnsweredQuestions() {
        val q = PaperQuiz("test", "Paper", questions, listOf(1)).checked()
        assertFalse(q.complete); assertEquals(1, q.score)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsInvalidAnswer() {
        PaperQuiz("test", "Paper", questions, listOf(4)).checked()
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsDuplicateOptions() {
        PaperQuiz("test", "Paper", listOf(questions[0].copy(options = listOf("a","a","c","d")))).checked()
    }
}
