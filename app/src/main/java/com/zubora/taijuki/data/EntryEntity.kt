package com.zubora.taijuki.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey val date: String, // ISO yyyy-MM-dd
    val weight: Double,
    val stamps: String, // comma-separated StampType ids, "" if none
    val memo: String,
)

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY date ASC")
    fun observeAll(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): EntryEntity?

    @Upsert
    suspend fun upsert(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE date = :date")
    suspend fun deleteByDate(date: String)
}

@Database(entities = [EntryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zubora.db",
                ).build().also { instance = it }
            }
    }
}
