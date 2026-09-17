package app.worn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SettingsEntity::class,
        ActivityTypeEntity::class,
        SessionEntity::class,
        DailyRecordEntity::class,
        AlignerSetEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class WornDatabase : RoomDatabase() {
    abstract fun settings(): SettingsDao
    abstract fun activities(): ActivityTypeDao
    abstract fun sessions(): SessionDao
    abstract fun days(): DailyRecordDao
    abstract fun aligners(): AlignerSetDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN replacementRemindersEnabled INTEGER NOT NULL DEFAULT 1",
                )
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN replacementIntervalDays INTEGER NOT NULL DEFAULT 10",
                )
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN removalReminderSoundEnabled INTEGER NOT NULL DEFAULT 1",
                )
            }
        }
    }
}
