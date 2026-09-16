package app.worn

import android.app.Application
import androidx.room.Room
import app.worn.data.TrackingRepository
import app.worn.data.local.WornDatabase
import app.worn.notifications.ReplacementReminders
import app.worn.notifications.WornNotifications

class WornApp : Application() {
    lateinit var database: WornDatabase
        private set
    lateinit var repository: TrackingRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = Room.databaseBuilder(this, WornDatabase::class.java, "worn.db")
            .addMigrations(WornDatabase.MIGRATION_1_2, WornDatabase.MIGRATION_2_3)
            .build()
        repository = TrackingRepository(database)
        WornNotifications.ensureChannels(this)
        ReplacementReminders.ensureChannel(this)
    }

    companion object {
        lateinit var instance: WornApp
            private set
    }
}
