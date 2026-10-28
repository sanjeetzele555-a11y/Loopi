package com.example.myapplication

/**
 * Turns study notes into multiple-choice questions without any network access.
 *
 * It looks for two things in the text:
 *  - definitions ("Normalization is the process of ...", "Firewall: a device that ...")
 *  - key terms that repeat across the document
 * Definitions become "which term matches this description" questions, and sentences that
 * mention a key term become fill-in-the-blank questions. Wrong options are other key terms
 * from the same document, so they are plausible but clearly wrong to someone who knows it.
 */
data class GeneratedQuestion(
    val question: String,
    val answer: String,
    val distractors: List<String>,
    val explanation: String
)

object McqGenerator {

    private val stopWords = setOf(
        "about", "above", "after", "again", "against", "also", "although", "always", "among", "another",
        "because", "been", "before", "being", "below", "between", "both", "cannot", "could", "does",
        "doing", "during", "each", "either", "every", "example", "first", "following", "from", "further",
        "have", "having", "here", "however", "into", "itself", "just", "least", "less", "like", "made",
        "make", "many", "more", "most", "much", "must", "never", "only", "other", "otherwise", "over",
        "same", "second", "should", "since", "some", "such", "than", "that", "their", "them", "then",
        "there", "these", "they", "this", "those", "through", "under", "until", "upon", "used", "uses",
        "using", "very", "what", "when", "where", "whether", "which", "while", "with", "within", "without",
        "would", "your", "page", "chapter", "figure", "table", "section", "lecture", "slide", "student",
        "students", "important", "different", "number", "called", "known", "means", "refers", "defined"
    )

    private val definitionPattern = Regex(
        "^(?:An?|The)?\\s*([A-Za-z][A-Za-z0-9()/\\- ]{1,48}?)\\s+(?:is|are|refers to|means|is defined as|is known as|is called|describes)\\s+(.{15,})$"
    )
    private val colonPattern = Regex("^(?:[-•*\\u2022]\\s*)?([A-Za-z][A-Za-z0-9()/\\- ]{1,48}?)\\s*[:\\u2013\\u2014-]\\s+(.{15,})$")

