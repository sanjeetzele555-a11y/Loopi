package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeProgressTest {
    @Test
    fun reviewedCardCountCountsDistinctSavedCardsThatStillExist() {
        val cards = listOf(
            FlashcardEntity(id = 1, topicId = 10, question = "Q1", answer = "A1"),
            FlashcardEntity(id = 2, topicId = 10, question = "Q2", answer = "A2"),
            FlashcardEntity(id = 3, topicId = 20, question = "Q3", answer = "A3")
        )
        val answers = listOf(
            result(flashcardId = 1),
            result(flashcardId = 1),
            result(flashcardId = 3),
            result(flashcardId = 99)
        )

        assertEquals(2, countReviewedCards(cards, answers))
    }

    @Test
    fun dailyGoalCountUsesDistinctCardsAnsweredSinceStartOfDay() {
        val cards = listOf(
            FlashcardEntity(id = 1, topicId = 10, question = "Q1", answer = "A1"),
            FlashcardEntity(id = 2, topicId = 10, question = "Q2", answer = "A2")
        )
        val answers = listOf(
            result(flashcardId = 1, reviewedAt = 100L),
            result(flashcardId = 1, reviewedAt = 200L),
            result(flashcardId = 2, reviewedAt = 50L),
            result(flashcardId = 99, reviewedAt = 300L)
        )

        assertEquals(1, countCardsReviewedSince(cards, answers, startMillis = 100L))
    }

    @Test
    fun starterCatalogContainsAllExpectedReviewCards() {
        assertEquals(52, DefaultStudyContent.byTopic.values.sumOf { it.size })
        assertEquals("Edges", DefaultStudyContent.byTopic.getValue("Algorithms").first { it.question.startsWith("A graph is made up") }.answer)
    }

    private fun result(flashcardId: Int, reviewedAt: Long = 0L) = ReviewResultEntity(
        topicId = 1,
        flashcardId = flashcardId,
        question = "Question",
        correctAnswer = "Answer",
        userAnswer = "Answer",
        isCorrect = true,
        confidence = "I know it",
        reviewedAt = reviewedAt
    )
}
