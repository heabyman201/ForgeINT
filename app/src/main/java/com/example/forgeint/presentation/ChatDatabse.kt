package com.example.forgeint.presentation

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.Insert

import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `message_embeddings` (
                `messageId` INTEGER NOT NULL,
                `vector` TEXT NOT NULL,
                `model` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`messageId`),
                FOREIGN KEY(`messageId`) REFERENCES `messages`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_message_embeddings_messageId` ON `message_embeddings` (`messageId`)")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `message_embeddings_new` (
                `messageId` INTEGER NOT NULL,
                `vector` BLOB NOT NULL,
                `model` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`messageId`),
                FOREIGN KEY(`messageId`) REFERENCES `messages`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.query("SELECT messageId, vector, model, createdAt FROM message_embeddings").use { cursor ->
            val messageIdIdx = cursor.getColumnIndexOrThrow("messageId")
            val vectorIdx = cursor.getColumnIndexOrThrow("vector")
            val modelIdx = cursor.getColumnIndexOrThrow("model")
            val createdAtIdx = cursor.getColumnIndexOrThrow("createdAt")
            while (cursor.moveToNext()) {
                val messageId = cursor.getLong(messageIdIdx)
                val vectorCsv = cursor.getString(vectorIdx).orEmpty()
                val model = cursor.getString(modelIdx).orEmpty()
                val createdAt = cursor.getLong(createdAtIdx)
                val vectorBlob = csvVectorToBlob(vectorCsv)

                db.execSQL(
                    "INSERT OR REPLACE INTO message_embeddings_new(messageId, vector, model, createdAt) VALUES (?, ?, ?, ?)",
                    arrayOf(messageId, vectorBlob, model, createdAt)
                )
            }
        }

        db.execSQL("DROP TABLE message_embeddings")
        db.execSQL("ALTER TABLE message_embeddings_new RENAME TO message_embeddings")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_message_embeddings_messageId` ON `message_embeddings` (`messageId`)")
    }

    private fun csvVectorToBlob(csv: String): ByteArray {
        if (csv.isBlank()) return ByteArray(0)
        val values = csv.split(',')
            .mapNotNull { it.trim().toFloatOrNull() }
        if (values.isEmpty()) return ByteArray(0)
        val buf = ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        values.forEach { buf.putFloat(it) }
        return buf.array()
    }
}

// --- FTS Entities ---
@Entity(tableName = "conversations_fts")
@Fts4(contentEntity = Conversation::class)
data class ConversationFts(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int,
    @ColumnInfo(name = "summary")
    val summary: String
)

@Entity(tableName = "messages_fts")
@Fts4(contentEntity = Message::class)
data class MessageFts(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int,
    @ColumnInfo(name = "text")
    val text: String
)

// --- Standard Entities ---
@Entity(tableName = "conversations")
@Immutable
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
@Immutable
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "message_embeddings",
    indices = [Index(value = ["messageId"])],
    foreignKeys = [ForeignKey(
        entity = Message::class,
        parentColumns = ["id"],
        childColumns = ["messageId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MessageEmbedding(
    @PrimaryKey val messageId: Long,
    val vector: ByteArray,
    val model: String = "openai/text-embedding-3-small",
    val createdAt: Long = System.currentTimeMillis()
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

    @Query(
        """
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

    @Query(
        """
        SELECT * FROM conversations
        WHERE id != :excludeConversationId
        ORDER BY isBookmarked DESC, timestamp DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentConversationsForMemory(excludeConversationId: Long, limit: Int = 48): List<Conversation>

    @Query(
        """
        SELECT id, conversationId, SUBSTR(text, 1, 1200) AS text, isUser, timestamp
        FROM (
            SELECT id, conversationId, text, isUser, timestamp
            FROM messages
            WHERE conversationId = :conversationId
            ORDER BY timestamp DESC
            LIMIT :limit
        )
        ORDER BY timestamp ASC
        """
    )
    suspend fun getRecentMessagesForConversation(conversationId: Long, limit: Int = 18): List<Message>

    @Transaction
    @Query("SELECT * FROM conversations WHERE id = :id")
    fun getConversationWithMessages(id: Long): Flow<ConversationWithMessages>

    @Insert
    suspend fun insertConversation(conversation: Conversation): Long

    @Insert
    suspend fun insertMessage(message: Message): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessageEmbedding(embedding: MessageEmbedding)

    @Query("SELECT * FROM message_embeddings WHERE messageId IN (:messageIds)")
    suspend fun getEmbeddingsForMessages(messageIds: List<Long>): List<MessageEmbedding>

    @Delete
    suspend fun deleteConversation(conversation: Conversation)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long)

    @Query("UPDATE conversations SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun toggleBookmark(id: Long, isBookmarked: Boolean)

    @Query("UPDATE conversations SET summary = :summary WHERE id = :id")
    suspend fun updateConversationSummary(id: Long, summary: String)
}

// --- Database ---
@Database(
    entities = [
        Conversation::class,
        Message::class,
        MessageEmbedding::class,
        UserTrait::class,
        ConversationFts::class,
        MessageFts::class
    ],
    version = 10
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
                    "wear_chat_db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_5_6)
                    .addMigrations(MIGRATION_8_9)
                    .addMigrations(MIGRATION_9_10)
                    .setJournalMode(JournalMode.TRUNCATE)
                    .setQueryExecutor(Executors.newSingleThreadExecutor())
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
