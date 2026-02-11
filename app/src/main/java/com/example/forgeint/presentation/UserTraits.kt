package com.example.forgeint.presentation

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

enum class MemoryType {

    SHORT_TERM, LONG_TERM

}



@Entity(tableName = "user_traits")

data class UserTrait(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val content: String,

    val source: String = "auto",

    val timestamp: Long = System.currentTimeMillis(),

    val type: MemoryType = MemoryType.LONG_TERM

)



@Dao

interface TraitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)

    suspend fun insertTrait(trait: UserTrait)



    @Query("SELECT * FROM user_traits ORDER BY timestamp DESC")

    suspend fun getAllTraits(): List<UserTrait>



    @Query("SELECT * FROM user_traits WHERE type = :type ORDER BY timestamp DESC")

    suspend fun getTraitsByType(type: MemoryType): List<UserTrait>



    @Query("SELECT * FROM user_traits WHERE content LIKE '%' || :query || '%'")

    suspend fun searchTraits(query: String): List<UserTrait>



    @Query("DELETE FROM user_traits WHERE id = :id")

    suspend fun deleteTrait(id: Int)



    @Query("SELECT * FROM user_traits ORDER BY timestamp DESC")

    fun getAllTraitsFlow(): Flow<List<UserTrait>>



    @Query("DELETE FROM user_traits")

    suspend fun deleteAllTraits()



    @Query("DELETE FROM user_traits WHERE type = 'SHORT_TERM' AND timestamp < :expiryTime")

    suspend fun deleteExpiredShortTermMemory(expiryTime: Long)

}



object MemoryPrompter {

    val extractionPrompt = """

        Analyze the user message for information to be remembered.

        Categorize information into two types:

        1. SHORT_TERM: Ephemeral facts, current mood, immediate context, or trivial details that might change soon. (e.g., "Feeling tired today", "Eating pizza now", "Working on a bug").

        2. LONG_TERM: Important, stable facts about the user that should be remembered indefinitely. (e.g., "Lives in New York", "Software Engineer", "Allergic to nuts", "Likes sci-fi movies").



        Capture:

        - Specific facts (names, places, numbers).

        - Preferences (likes/dislikes).

        - Current context (mood, immediate activity).

        - Technical details.



        Rules:
        - Only output concrete, user-specific facts or preferences.
        - Do NOT output meta statements about conversation behavior.
        - Reject vague lines like "User is talking about something" or "User asked a question".
        - Do NOT store user instructions such as "save this to long term" as memories.
        - Do NOT store lexical/meta observations like "user used X word".
        - If a fact is temporary (today/now/current mood), use SHORT_TERM.
        - If a fact is stable and reusable, use LONG_TERM.

        Output format:

        [TYPE] Fact

        

        Examples:

        [SHORT_TERM] Feeling productive this morning.

        [LONG_TERM] Has a dog named Max.

        [SHORT_TERM] Currently drinking coffee.

        [LONG_TERM] Prefers Kotlin over Java.

        

        If no new useful information is present, return: NULL

    """.trimIndent()

}
