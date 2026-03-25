package com.example.forgeint.presentation

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

data class RankedConversationMemory(
    val conversation: Conversation,
    val finalScore: Double,
    val vectorScore: Double,
    val semanticScore: Double,
    val keywordCoverage: Double,
    val recencyScore: Double,
    val importanceScore: Double,
    val selectedMessages: List<Message>
)

object MemoryRetrievalEngine {
    private const val MAX_CONTEXT_CHARS = 1700
    private const val MAX_SNIPPET_CHARS_PER_MSG = 170
    private const val MAX_MESSAGES_PER_CONVO = 3
    private const val MIN_FINAL_SCORE = 0.18

    private val stopWords = setOf(
        "the", "and", "for", "are", "you", "your", "with", "that", "this", "from", "have",
        "has", "had", "what", "when", "where", "which", "would", "could", "should", "about",
        "into", "than", "then", "just", "like", "want", "need", "help", "please", "tell",
        "how", "why", "can", "cant", "dont", "does", "did", "not", "was", "were", "been",
        "will", "shall", "who", "them", "they", "their", "there", "here", "also", "today",
        "http", "https", "com", "www"
    )

    private val personalSignalWords = setOf(
        "i", "im", "my", "mine", "prefer", "love", "hate", "like", "dislike", "allergic",
        "work", "job", "career", "name", "birthday", "family", "partner", "husband", "wife"
    )

    fun rankConversations(
        query: String,
        conversations: List<Conversation>,
        messagesByConversation: Map<Long, List<Message>>,
        vectorScoresByConversation: Map<Long, Double> = emptyMap(),
        nowMs: Long = System.currentTimeMillis(),
        maxResults: Int = 7
    ): List<RankedConversationMemory> {
        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return emptyList()

        val documents = conversations.mapNotNull { conversation ->
            val msgs = messagesByConversation[conversation.id].orEmpty()
            if (msgs.isEmpty()) return@mapNotNull null
            val docText = buildString {
                append(conversation.summary)
                append(' ')
                msgs.forEach { append(it.text).append(' ') }
            }
            Triple(conversation, msgs, tokenize(docText))
        }
        if (documents.isEmpty()) return emptyList()

        val idf = buildIdfMap(queryTokens, documents.map { it.third.toSet() })
        val queryVec = tfidfVector(queryTokens, idf)
        val queryTerms = queryTokens.toSet()

        val ranked = documents.mapNotNull { (conversation, messages, docTokens) ->
            val vectorScore = vectorScoresByConversation[conversation.id] ?: 0.0
            val semantic = cosineSimilarity(queryVec, tfidfVector(docTokens, idf))
            val matchedTerms = docTokens.toSet().intersect(queryTerms)
            val coverage = matchedTerms.size.toDouble() / queryTerms.size.toDouble()
            if (semantic <= 0.03 && coverage <= 0.20 && vectorScore <= 0.15) return@mapNotNull null

            val ageHours = (nowMs - conversation.timestamp).coerceAtLeast(0L) / 3_600_000.0
            val recency = exp(-ageHours / 72.0)
            val importance = computeImportance(messages)

            val finalScore =
                (vectorScore * 0.40) +
                (semantic * 0.30) +
                (coverage * 0.15) +
                (recency * 0.10) +
                (importance * 0.05)
            val selected = selectMessagesForContext(messages, queryTerms)

            RankedConversationMemory(
                conversation = conversation,
                finalScore = finalScore,
                vectorScore = vectorScore,
                semanticScore = semantic,
                keywordCoverage = coverage,
                recencyScore = recency,
                importanceScore = importance,
                selectedMessages = selected
            )
        }

        return ranked
            .sortedByDescending { it.finalScore }
            .filter { it.finalScore >= MIN_FINAL_SCORE && it.selectedMessages.isNotEmpty() }
            .take(maxResults)
    }

