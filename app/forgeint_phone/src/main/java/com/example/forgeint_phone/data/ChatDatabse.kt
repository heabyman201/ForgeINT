package com.example.forgeint_phone.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.Executors

// --- Migrations ---
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_conversationId` ON `messages` (`conversationId`)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `user_traits` (
                `traitKey` TEXT NOT NULL, 
                `traitValue` TEXT NOT NULL, 
                `category` TEXT NOT NULL, 
                PRIMARY KEY(`traitKey`)
            )
            """.trimIndent()
        )
    }
}

// FIXED: Migration to set up FTS correctly and backfill data
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Drop old FTS tables if they exist (clean slate)
        db.execSQL("DROP TABLE IF EXISTS `conversations_fts`")
        db.execSQL("DROP TABLE IF EXISTS `messages_fts`")

        // 2. Create Virtual Tables linked to content
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `conversations_fts` USING FTS4(summary, content=`conversations`)")
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `messages_fts` USING FTS4(text, content=`messages`)")

        // 3. Backfill existing data (CRITICAL step for search to work immediately)
        db.execSQL("INSERT INTO conversations_fts(docid, summary) SELECT id, summary FROM conversations")
        db.execSQL("INSERT INTO messages_fts(docid, text) SELECT id, text FROM messages")

        // 4. Create Triggers (Conversations)
        db.execSQL("CREATE TRIGGER IF NOT EXISTS conversations_bu BEFORE UPDATE ON conversations BEGIN DELETE FROM conversations_fts WHERE docid=old.id; END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS conversations_bd BEFORE DELETE ON conversations BEGIN DELETE FROM conversations_fts WHERE docid=old.id; END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS conversations_au AFTER UPDATE ON conversations BEGIN INSERT INTO conversations_fts(docid, summary) VALUES(new.id, new.summary); END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS conversations_ai AFTER INSERT ON conversations BEGIN INSERT INTO conversations_fts(docid, summary) VALUES(new.id, new.summary); END")

        // 5. Create Triggers (Messages)
        db.execSQL("CREATE TRIGGER IF NOT EXISTS messages_bu BEFORE UPDATE ON messages BEGIN DELETE FROM messages_fts WHERE docid=old.id; END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS messages_bd BEFORE DELETE ON messages BEGIN DELETE FROM messages_fts WHERE docid=old.id; END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS messages_au AFTER UPDATE ON messages BEGIN INSERT INTO messages_fts(docid, text) VALUES(new.id, new.text); END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS messages_ai AFTER INSERT ON messages BEGIN INSERT INTO messages_fts(docid, text) VALUES(new.id, new.text); END")
    }
}

// --- FTS Entities ---
@Entity(tableName = "conversations_fts")
@Fts4(contentEntity = Conversation::class)
data class ConversationFts(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowid: Int,
    @ColumnInfo(name = "summary") val summary: String,
)

@Entity(tableName = "messages_fts")
@Fts4(contentEntity = Message::class)
data class MessageFts(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowid: Int,
    @ColumnInfo(name = "text") val text: String
)

// --- Standard Entities ---
@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false
)

@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversationId"])],
    foreignKeys = [ForeignKey(
        entity = Conversation::class,
        parentColumns = ["id"],
        childColumns = ["conversationId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ConversationWithMessages(
    @Embedded val conversation: Conversation,
    @Relation(parentColumn = "id", entityColumn = "conversationId")
    val messages: List<Message>
)

// --- DAO ---
@Dao
interface ChatDao {
    @Query("SELECT id, summary, timestamp, isBookmarked FROM conversations ORDER BY isBookmarked DESC, timestamp DESC LIMIT 15")
    fun getConversations(): Flow<List<Conversation>>

    // FIXED: Using `docid` for the JOIN.
    // This is the fastest possible way to search text in SQLite.
    @Query("""
        SELECT c.id, c.summary, c.timestamp, c.isBookmarked 
        FROM conversations AS c
        WHERE c.id IN (
            SELECT docid FROM conversations_fts WHERE conversations_fts MATCH :query
            UNION
            SELECT m.conversationId FROM messages AS m
            JOIN messages_fts AS fts ON m.id = fts.docid 
            WHERE fts.text MATCH :query
        )
        ORDER BY c.isBookmarked DESC, c.timestamp DESC
    """
    )
    fun searchConversations(query: String): Flow<List<Conversation>>

    @Query("SELECT * FROM messages WHERE conversationId = :id ORDER BY timestamp DESC LIMIT :limit")
    fun getMessages(id: Long, limit: Int): Flow<List<Message>>

    @Transaction
    @Query("SELECT * FROM conversations WHERE id = :id")
    fun getConversationWithMessages(id: Long): Flow<ConversationWithMessages>

    @Insert
    suspend fun insertConversation(conversation: Conversation): Long

    @Insert
    suspend fun insertMessage(message: Message)

    @Delete
    suspend fun deleteConversation(conversation: Conversation)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long)

    @Query("UPDATE conversations SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun toggleBookmark(id: Long, isBookmarked: Boolean)
}

// --- Database ---
@Database(
    entities = [
        Conversation::class,
        Message::class,
        UserTrait::class,
        ConversationFts::class,
        MessageFts::class
    ],
    version = 6
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun traitDao(): TraitDao

    companion object {
        @Volatile private var INSTANCE: ChatDatabase? = null

        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "phone_chat_db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_5_6) // Apply the fix
                    .setJournalMode(JournalMode.TRUNCATE)
                    .setQueryExecutor(Executors.newSingleThreadExecutor())
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}