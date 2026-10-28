package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class McqGeneratorTest {
    private val notes = """
        Database Systems - Week 3 notes

        Normalization is the process of organising tables to reduce duplication and update anomalies.
        A primary key is a column that uniquely identifies each row in a table.
        A foreign key is a column that links a row to the primary key of another table.
        Indexing improves query lookup speed but makes inserts slower.
        Transaction: a group of operations that must all succeed or all fail together.
        The transaction log records every change so the database can recover after a crash.
        A view is a saved query that behaves like a virtual table for the user.
        Normalization usually splits large tables, and indexing helps when joining them again.
    """.trimIndent()

    @Test
    fun definitionsBecomeQuestionsWithThreeWrongOptions() {
        val questions = McqGenerator.generate(notes)
        assertTrue("expected several questions, got ${questions.size}", questions.size >= 4)
        val normalization = questions.first { it.answer.equals("Normalization", ignoreCase = true) }
        assertTrue(normalization.question.contains("organising tables"))
        assertEquals(3, normalization.distractors.size)
        assertTrue(normalization.distractors.none { it.equals("Normalization", ignoreCase = true) })
    }

    @Test
    fun colonStyleNotesAreRecognised() {
        val questions = McqGenerator.generate(notes)
        assertTrue(questions.any { it.answer == "Transaction" && it.question.contains("all succeed") })
    }

    @Test
    fun answerNeverAppearsInsideItsOwnBlank() {
        McqGenerator.generate(notes).filter { it.question.startsWith("Fill in the blank") }.forEach {
            assertTrue(it.question.contains("_____"))
            assertTrue(!it.question.contains(it.answer, ignoreCase = true) || it.answer.length < 4)
        }
    }

    @Test
    fun tooLittleTextGivesNoQuestions() {
        assertTrue(McqGenerator.generate("Short note.").isEmpty())
    }

    @Test
    fun brokenLineHyphensAreJoined() {
        assertEquals("normalization", McqGenerator.cleanText("normal-\nization"))
    }
}
