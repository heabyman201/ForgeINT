package com.example.forgeint.presentation

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
        Analyze the user message for ANY permanent traits, preferences, facts, or context.
        You are a memory engine. Your goal is to extract as much useful long-term information as possible.
        
        You MUST extract multiple traits if present. Do not limit yourself to one.
        Extract implicit facts (e.g., "I'm tired from coding all night" -> User is a Developer | Profession | Work).
        
        Each key must be SPECIFIC (e.g., 'PREF_LANGUAGE' instead of just 'LANGUAGE').
        
        Format: KEY|VALUE|CATEGORY
        Example: 
        NAME|Ali|PERSONAL
        FAV_LANG|Kotlin|CODING
        GYM_STYLE|Bodybuilding|FITNESS
        CURRENT_PROJECT|ForgeInt|WORK
        DEVICE|Pixel 9 Pro|TECH
        
        If nothing worth remembering, return "NULL".
    """.trimIndent()
}