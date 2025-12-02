package com.example.personal_secretary

import android.content.Context
import androidx.room.*


@Entity(
    tableName = "ai_response",
    primaryKeys = ["userId"]
)
data class AiResponseEntity(
    val userId: String,
    val date: String,
    val response: String
)


@Dao
interface AiResponseDao {

    @Query("SELECT * FROM ai_response WHERE userId = :userId LIMIT 1")
    suspend fun getResponse(userId: String): AiResponseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveResponse(aiResponse: AiResponseEntity)

    @Query("DELETE FROM ai_response WHERE userId = :userId")
    suspend fun deleteResponse(userId: String)
}


@Database(
    entities = [AiResponseEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun aiResponseDao(): AiResponseDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_response_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}


class ResponseRepository(context: Context) {

    private val dao = AppDatabase.getDatabase(context).aiResponseDao()

    suspend fun getSavedResponse(userId: String): AiResponseEntity? {
        return dao.getResponse(userId)
    }

    suspend fun saveResponse(userId: String, date: String, text: String) {
        dao.saveResponse(
            AiResponseEntity(
                userId = userId,
                date = date,
                response = text
            )
        )
    }

    suspend fun clearResponse(userId: String) {
        dao.deleteResponse(userId)
    }
}
