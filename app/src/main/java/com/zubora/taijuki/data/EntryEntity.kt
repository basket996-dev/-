package com.zubora.taijuki.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey val date: String, // ISO yyyy-MM-dd
    val weight: Double,
    val stamps: String, // comma-separated Stamp ids, "" if none
    val memo: String,
    /** When the day's weight was first saved on that same day (epoch ms). Null for older rows and back-filled days. */
    val recordedAt: Long? = null,
)

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY date ASC")
    fun observeAll(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): EntryEntity?

    @Query("SELECT * FROM entries ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): EntryEntity?

    @Query("SELECT * FROM entries WHERE date < :date ORDER BY date DESC LIMIT 1")
    suspend fun getLatestBefore(date: String): EntryEntity?

    @Query("SELECT COUNT(*) FROM entries WHERE date BETWEEN :from AND :to")
    suspend fun countBetween(from: String, to: String): Int

    @Upsert
    suspend fun upsert(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE date = :date")
    suspend fun deleteByDate(date: String)
}

@Database(entities = [EntryEntity::class, StampEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun stampDao(): StampDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** v2: the record time, and stamps the user can add to instead of a fixed six. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN recordedAt INTEGER")
                db.execSQL(StampSeed.CREATE_TABLE)
                StampSeed.insertDefaults(db)
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zubora.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) = StampSeed.insertDefaults(db)
                    })
                    .build()
                    .also { instance = it }
            }
    }
}