    fun buildPromptContext(ranked: List<RankedConversationMemory>): String {
        if (ranked.isEmpty()) return ""
        return buildString {
            append("\n[RELEVANT PAST CONVERSATIONS (Hybrid Retrieval)]:\n")
            var usedChars = 0
            ranked.forEach { item ->
                if (usedChars >= MAX_CONTEXT_CHARS) return@forEach
                val conversationHeader = "- Conversation: ${truncate(item.conversation.summary, 90)}\n"
                if (usedChars + conversationHeader.length > MAX_CONTEXT_CHARS) return@forEach
                append(conversationHeader)
                usedChars += conversationHeader.length
                item.selectedMessages.forEach { msg ->
                    if (usedChars >= MAX_CONTEXT_CHARS) return@forEach
                    val role = if (msg.isUser) "User" else "Assistant"
                    val line = "  $role: ${truncate(msg.text, MAX_SNIPPET_CHARS_PER_MSG)}\n"
                    if (usedChars + line.length <= MAX_CONTEXT_CHARS) {
                        append(line)
                        usedChars += line.length
                    }
                }
            }
            append("[PAST_CONVERSATION_PROTOCOL]: Use this as optional context when relevant. ")
            append("If anything conflicts, trust the user's latest message.\n")
        }
    }

    private fun selectMessagesForContext(messages: List<Message>, queryTerms: Set<String>): List<Message> {
        if (messages.isEmpty()) return emptyList()

        val scored = messages.map { msg ->
            val text = compactWhitespace(msg.text)
            if (text.isBlank()) return@map (msg to Double.NEGATIVE_INFINITY)
            val tokens = tokenize(text).toSet()
            val match = tokens.intersect(queryTerms).size.toDouble()
            val personalBoost = if (msg.isUser && tokens.any { it in personalSignalWords }) 0.65 else 0.0
            val numericBoost = if (Regex("\\d").containsMatchIn(text)) 0.18 else 0.0
            val recencyBoost = ((msg.timestamp - messages.first().timestamp).coerceAtLeast(0L).toDouble() /
                (messages.last().timestamp - messages.first().timestamp).coerceAtLeast(1L).toDouble()) * 0.22
            val denseLengthPenalty = (text.length / 1200.0).coerceIn(0.0, 1.0) * 0.15
            msg to (match + personalBoost + numericBoost + recencyBoost - denseLengthPenalty)
        }

        val picked = mutableListOf<Message>()
        val seen = LinkedHashSet<String>()
        scored.sortedByDescending { it.second }.forEach { (msg, score) ->
            if (picked.size >= MAX_MESSAGES_PER_CONVO) return@forEach
            if (score <= 0.0) return@forEach
            val compressed = compressMessageForContext(msg, queryTerms) ?: return@forEach
            val signature = compactWhitespace(compressed.text).lowercase()
            if (signature.length >= 16 && !seen.add(signature.take(140))) return@forEach
            picked.add(compressed)
        }
        if (picked.isNotEmpty()) return picked.sortedBy { it.timestamp }

        return messages
            .takeLast(2)
            .mapNotNull { compressMessageForContext(it, queryTerms) }
    }

    private fun compressMessageForContext(msg: Message, queryTerms: Set<String>): Message? {
        val text = compactWhitespace(msg.text)
        if (text.isBlank()) return null

        val sentences = splitSentences(text)
        if (sentences.isEmpty()) {
            val fallback = truncate(text, MAX_SNIPPET_CHARS_PER_MSG)
            return if (fallback.isBlank()) null else msg.copy(text = fallback)
        }

        val rankedSentences = sentences.map { sentence ->
            val tokenSet = tokenize(sentence).toSet()
            val hit = tokenSet.intersect(queryTerms).size.toDouble()
            val personal = if (tokenSet.any { it in personalSignalWords }) 0.7 else 0.0
            val hasNumber = if (Regex("\\d").containsMatchIn(sentence)) 0.2 else 0.0
            val lengthBonus = (sentence.length.coerceAtMost(140) / 140.0) * 0.2
            sentence to (hit + personal + hasNumber + lengthBonus)
        }.sortedByDescending { it.second }

        val selected = mutableListOf<String>()
        var total = 0
        for ((sentence, score) in rankedSentences) {
            if (score <= 0.05) continue
            val s = sentence.trim()
            if (s.isBlank()) continue
            val addLen = s.length + if (selected.isEmpty()) 0 else 1
            if (total + addLen > MAX_SNIPPET_CHARS_PER_MSG) continue
            selected.add(s)
            total += addLen
            if (selected.size >= 2) break
        }

        val compressed = if (selected.isEmpty()) {
            truncate(text, MAX_SNIPPET_CHARS_PER_MSG)
        } else {
            truncate(selected.joinToString(" "), MAX_SNIPPET_CHARS_PER_MSG)
        }
        return if (compressed.isBlank()) null else msg.copy(text = compressed)
    }

