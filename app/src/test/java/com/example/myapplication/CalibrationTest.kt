package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalibrationTest {
    private fun answer(correct: Boolean, confidence: String) = ReviewResultEntity(
        topicId = 1,
        flashcardId = 1,
        question = "Question",
        correctAnswer = "A",
        userAnswer = if (correct) "A" else "B",
        isCorrect = correct,
        confidence = confidence
    )

    @Test
    fun confidentAndRightOrUnsureAndWrongCountAsMatches() {
        val result = calculateCalibration(
            listOf(
                answer(true, "I know it"),
                answer(false, "Not sure"),
                answer(false, "I know it"),
                answer(true, "Maybe")
            )
        )!!
        // 1 + 1 + 0 + 0.5 out of 4
        assertEquals(63, result.score)
        assertEquals(1, result.overconfident)
        assertEquals(0, result.underconfident)
    }

    @Test
    fun noRatedAnswersMeansNoScore() {
        assertNull(calculateCalibration(emptyList()))
    }

    @Test
    fun feedbackFlagsOverconfidence() {
        assertEquals(
            "You felt sure, but this one needs another look. Added to your weak spots.",
            calibrationMessage(correct = false, confidence = "I know it")
        )
    }
}
