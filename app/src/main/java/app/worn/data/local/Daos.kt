package app.worn.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun get(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SettingsEntity)
}

@Dao
interface ActivityTypeDao {
    @Query("SELECT * FROM activity_types ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<ActivityTypeEntity>>

    @Query("SELECT * FROM activity_types ORDER BY sortOrder ASC")
    suspend fun getAll(): List<ActivityTypeEntity>

    @Query("SELECT * FROM activity_types WHERE id = :id")
    suspend fun get(id: String): ActivityTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ActivityTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ActivityTypeEntity>)

    @Query("DELETE FROM activity_types WHERE id = :id AND isDefault = 0")
    suspend fun deleteCustom(id: String)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startMillis ASC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startMillis ASC")
    suspend fun getAll(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun get(id: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE endMillis IS NULL LIMIT 1")
    suspend fun getOpen(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE endMillis IS NULL LIMIT 1")
    fun observeOpen(): Flow<SessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface DailyRecordDao {
    @Query("SELECT * FROM daily_records WHERE localDate = :date")
    suspend fun get(date: String): DailyRecordEntity?

    @Query("SELECT * FROM daily_records ORDER BY localDate DESC")
    fun observeAll(): Flow<List<DailyRecordEntity>>

    @Query("SELECT * FROM daily_records ORDER BY localDate DESC")
    suspend fun getAll(): List<DailyRecordEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: DailyRecordEntity): Long

    @Update
    suspend fun update(entity: DailyRecordEntity)
}

@Dao
interface AlignerSetDao {
    @Query("SELECT * FROM aligner_sets ORDER BY setNumber DESC")
    fun observeAll(): Flow<List<AlignerSetEntity>>

    @Query("SELECT * FROM aligner_sets ORDER BY setNumber DESC")
    suspend fun getAll(): List<AlignerSetEntity>

    @Query("SELECT * FROM aligner_sets WHERE endDate IS NULL LIMIT 1")
    suspend fun getCurrent(): AlignerSetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AlignerSetEntity)
}