    private fun splitSentences(text: String): List<String> {
        return text
            .split(Regex("(?<=[.!?])\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.length >= 18 }
            .take(16)
    }

    private fun compactWhitespace(text: String): String {
        return text.replace(Regex("\\s+"), " ").trim()
    }

    private fun computeImportance(messages: List<Message>): Double {
        if (messages.isEmpty()) return 0.0
        var score = 0.0
        var considered = 0
        messages.takeLast(20).forEach { msg ->
            if (!msg.isUser) return@forEach
            considered++
            val tokens = tokenize(msg.text).toSet()
            val hasPersonalSignal = tokens.any { it in personalSignalWords }
            val hasNumber = Regex("\\d").containsMatchIn(msg.text)
            val hasPreferencePhrase = msg.text.contains("I ", ignoreCase = true) ||
                msg.text.contains("my ", ignoreCase = true) ||
                msg.text.contains("I am", ignoreCase = true) ||
                msg.text.contains("I like", ignoreCase = true) ||
                msg.text.contains("I prefer", ignoreCase = true)
            if (hasPersonalSignal) score += 0.45
            if (hasNumber) score += 0.20
            if (hasPreferencePhrase) score += 0.35
        }
        if (considered == 0) return 0.0
        return (score / considered).coerceIn(0.0, 1.0)
    }

    private fun tokenize(text: String): List<String> {
        return Regex("[a-zA-Z0-9']+")
            .findAll(text.lowercase())
            .map { it.value.trim('\'') }
            .filter { it.length >= 3 }
            .filterNot { it in stopWords }
            .map { stem(it) }
            .toList()
    }

    private fun stem(token: String): String {
        return when {
            token.length > 6 && token.endsWith("ing") -> token.dropLast(3)
            token.length > 5 && token.endsWith("ed") -> token.dropLast(2)
            token.length > 5 && token.endsWith("es") -> token.dropLast(2)
            token.length > 4 && token.endsWith("s") -> token.dropLast(1)
            else -> token
        }
    }

    private fun buildIdfMap(queryTokens: List<String>, docTokenSets: List<Set<String>>): Map<String, Double> {
        val n = docTokenSets.size.toDouble()
        return queryTokens.toSet().associateWith { term ->
            val df = docTokenSets.count { it.contains(term) }.toDouble()
            ln((n + 1.0) / (df + 1.0)) + 1.0
        }
    }

    private fun tfidfVector(tokens: List<String>, idf: Map<String, Double>): Map<String, Double> {
        if (tokens.isEmpty()) return emptyMap()
        val tf = tokens.groupingBy { it }.eachCount()
        val maxTf = tf.values.maxOrNull()?.toDouble() ?: 1.0
        return tf.mapValues { (term, count) ->
            val normalizedTf = count.toDouble() / maxTf
            normalizedTf * (idf[term] ?: 1.0)
        }
    }

    private fun cosineSimilarity(a: Map<String, Double>, b: Map<String, Double>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val dot = a.entries.sumOf { (term, value) -> value * (b[term] ?: 0.0) }
        val normA = sqrt(a.values.sumOf { it * it })
        val normB = sqrt(b.values.sumOf { it * it })
        if (normA == 0.0 || normB == 0.0) return 0.0
        return (dot / (normA * normB)).coerceIn(0.0, 1.0)
    }

    private fun truncate(text: String, maxChars: Int): String {
        val cleaned = compactWhitespace(text)
        return if (cleaned.length <= maxChars) cleaned else "${cleaned.take(maxChars)}..."
    }
}
