package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class Flow5AnalyticsTest {
    private fun answer(correct: Boolean, time: Long = 0L, reviewedAt: Long = 0L) = ReviewResultEntity(
        topicId = 1,
        flashcardId = reviewedAt.toInt(),
        question = "Question",
        correctAnswer = "A",
        userAnswer = if (correct) "A" else "B",
        isCorrect = correct,
        confidence = "Maybe",
        responseTimeMillis = time,
        reviewedAt = reviewedAt
    )

    @Test
    fun accuracyUsesSavedAnswersAndEmptyDataHasNoPercentage() {
        assertEquals(67, calculateAccuracy(listOf(answer(true), answer(true), answer(false))))
        assertNull(calculateAccuracy(emptyList()))
    }

    @Test
    fun weeklyBucketsOnlyCountAnswersInTheSevenDayWindow() {
        val now = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 18, 12, 0, 0)
        }.timeInMillis
        val todayStart = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val records = listOf(
            answer(true, reviewedAt = todayStart + 1_000),
            answer(false, reviewedAt = todayStart - 8L * 86_400_000L)
        )

        val buckets = accuracyBuckets(records, ProgressPeriod.Week, now)

        assertEquals(7, buckets.size)
        assertEquals(1, buckets.sumOf { it.answers })
        assertEquals(100, buckets.last().percent)
    }

    @Test
    fun averageAnswerTimeExcludesLegacyRowsWithoutTiming() {
        assertEquals(3.0, averageResponseSeconds(listOf(answer(true, 2_000), answer(false, 4_000), answer(true)))!!, 0.0001)
        assertNull(averageResponseSeconds(listOf(answer(true))))
    }
}