    /** Joins words split across lines and squeezes whitespace, keeping line breaks for bullet parsing. */
    fun cleanText(raw: String): String = raw
        .replace("\r", "\n")
        .replace(Regex("(\\w)-\\n(\\w)"), "$1$2")
        .replace(Regex("[\\t\\u00A0 ]+"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

    /** Topic name from the document's first heading-like line, else the file name. */
    fun suggestTitle(rawText: String, fileName: String): String {
        val heading = cleanText(rawText).lines()
            .map { it.trim() }
            .firstOrNull { line ->
                line.length in 4..60 && line.count { it.isLetter() } >= 4 && !line.endsWith(".") && line.split(" ").size <= 8
            }
            // "Operating Systems - Lecture 4: ..." keeps just the subject.
            ?.split(Regex("\\s+[-\u2013\u2014:|]\\s+|:\\s*"))?.firstOrNull()?.trim()
        val fromFile = fileName.substringBeforeLast('.').replace(Regex("[_\\-]+"), " ").trim()
        return (heading?.takeIf { it.length >= 3 } ?: fromFile)
            .split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
            .take(50)
            .ifBlank { "PDF Notes" }
    }

    /** First full sentence that reads like content, shortened for the topic card. */
    fun suggestDescription(rawText: String): String {
        // Split on line breaks too, so a heading line never gets glued onto the first sentence.
        val sentence = cleanText(rawText)
            .split(Regex("(?<=[.!?])\\s+|\\n"))
            .map { it.trim() }
            .firstOrNull { it.length in 30..200 && it.split(" ").size >= 6 && it.last() in ".!?" }
        return (sentence ?: "Generated from PDF notes").take(120)
    }

    /** Rough category from words in the notes, matching the Add Topic categories. */
    fun suggestCategory(rawText: String): String {
        val text = rawText.lowercase()
        val scores = mapOf(
            "Computer Science" to listOf("algorithm", "process", "memory", "program", "database", "operating system", "data structure", "software", "compiler"),
            "Information Technology" to listOf("network", "server", "security", "firewall", "protocol", "cloud", "internet", "router"),
            "Engineering" to listOf("circuit", "voltage", "force", "mechanical", "electrical", "design", "material"),
            "Business" to listOf("market", "customer", "profit", "finance", "management", "strategy", "accounting"),
            "Mathematics" to listOf("equation", "theorem", "matrix", "integral", "derivative", "probability", "function")
        ).mapValues { (_, words) -> words.sumOf { word -> Regex(Regex.escape(word)).findAll(text).count() } }
        return scores.maxByOrNull { it.value }?.takeIf { it.value >= 2 }?.key ?: "Other"
    }

    fun generate(rawText: String, maxQuestions: Int = 15): List<GeneratedQuestion> {
        val text = cleanText(rawText)
        if (text.length < 80) return emptyList()

        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val sentences = text
            .replace(Regex("\\n(?=[a-z])"), " ")
            .split(Regex("(?<=[.!?])\\s+|\\n"))
            .map { it.trim() }
            .filter { it.length in 30..280 && it.count { c -> c.isLetter() } > it.length / 2 }
            .distinct()

        // 1. Definitions from sentences and from "Term: description" lines.
        val definitions = linkedMapOf<String, Pair<String, String>>() // term -> (description, source)
        sentences.forEach { sentence ->
            definitionPattern.find(sentence)?.let { match ->
                addDefinition(definitions, match.groupValues[1], match.groupValues[2], sentence)
            }
        }
        lines.forEach { line ->
            colonPattern.find(line)?.let { match ->
                addDefinition(definitions, match.groupValues[1], match.groupValues[2], line)
            }
        }

        // 2. Key terms: defined terms first, then frequent meaningful words and capitalised phrases.
        val frequency = Regex("[A-Za-z][A-Za-z\\-]{4,}")
            .findAll(text)
            .map { it.value.lowercase() }
            .filter { it !in stopWords }
            .groupingBy { it }
            .eachCount()
        val phrases = Regex("\\b([A-Z][a-z]+(?:\\s+[A-Z][a-z]+){1,3})\\b")
            .findAll(text)
            .map { it.value }
            .filter { phrase -> phrase.split(" ").none { it.lowercase() in stopWords } }
            .groupingBy { it }
            .eachCount()
            .filter { it.value >= 2 }
            .keys
        val frequentWords = frequency.filter { it.value >= 2 }.entries
            .sortedByDescending { it.value }
            .take(40)
            .map { it.key }
        val terms = (definitions.keys + phrases + frequentWords)
            .map { it.trim() }
            .distinctBy { it.lowercase() }

        val questions = mutableListOf<GeneratedQuestion>()
        val usedAnswers = mutableMapOf<String, Int>()

        fun accept(question: GeneratedQuestion): Boolean {
            val key = question.answer.lowercase()
            if ((usedAnswers[key] ?: 0) >= 2 || question.distractors.size < 3) return false
            if (questions.any { it.question.equals(question.question, ignoreCase = true) }) return false
            usedAnswers[key] = (usedAnswers[key] ?: 0) + 1
            questions.add(question)
            return true
        }

        definitions.forEach { (term, value) ->
            val (description, source) = value
            accept(
                GeneratedQuestion(
                    question = "Which term matches this description: \"${description.trimEnd('.')}\"?",
                    answer = term,
                    distractors = pickDistractors(term, terms),
                    explanation = source
                )
            )
        }

        val definitionSources = definitions.values.map { it.second }.toSet()
        for (sentence in sentences) {
            if (questions.size >= maxQuestions * 2) break
            // A definition sentence already has its own question; blanking it again would repeat it.
            if (sentence in definitionSources) continue
            val term = terms.firstOrNull { candidate ->
                candidate.length >= 4 && Regex("\\b${Regex.escape(candidate)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(sentence)
            } ?: continue
            val blanked = sentence.replaceFirst(Regex("\\b${Regex.escape(term)}\\b", RegexOption.IGNORE_CASE), "_____")
            // Skip sentences where the blank leaves almost nothing to reason from.
            if (blanked.length < 30) continue
            accept(
                GeneratedQuestion(
                    question = "Fill in the blank: $blanked",
                    answer = displayTerm(term, sentence),
                    distractors = pickDistractors(term, terms),
                    explanation = sentence
                )
            )
        }

        // Definition questions are usually the clearest, so they come first.
        return questions.take(maxQuestions)
    }

    private fun addDefinition(target: MutableMap<String, Pair<String, String>>, rawTerm: String, rawDescription: String, source: String) {
        val term = rawTerm.trim().trim('-', '•', '*').trim()
        val words = term.split(" ")
        if (words.size > 5 || term.length < 3) return
        if (words.first().lowercase() in stopWords || term.lowercase() in setOf("it", "this", "there", "they", "which")) return
        val description = rawDescription.trim()
        if (description.split(" ").size < 4) return
        if (target.keys.none { it.equals(term, ignoreCase = true) }) {
            target[term.replaceFirstChar { it.uppercase() }] = description.take(220) to source
        }
    }

    /** Uses the casing the sentence had, so "TCP" stays "TCP" and "kernel" stays readable. */
    private fun displayTerm(term: String, sentence: String): String {
        val match = Regex("\\b${Regex.escape(term)}\\b", RegexOption.IGNORE_CASE).find(sentence)?.value ?: term
        return if (match.all { it.isLowerCase() || !it.isLetter() }) match.replaceFirstChar { it.uppercase() } else match
    }

    private fun pickDistractors(answer: String, terms: List<String>): List<String> {
        val answerWords = answer.split(" ").size
        return terms
            .filter { !it.equals(answer, ignoreCase = true) }
            .filter { !it.contains(answer, ignoreCase = true) && !answer.contains(it, ignoreCase = true) }
            // Single words lifted out of a longer defined term ("Primary" from "Primary key") read as broken options.
            .filter { candidate -> terms.none { other -> other != candidate && other.contains(" ") && other.split(" ").any { it.equals(candidate, ignoreCase = true) } } }
            // Prefer terms of a similar shape so the right answer doesn't stand out.
            .sortedBy { kotlin.math.abs(it.split(" ").size - answerWords) * 10 + kotlin.math.abs(it.length - answer.length) }
            .take(3)
            .map { it.replaceFirstChar { c -> c.uppercase() } }
    }
}
