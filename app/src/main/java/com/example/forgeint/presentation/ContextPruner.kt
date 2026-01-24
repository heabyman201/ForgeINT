package com.example.forgeint.presentation

import java.util.Locale
import java.util.HashSet

object ContextPruner {

    // Optimized: Explicit HashSet for O(1) lookups
    private val STOP_WORDS = HashSet(listOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't", "as", "at",
        "be", "because", "been", "before", "being", "below", "between", "both", "but", "by",
        "can", "can't", "cannot", "could", "couldn't",
        "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during",
        "each",
        "few", "for", "from", "further",
        "had", "hadn't", "has", "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself", "him", "himself", "his", "how", "how's",
        "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's", "its", "itself",
        "let's",
        "me", "more", "most", "mustn't", "my", "myself",
        "no", "nor", "not",
        "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own",
        "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such",
        "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "there", "there's", "these", "they", "they'd", "they'll", "they're", "they've", "this", "those", "through", "to", "too",
        "under", "until", "up",
        "very",
        "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were", "weren't", "what", "what's", "when", "when's", "where", "where's", "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would", "wouldn't",
        "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself", "yourselves"
    ))

    fun pruneMessages(
        messages: List<Message>,
        maxContextMessages: Int = 20,
        recentKeepCount: Int = 4,
        maxLookbackMillis: Long = 0L // 0L means disabled
    ): List<Message> {
        if (messages.size <= maxContextMessages) {
            return messages
        }

        val splitIndex = (messages.size - recentKeepCount).coerceAtLeast(0)
        val recentMessages = messages.subList(splitIndex, messages.size)
        var olderMessages = messages.subList(0, splitIndex)

        // Optimization: Timestamp-Based Filtering First
        if (maxLookbackMillis > 0) {
            val cutoff = System.currentTimeMillis() - maxLookbackMillis
            olderMessages = olderMessages.filter { it.timestamp >= cutoff }
        }

        if (olderMessages.isEmpty()) {
            return recentMessages
        }

        val slotsForOlder = (maxContextMessages - recentMessages.size).coerceAtLeast(0)
        if (slotsForOlder == 0) {
            return recentMessages
        }

        // Optimization: Single pass frequency map construction using Manual Scanner
        val wordFrequencies = HashMap<String, Int>()
        // We scan ALL messages (recent + older) to build the "context"
        for (i in messages.indices) {
             scanAndAction(messages[i].text) { word ->
                 if (!STOP_WORDS.contains(word)) {
                     wordFrequencies[word] = (wordFrequencies[word] ?: 0) + 1
                 }
             }
        }

        // Optimization: Use Sequence for lazy evaluation
        // Optimization: Zero-allocation scoring inside the sequence map
        val selectedOlderMessages = olderMessages.asSequence()
            .map { msg ->
                val score = calculateScore(msg.text, wordFrequencies)
                msg to score
            }
            .sortedByDescending { it.second }
            .take(slotsForOlder)
            .map { it.first }
            .sortedBy { it.timestamp }
            .toList()

        return selectedOlderMessages + recentMessages
    }

    // Optimization: Manual Scanner to avoid regex and split allocations
    private inline fun scanAndAction(text: CharSequence, action: (String) -> Unit) {
        val len = text.length
        var start = -1
        
        for (i in 0 until len) {
            val c = text[i]
            // Simple check for alphanumeric
            if (Character.isLetterOrDigit(c)) {
                if (start == -1) start = i
            } else {
                if (start != -1) {
                    val word = text.substring(start, i).lowercase(Locale.ROOT)
                    action(word)
                    start = -1
                }
            }
        }
        if (start != -1) {
            val word = text.substring(start, len).lowercase(Locale.ROOT)
            action(word)
        }
    }

    // Optimization: Zero-Allocation Scoring (re-uses scan logic but optimized for lookup)
    private fun calculateScore(text: CharSequence, frequencies: Map<String, Int>): Int {
        var score = 0
        scanAndAction(text) { word ->
            if (!STOP_WORDS.contains(word)) {
                 score += frequencies[word] ?: 0
            }
        }
        return score
    }
}