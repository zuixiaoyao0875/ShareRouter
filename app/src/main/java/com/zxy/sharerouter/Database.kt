package com.zxy.sharerouter

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "share_history")
data class ShareHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val type: String, // "TEXT", "IMAGE", "VIDEO", "FILE", "MULTIPLE"
    val contentText: String? = null,
    val filePaths: String? = null, // Store as comma-separated or JSON
    val attachmentNames: String? = null, // Original filenames
    val mimeType: String? = null,
    val sourceApp: String? = null, // Display name
    val sourcePackage: String? = null // Package name for icon/deep link
)

@Dao
interface ShareHistoryDao {
    @Query("SELECT * FROM share_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ShareHistoryEntry>>

    @Insert
    fun insert(entry: ShareHistoryEntry)

    @Delete
    fun delete(entry: ShareHistoryEntry)

    @Query("DELETE FROM share_history WHERE id NOT IN (SELECT id FROM share_history ORDER BY timestamp DESC LIMIT :limit)")
    fun trim(limit: Int)

    @Query("DELETE FROM share_history")
    fun clearAll()
}

@Database(entities = [ShareHistoryEntry::class], version = 2)
abstract class ShareDatabase : RoomDatabase() {
    abstract fun historyDao(): ShareHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: ShareDatabase? = null

        fun getDatabase(context: android.content.Context): ShareDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShareDatabase::class.java,
                    "share_router_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
