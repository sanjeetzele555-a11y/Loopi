package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyPackFormatTest {
    @Test
    fun versionTwoPackCarriesTopicsAndCards() {
        val parsed = parseStudyPack(
            """
            {"type":"loopi_study_pack","version":2,"name":"Exam pack","description":"",
             "topics":["Databases"],
             "topicData":[{"title":"Databases","category":"IT","difficulty":"Hard",
               "cards":[{"question":"What does SQL stand for?","answer":"Structured Query Language","explanation":""},
                        {"question":"","answer":"skipped"}]}]}
            """.trimIndent()
        )
        assertEquals("Exam pack", parsed.pack.name)
        assertEquals(1, parsed.topics.size)
        assertEquals(1, parsed.topics[0].cards.size)
    }

    @Test
    fun versionOnePackStillImportsTopicNames() {
        val parsed = parseStudyPack("""{"type":"loopi_study_pack","version":1,"name":"Old","topics":["Networks"]}""")
        assertTrue(parsed.topics.isEmpty())
        assertTrue(parsed.pack.topicNamesJson.contains("Networks"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun otherFilesAreRejected() {
        parseStudyPack("""{"type":"something_else","version":2,"name":"x","topics":["a"]}""")
    }
}
