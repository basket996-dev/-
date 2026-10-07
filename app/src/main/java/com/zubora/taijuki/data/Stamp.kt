package com.zubora.taijuki.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zubora.taijuki.ui.theme.CustomStampColors
import com.zubora.taijuki.ui.theme.StampType
import kotlinx.coroutines.flow.Flow

/**
 * A stamp on the input screen. The six original stamps keep their hand-drawn
 * icons ([builtin]); stamps the user makes show an [emoji] instead.
 */
data class Stamp(
    val id: String,
    val label: String,
    val emoji: String?,
    val color: Color,
    /** Shown on the input screen. Put-away stamps still render on the days that used them. */
    val active: Boolean,
    val sortOrder: Int,
) {
    val builtin: StampType? get() = StampType.fromId(id)
}

@Entity(tableName = "stamps")
data class StampEntity(
    @PrimaryKey val id: String,
    val label: String,
    val emoji: String?,
    val color: Int, // ARGB
    val active: Boolean,
    val sortOrder: Int,
)

fun StampEntity.toDomain(): Stamp = Stamp(id, label, emoji, Color(color), active, sortOrder)

fun Stamp.toEntity(): StampEntity = StampEntity(id, label, emoji, color.toArgb(), active, sortOrder)

@Dao
interface StampDao {
    @Query("SELECT * FROM stamps ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<StampEntity>>

    @Query("SELECT * FROM stamps ORDER BY sortOrder ASC")
    suspend fun getAll(): List<StampEntity>

    @Upsert
    suspend fun upsertAll(stamps: List<StampEntity>)
}

/**
 * What a fresh install — or the upgrade from the fixed six stamps — starts with:
 * the two originals that actually got used, plus 外食・コンビニ. The other
 * originals are put away rather than deleted, so old days still show them.
 */
object StampSeed {
    const val CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS `stamps` (`id` TEXT NOT NULL, `label` TEXT NOT NULL, `emoji` TEXT, " +
            "`color` INTEGER NOT NULL, `active` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`))"

    val defaults: List<StampEntity> = listOf(
        builtin(StampType.Tabesugi, active = true, order = 0),
        builtin(StampType.Benzuu, active = true, order = 1),
        StampEntity("gaishoku", "外食・コンビニ", "🍽️", CustomStampColors[0].toArgb(), active = true, sortOrder = 2),
        builtin(StampType.Osake, active = false, order = 3),
        builtin(StampType.Undou, active = false, order = 4),
        builtin(StampType.Gohoubi, active = false, order = 5),
        builtin(StampType.Ganbatta, active = false, order = 6),
    )

    private fun builtin(type: StampType, active: Boolean, order: Int) =
        StampEntity(type.id, type.label, null, type.color.toArgb(), active, order)

    fun insertDefaults(db: SupportSQLiteDatabase) {
        defaults.forEach { s ->
            db.execSQL(
                "INSERT OR IGNORE INTO stamps (id, label, emoji, color, active, sortOrder) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(s.id, s.label, s.emoji, s.color, if (s.active) 1 else 0, s.sortOrder),
            )
        }
    }
}
