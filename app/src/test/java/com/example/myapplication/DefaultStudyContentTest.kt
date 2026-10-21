package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultStudyContentTest {
    @Test
    fun starterTopicsHaveEnoughDistinctQuestionAndAnswerPairsForReview() {
        assertEquals(setOf("Operating Systems", "Databases", "Networks", "Algorithms"), DefaultStudyContent.byTopic.keys)
        DefaultStudyContent.byTopic.values.forEach { cards ->
            assertTrue(cards.size >= 4)
            assertTrue(cards.all { it.question.isNotBlank() && it.answer.isNotBlank() && it.explanation.isNotBlank() })
            assertEquals(cards.size, cards.map { it.question }.distinct().size)
            assertFalse(cards.any { it.question == it.answer })
        }
    }
}
