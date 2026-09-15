package app.worn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SettingsEntity::class,
        ActivityTypeEntity::class,
        SessionEntity::class,
        DailyRecordEntity::class,
        AlignerSetEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class WornDatabase : RoomDatabase() {
    abstract fun settings(): SettingsDao
    abstract fun activities(): ActivityTypeDao
    abstract fun sessions(): SessionDao
    abstract fun days(): DailyRecordDao
    abstract fun aligners(): AlignerSetDao
}
