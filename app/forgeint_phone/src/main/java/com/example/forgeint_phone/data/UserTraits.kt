package com.example.forgeint.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "user_traits")
data class UserTrait(
    @PrimaryKey val traitKey: String,
    val traitValue: String,
    val category: String
)


@Dao
interface TraitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTrait(trait: UserTrait)

    @Query("SELECT * FROM user_traits")
    suspend fun getAllTraits(): List<UserTrait>

    @Query("DELETE FROM user_traits WHERE traitKey = :key")
    suspend fun deleteTrait(key: String)
    @Query("SELECT * FROM user_traits")
    fun getAllTraitsFlow(): kotlinx.coroutines.flow.Flow<List<UserTrait>> // Added Flow
    @Query("DELETE FROM user_traits") // Added for "Format Memory"
    suspend fun deleteAllTraits()
}
object MemoryPrompter {
    val extractionPrompt = """
        Analyze the user message for permanent traits, preferences, or facts.
        You MUST extract multiple traits if present. 
        Each key must be SPECIFIC (e.g., 'PREF_LANGUAGE' instead of just 'LANGUAGE').
        Do NOT output vague or meta-conversation facts like "User is talking about something".
        Do NOT store instruction text like "save this to long term" as memory.
        Do NOT store lexical observations like "user used X word".
        Only output concrete, user-specific facts/preferences.
        
        Format: KEY|VALUE|CATEGORY
        Example: 
        NAME|Ali|PERSONAL
        FAV_LANG|Kotlin|CODING
        GYM_STYLE|Bodybuilding|FITNESS
        
        If nothing worth remembering, return "NULL".
    """.trimIndent()
}
